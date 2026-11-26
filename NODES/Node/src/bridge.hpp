/*
*************** Name: KS Node
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 26.11.2026.
*************** Location: Kragujevac, Serbia
*/

#ifndef BRIDGE_H
#define BRIDGE_H

#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <vector>
#include "node.hpp"
#include "node_instruction.hpp"
#include "crypto.hpp"

/*
Bridge receives AES-encrypted messages from SHUNT, executes instructions for its own node index,
and distributes downstream instructions to SLAVE nodes over its WPA2-encrypted Wi-Fi AP (plain TCP).
Confirmations from SLAVE nodes are collected, merged with BRIDGE's own confirmations, encrypted with AES,
and returned to SHUNT.
*/
class BRIDGE {
    public:
        class bridgeNODE : public NODE {
            public:
                bridgeNODE(NODE *baseNode, BRIDGE *owner);
                virtual ~bridgeNODE() {}

                virtual String getResponse() override;
                virtual void processMessage(String rawMessage) override;
                void processSlaveConfirmations(String message);

            private:
                BRIDGE *parentBridge;
        };

        BRIDGE(NODE *baseNode, String shuntIP, int shuntPORT, int bridgePORT, String aesSecretKey = NODE_AES_SECRET_KEY);
        ~BRIDGE();

        void setControl(bool value);
        bool getControl() const;

        void setAesKey(String secretKey);
        String getAesKey() const;

        // Optional helper to start WPA2-PSK Wi-Fi Access Point on the BRIDGE so SLAVE nodes enjoy hardware radio encryption
        bool startAccessPoint(const char *apSsid, const char *apPassword, int channel = 1, bool hidden = false, int maxConnections = 8);

        void initializeShuntHandler();
        void initializeSlaveHandler();

        // Non-blocking step functions to be called from Arduino loop()
        void handleShuntStep();
        void handleSlaveStep();

        // Medium-agnostic helpers (useful when SHUNT <-> BRIDGE uses LoRa/UART instead of TCP)
        String buildEncryptedPayloadForShunt();
        void processEncryptedPayloadFromShunt(String encryptedPayload);

        std::vector<NODE_INSTRUCTION> getForShunt() const;
        void clearForShunt();
        void addForShuntConfirmation(const NODE_INSTRUCTION &confirmation);
        void removeFromShuntConfirmation(const NODE_INSTRUCTION &confirmation);

        std::vector<NODE_INSTRUCTION> getForSlaves() const;
        void removeForSlavesByIndex(int index);
        std::vector<NODE_INSTRUCTION> getForSlavesByIndex(int index) const;
        String getMessageForSlaveByIndex(int index);

        bridgeNODE *getNode();

        void writeSwitch(int index, bool value);
        void writeSensor(int index, float value);
        void writeTransmitter(int index, String value);
        bool getSwitchValue(int index);
        float getSensorValue(int index);
        String getTransmitterValue(int index);

    private:
        bridgeNODE *node;
        String shuntIP;
        int shuntPORT;
        int bridgePORT;
        bool control;
        String aesKey;
        std::vector<NODE_INSTRUCTION> forSlaves;
        std::vector<NODE_INSTRUCTION> forShunt;
        WiFiServer *slaveServer;
};

#endif
