/*
*************** Name: KS Node
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 10.07.2026.
*************** Location: Kragujevac, Serbia
*/

#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include "node.hpp"
#include <IPAddress.h>

IPAddress server_address(192,168,10,1);
//IPAddress gateway(192,168,10,1);
//IPAddress subnet(255, 255, 255, 0);

WiFiClient cl;

NODE *node;
int switches[2]={D7, D8};
int numOfSwitches = 2;
boolean states[2]= {false, false};
String transmitters[2] = {"none", "none"};
float sensors[2]={0.0, 0.0}; 

void setup() {

    Serial.begin(9600);
    Serial.println();
    Serial.println("Connecting...");

    WiFi.begin(NODE_WLAN_SSID, NODE_WLAN_PASSWORD);
    while(WiFi.status() != WL_CONNECTED){
        delay(500);
        Serial.print("*");
    }
    Serial.println();
    Serial.print("Connected to wifi with address of ");
    Serial.println(WiFi.localIP());

    for(int n:switches){
        pinMode(n, OUTPUT);
        digitalWrite(n, LOW);
    }

    node = new NODE(NODE_MASTER, 
                    NODE_NO_SLAVE,
                    switches,
                    1,
                    states,
                    transmitters,
                    sensors,
                    numOfSwitches,
                    2,
                    2);
}

long t = 0;
void loop() {
    
    if(t==0)
        t= millis();

    if(!cl.connected())
        cl.connect(server_address, SERVER_PORT);

    if(cl.connected()){
        if(millis() - t > 1000){
            String message = node->getResponse();
            cl.println(message);
            String response = cl.readStringUntil('\n');
            node->processMessage(response);
            t= millis();
        }
    }
}