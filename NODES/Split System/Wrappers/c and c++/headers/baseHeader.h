#include <pthread.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <arpa/inet.h>

#ifndef BASE_HEADER_H
#define BASE_HEADER_H

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
}NODE;

typedef struct{
    int node_index;
    int elment_index;
    char type;
    int int_value;
    float float_value;
    char string_value[40];
}NODE_INSTRUCTION;

void node_initializer(NODE *node);
int node_check_control(NODE *node);
void writeSensor(int index, float value, NODE *node);
char* readTransmitter(int index, NODE *node);
float readSensor(int index, NODE *node);
bool readSwitch(int index, NODE *node);
void freeNode(NODE* client);

void node_initializer(NODE *node){
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

int node_check_control(NODE *node){
    int c= 0;
    pthread_mutex_lock(&node->client_node_mutex);
    c = node->control_varible;
    pthread_mutex_unlock(&node->client_node_mutex);
    return c;
}

void node_stop(NODE *node){
    if(!node->started)
        return;

    pthread_mutex_lock(&node->client_node_mutex);
    node->control_varible = 0;
    pthread_mutex_unlock(&node->client_node_mutex);
    pthread_join(node->client_node_thread, NULL);
    node->started = false;
}

void writeSensor(int index, float value, NODE *node){
    int ii = node->numOfSensors;

    if(index>= ii || index< 0){
        printf("Error writing sensors! Index is not in acceptable range!\n");
        return;
    }

    pthread_mutex_lock(&node->client_node_mutex);
    node->client_node_sensors[index]= value;
    pthread_mutex_unlock(&node->client_node_mutex);
}

float readSensor(int index, NODE *node){
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

char* readTransmitter(int index, NODE *node){
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

bool readSwitch(int index, NODE *node){
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

void freeNode(NODE* client){
    if(client->serverIP!= NULL)
        free(client->serverIP);
    
    if(client->client_node_switches!= NULL)
        free(client->client_node_switches);
    
    if(client->client_node_sensors!= NULL)
        free(client->client_node_sensors);

    for(int i = 0; i < client->numOfTransmitters; i++)
        if(client->client_node_transmitters[i]!= NULL){
            free(client->client_node_transmitters[i]);
            client->client_node_transmitters[i]= NULL;
        }

    client->serverIP = NULL;
    client->client_node_switches = NULL;
    client->client_node_sensors = NULL;

    free(client);
    client= NULL;
}

#endif