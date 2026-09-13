/*
*************** Name: Forge Node
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 10.07.2026.
*************** Location: Kragujevac, Serbia
*/

#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <WiFiServer.h>
#include "node.hpp"
#include "parser.hpp"
#include <IPAddress.h>

WiFiServer server(SERVER_PORT);

const int nodeCount = 1;
NODE* nodovi[nodeCount];   
WiFiClient clients[nodeCount];

void declareNodes();

void setup() {

    Serial.begin(9600);
    Serial.println();
    Serial.println("Connecting...");

    WiFi.mode(WIFI_STA);

    WiFi.begin(NODE_WLAN_SSID, NODE_WLAN_PASSWORD);

    while (WiFi.status() != WL_CONNECTED){
        delay(500);
        Serial.print(".");
    }

    Serial.println();
    Serial.print("IP address: ");
    Serial.println(WiFi.localIP());

    server.begin();
    declareNodes();

    for(int i=0; i < nodeCount; i++)
        for(int a=0; a < nodovi[i]->numOfSwitches; i++)
            pinMode(nodovi[i]->switchPins[a], INPUT);

}

void loop() {
    WiFiClient novo = server.accept();
    if (novo) {
        for (int i = 0; i < nodeCount; i++) {
            if (!clients[i] || !clients[i].connected()) {
              clients[i] = novo;
              break;
            }
        }
    }

    for (int i = 0; i < nodeCount; i++) {
        if (!clients[i]) continue;

        if (!clients[i].connected()) {
            clients[i].stop();
            continue;
        }

        if (clients[i].available()) {
          Serial.println("client connected...");
            String message = clients[i].readStringUntil('\0');
            NODE* node = getNodeByIndex(nodovi, nodeCount, getNodeIndex(message));
            writeTransmitter(node, 0, "hey");
            writeTransmitter(node, 1, "ho");
            Serial.println(message);
            String response;
            response = parseMessage(node, message);
            Serial.printf("sensor 1: %.2f\tsensor 2: %.2f", readSensor(node, 0), readSensor(node, 1));
            clients[i].println(response);
            Serial.println(response);
        }
    }

    delay(100);
}

void declareNodes(){
    bool* switches = (bool*)malloc(sizeof(bool) * 2);
    float* sensors = (float*)malloc(sizeof(float) * 2);
    char** transmitters = (char**)malloc(sizeof(char) * 2);

    int* pins = (int*)malloc(sizeof(int) * 2);
    pins[0] = D7;
    pins[1] = D8;

    for(int i = 0; i < 2; i++)
        switches[i]= false;

    for(int i = 0; i < 2; i++){
        transmitters[i] = (char*)malloc(40);
        memset(transmitters[i],'\0', 40);
    }

    for(int i = 0; i < 2; i++)
        sensors[i]= 0.0f;

    NODE* node1= getNode(1, 2,2,2, pins, switches, sensors, transmitters);
    nodovi[0] = node1;
}