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

#define NODE_WLAN_SSID           ""
#define NODE_WLAN_PASSWORD       ""

#define SERVER_PORT              10001

int switches[2]=                        {D7, D8};

int nodes[4] =                          {1, 1};
int numNodeSwitches =                   2;
int nodeSwitches[2] =                   {0, 1};

int ns =                                2;
int nodeSensors[2] =                    {1, 1};
int sensorIndex[2] =                    {0, 1};
float nodeSensorsValues[2] =            {0.0, 0.0};

int numNodeTransmitters =               2;
int transmitters[2] =                   {1, 1};
int transmitterIndex[2] =               {0, 1};
String transmittersValues[2] =      {"1", "2"};

#endif