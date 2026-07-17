#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>

#ifndef CLIENTNODEWRAPPER_H
#define CLIENTNODEWRAPPER_H

#define serverIP            "192.168.10.1"
#define ssid                "somethingSpecialIndeed"
#define password            "thisIsSomeSpecialPasswordThatYouShouldChange"
int port =                  10001;

#define nodeIndex           1
#define numOfSwitches       2
#define numOfSensors        2
#define numOfTransmitters   2

void* communication(void* arg){

    while(1){
        printf("hey\n");
        usleep(1000000);
    }

    return NULL;
}



#endif