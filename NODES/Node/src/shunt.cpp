/*
*************** Name: KS Node
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 26.11.2026.
*************** Location: Kragujevac, Serbia
*/

#include "shunt.hpp"
#include <stdio.h>
#include <string.h>

SHUNT::shuntNode::shuntNode(NODE *baseNode, int bridgePort, String aesSecretKey)
    : NODE(NODE_SHUNT,
           baseNode->getSlaveTo(),
           baseNode->getSwitches(),
           baseNode->getNodeIndex(),
           baseNode->getSwitchStates(),
           baseNode->getTransmitters(),
           baseNode->getSensors(),
           baseNode->getNumOfSwitches(),
           baseNode->getNumOfSensors(),
           baseNode->getNumOfTransmitters()) {
    this->bridgingControl = false;
    this->bridgePort = bridgePort;
    this->aesKey = aesSecretKey;
    this->bridgeServer = NULL;
}

SHUNT::shuntNode::~shuntNode() {
    if (this->bridgeServer != NULL) {
        this->bridgeServer->close();
        delete this->bridgeServer;
        this->bridgeServer = NULL;
    }
}

String SHUNT::shuntNode::getResponse() {
    String message = NODE::getResponse();
    message.replace("\n", "");

    for (size_t i = 0; i < this->confirmations.size(); i++) {
        const NODE_INSTRUCTION &n = this->confirmations[i];
        char part[40];
        memset(part, '\0', sizeof(part));
        if (n.getType() == 'C') {
            snprintf(part, sizeof(part), "Cr_%d_C_%d_%d ", n.getNodeIndex(), n.getElementIndex(), n.getIntValue());
            message += String(part);
        } else if (n.getType() == 'R') {
            snprintf(part, sizeof(part), "Cr_%d_R_%d_%0.1f ", n.getNodeIndex(), n.getElementIndex(), n.getFloatValue());
            message += String(part);
        }
    }

    this->confirmations.clear();
    return message;
}

void SHUNT::shuntNode::processMessage(String rawMessage) {
    rawMessage.trim();
    if (rawMessage.length() == 0 || rawMessage.startsWith("Cr"))
        return;

    this->instructions = NODE_INSTRUCTION::getListOfInstructions(rawMessage);
    this->towardsBridge.clear();

    for (size_t i = 0; i < this->instructions.size(); i++) {
        const NODE_INSTRUCTION &inst = this->instructions[i];
        if (inst.getNodeIndex() != this->getNodeIndex()) {
            this->towardsBridge.push_back(inst);
        } else {
            if (inst.getType() == 'C') {
                this->setSwitchState(inst.getElementIndex(), inst.getIntValue() == 1 ? 1 : 0);
            } else if (inst.getType() == 'T') {
                this->setTransmitterValue(inst.getElementIndex(), inst.getStringValue());
            }
        }
    }

    this->processSensors();
    this->validate();
}

void SHUNT::shuntNode::startBridging() {
    if (this->bridgeServer == NULL) {
        this->bridgeServer = new WiFiServer(this->bridgePort);
    }
    this->bridgeServer->begin();
}

String SHUNT::shuntNode::exchangeBridgePayload(String encryptedIncomingFromBridge) {
    String decrypted = CRYPTO::decrypt(encryptedIncomingFromBridge, this->aesKey);
    decrypted.trim();
    if (decrypted.startsWith("Cr")) {
        appendBridgeConfirmations(decrypted);
    }

    String plainForBridge = generateMessageForTheBridge();
    return CRYPTO::encrypt(plainForBridge, this->aesKey);
}

void SHUNT::shuntNode::handleBridgeClient() {
    if (!this->bridgingControl || this->bridgeServer == NULL)
        return;

    WiFiClient client = this->bridgeServer->accept();
    if (!client)
        return;

    client.setTimeout(5000);
    unsigned long waitStart = millis();
    while (!client.available() && client.connected() && (millis() - waitStart < 3000)) {
        delay(5);
    }

    if (client.available()) {
        String incoming = client.readStringUntil('\n');
        incoming.trim();
        String encryptedReply = exchangeBridgePayload(incoming);
        client.print(encryptedReply);
        client.flush();
    }
    client.stop();
}

String SHUNT::shuntNode::generateMessageForTheBridge() {
    String message = "";
    for (size_t i = 0; i < this->towardsBridge.size(); i++) {
        const NODE_INSTRUCTION &n = this->towardsBridge[i];
        message += "I_" + String(n.getNodeIndex()) + "_" + String(n.getType()) + "_" + String(n.getElementIndex());
        if (n.getType() == 'S') {
            message += " ";
        } else if (n.getType() == 'C') {
            message += "_" + String(n.getIntValue()) + " ";
        } else if (n.getType() == 'T') {
            message += "_" + n.getStringValue() + " ";
        }
    }
    this->towardsBridge.clear();
    message += "\n";
    return message;
}

void SHUNT::shuntNode::appendBridgeConfirmations(String rawMessage) {
    std::vector<NODE_INSTRUCTION> lista = NODE_INSTRUCTION::getListOfInstructions(rawMessage);
    for (size_t i = 0; i < lista.size(); i++) {
        const NODE_INSTRUCTION &incoming = lista[i];
        for (size_t j = 0; j < this->confirmations.size(); ) {
            if (this->confirmations[j].getNodeIndex() == incoming.getNodeIndex() &&
                this->confirmations[j].getType() == incoming.getType() &&
                this->confirmations[j].getElementIndex() == incoming.getElementIndex()) {
                this->confirmations.erase(this->confirmations.begin() + j);
            } else {
                j++;
            }
        }
        this->confirmations.push_back(incoming);
    }
}

bool SHUNT::shuntNode::getBridgingControl() const {
    return this->bridgingControl;
}

void SHUNT::shuntNode::setBridgingControl(bool value) {
    this->bridgingControl = value;
}

void SHUNT::shuntNode::setAesKey(String secretKey) {
    this->aesKey = secretKey;
}

String SHUNT::shuntNode::getAesKey() const {
    return this->aesKey;
}

SHUNT::SHUNT(NODE *baseNode, int master, int bridge, int bridgePort, String aesSecretKey) {
    this->node = new shuntNode(baseNode, bridgePort, aesSecretKey);
    this->master = master;
    this->bridge = bridge;
}

SHUNT::~SHUNT() {
    if (this->node != NULL) {
        delete this->node;
        this->node = NULL;
    }
}

SHUNT::shuntNode *SHUNT::getNode() {
    return this->node;
}

int SHUNT::getMasterIndex() const {
    return this->master;
}

int SHUNT::getBridgeIndex() const {
    return this->bridge;
}
