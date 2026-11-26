/*
*************** Name: KS Node
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 26.11.2026.
*************** Location: Kragujevac, Serbia
*/

#ifndef SLAVE_H
#define SLAVE_H

#include <Arduino.h>
#include <ESP8266WiFi.h>
#include "node.hpp"

/*
SLAVE connects to the BRIDGE's WPA2-encrypted Wi-Fi Access Point and exchanges confirmations
and instructions with BRIDGE over TCP.
*/
class SLAVE {
    public:
        SLAVE(NODE *node, String bridgeAddress, int bridgePORT);
        ~SLAVE() {}

        void setControl(bool value);
        bool getControl() const;
        bool startHandler();

        // Executes one communication exchange with BRIDGE (call periodically in Arduino loop())
        void handleStep();

        NODE *getNode();

        void writeSwitch(int index, bool value);
        void writeSensor(int index, float value);
        void writeTransmitter(int index, String value);
        bool getSwitchValue(int index);
        float getSensorValue(int index);
        String getTransmitterValue(int index);

    private:
        String getBridgeAddress() const;
        int getBridgePORT() const;

        String bridgeAddress;
        int bridgePORT;
        NODE *node;
        bool control;
};

#endif
