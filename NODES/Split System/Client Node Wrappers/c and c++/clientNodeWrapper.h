#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <string.h>
#include <arpa/inet.h>
#include <stdbool.h>

#ifndef CLIENTNODEWRAPPER_H
#define CLIENTNODEWRAPPER_H

#define client_node_serverIP            "192.168.10.1"
#define client_node_ssid                "somethingSpecialIndeed"
#define client_node_password            "thisIsSomeSpecialPasswordThatYouShouldChange"
int client_node_port =                  10001;
int client_node_control_varible =       1;
bool client_node_started =              false;

#define client_node_nodeIndex           1
#define client_node_numOfSwitches       2
#define client_node_numOfSensors        2
#define client_node_numOfTransmitters   2

pthread_mutex_t client_node_mutex = PTHREAD_MUTEX_INITIALIZER;
pthread_t client_node_thread;

bool client_node_switches[client_node_numOfSwitches];
float client_node_sensors[client_node_numOfSensors];
char* client_node_transmitters[client_node_numOfTransmitters];

void client_node_start();
void client_node_stop();
void client_node_talk();
void client_node_sendConfirmation();
void client_node_initializer();
int client_node_check_control();
char* client_node_generate_message();

void* client_node_communication(void* arg){
    int c = client_node_check_control();
    pthread_mutex_lock(&client_node_mutex);
    client_node_initializer();
    pthread_mutex_unlock(&client_node_mutex);

    while(c){
        printf("hey\n");
        client_node_talk();
        c = client_node_check_control();
        usleep(1000000);
    }

    return NULL;
}

void client_node_initializer(){
    for(int i = 0; i < client_node_numOfSwitches; i++)
        client_node_switches[i]= false;
    for(int i = 0; i < client_node_numOfTransmitters; i++){
        client_node_transmitters[i]= (char*)malloc(40);
        memset(client_node_transmitters[i],'\0', 40);
    }
    for(int i = 0; i < client_node_numOfSensors; i++)
        client_node_sensors[i]= 0.0f;
}

void client_node_talk(){
    char* message = client_node_generate_message();

    int sock;
    struct sockaddr_in server;
    sock = socket(AF_INET, SOCK_STREAM, 0);
    
    if(sock < 0){
        perror("Socket error!");
        return;
    }

    memset(&server, '\0', sizeof(server));
    server.sin_family = AF_INET;
    server.sin_port = htons(client_node_port);

    if(inet_pton(AF_INET, client_node_serverIP, &server.sin_addr) <= 0){
        perror("inet_pton function problem!");
        close(sock);
        return;
    }

    if(connect(sock, (struct sockaddr*)&server, sizeof(server)<=0)){
        perror("connect error!");
        close(sock);
        return;
    }

    if (send(sock, message, strlen(message), 0) < 0){
        perror("send");
    }

    char buffer[1024];

    int n = recv(sock, buffer, sizeof(buffer) - 1, 0);

    if (n > 0){
        buffer[n] = '\0';
        printf("Server: %s\n", buffer);
    }

    close(sock);
    free(message);
}

char* client_node_generate_message(){


//pre no se testira, mora da se odradi implementacija ovog koda




}

int client_node_check_control(){
    int c= 0;
    pthread_mutex_lock(&client_node_mutex);
    c = client_node_control_varible;
    pthread_mutex_unlock(&client_node_mutex);
    return c;
}

void client_node_stop(){
    if(!client_node_started)
        return;

    pthread_mutex_lock(&client_node_mutex);
    client_node_control_varible = 0;
    pthread_mutex_unlock(&client_node_mutex);
    pthread_join(client_node_thread, NULL);
    client_node_started = false;
}

void client_node_start(){    
    if(client_node_started)
        return;
    
    client_node_control_varible = 1;
    
    pthread_create(&client_node_thread, NULL, client_node_communication, NULL);
    
    client_node_started = true;
}


#endif