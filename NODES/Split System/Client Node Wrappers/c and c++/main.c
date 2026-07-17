#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include "clientNodeWrapper.h"

int main(int argv, char* argc){
    pthread_t thread;
    pthread_create(&thread, NULL, communication, NULL);
    pthread_join(thread, NULL);

    return 0;
}