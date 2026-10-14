/*
*************** Name: Forge Node
*************** Part of: Java Wrapper libraries
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 20.8.2026.
*************** Location: Kragujevac, Serbia
*/

package com.forge_node.wrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class BRIDGE {
    
    private bridgeNODE node = null;
    private String shuntIP = "";
    private int shuntPORT = -1;
    private int bridgePORT = -1;
    private boolean control= false;
    private List<NODE_INSTRUCTION>forSlaves;
    private List<NODE_INSTRUCTION>forShunt;
    private shuntHandler shunt;
    private slaveHandler slaver;

    private final Object lock = new Object();

    public BRIDGE(NODE node, String shuntIP, int shuntPORT, int bridgePORT){
        this.node = new bridgeNODE(node);
        this.shuntIP = shuntIP;
        this.shuntPORT = shuntPORT;
        this.bridgePORT = bridgePORT;

        forSlaves = new ArrayList<>();
        forShunt = new ArrayList<>();
    }

    public void setControl(boolean value){
        synchronized(lock){
            this.control = value;
        }
    }

    public boolean getControl(){
        boolean value= false;
        synchronized(lock){
            value = this.control;
        }
        return value;
    }

    public List<NODE_INSTRUCTION> getForShunt(){
        List<NODE_INSTRUCTION>lista= new ArrayList<>();
        synchronized(lock){
            for(NODE_INSTRUCTION n : forShunt)
                lista.add(n);
        }
        return lista;
    }

    public void clearForShunt(){
        synchronized(lock){
            this.forShunt.clear();
            this.forShunt = new ArrayList<>();
        }
    }

    public void addForShuntConfirmation(NODE_INSTRUCTION confirmation){
        synchronized(lock){
            this.forShunt.add(confirmation);
        }
    }

    public void removeFromShuntConfirmation(NODE_INSTRUCTION confirmation){
        synchronized(lock){
            NODE_INSTRUCTION nn = null;
            for(NODE_INSTRUCTION n : this.forShunt)
                if(n.getNodeIndex() == confirmation.getNodeIndex()
                    && n.getType() == confirmation.getType()
                    && n.getElementIndex() == confirmation.getElementIndex()){
                    nn = n;
                    break;
                }
            if(nn != null)
                this.forShunt.remove(nn);
        }
    }

    public List<NODE_INSTRUCTION> getForSlaves(){
        List<NODE_INSTRUCTION>lista= new ArrayList<>();
        synchronized(lock){
            for(NODE_INSTRUCTION n : forSlaves)
                lista.add(n);
        }
        return lista;
    }

    public void removeForSlavesByIndex(int index){
        List<NODE_INSTRUCTION> temp = new ArrayList<>();
        synchronized(lock){
            List<NODE_INSTRUCTION> lista = getForSlaves();
            for(NODE_INSTRUCTION n : this.forSlaves)
                if(n.getNodeIndex() == index)
                    temp.add(n);
            if(lista.size()>0)
                for(NODE_INSTRUCTION n: temp)
                    this.forSlaves.remove(n);
        }
    }

    public List<NODE_INSTRUCTION> getForSlavesByIndex(int index){
        List<NODE_INSTRUCTION> temp = new ArrayList<>();
        synchronized(lock){
            for(NODE_INSTRUCTION n : this.forSlaves)
                if(n.getNodeIndex() == index)
                    temp.add(n);
        }
        return temp;
    }

    public String getMessageForSlaveByIndex(int index){
        String message = "";
        List<NODE_INSTRUCTION> lista = getForSlavesByIndex(index);
        for(NODE_INSTRUCTION n : lista){
            message = message + "I_" + n.getNodeIndex() + "_" + n.getType() + "_" + 
            n.getElementIndex() + "_";
            if(n.getType()=='C')
                message = message + n.getIntValue() + " ";
            else if (n.getType()=='R')
                message = message + n.getFloatValue() + " ";
        }
        message = message + "\n";
        removeForSlavesByIndex(index);
        return message;
    }

    public void initializeShuntHandler(){
        this.shunt = new shuntHandler(this.shuntIP, this.shuntPORT, this.node);
        this.shunt.startHandling();
    }

    public void initializeSlaveHandler(){
        this.slaver = new slaveHandler(this.bridgePORT, node);
        this.slaver.startHandling();
    }

    /*
    ***** This list of functions must exist in BRIDGE too, for the sake of synchronization *****
    */

    public void writeSwitch(int index, boolean value){
        synchronized(lock){
            this.node.writeSwitch(index, value);
        }
    }

    public void writeSensor(int index, float value){
        synchronized(lock){
            this.node.writeSensor(index, value);
        }
    }

    public void writeTransmitter(int index, String value){
        synchronized(lock){
            this.node.writeTransmitter(index, value);
        }
    }

    public boolean getSwitchValue(int index){
        boolean value= false;
        synchronized(lock){
            value = this.node.getSwitchValue(index);
        }
        return  value;
    }

    public float getSensorValue(int index){
        float value= 0f;
        synchronized(lock){
            value = this.node.getSensorValue(index);
        }
        return  value;
    }

    public String getTransmitterValue(int index){
        String value= "";
        synchronized(lock){
            value = this.node.getTransmitterValue(index);
        }
        return  value;
    }

    /* *********************************************************************************************** */

    private class shuntHandler{

        Thread handlignThread = null;

        public void startHandling(){
            if(handlignThread!=null)
                handlignThread.start();
        }

        public shuntHandler(String shuntIP, int shuntPORT, NODE node){

            handlignThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    while(getControl()){
                        try {
                            Socket shuntClient = new Socket(shuntIP, shuntPORT);
                            shuntClient.setSoTimeout(60000);
                            InputStream is = shuntClient.getInputStream();
                            OutputStream os = shuntClient.getOutputStream();
                            String confirmations = node.getMessage();
                            byte data[] = confirmations.getBytes();
                            os.write(data, 0, data.length);
                            os.flush();
                            data= new byte[2048];
                            is.read(data);
                            String instructions = new String(data, "UTF8");
                            node.processInstructions(instructions);
                            shuntClient.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                
            });
        }
    }

    private class slaveHandler{
        Thread handlignThread = null;

        public void startHandling(){
            if(handlignThread!=null)
                handlignThread.start();
        }

        public slaveHandler(int bridgePORT, bridgeNODE node){

            handlignThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    ServerSocket server = null;
                    try {
                        server = new ServerSocket(bridgePORT);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Socket slaveClient = null;
                    while(getControl()){
                        try {
                            slaveClient = server.accept();
                            slaveClient.setSoTimeout(10);
                            InputStream is = slaveClient.getInputStream();
                            OutputStream os = slaveClient.getOutputStream();
                            byte data[] = new byte[2048];
                            is.read(data);
                            String instructions = new String(data, "UTF8");
                            node.processSlaveConfirmations(instructions);
                            List<NODE_INSTRUCTION> lista= NODE_INSTRUCTION.getListOfInstructions(instructions);
                            int index = lista.get(0).getNodeIndex();
                            String message = getMessageForSlaveByIndex(index);
                            data = message.getBytes();
                            os.write(data, 0, data.length);
                            os.flush();
                            slaveClient.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            if(slaveClient != null)
                                try {
                                    slaveClient.close();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                        }
                    }

                    if(server!=null)
                        try {
                            server.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }
            });
        }
    }

    public class bridgeNODE extends NODE{

        public bridgeNODE(NODE node){
            super(node.getNodeIndex(), node.getNumOfSwitches(), node.getNumOfSensors(), node.getNumOfTransmitters(),
            node.getClientNodeSwitches(), node.getClientNodeSensors(), node.getClientNodeTransmitters());
        }

        @Override 
        public String getMessage(){
            String message = super.getMessage();
            message = message.replace("\n", "");
            List<NODE_INSTRUCTION> lista = getForShunt();
            for(NODE_INSTRUCTION n : lista){
                message = message + "Cr_" + n.getNodeIndex() + "_" + n.getType() + "_" + 
                n.getElementIndex() + "_";
                if(n.getType()=='C')
                    message = message + n.getIntValue() + " ";
                else if (n.getType()=='R')
                    message = message + n.getFloatValue() + " ";
            }
            message = message + "\n";

            clearForShunt();
            return message;
        }

        @Override 
        public void processInstructions(String rawMessage){
            if(rawMessage.startsWith("Cr"))
                return;
            List<NODE_INSTRUCTION> allInstructions= NODE_INSTRUCTION.getListOfInstructions(rawMessage);
            synchronized(lock){
                forSlaves= new ArrayList<>();
            }
            for(int i = 0; i < allInstructions.size(); i++){
                if(allInstructions.get(i).getNodeIndex() != this.getNodeIndex()){
                    synchronized(lock){
                        forSlaves.add(allInstructions.get(i));
                    }
                }else{
                    if(allInstructions.get(i).getType()=='C')
                        this.writeSwitch(allInstructions.get(i).getElementIndex(), (allInstructions.get(i).getIntValue()==1? true : false));
                    else if(allInstructions.get(i).getType()=='T')
                        this.writeTransmitter(allInstructions.get(i).getElementIndex(), allInstructions.get(i).getStringValue());
                }
            }
        }

        public void processSlaveConfirmations(String message){
            List<NODE_INSTRUCTION> confirmations = NODE_INSTRUCTION.getListOfInstructions(message);
            for(NODE_INSTRUCTION n : confirmations)
                removeFromShuntConfirmation(n);
            for(NODE_INSTRUCTION n : confirmations)
                addForShuntConfirmation(n);
        }
    }
}