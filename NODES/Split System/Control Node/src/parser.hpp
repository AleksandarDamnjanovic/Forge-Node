/*
*************** Name: KS Node
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 10.07.2026.
*************** Location: Kragujevac, Serbia
*/

#ifndef PARSER_H
#define PARSER_H

#include <Arduino.h>
#include "node.hpp"

typedef struct sensorElements {
    char element[20];
    struct sensorElements *next;
} sensorElements;

void processSensors(String message, int index){
    const char* mess1 = message.c_str();
    int ln = message.length();
    char mess[ln+1];
    memset(mess, '\0', ln + 1);
    strcpy(mess, mess1);

    char *token = strtok(mess, " ");
    char pattern[10];
    memset(pattern, '\0', 10);
    sprintf(pattern, "Cr_%d_R_", index);
    ln= strlen(pattern);

    sensorElements first;
    first.next = NULL;
    memset(first.element, '\0', 20);
    sensorElements *pnt= NULL;

    while(token!=NULL){
        char element[20];
        memset(element, '\0', 20);
        strcpy(element, token);
        if (strncmp(element, pattern, ln) == 0) {

            if(pnt==NULL){
                strcpy(first.element, element);
                pnt = &first;
            }else{
                sensorElements *nextOne = (sensorElements*)malloc(sizeof(sensorElements));
                memset(nextOne->element,'\0', 20);
                strcpy(nextOne->element, element);
                nextOne->next = NULL;
                pnt->next = nextOne;
                pnt = nextOne;
            }
        }

        token = strtok(NULL, " ");        
    }

    sensorElements *test= &first;
    while(test!=NULL){
        memmove(test->element, test->element + ln, strlen(test->element + ln) + 1);
        char* tk = strtok(test->element, "_");
        int inn = atoi(tk);
        tk = strtok(NULL, "_");
        float value = atof(tk);
        for(int i = 0; i < ns; i++){
            if(nodeSensors[i]==index)
                if(sensorIndex[i]==inn)
                    nodeSensorsValues[i]= value;
        }

        test = test->next;
    }

    test= &first;
    sensorElements *temp= NULL;
    bool initial = true;
    while(test!=NULL){
        temp = test->next;
        if(!initial)
            free(test);
        initial = false;
        test= NULL;
        test= temp;
    }

}

String parseMessage(String message){
    const char* mess1 = message.c_str();
    int ln = message.length();
    char mess[ln+1];
    memset(mess, '\0', ln + 1);
    strcpy(mess, mess1);

    char ind[4] = {'\0','\0','\0','\0'};
    char key[2] = {'\0','\0'};

    char *token = strtok(mess, "_");
    token = strtok(NULL, "_");

    strcpy(ind, token);
    int index = atoi(ind);

    token = strtok(NULL, "_");
    strcpy(key, token);

    processSensors(message, index);

    char full[160];
    memset(full, '\0', 169);
    for(int i = 0; i < numNodeSwitches; i++){
        char part[40];
        memset(part, '\0', 40);
        if(nodes[i]==index)
            sprintf(part, "I_%d_C_%d_%d ", index, 
                nodeSwitches[i], 
                digitalRead(switches[i])==HIGH?1:0);
        if(full[0]=='\0')
            strcpy(full, part);
        else
            strcat(full, part);
    }

    for(int i = 0; i < numNodeTransmitters; i++){
        char part[40];
        memset(part, '\0', 40);
        if(transmitters[i]==index)
            sprintf(part, "I_%d_T_%d_%s ", index, 
                transmitterIndex[i], 
                transmittersValues[i].c_str());
        if(full[0]=='\0')
            strcpy(full, part);
        else
            strcat(full, part);
    }

    return String(full);
    
}

#endif