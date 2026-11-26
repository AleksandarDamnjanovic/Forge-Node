/*
*************** Name: KS Node
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 26.11.2024.
*************** Location: Kragujevac, Serbia
*/

#include "node.hpp"
#include <Arduino.h>
#include <string.h>

NODE::NODE(int type, 
            int slaveTo, 
            int *switches,
            int nodeIndex, 
            boolean *switchStates, 
            String *transmitters, 
            float *sensors,
            int numOfSwitches,
            int numOfSensors,
            int numOfTransmitters){
    NODE::type = type;
    NODE::slaveTo = slaveTo;
    NODE::switches = switches;
    NODE::switchStates = switchStates;
    NODE::transmitters = transmitters;
    NODE::sensors = sensors;
    NODE::nodeIndex = nodeIndex;
    NODE::numOfSwitches = numOfSwitches;
    NODE::numOfSensors = numOfSensors;
    NODE::numOfTransmitters = numOfTransmitters;
}

String NODE::getResponse(){
    String response = "";
    for(int i = 0; i < NODE::numOfSwitches; i++){
        char part[32];
        memset(part, '\0', sizeof(part));
        snprintf(part, sizeof(part), "Cr_%d_C_%d_%d ", getNodeIndex(), i, NODE::switchStates[i]?1:0);
        response = response + String(part);
    }
    for(int i = 0; i < NODE::numOfSensors; i++){
        char part[32];
        memset(part, '\0', sizeof(part));
        snprintf(part, sizeof(part), "Cr_%d_R_%d_%0.1f ", getNodeIndex(), i, NODE::sensors[i]);
        response = response + String(part);
    }
    return response;
}

String NODE::compileMessage(){
    return getResponse();
}

int NODE::getSwitchByIndex(int index){
    if(index >= 0 && index < NODE::numOfSwitches)
        return NODE::switches[index];
    return -1;
}

boolean NODE::getSwitchStateByIndex(int index){
    if(index >= 0 && index < NODE::numOfSwitches)
        return NODE::switchStates[index];
    return false;
}

String NODE::getTransmitterByIndex(int index){
    if(index >= 0 && index < NODE::numOfTransmitters)
        return NODE::transmitters[index];
    return "";
}

float NODE::getSensorByIndex(int index){
    if(index >= 0 && index < NODE::numOfSensors)
        return NODE::sensors[index];
    return 0.0f;
}

void NODE::setSwitchState(int index, int value){
    if(index >= 0 && index < NODE::numOfSwitches)
        NODE::switchStates[index] = (value == 1);
}

void NODE::setTransmitterValue(int index, String value){
    if(index >= 0 && index < NODE::numOfTransmitters)
        NODE::transmitters[index] = value;
}

void NODE::setSensorValue(int index, float value){
    if(index >= 0 && index < NODE::numOfSensors)
        NODE::sensors[index] = value;
}

int *NODE::getSwitches(){
    return NODE::switches;
}

boolean *NODE::getSwitchStates(){
    return NODE::switchStates;
}

String *NODE::getTransmitters(){
    return NODE::transmitters;
}

float *NODE::getSensors(){
    return NODE::sensors;
}

int NODE::getNumOfSwitches(){
    return NODE::numOfSwitches;
}

int NODE::getNumOfSensors(){
    return NODE::numOfSensors;
}

int NODE::getNumOfTransmitters(){
    return NODE::numOfTransmitters;
}

void NODE::sensorRead(int index){
    processSensors();
}

boolean NODE::validate(){
    for(int i = 0; i < NODE::numOfSwitches; i++)
        digitalWrite(NODE::switches[i], NODE::switchStates[i]?HIGH:LOW);

    //all that is necessary to process transmitters should be done in here

    return true;
}

boolean NODE::processSensors(){
    // all that is necessary to read sensors should be done in here
    return true;
}

boolean NODE::processSwitches(int index, char* value){
    int val = atoi(value);
    bool v = val==1?true:false;
    for(int i=0; i < NODE::numOfSwitches; i++)
        if(i==index)
            if(NODE::switchStates[i]!=v)
                NODE::switchStates[i] = v;
    return true;
}

boolean NODE::processTransmitters(int index, char* value){
    for(int i = 0; i < NODE::numOfTransmitters; i++)
        if(i == index)
            NODE::transmitters[i] = String(value);
    return true;
}

boolean NODE::processInstruction(char* instruction){
    if(instruction == NULL || instruction[0] == '\0')
        return false;

    char inst[8];
    char ind[8];
    int index;
    char type[4];
    char elementIndex[8];
    int element;
    char value[32];
    memset(inst, '\0', sizeof(inst));
    memset(ind, '\0', sizeof(ind));
    memset(type, '\0', sizeof(type));
    memset(elementIndex, '\0', sizeof(elementIndex));
    memset(value, '\0', sizeof(value));

    char *saveptr = NULL;
    char *token = strtok_r(instruction, "_", &saveptr);
    if(token == NULL)
        return false;
    strncpy(inst, token, sizeof(inst) - 1);

    token = strtok_r(NULL, "_", &saveptr);
    if(token == NULL)
        return false;
    strncpy(ind, token, sizeof(ind) - 1);
    index = atoi(ind);

    if(index != getNodeIndex())
        return false; 

    token = strtok_r(NULL, "_", &saveptr);
    if(token == NULL)
        return false;
    strncpy(type, token, sizeof(type) - 1);   

    token = strtok_r(NULL, "_", &saveptr);
    if(token == NULL)
        return false;
    strncpy(elementIndex, token, sizeof(elementIndex) - 1);

    element = atoi(elementIndex);

    if(type[0]=='S')
        return true;

    token = strtok_r(NULL, "_", &saveptr);
    if(token == NULL)
        return false;
    strncpy(value, token, sizeof(value) - 1);

    if(type[0]=='C')
        processSwitches(element, value);
    else if(type[0]=='T')
        processTransmitters(element, value);

    return true;
}

void NODE::processMessage(String message){
    int len = message.length() + 1;
    if(len <= 1)
        return;
    char first[len];
    char second[len];
    memset(first, '\0', len);
    memset(second, '\0', len);
    strcpy(first, message.c_str());
    strcpy(second, message.c_str());

    int counter = 0;
    char *saveptr1 = NULL;
    char *token= strtok_r(first, " ", &saveptr1);
    
    while(token!=NULL){
        counter +=1;
        token = strtok_r(NULL, " ", &saveptr1);
    }

    if(counter==0)
        return;

    char instructions[counter][48];
    for(int i = 0; i < counter; i++)
        memset(instructions[i], '\0', 48);

    char *saveptr2 = NULL;
    token= strtok_r(second, " ", &saveptr2);

    int count = 0;
    while(token!=NULL && count < counter){
        strncpy(instructions[count], token, 47);
        token = strtok_r(NULL, " ", &saveptr2);
        count += 1;
    }

    for(int i = 0; i < count; i++)
        processInstruction(instructions[i]);

    processSensors();
    validate();
}

int NODE::getType(){
    return NODE::type;
}

int NODE::getSlaveTo(){
    return NODE::slaveTo;
}

int NODE::getNodeIndex(){
    return NODE::nodeIndex;
}