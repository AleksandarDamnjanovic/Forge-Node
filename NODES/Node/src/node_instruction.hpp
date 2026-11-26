/*
*************** Name: KS Node
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 26.11.2026.
*************** Location: Kragujevac, Serbia
*/

#ifndef NODE_INSTRUCTION_H
#define NODE_INSTRUCTION_H

#include <Arduino.h>
#include <vector>

class NODE_INSTRUCTION {
    public:
        NODE_INSTRUCTION();
        NODE_INSTRUCTION(String raw);

        static std::vector<NODE_INSTRUCTION> getListOfInstructions(String rawMessage);

        int getNodeIndex() const;
        int getElementIndex() const;
        char getType() const;
        int getIntValue() const;
        float getFloatValue() const;
        String getStringValue() const;

    private:
        int nodeIndex;
        int elementIndex;
        char type;
        int intValue;
        float floatValue;
        String stringValue;
};

#endif
