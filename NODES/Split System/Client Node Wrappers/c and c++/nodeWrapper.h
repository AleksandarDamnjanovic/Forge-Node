#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <string.h>
#include <arpa/inet.h>
#include <stdbool.h>

#ifndef NODEWRAPPER_H
#define NODEWRAPPER_H

typedef struct{
    int node_index;
    int numOfSwitches;
    int numOfSensors;
    int numOfTransmitters;

    char* serverIP;
    int port;
    int control_varible;
    bool started;

    pthread_mutex_t client_node_mutex;
    pthread_t client_node_thread;

    bool* client_node_switches;
    float* client_node_sensors;
    char** client_node_transmitters;
}NODE_CLIENT;

NODE_CLIENT* getNode(int index, int port, int controlVariable, char address[], int numOfSensors, int numOfSwitches, int numOfTransmitters);
void client_node_start(NODE_CLIENT *node);
void client_node_stop(NODE_CLIENT *node);
void client_node_talk(NODE_CLIENT *node);
void client_node_sendConfirmation();
void client_node_initializer(NODE_CLIENT *node);
int client_node_check_control(NODE_CLIENT *node);
char* client_node_generate_message(NODE_CLIENT *node);
void client_node_process_message(char* forProcessing, NODE_CLIENT *node);
void client_node_process_instruction(char* instruction, NODE_CLIENT *node);
void client_node_writeSensors(int index, float value, NODE_CLIENT *node);
char* client_node_read_transmitter_value(int index, NODE_CLIENT *node);
float client_node_readSensors(int index, NODE_CLIENT *node);
bool client_node_readSwitch(int index, NODE_CLIENT *node);
void client_node_freeNode(NODE_CLIENT* client);

typedef struct{
    int node_index;
    int elment_index;
    char type;
    int int_value;
    float float_value;
    char string_value[40];
}NODE_INSTRUCTION;

NODE_CLIENT* getNode(int index, int port, int controlVariable, char address[], int numOfSensors, int numOfSwitches, int numOfTransmitters){
    NODE_CLIENT* client;
    client= (NODE_CLIENT*)malloc(sizeof(NODE_CLIENT));
    client->node_index = index;
    client->port = port;
    client->control_varible = controlVariable;
    client->serverIP = (char*)malloc(sizeof(char) * 17);
    memset(client->serverIP, '\0', 17);
    strncpy(client->serverIP, address, strlen(address));

    client->numOfSwitches = numOfSwitches;
    client->numOfSensors = numOfSensors;
    client->numOfTransmitters = numOfTransmitters;

    return client;
}

void* client_node_communication(void* arg){
    NODE_CLIENT *node= (NODE_CLIENT*)arg;
    int c = client_node_check_control(node);

    while(c){
        printf("hey\n");
        client_node_talk(node);
        c = client_node_check_control(node);
        usleep(1000000);
    }

    return NULL;
}

void client_node_initializer(NODE_CLIENT *node){
    bool* switches = (bool*)malloc(sizeof(bool) * node->numOfSwitches);
    float* sensors = (float*)malloc(sizeof(float) * node->numOfSensors);
    char** transmitters = (char**)malloc(sizeof(char) * node->numOfTransmitters);

    for(int i = 0; i < node->numOfSwitches; i++)
        switches[i]= false;
    node->client_node_switches = switches;

    for(int i = 0; i < node->numOfTransmitters; i++){
        transmitters[i] = (char*)malloc(40);
        memset(transmitters[i],'\0', 40);
    }
    node->client_node_transmitters= transmitters;

    for(int i = 0; i < node->numOfSensors; i++)
        sensors[i]= 0.0f;
    node->client_node_sensors = sensors;
}

void client_node_talk(NODE_CLIENT *node){
    char* message = client_node_generate_message(node);

    int sock;
    struct sockaddr_in server;
    sock = socket(AF_INET, SOCK_STREAM, 0);
    
    if(sock < 0){
        perror("Socket error!");
        return;
    }

    memset(&server, '\0', sizeof(server));
    server.sin_family = AF_INET;
    server.sin_port = htons(node->port);

    if(inet_pton(AF_INET, node->serverIP, &server.sin_addr) <= 0){
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

    client_node_process_message(forProcessing, node);

    close(sock);
    free(message);
}

void client_node_process_instruction(char* instruction, NODE_CLIENT *node){
    int ln= sizeof(char) * strlen(instruction);
    char ff[ln+1];
    memset(ff,'\0', ln + 1);
    strncpy(ff, instruction, ln);
    char* part = strtok(ff, "_");

    NODE_INSTRUCTION inst;
    part= strtok(NULL, "_");
    inst.node_index = atoi(part);
    
    if(inst.node_index!=node->node_index)
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
        pthread_mutex_lock(&node->client_node_mutex);
        node->client_node_switches[inst.elment_index]= (*part == one)? true : false;
        pthread_mutex_unlock(&node->client_node_mutex);
    
    }else if(inst.type == t){
        pthread_mutex_lock(&node->client_node_mutex);
        memset(node->client_node_transmitters[inst.elment_index], '\0', 40);
        strcpy(node->client_node_transmitters[inst.elment_index], part);
        pthread_mutex_unlock(&node->client_node_mutex);
    }

}

