#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <string.h>
#include <arpa/inet.h>
#include <stdbool.h>

#ifndef CLIENTNODEWRAPPER_H
#define CLIENTNODEWRAPPER_H

#define client_node_serverIP            "192.168.0.101"
#define client_node_port                10001
int client_node_control_varible =       1;
bool client_node_started =              false;

#define client_node_nodeIndex           1
#define client_node_numOfSwitches       2
#define client_node_numOfSensors        2
#define client_node_numOfTransmitters   2

pthread_mutex_t client_node_mutex = PTHREAD_MUTEX_INITIALIZER;
pthread_t client_node_thread;

bool client_node_switches[client_node_numOfSwitches];
float client_node_sensors[client_node_numOfSensors];
char* client_node_transmitters[client_node_numOfTransmitters];

void client_node_start();
void client_node_stop();
void client_node_talk();
void client_node_sendConfirmation();
void client_node_initializer();
int client_node_check_control();
char* client_node_generate_message();
void client_node_process_message(char* forProcessing);
void client_node_process_instruction(char* instruction);
void client_node_writeSensors(int index, float value);
char* client_node_read_transmitter_value(int index);
float client_node_readSensors(int index);
bool client_node_readSwitch(int index);

typedef struct{
    int node_index;
    int elment_index;
    char type;
    int int_value;
    float float_value;
    char string_value[40];
}NODE_INSTRUCTION;

void* client_node_communication(void* arg){
    int c = client_node_check_control();

    while(c){
        printf("hey\n");
        client_node_talk();
        c = client_node_check_control();
        usleep(1000000);
    }

    return NULL;
}

void client_node_initializer(){
    for(int i = 0; i < client_node_numOfSwitches; i++)
        client_node_switches[i]= false;
    for(int i = 0; i < client_node_numOfTransmitters; i++){
        client_node_transmitters[i]= (char*)malloc(40);
        memset(client_node_transmitters[i],'\0', 40);
    }
    for(int i = 0; i < client_node_numOfSensors; i++)
        client_node_sensors[i]= 0.0f;
}

void client_node_talk(){
    char* message = client_node_generate_message();

    int sock;
    struct sockaddr_in server;
    sock = socket(AF_INET, SOCK_STREAM, 0);
    
    if(sock < 0){
        perror("Socket error!");
        return;
    }

    memset(&server, '\0', sizeof(server));
    server.sin_family = AF_INET;
    server.sin_port = htons(client_node_port);

    if(inet_pton(AF_INET, client_node_serverIP, &server.sin_addr) <= 0){
        perror("inet_pton function problem!");
        close(sock);
        return;
    }

    if(connect(sock, (struct sockaddr*)&server, sizeof(server))<0){
        perror("connect error!");
        close(sock);
        return;
    }

    int ln = send(sock, message, strlen(message) + 1, 0);
    if (ln <= 0){
        perror("send");
    }

    char buffer[1024];
    memset(buffer, '\0', 1024);

    int n = recv(sock, buffer, sizeof(buffer) - 1, 0);

    int len= (strlen(buffer)*sizeof(char)) + 1;
    char* forProcessing= (char*)malloc(len);
    strcpy(forProcessing, buffer);

    printf("message sent: %smessage received: %s\n", message, forProcessing);

    client_node_process_message(forProcessing);

    close(sock);
    free(message);
}

void client_node_process_instruction(char* instruction){
    int ln= sizeof(char) * strlen(instruction);
    char ff[ln+1];
    memset(ff,'\0', ln + 1);
    strncpy(ff, instruction, ln);
    char* part = strtok(ff, "_");

    NODE_INSTRUCTION inst;
    part= strtok(NULL, "_");
    inst.node_index = atoi(part);
    
    if(inst.node_index!=client_node_nodeIndex)
        return;
    
    char c = 'C';
    char t = 'T';
    char one = '1';

    part= strtok(NULL, "_");
    inst.type= *part;
    part= strtok(NULL, "_");
    inst.elment_index = atoi(part);
    part= strtok(NULL, "_");
    if(inst.type == c){
        pthread_mutex_lock(&client_node_mutex);
        client_node_switches[inst.elment_index]= (*part == one)? true : false;
        pthread_mutex_unlock(&client_node_mutex);
    
    }else if(inst.type == t){
        pthread_mutex_lock(&client_node_mutex);
        strcpy(client_node_transmitters[inst.elment_index], part);
        pthread_mutex_unlock(&client_node_mutex);
    }

}

