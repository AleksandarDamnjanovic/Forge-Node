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

    char full[80];
    memset(full, '\0', 80);
    int ln1 = (sizeof(nodeSwitches)/sizeof(nodeSwitches[0]));
    for(int i = 0; i < ln1; i++){
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

    return String(full);
    
}

#endif