void client_node_process_message(char* forProcessing, NODE_CLIENT *node){
    int ln= sizeof(char) * strlen(forProcessing);
    char ff[ln+1];
    memset(ff, '\0', ln+1);
    strncpy(ff, forProcessing, ln);
    
    char* save;
    char* part = strtok_r(ff, " ", &save);

    while(part!=NULL){
        client_node_process_instruction(part, node);
        part= strtok_r(NULL, " ", &save);
    }

    free(forProcessing);
}

char* client_node_generate_message(NODE_CLIENT *node){
    int ns = node->numOfSwitches;
    int nse = node->numOfSensors;
        
    char buffer[1024];
    memset(buffer, '\0', 1024);
    char part[100];

    for(int i = 0; i < ns; i++){
        memset(part, '\0', 100);
        pthread_mutex_lock(&node->client_node_mutex);
        sprintf(part, "Cr_%d_C_%d_%d ", node->node_index, i, node->client_node_switches[i]? 1 : 0);
        pthread_mutex_unlock(&node->client_node_mutex);
        if(strlen(buffer)==0)
            strcpy(buffer, part);
        else
            strcat(buffer, part);
    }
    
    for(int i = 0; i < nse; i++){
        memset(part, '\0', 100);
        pthread_mutex_lock(&node->client_node_mutex);
        sprintf(part, "Cr_%d_R_%d_%.2f ", node->node_index, i, node->client_node_sensors[i]);
        pthread_mutex_unlock(&node->client_node_mutex);
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

int client_node_check_control(NODE_CLIENT *node){
    int c= 0;
    pthread_mutex_lock(&node->client_node_mutex);
    c = node->control_varible;
    pthread_mutex_unlock(&node->client_node_mutex);
    return c;
}

void client_node_stop(NODE_CLIENT *node){
    if(!node->started)
        return;

    pthread_mutex_lock(&node->client_node_mutex);
    node->control_varible = 0;
    pthread_mutex_unlock(&node->client_node_mutex);
    pthread_join(node->client_node_thread, NULL);
    node->started = false;
}

void client_node_start(NODE_CLIENT *node){
    if(node->started && node->control_varible == 0)
        return;

    pthread_mutex_lock(&node->client_node_mutex);
    client_node_initializer(node);
    pthread_mutex_unlock(&node->client_node_mutex);
    
    node->control_varible = 1;
    pthread_create(&node->client_node_thread, NULL, client_node_communication, node);
    node->started = true;
}

void client_node_writeSensors(int index, float value, NODE_CLIENT *node){
    int ii = node->numOfSensors;

    if(index>= ii || index< 0){
        printf("Error writing sensors! Index is not in acceptable range!\n");
        return;
    }

    pthread_mutex_lock(&node->client_node_mutex);
    node->client_node_sensors[index]= value;
    pthread_mutex_unlock(&node->client_node_mutex);
}

float client_node_readSensors(int index, NODE_CLIENT *node){
    int ii = node->numOfSensors;

    if(index>= ii || index< 0){
        printf("Error reading sensors! Index is not in acceptable range!\n");
        return 0.0f;
    }

    float value= 0.0f;

    pthread_mutex_lock(&node->client_node_mutex);
    value = node->client_node_sensors[index];
    pthread_mutex_unlock(&node->client_node_mutex);

    return value;
}

char* client_node_read_transmitter_value(int index, NODE_CLIENT *node){
    int ii = node->numOfTransmitters;

    int ln= 0;
    char* val= NULL;

    if(index>= ii || index< 0){
        printf("Error reading transmitters! Index is not in acceptable range!\n");
        return val;
    }

    pthread_mutex_lock(&node->client_node_mutex);
    val = node->client_node_transmitters[index];
    pthread_mutex_unlock(&node->client_node_mutex);

    return val;
}

bool client_node_readSwitch(int index, NODE_CLIENT *node){
    int ii = node->numOfSwitches;

    if(index>= ii || index< 0){
        printf("Error reading switches! Index is not in acceptable range!\n");
        return false;
    }

    bool value= false;

    pthread_mutex_lock(&node->client_node_mutex);
    value = node->client_node_switches[index];
    pthread_mutex_unlock(&node->client_node_mutex);

    return value;
}

void client_node_freeNode(NODE_CLIENT* client){
    free(client->serverIP);
    
    free(client->client_node_switches);
    free(client->client_node_sensors);

    for(int i = 0; i < client->numOfTransmitters; i++)
        free(client->client_node_transmitters[i]);

    free(client);
}

#endif