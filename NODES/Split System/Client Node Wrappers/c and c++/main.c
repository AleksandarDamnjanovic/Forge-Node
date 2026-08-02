#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include "clientNodeWrapper.h"

int main(int argv, char* argc){
    client_node_start();
    client_node_writeSensors(0, 12.4);
    client_node_writeSensors(1, 21.05);
   
    sleep(2);
    char* t1= client_node_read_transmitter_value(0);
    char* t2 = client_node_read_transmitter_value(1);
    printf("---\nsensor 0: %.2f\nsensor: 1: %.2f\nswitch 0: %d\nswitch 1: %d\ntransmitter 0: %s\ntransmitter 1: %s\n----",
    client_node_readSensors(0), client_node_readSensors(1),client_node_readSwitch(0)?1:0,client_node_readSwitch(1)?1:0,
    t1, t2);
    client_node_stop();
    return 0;
}