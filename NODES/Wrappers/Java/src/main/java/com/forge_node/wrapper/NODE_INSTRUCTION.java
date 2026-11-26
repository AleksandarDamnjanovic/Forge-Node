/*
*************** Name: Forge Node
*************** Part of: Java Wrapper libraries
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 20.8.2026.
*************** Location: Kragujevac, Serbia
*/

package com.forge_node.wrapper;

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
        if(rawMessage == null || rawMessage.isBlank())
            return lista;
        String cleaned = rawMessage.replace("\0", "").trim();
        if(cleaned.isEmpty())
            return lista;
        String tokens[] = cleaned.split("\\s+");
        for(String t: tokens)
            if(t.startsWith("I_") || t.startsWith("Cr_")){
                NODE_INSTRUCTION inst = new NODE_INSTRUCTION(t);
                if(inst.getNodeIndex() > 0)
                    lista.add(inst);
            }

        return lista;
    }

    public NODE_INSTRUCTION(String raw){
        String cleaned = raw == null ? "" : raw.replace("\0", "").trim();
        String tokens[] = cleaned.split("_");
        try{
            if(tokens.length < 4)
                return;
            nodeIndex = Integer.parseInt(tokens[1].trim());
            type = tokens[2].trim().charAt(0);
            elementIndex = Integer.parseInt(tokens[3].trim());
            switch(type){
                case 'C':
                    if(tokens.length >= 5)
                        intValue = Integer.parseInt(tokens[4].trim());
                break;
                case 'R':
                    if(tokens.length >= 5)
                        floatValue = Float.parseFloat(tokens[4].trim());
                break;
                case 'T':
                    if(tokens.length >= 5)
                        stringValue = tokens[4].trim();
                    else
                        stringValue = "";
                break;
                case 'S':
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
