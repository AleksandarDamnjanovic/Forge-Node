/*
*************** Name: KS Node
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 10.07.2026.
*************** Location: Kragujevac, Serbia
*/

#ifndef NODE_H
#define NODE_H

#include <Arduino.h>

#define NODE_WLAN_SSID           ""
#define NODE_WLAN_PASSWORD       ""

#define SERVER_PORT              10001

typedef struct{
    int index;
    int numOfSwitches;
    int numOfSensors;
    int numOfTransmitters;

    int* switchPins;
    bool* switches;
    float* sensors;
    char** transmitters;
}NODE;

NODE* getNode(int index, int numOfSwitches, int numOfSensors, int numOfTransmitters, int* switchPins,
                bool* switches, float* sensors, char** transmitters){

    NODE* client;
    client= (NODE*)malloc(sizeof(NODE));
    client->index = index;

    client->numOfSwitches = numOfSwitches;
    client->numOfSensors = numOfSensors;
    client->numOfTransmitters = numOfTransmitters;

    client->switchPins = switchPins;
    client->switches = switches;
    client->sensors = sensors;
    client->transmitters = transmitters;

    return client;
}

int getNodeIndex(String message){
    const char* mess1 = message.c_str();
    int ln = message.length();
    char mess[ln+1];
    memset(mess, '\0', ln + 1);
    strcpy(mess, mess1);

    char ind[4] = {'\0','\0','\0','\0'};

    char *token = strtok(mess, "_");
    token = strtok(NULL, "_");

    strcpy(ind, token);
    int index = atoi(ind);
    return index;
}

NODE* getNodeByIndex(NODE* n[], int nodeCount, int nodeIndex){
    for(int i = 0; i < nodeCount; i++)
        if(n[i]->index == nodeIndex)
            return n[i];

    return NULL;
}

void writeSwitch(NODE* node, int index, bool value){
    node->switches[index] = value;
}

void writeTransmitter(NODE* node, int index, char* value){
    memset(node->transmitters[index], '\0', 40);
    strcpy(node->transmitters[index], value);
}

bool readSwitch(NODE* node, int index){
    return node->switches[index];
}

float readSensor(NODE* node, int index){
    return node->sensors[index];
}

char* readTransmitter(NODE* node, int index){
    return node->transmitters[index];
}

#endif