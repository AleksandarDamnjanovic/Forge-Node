#include "baseHeader.h"

#ifndef CLIENTNODEWRAPPER_H
#define CLIENTNODEWRAPPER_H

NODE* getClientNode(int index, int port, int controlVariable, char address[], int numOfSensors, int numOfSwitches, int numOfTransmitters);
void client_node_talk(NODE *node);
void client_node_sendConfirmation();
char* client_node_generate_message(NODE *node);
void client_node_process_message(char* forProcessing, NODE *node);
void client_node_process_instruction(char* instruction, NODE *node);
void* client_communication(void* arg);
void client_node_start(NODE *node);

NODE* getClientNode(int index, int port, int controlVariable, char address[], int numOfSensors, int numOfSwitches, int numOfTransmitters){
    NODE* client;
    client= (NODE*)malloc(sizeof(NODE));
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

void client_node_talk(NODE *node){
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

void client_node_process_instruction(char* instruction, NODE *node){
    if(instruction[0]=='\r' || instruction==NULL)
        return;

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
        if(part!=NULL){
            pthread_mutex_lock(&node->client_node_mutex);
            memset(node->client_node_transmitters[inst.elment_index], '\0', 40);
            strcpy(node->client_node_transmitters[inst.elment_index], part);
            pthread_mutex_unlock(&node->client_node_mutex);
        }
    }

}

void client_node_process_message(char* forProcessing, NODE *node){
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

char* client_node_generate_message(NODE *node){
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

void* client_communication(void* arg){
    NODE *node= (NODE*)arg;
    int c = node_check_control(node);

    while(c){
        client_node_talk(node);
        c = node_check_control(node);
        usleep(1000000);
    }

    return NULL;
}

void client_node_start(NODE *node){
    if(node->started && node->control_varible == 0)
        return;

    pthread_mutex_lock(&node->client_node_mutex);
    node_initializer(node);
    pthread_mutex_unlock(&node->client_node_mutex);
    
    node->control_varible = 1;
    pthread_create(&node->client_node_thread, NULL, client_communication, node);
    node->started = true;
}

#endif