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
#include <WiFiServer.h>
#include "node.hpp"
#include "parser.hpp"
#include <IPAddress.h>

IPAddress server_address(192,168,10,1);
IPAddress gateway(192,168,10,1);
IPAddress subnet(255, 255, 255, 0);

WiFiClient client;
WiFiServer server(SERVER_PORT);

void setup() {

    Serial.begin(9600);
    Serial.println();
    Serial.println("Connecting...");

    WiFi.softAPConfig(server_address, gateway, subnet);
    WiFi.softAP(NODE_WLAN_SSID, NODE_WLAN_PASSWORD);
    server.begin();

    int len = sizeof(switches)/sizeof(switches[0]);
    for(int i = 0; i < len; i++)
        pinMode(switches[i], INPUT);

}

void loop() {

    client = server.accept();

    if(client){
        if(client.available()){
            Serial.println("client connected...");
            String message = client.readStringUntil('\n');
            Serial.println(message);
            String response;
            response = parseMessage(message);
            client.println(response);
            Serial.println(response);
        }
    }
    client.stop();
    delay(100);
}