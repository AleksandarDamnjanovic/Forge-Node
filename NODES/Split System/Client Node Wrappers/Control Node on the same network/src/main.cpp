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

WiFiClient client;
WiFiServer server(SERVER_PORT);

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

    int len = sizeof(switches)/sizeof(switches[0]);
    for(int i = 0; i < len; i++)
        pinMode(switches[i], INPUT);

}

void loop() {

    client = server.accept();

    if(client){
        if(client.available()){
            Serial.println("client connected...");
            String message = client.readStringUntil('\0');
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