#include "baseHeader.h"

#ifndef CONTROLNODEWRAPPER_H
#define CONTROLNODEWRAPPER_H

char* control_node_generate_message(NODE* node);
void control_node_process_message(char* forProcessing, NODE* node);
void control_node_process_message(char* forProcessing, NODE *node);
void control_node_process_instruction(char* instruction, NODE *node);
void writeSwitch(int index, bool value, NODE *node);
void writeTransmitter(int index, char* value, NODE *node);
void* control_communication(void* arg);
void control_node_start(NODE *node);

NODE* getControlNode(int index, int port, int controlVariable, int numOfSensors, int numOfSwitches, int numOfTransmitters){
    NODE* client;
    client= (NODE*)malloc(sizeof(NODE));
    client->node_index = index;
    client->port = port;
    client->control_varible = controlVariable;

    client->numOfSwitches = numOfSwitches;
    client->numOfSensors = numOfSensors;
    client->numOfTransmitters = numOfTransmitters;

    return client;
}

void control_node_talk(NODE *node){

    int server = -1;
    int client = -1;

    struct sockaddr_in server_address;
    struct sockaddr_in client_address;
    socklen_t client_len = sizeof(client_address);
    
    server = socket(AF_INET, SOCK_STREAM, 0);
    if(server < 0){
        perror("Socket error!");
        return;
    }

    int opt = 1;
    if (setsockopt(server, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0) {
        perror("setsockopt");
        close(server);
        return;
    }

    memset(&server_address, '\0', sizeof(server_address));
    server_address.sin_family = AF_INET;
    server_address.sin_addr.s_addr = htonl(INADDR_ANY);
    server_address.sin_port = htons(node->port);

    if (bind(server, (struct sockaddr *)&server_address, sizeof(server_address)) < 0) {
        perror("bind");
        close(server);
        return;
    }

    if (listen(server, 5) < 0) {
        perror("listen");
        close(server);
        return;
    }

    client = accept(server, (struct sockaddr *)&client_address, &client_len);
    if (client < 0) {
        perror("accept");
        close(server);
        return;
    }

    char buffer[1024];
    memset(buffer, '\0', 1024);

    int n = recv(client, buffer, sizeof(buffer) - 1, 0);

    int len= (strlen(buffer)*sizeof(char)) + 1;
    char* forProcessing= (char*)malloc(len);
    strcpy(forProcessing, buffer);

    control_node_process_message(forProcessing, node);
    char* message = control_node_generate_message(node);
    printf("message sent: %smessage received: %s\n", message, forProcessing);

    int ln = send(client, message, strlen(message) + 1, 0);
    if (ln <= 0){
        perror("send");
    }

    close(client);
    close(server);
    free(message);
}

char* control_node_generate_message(NODE* node){
    int ns = node->numOfSwitches;
    int nse = node->numOfTransmitters;
        
    char buffer[1024];
    memset(buffer, '\0', 1024);
    char part[100];

    for(int i = 0; i < ns; i++){
        memset(part, '\0', 100);
        pthread_mutex_lock(&node->client_node_mutex);
        sprintf(part, "I_%d_C_%d_%d ", node->node_index, i, node->client_node_switches[i]? 1 : 0);
        pthread_mutex_unlock(&node->client_node_mutex);
        if(strlen(buffer)==0)
            strcpy(buffer, part);
        else
            strcat(buffer, part);
    }
    
    for(int i = 0; i < nse; i++){
        memset(part, '\0', 100);
        pthread_mutex_lock(&node->client_node_mutex);
        sprintf(part, "I_%d_T_%d_%s ", node->node_index, i, node->client_node_transmitters[i]);
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

void control_node_process_message(char* forProcessing, NODE *node){
    int ln= sizeof(char) * strlen(forProcessing);
    char ff[ln+1];
    memset(ff, '\0', ln+1);
    strncpy(ff, forProcessing, ln);
    
    char* save;
    char* part = strtok_r(ff, " ", &save);

    while(part!=NULL){
        control_node_process_instruction(part, node);
        part= strtok_r(NULL, " ", &save);
    }

    free(forProcessing);
}

void control_node_process_instruction(char* instruction, NODE *node){
    if(instruction[0]=='\r'  || instruction==NULL)
        return;
    
    int ln= sizeof(char) * strlen(instruction);
    char ff[ln+1];
    memset(ff,'\0', ln + 1);
    strncpy(ff, instruction, ln);
    char* part = strtok(ff, "_");

    NODE_INSTRUCTION inst;
    part= strtok(NULL, "_");
    inst.node_index = atoi(part);
    
    if(inst.node_index != node->node_index)
        return;
    
    char r = 'R';

    part= strtok(NULL, "_");
    inst.type= *part;
    part= strtok(NULL, "_");
    inst.elment_index = atoi(part);
    part= strtok(NULL, "_");
    if(inst.type == r){
        pthread_mutex_lock(&node->client_node_mutex);
        node->client_node_sensors[inst.elment_index]= atof(part);
        pthread_mutex_unlock(&node->client_node_mutex);
    }

}

void writeSwitch(int index, bool value, NODE *node){
    int ii = node->numOfSwitches;

    if(index>= ii || index< 0){
        printf("Error writing sensors! Index is not in acceptable range!\n");
        return;
    }

    pthread_mutex_lock(&node->client_node_mutex);
    node->client_node_switches[index]= value?"1":"0";
    pthread_mutex_unlock(&node->client_node_mutex);
}

void writeTransmitter(int index, char* value, NODE *node){
    int ii = node->numOfSensors;

    if(index>= ii || index< 0){
        printf("Error writing sensors! Index is not in acceptable range!\n");
        return;
    }

    pthread_mutex_lock(&node->client_node_mutex);
    memset(node->client_node_transmitters[index],'\0', 40);
    strcpy(node->client_node_transmitters[index], value);
    pthread_mutex_unlock(&node->client_node_mutex);
}

void* control_communication(void* arg){
    NODE *node= (NODE*)arg;
    int c = node_check_control(node);

    while(c){
        control_node_talk(node);
        c = node_check_control(node);
        usleep(1000000);
    }

    return NULL;
}

void control_node_start(NODE *node){
    if(node->started && node->control_varible == 0)
        return;

    pthread_mutex_lock(&node->client_node_mutex);
    node_initializer(node);
    pthread_mutex_unlock(&node->client_node_mutex);
    
    node->control_varible = 1;
    pthread_create(&node->client_node_thread, NULL, control_communication, node);
    node->started = true;
}

#endif