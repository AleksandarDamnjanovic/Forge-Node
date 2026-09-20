#include "headers/clientNodeWrapper.h"
#include "headers/controlNodeWrapper.h"

void* clientNodeExample(void* arg);
void* controlNodeExample(void* arg);

int main(int argv, char* argc){
    pthread_t first;
    pthread_t second;

    //pthread_create(&first, NULL, controlNodeExample, NULL);
    //sleep(0.5f);
    pthread_create(&second, NULL, clientNodeExample, NULL);

    sleep(10);

    return 0;
}

void* controlNodeExample(void* arg){
    NODE* control= (NODE*)getControlNode(1, 10001, 0, 2, 2, 2);

    control_node_start(control);
    writeSwitch(0, true, control);
    writeSwitch(1, false, control);
    writeTransmitter(0, "hey", control);
    writeTransmitter(1, "ha", control);
    sleep(2);

    printf("---\nsensor 0: %.2f\nsensor: 1: %.2f", readSensor(0, control), readSensor(1, control));

    node_stop(control);
    freeNode(control); 
}

void* clientNodeExample(void* arg){
    NODE* client= (NODE*)getClientNode(1, 10001, 0, "192.168.0.101", 2, 2, 2);

    client_node_start(client);
    writeSensor(0, 12.4, client);
    writeSensor(1, 21.05, client);
   
    sleep(2);
    char* t1= (char*)readTransmitter(0, client);
    char* t2 = (char*)readTransmitter(1, client);
    printf("---\nsensor 0: %.2f\nsensor: 1: %.2f\nswitch 0: %d\nswitch 1: %d\ntransmitter 0: %s\ntransmitter 1: %s\n----",
    readSensor(0, client), readSensor(1, client)
    ,readSwitch(0, client)?1:0,readSwitch(1, client)?1:0, t1, t2);
    node_stop(client);
    freeNode(client);    
}