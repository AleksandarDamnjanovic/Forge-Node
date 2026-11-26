/*
*************** Name: KS Node
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 26.11.2026.
*************** Location: Kragujevac, Serbia
*/

#include "node_instruction.hpp"
#include <stdlib.h>
#include <string.h>

NODE_INSTRUCTION::NODE_INSTRUCTION() {
    this->nodeIndex = -1;
    this->elementIndex = -1;
    this->type = '\0';
    this->intValue = 0;
    this->floatValue = 0.0f;
    this->stringValue = "";
}

NODE_INSTRUCTION::NODE_INSTRUCTION(String raw) {
    this->nodeIndex = -1;
    this->elementIndex = -1;
    this->type = '\0';
    this->intValue = 0;
    this->floatValue = 0.0f;
    this->stringValue = "";

    raw.trim();
    if (raw.length() == 0)
        return;

    int len = raw.length() + 1;
    char buf[len];
    memset(buf, '\0', len);
    strcpy(buf, raw.c_str());

    char *saveptr = NULL;
    char *tok0 = strtok_r(buf, "_", &saveptr);
    if (tok0 == NULL)
        return;

    char *tok1 = strtok_r(NULL, "_", &saveptr);
    if (tok1 == NULL)
        return;
    this->nodeIndex = atoi(tok1);

    char *tok2 = strtok_r(NULL, "_", &saveptr);
    if (tok2 == NULL || tok2[0] == '\0')
        return;
    this->type = tok2[0];

    char *tok3 = strtok_r(NULL, "_", &saveptr);
    if (tok3 == NULL)
        return;
    this->elementIndex = atoi(tok3);

    char *tok4 = strtok_r(NULL, "_", &saveptr);
    switch (this->type) {
        case 'C':
            if (tok4 != NULL)
                this->intValue = atoi(tok4);
            break;
        case 'R':
            if (tok4 != NULL)
                this->floatValue = (float)atof(tok4);
            break;
        case 'T':
            if (tok4 != NULL)
                this->stringValue = String(tok4);
            break;
        case 'S':
        default:
            break;
    }
}

std::vector<NODE_INSTRUCTION> NODE_INSTRUCTION::getListOfInstructions(String rawMessage) {
    std::vector<NODE_INSTRUCTION> lista;
    rawMessage.trim();
    if (rawMessage.length() == 0)
        return lista;

    int len = rawMessage.length() + 1;
    char buf[len];
    memset(buf, '\0', len);
    strcpy(buf, rawMessage.c_str());

    char *saveptr = NULL;
    char *tok = strtok_r(buf, " \r\n\t", &saveptr);
    while (tok != NULL) {
        String t = String(tok);
        t.trim();
        if (t.startsWith("I_") || t.startsWith("Cr_")) {
            NODE_INSTRUCTION inst(t);
            if (inst.getNodeIndex() > 0)
                lista.push_back(inst);
        }
        tok = strtok_r(NULL, " \r\n\t", &saveptr);
    }

    return lista;
}

int NODE_INSTRUCTION::getNodeIndex() const {
    return this->nodeIndex;
}

int NODE_INSTRUCTION::getElementIndex() const {
    return this->elementIndex;
}

char NODE_INSTRUCTION::getType() const {
    return this->type;
}

int NODE_INSTRUCTION::getIntValue() const {
    return this->intValue;
}

float NODE_INSTRUCTION::getFloatValue() const {
    return this->floatValue;
}

String NODE_INSTRUCTION::getStringValue() const {
    return this->stringValue;
}
