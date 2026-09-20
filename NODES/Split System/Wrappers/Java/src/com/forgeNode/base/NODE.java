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

public class NODE {
    private int nodeIndex;
    private int numOfSwitches;
    private int numOfSensors;
    private int numOfTransmitters;

    private String serverIP;
    private int port;

    private boolean clientNodeSwitches[];
    private float clientNodeSensors[];
    private String clientNodeTransmitters[];

    /* 
        Node can be accessed directly only if outside of the process.
        When process is running, node can be accessed through the process, otherwise
        you are risking synchronization issue
    */
    public NODE(int nodeIndex, int numOfSwitches, int numOfSensors, int numOfTransmitters,
        String serverIP, int port){

        this.nodeIndex = nodeIndex;
        this.numOfSwitches = numOfSwitches;
        this.numOfSensors = numOfSensors;
        this.numOfTransmitters = numOfTransmitters;

        this.serverIP = serverIP;
        this.port = port;

        boolean switches[]= new boolean[this.numOfSwitches];
        for(int i = 0 ; i < this.getNumOfSwitches(); i++)
            switches[i] = false;

        float sensors[]= new float[this.numOfSensors];
        for(int i = 0 ; i < this.getNumOfSensors(); i++)
            sensors[i] = 0;

        String transmitters[]= new String[this.numOfTransmitters];
        for(int i = 0 ; i < this.getNumOfTransmitters(); i++)
            transmitters[i] = "";

        this.clientNodeSwitches = switches;
        this.clientNodeSensors = sensors;
        this.clientNodeTransmitters= transmitters;
    }

    public NODE(int nodeIndex, int numOfSwitches, int numOfSensors, int numOfTransmitters,
        String serverIP, int port, boolean clientNodeSwitches[],
        float clientNodeSensors[], String clientNodeTransmitters[]){

        this.nodeIndex = nodeIndex;
        this.numOfSwitches = numOfSwitches;
        this.numOfSensors = numOfSensors;
        this.numOfTransmitters = numOfTransmitters;

        this.serverIP = serverIP;
        this.port = port;
        this.clientNodeSwitches = clientNodeSwitches;
        this.clientNodeSensors = clientNodeSensors;
        this.clientNodeTransmitters= clientNodeTransmitters;
    }

    public String getMessage(){
        String message= new String();
        for(int i = 0; i < getNumOfSwitches(); i++)
            message = message + "Cr_" + getNodeIndex() + "_C_" + i + "_" + (getSwitchValue(i)?"1":"0") + " ";
        
        for(int i = 0; i < getNumOfSensors(); i++)
            message = message + "Cr_" + getNodeIndex() + "_R_" + i + "_" + String.format("%.2f", getSensorValue(i)) + " ";
        
        message = message + "\n";

        return message;
    }

    public String generateInstructions(){
        String message= new String();
        for(int i = 0; i < getNumOfSwitches(); i++)
            message = message + "I_" + getNodeIndex() + "_C_" + i + "_" + (getSwitchValue(i)?"1":"0") + " ";
        
        for(int i = 0; i < getNumOfTransmitters(); i++)
            message = message + "I_" + getNodeIndex() + "_T_" + i + "_" + getTransmitterValue(i)+ " ";
        
        message = message + "\n";

        return message;
    }

    public void processReport(String rawMessage){
        ArrayList<NODE_INSTRUCTION> lista= NODE_INSTRUCTION.getListOfInstructions(rawMessage);
        for(int i = 0; i < lista.size(); i++){
            if(lista.get(i).getNodeIndex() != this.getNodeIndex())
                continue;
            else{
                if(lista.get(i).getType()=='R')
                    this.writeSensor(lista.get(i).getElementIndex(), lista.get(i).getFloatValue());
            }
        }
    }

    public void processInstructions(String rawMessage){
        ArrayList<NODE_INSTRUCTION> lista= NODE_INSTRUCTION.getListOfInstructions(rawMessage);
        for(int i = 0; i < lista.size(); i++){
            if(lista.get(i).getNodeIndex() != this.getNodeIndex())
                continue;
            else{
                if(lista.get(i).getType()=='C')
                    this.writeSwitch(lista.get(i).getElementIndex(), (lista.get(i).getIntValue()==1? true : false));
                else if(lista.get(i).getType()=='T')
                    this.writeTransmitter(lista.get(i).getElementIndex(), lista.get(i).getStringValue());
            }
        }
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public int getNumOfSwitches() {
        return numOfSwitches;
    }

    public int getNumOfSensors() {
        return numOfSensors;
    }

    public int getNumOfTransmitters() {
        return numOfTransmitters;
    }

    public String getServerIP() {
        return serverIP;
    }

    public int getPort() {
        return port;
    }

    public boolean[] getClientNodeSwitches() {
        return clientNodeSwitches;
    }

    public float[] getClientNodeSensors() {
        return clientNodeSensors;
    }

    public String[] getClientNodeTransmitters() {
        return clientNodeTransmitters;
    }

    public void writeSwitch(int index, boolean value){
        if(index < this.getNumOfSwitches()  && index > -1)
            this.getClientNodeSwitches()[index]= value;
    }

    public void writeSensor(int index, float value){
        if(index < this.getNumOfSensors()  && index > -1)
            this.getClientNodeSensors()[index]= value;
    }

    public void writeTransmitter(int index, String value){
        if(index < this.getNumOfTransmitters()  && index > -1)
            this.getClientNodeTransmitters()[index]= value;
    }

    public boolean getSwitchValue(int index){
        if(index < this.getNumOfSwitches() && index > -1)
            return this.getClientNodeSwitches()[index];
        else
            return false;
    }

    public float getSensorValue(int index){
        if(index < this.getNumOfSensors() && index > -1)
            return this.getClientNodeSensors()[index];
        else
            return -1;
    }

    public String getTransmitterValue(int index){
        if(index < this.getNumOfTransmitters() && index > -1)
            return this.getClientNodeTransmitters()[index];
        else
            return "";
    }

}