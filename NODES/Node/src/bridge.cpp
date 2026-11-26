/*
*************** Name: KS Node
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 26.11.2026.
*************** Location: Kragujevac, Serbia
*/

#include "bridge.hpp"
#include <stdio.h>
#include <string.h>

BRIDGE::bridgeNODE::bridgeNODE(NODE *baseNode, BRIDGE *owner)
    : NODE(NODE_BRIDGE,
           baseNode->getSlaveTo(),
           baseNode->getSwitches(),
           baseNode->getNodeIndex(),
           baseNode->getSwitchStates(),
           baseNode->getTransmitters(),
           baseNode->getSensors(),
           baseNode->getNumOfSwitches(),
           baseNode->getNumOfSensors(),
           baseNode->getNumOfTransmitters()) {
    this->parentBridge = owner;
}

String BRIDGE::bridgeNODE::getResponse() {
    String message = NODE::getResponse();
    message.replace("\n", "");

    if (this->parentBridge != NULL) {
        std::vector<NODE_INSTRUCTION> lista = this->parentBridge->getForShunt();
        for (size_t i = 0; i < lista.size(); i++) {
            const NODE_INSTRUCTION &n = lista[i];
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
        this->parentBridge->clearForShunt();
    }

    message += "\n";
    return message;
}

void BRIDGE::bridgeNODE::processMessage(String rawMessage) {
    rawMessage.trim();
    if (rawMessage.length() == 0 || rawMessage.startsWith("Cr"))
        return;

    std::vector<NODE_INSTRUCTION> allInstructions = NODE_INSTRUCTION::getListOfInstructions(rawMessage);
    if (allInstructions.empty())
        return;

    for (size_t i = 0; i < allInstructions.size(); i++) {
        const NODE_INSTRUCTION &incoming = allInstructions[i];
        if (incoming.getNodeIndex() != this->getNodeIndex()) {
            if (this->parentBridge != NULL) {
                for (size_t j = 0; j < this->parentBridge->forSlaves.size(); ) {
                    if (this->parentBridge->forSlaves[j].getNodeIndex() == incoming.getNodeIndex() &&
                        this->parentBridge->forSlaves[j].getType() == incoming.getType() &&
                        this->parentBridge->forSlaves[j].getElementIndex() == incoming.getElementIndex()) {
                        this->parentBridge->forSlaves.erase(this->parentBridge->forSlaves.begin() + j);
                    } else {
                        j++;
                    }
                }
                this->parentBridge->forSlaves.push_back(incoming);
            }
        } else {
            if (incoming.getType() == 'C') {
                this->setSwitchState(incoming.getElementIndex(), incoming.getIntValue() == 1 ? 1 : 0);
            } else if (incoming.getType() == 'T') {
                this->setTransmitterValue(incoming.getElementIndex(), incoming.getStringValue());
            }
        }
    }

    this->processSensors();
    this->validate();
}

void BRIDGE::bridgeNODE::processSlaveConfirmations(String message) {
    if (this->parentBridge == NULL)
        return;
    std::vector<NODE_INSTRUCTION> confirmations = NODE_INSTRUCTION::getListOfInstructions(message);
    for (size_t i = 0; i < confirmations.size(); i++) {
        this->parentBridge->removeFromShuntConfirmation(confirmations[i]);
        this->parentBridge->addForShuntConfirmation(confirmations[i]);
    }
}

BRIDGE::BRIDGE(NODE *baseNode, String shuntIP, int shuntPORT, int bridgePORT, String aesSecretKey) {
    this->node = new bridgeNODE(baseNode, this);
    this->shuntIP = shuntIP;
    this->shuntPORT = shuntPORT;
    this->bridgePORT = bridgePORT;
    this->control = false;
    this->aesKey = aesSecretKey;
    this->slaveServer = NULL;
}

BRIDGE::~BRIDGE() {
    if (this->slaveServer != NULL) {
        this->slaveServer->close();
        delete this->slaveServer;
        this->slaveServer = NULL;
    }
    if (this->node != NULL) {
        delete this->node;
        this->node = NULL;
    }
}

void BRIDGE::setControl(bool value) {
    this->control = value;
}

bool BRIDGE::getControl() const {
    return this->control;
}

void BRIDGE::setAesKey(String secretKey) {
    this->aesKey = secretKey;
}

String BRIDGE::getAesKey() const {
    return this->aesKey;
}

bool BRIDGE::startAccessPoint(const char *apSsid, const char *apPassword, int channel, bool hidden, int maxConnections) {
    // WPA2-PSK requires an 8..63 character password; encrypts all Wi-Fi radio frames between BRIDGE AP and SLAVEs
    if (apPassword == NULL || strlen(apPassword) < 8)
        return false;
    return WiFi.softAP(apSsid, apPassword, channel, hidden, maxConnections);
}

void BRIDGE::initializeShuntHandler() {
    // Connection to SHUNT is executed per cycle via handleShuntStep() (TCP) or buildEncryptedPayloadForShunt() (LoRa/Serial)
}

void BRIDGE::initializeSlaveHandler() {
    if (this->slaveServer == NULL) {
        this->slaveServer = new WiFiServer(this->bridgePORT);
    }
    this->slaveServer->begin();
}

String BRIDGE::buildEncryptedPayloadForShunt() {
    String confirmations = this->node->getResponse();
    return CRYPTO::encrypt(confirmations, this->aesKey);
}

void BRIDGE::processEncryptedPayloadFromShunt(String encryptedPayload) {
    String decrypted = CRYPTO::decrypt(encryptedPayload, this->aesKey);
    decrypted.trim();
    if (decrypted.length() > 0) {
        this->node->processMessage(decrypted);
    }
}

void BRIDGE::handleShuntStep() {
    if (!this->control || this->shuntIP.length() == 0 || this->shuntPORT <= 0)
        return;

    WiFiClient shuntClient;
    shuntClient.setTimeout(5000);
    if (!shuntClient.connect(this->shuntIP.c_str(), this->shuntPORT)) {
        return;
    }

    String encryptedConfirmations = buildEncryptedPayloadForShunt();
    shuntClient.print(encryptedConfirmations);
    shuntClient.flush();

    unsigned long waitStart = millis();
    while (!shuntClient.available() && shuntClient.connected() && (millis() - waitStart < 3000)) {
        delay(5);
    }

    if (shuntClient.available()) {
        String encryptedInstructions = shuntClient.readStringUntil('\n');
        encryptedInstructions.trim();
        if (encryptedInstructions.length() > 0) {
            processEncryptedPayloadFromShunt(encryptedInstructions);
        }
    }

    shuntClient.stop();
}

void BRIDGE::handleSlaveStep() {
    if (!this->control || this->slaveServer == NULL)
        return;

    WiFiClient slaveClient = this->slaveServer->accept();
    if (!slaveClient)
        return;

    slaveClient.setTimeout(5000);
    unsigned long waitStart = millis();
    while (!slaveClient.available() && slaveClient.connected() && (millis() - waitStart < 3000)) {
        delay(5);
    }

    if (slaveClient.available()) {
        // Communication between BRIDGE and SLAVE relies on WPA2 radio encryption on the BRIDGE's Wi-Fi AP (plain TCP payload)
        String instructions = slaveClient.readStringUntil('\n');
        instructions.trim();
        if (instructions.length() > 0) {
            this->node->processSlaveConfirmations(instructions);
            std::vector<NODE_INSTRUCTION> lista = NODE_INSTRUCTION::getListOfInstructions(instructions);
            if (!lista.empty()) {
                int index = lista[0].getNodeIndex();
                String message = getMessageForSlaveByIndex(index);
                slaveClient.print(message);
                slaveClient.flush();
            }
        }
    }

    slaveClient.stop();
}

std::vector<NODE_INSTRUCTION> BRIDGE::getForShunt() const {
    return this->forShunt;
}

void BRIDGE::clearForShunt() {
    this->forShunt.clear();
}

void BRIDGE::addForShuntConfirmation(const NODE_INSTRUCTION &confirmation) {
    this->forShunt.push_back(confirmation);
}

void BRIDGE::removeFromShuntConfirmation(const NODE_INSTRUCTION &confirmation) {
    for (size_t i = 0; i < this->forShunt.size(); ) {
        if (this->forShunt[i].getNodeIndex() == confirmation.getNodeIndex() &&
            this->forShunt[i].getType() == confirmation.getType() &&
            this->forShunt[i].getElementIndex() == confirmation.getElementIndex()) {
            this->forShunt.erase(this->forShunt.begin() + i);
            break;
        } else {
            i++;
        }
    }
}

std::vector<NODE_INSTRUCTION> BRIDGE::getForSlaves() const {
    return this->forSlaves;
}

void BRIDGE::removeForSlavesByIndex(int index) {
    for (size_t i = 0; i < this->forSlaves.size(); ) {
        if (this->forSlaves[i].getNodeIndex() == index) {
            this->forSlaves.erase(this->forSlaves.begin() + i);
        } else {
            i++;
        }
    }
}

std::vector<NODE_INSTRUCTION> BRIDGE::getForSlavesByIndex(int index) const {
    std::vector<NODE_INSTRUCTION> temp;
    for (size_t i = 0; i < this->forSlaves.size(); i++) {
        if (this->forSlaves[i].getNodeIndex() == index) {
            temp.push_back(this->forSlaves[i]);
        }
    }
    return temp;
}

String BRIDGE::getMessageForSlaveByIndex(int index) {
    String message = "";
    std::vector<NODE_INSTRUCTION> lista = getForSlavesByIndex(index);
    for (size_t i = 0; i < lista.size(); i++) {
        const NODE_INSTRUCTION &n = lista[i];
        message += "I_" + String(n.getNodeIndex()) + "_" + String(n.getType()) + "_" + String(n.getElementIndex());
        if (n.getType() == 'S') {
            message += " ";
        } else if (n.getType() == 'C') {
            message += "_" + String(n.getIntValue()) + " ";
        } else if (n.getType() == 'T') {
            message += "_" + n.getStringValue() + " ";
        }
    }
    message += "\n";
    removeForSlavesByIndex(index);
    return message;
}

BRIDGE::bridgeNODE *BRIDGE::getNode() {
    return this->node;
}

void BRIDGE::writeSwitch(int index, bool value) {
    this->node->setSwitchState(index, value ? 1 : 0);
    this->node->validate();
}

void BRIDGE::writeSensor(int index, float value) {
    this->node->setSensorValue(index, value);
}

void BRIDGE::writeTransmitter(int index, String value) {
    this->node->setTransmitterValue(index, value);
}

bool BRIDGE::getSwitchValue(int index) {
    return this->node->getSwitchStateByIndex(index);
}

float BRIDGE::getSensorValue(int index) {
    return this->node->getSensorByIndex(index);
}

String BRIDGE::getTransmitterValue(int index) {
    return this->node->getTransmitterByIndex(index);
}
