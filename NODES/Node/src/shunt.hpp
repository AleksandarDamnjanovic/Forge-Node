/*
*************** Name: KS Node
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 26.11.2026.
*************** Location: Kragujevac, Serbia
*/

#ifndef SHUNT_H
#define SHUNT_H

#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <vector>
#include "node.hpp"
#include "node_instruction.hpp"
#include "crypto.hpp"

/*
Shunt is a special mode in the Forge Node system. It receives instructions through MQTT and then uses only those
instructions meant for itself, while passing all others towards its bridge and slaves.
Communication between SHUNT and BRIDGE is encrypted using AES-128-CBC (CRYPTO).
*/
class SHUNT {
    public:
        class shuntNode : public NODE {
            public:
                shuntNode(NODE *baseNode, int bridgePort, String aesSecretKey = NODE_AES_SECRET_KEY);
                virtual ~shuntNode();

                virtual String getResponse() override;
                virtual void processMessage(String rawMessage) override;

                void startBridging();
                void handleBridgeClient();
                String exchangeBridgePayload(String encryptedIncomingFromBridge);

                String generateMessageForTheBridge();
                void appendBridgeConfirmations(String rawMessage);

                bool getBridgingControl() const;
                void setBridgingControl(bool value);
                void setAesKey(String secretKey);
                String getAesKey() const;

            private:
                std::vector<NODE_INSTRUCTION> instructions;
                std::vector<NODE_INSTRUCTION> towardsBridge;
                std::vector<NODE_INSTRUCTION> confirmations;
                bool bridgingControl;
                int bridgePort;
                String aesKey;
                WiFiServer *bridgeServer;
        };

        SHUNT(NODE *baseNode, int master, int bridge, int bridgePort, String aesSecretKey = NODE_AES_SECRET_KEY);
        ~SHUNT();

        shuntNode *getNode();
        int getMasterIndex() const;
        int getBridgeIndex() const;

    private:
        shuntNode *node;
        int master;
        int bridge;
};

#endif
