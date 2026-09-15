package com.forgeNode.base;

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




        return "";
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