#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include "nodeWrapper.h"

int main(int argv, char* argc){
    NODE_CLIENT* client= getNode(1, 10001, 0, "192.168.0.101", 2, 2, 2);

    client_node_start(client);
    client_node_writeSensors(0, 12.4, client);
    client_node_writeSensors(1, 21.05, client);
   
    sleep(2);
    char* t1= client_node_read_transmitter_value(0, client);
    char* t2 = client_node_read_transmitter_value(1, client);
    printf("---\nsensor 0: %.2f\nsensor: 1: %.2f\nswitch 0: %d\nswitch 1: %d\ntransmitter 0: %s\ntransmitter 1: %s\n----",
    client_node_readSensors(0, client), client_node_readSensors(1, client)
    ,client_node_readSwitch(0, client)?1:0,client_node_readSwitch(1, client)?1:0, t1, t2);
    client_node_stop(client);
    client_node_freeNode(client);
    return 0;
}