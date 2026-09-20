/*
*************** Name: Forge Node
*************** Part of: Java Wrapper libraries for nodes of split system
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 20.8.2026.
*************** Location: Kragujevac, Serbia
*/

package com.forgeNode.base;

import java.util.ArrayList;

public class NODE_INSTRUCTION {
    private int nodeIndex;
    private int elementIndex;
    char type;
    int intValue;
    float floatValue;
    String stringValue;

    public static ArrayList<NODE_INSTRUCTION>getListOfInstructions(String rawMessage){
        ArrayList<NODE_INSTRUCTION>lista = new ArrayList<NODE_INSTRUCTION>();
        String tokens[] = rawMessage.split(" ");
        for(String t: tokens)
            if(t.startsWith("I"))
                lista.add(new NODE_INSTRUCTION(t));

        return lista;
    }

    public NODE_INSTRUCTION(String raw){
        String tokens[] = raw.split("_");
        try{
            nodeIndex = Integer.parseInt(tokens[1]);
            type = tokens[2].charAt(0);
            elementIndex = Integer.parseInt(tokens[3]);
            switch(type){
                case 'C':
                    intValue = Integer.parseInt(tokens[4]);
                break;
                case 'R':
                    floatValue = Float.parseFloat(tokens[4]);
                break;
                case 'T':
                    stringValue = tokens[4];
                break;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public int getElementIndex() {
        return elementIndex;
    }

    public char getType() {
        return type;
    }

    public int getIntValue() {
        return intValue;
    }

    public float getFloatValue() {
        return floatValue;
    }

    public String getStringValue() {
        return stringValue;
    }

}
