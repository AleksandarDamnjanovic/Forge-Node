/*
*************** Name: KS Node
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 26.11.2026.
*************** Location: Kragujevac, Serbia
*/

#include "slave.hpp"

SLAVE::SLAVE(NODE *node, String bridgeAddress, int bridgePORT) {
    this->node = node;
    this->bridgeAddress = bridgeAddress;
    this->bridgePORT = bridgePORT;
    this->control = false;
}

String SLAVE::getBridgeAddress() const {
    return this->bridgeAddress;
}

int SLAVE::getBridgePORT() const {
    return this->bridgePORT;
}

void SLAVE::setControl(bool value) {
    this->control = value;
}

bool SLAVE::getControl() const {
    return this->control;
}

bool SLAVE::startHandler() {
    return this->node != NULL && this->bridgeAddress.length() > 0 && this->bridgePORT > 0;
}

void SLAVE::handleStep() {
    if (!this->control || this->node == NULL)
        return;

    WiFiClient client;
    client.setTimeout(5000);
    if (!client.connect(this->bridgeAddress.c_str(), this->bridgePORT)) {
        return;
    }

    String message = this->node->getResponse();
    if (!message.endsWith("\n"))
        message += "\n";
    client.print(message);
    client.flush();

    unsigned long waitStart = millis();
    while (!client.available() && client.connected() && (millis() - waitStart < 3000)) {
        delay(5);
    }

    if (client.available()) {
        String incoming = client.readStringUntil('\n');
        incoming.trim();
        if (incoming.length() > 0) {
            this->node->processMessage(incoming);
        }
    }

    client.stop();
}

NODE *SLAVE::getNode() {
    return this->node;
}

void SLAVE::writeSwitch(int index, bool value) {
    this->node->setSwitchState(index, value ? 1 : 0);
    this->node->validate();
}

void SLAVE::writeSensor(int index, float value) {
    this->node->setSensorValue(index, value);
}

void SLAVE::writeTransmitter(int index, String value) {
    this->node->setTransmitterValue(index, value);
}

bool SLAVE::getSwitchValue(int index) {
    return this->node->getSwitchStateByIndex(index);
}

float SLAVE::getSensorValue(int index) {
    return this->node->getSensorByIndex(index);
}

String SLAVE::getTransmitterValue(int index) {
    return this->node->getTransmitterByIndex(index);
}
