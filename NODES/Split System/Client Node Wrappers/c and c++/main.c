#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include "clientNodeWrapper.h"

int main(int argv, char* argc){
    client_node_start();
    sleep(5);
    client_node_stop();
    return 0;
}