/*
*************** Name: KS Node
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 10.07.2026.
*************** Location: Kragujevac, Serbia
*/

#ifndef NODE_H
#define NODE_H

#include <Arduino.h>

#define NODE_WLAN_SSID           "somethingSpecialIndeed"
#define NODE_WLAN_PASSWORD       "thisIsSomeSpecialPasswordThatYouShouldChange"

#define SERVER_PORT              10001

int switches[2]={D7, D8};
String transmitters[2] = {"none", "none"};
float sensors[2]={0.0, 0.0}; 

int nodes[4] =                  {1, 1, 1, 1};
int nodeSwitches[2] =           {0, 1};
int nodeSensors[2] =            {1, 1};
int sensorIndex[2] =            {0, 1};
int ns =                        2;
float nodeSensorsValues[2] =          {0.0, 0.0};
String nodeTransmitters[2] =    {"1", "2"};

#endif