void client_node_process_message(char* forProcessing){
    int ln= sizeof(char) * strlen(forProcessing);
    char ff[ln+1];
    memset(ff, '\0', ln+1);
    strncpy(ff, forProcessing, ln);
    
    char* save;
    char* part = strtok_r(ff, " ", &save);

    while(part!=NULL){
        client_node_process_instruction(part);
        part= strtok_r(NULL, " ", &save);
    }

    free(forProcessing);
}

char* client_node_generate_message(){
    int ns = client_node_numOfSwitches;
    int nse = client_node_numOfSensors;
        
    char buffer[1024];
    memset(buffer, '\0', 1024);
    char part[100];

    for(int i = 0; i < ns; i++){
        memset(part, '\0', 100);
        pthread_mutex_lock(&client_node_mutex);
        sprintf(part, "Cr_%d_C_%d_%d ", client_node_nodeIndex, i, client_node_switches[i]? 1 : 0);
        pthread_mutex_unlock(&client_node_mutex);
        if(strlen(buffer)==0)
            strcpy(buffer, part);
        else
            strcat(buffer, part);
    }
    
    for(int i = 0; i < nse; i++){
        memset(part, '\0', 100);
        pthread_mutex_lock(&client_node_mutex);
        sprintf(part, "Cr_%d_R_%d_%.2f ", client_node_nodeIndex, i, client_node_sensors[i]);
        pthread_mutex_unlock(&client_node_mutex);
        if(strlen(buffer)==0)
            strcpy(buffer, part);
        else
            strcat(buffer, part);
    }

    char end[] = "\r\n\0";
    strcat(buffer, end);
    int len= (strlen(buffer) * sizeof(char))+1;
    char* toReturn = (char*)malloc(len);
    memset(toReturn, '\0', len);
    strcpy(toReturn, buffer);

    return toReturn;
}

int client_node_check_control(){
    int c= 0;
    pthread_mutex_lock(&client_node_mutex);
    c = client_node_control_varible;
    pthread_mutex_unlock(&client_node_mutex);
    return c;
}

void client_node_stop(){
    if(!client_node_started)
        return;

    pthread_mutex_lock(&client_node_mutex);
    client_node_control_varible = 0;
    pthread_mutex_unlock(&client_node_mutex);
    pthread_join(client_node_thread, NULL);
    client_node_started = false;
}

void client_node_start(){
    if(client_node_started && client_node_control_varible == 0)
        return;

    pthread_mutex_lock(&client_node_mutex);
    client_node_initializer();
    pthread_mutex_unlock(&client_node_mutex);
    
    client_node_control_varible = 1;
    pthread_create(&client_node_thread, NULL, client_node_communication, NULL);
    client_node_started = true;
}

void client_node_writeSensors(int index, float value){
    int ii = client_node_numOfSensors;

    if(index>= ii || index< 0){
        printf("Error writing sensors! Index is not in acceptable range!\n");
        return;
    }

    pthread_mutex_lock(&client_node_mutex);
    client_node_sensors[index]= value;
    pthread_mutex_unlock(&client_node_mutex);
}

float client_node_readSensors(int index){
    int ii = client_node_numOfSensors;

    if(index>= ii || index< 0){
        printf("Error reading sensors! Index is not in acceptable range!\n");
        return 0.0f;
    }

    float value= 0.0f;

    pthread_mutex_lock(&client_node_mutex);
    value = client_node_sensors[index];
    pthread_mutex_unlock(&client_node_mutex);

    return value;
}

char* client_node_read_transmitter_value(int index){
    int ii = client_node_numOfTransmitters;

    int ln= 0;
    char* val= NULL;

    if(index>= ii || index< 0){
        printf("Error reading transmitters! Index is not in acceptable range!\n");
        return val;
    }

    pthread_mutex_lock(&client_node_mutex);
    val = client_node_transmitters[index];
    pthread_mutex_unlock(&client_node_mutex);

    return val;
}

bool client_node_readSwitch(int index){
    int ii = client_node_numOfSwitches;

    if(index>= ii || index< 0){
        printf("Error reading switches! Index is not in acceptable range!\n");
        return false;
    }

    bool value= false;

    pthread_mutex_lock(&client_node_mutex);
    value = client_node_switches[index];
    pthread_mutex_unlock(&client_node_mutex);

    return value;
}

#endif