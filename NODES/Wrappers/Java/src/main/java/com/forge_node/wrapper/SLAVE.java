/*
*************** Name: Forge Node
*************** Part of: Java Wrapper libraries
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 20.8.2026.
*************** Location: Kragujevac, Serbia
*/

package com.forge_node.wrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SLAVE {

    private String bridgeAddress = "";
    private int bridgePORT = -1;
    private NODE node = null;
    private boolean control = false;
    private final Object lock = new Object();
    private Thread handler = null;

    public SLAVE(NODE node, String bridgeAddress, int bridgePORT){
        this.node = node;
        this.bridgeAddress = bridgeAddress;
        this.bridgePORT = bridgePORT;

        handler = new Thread(new Runnable() {
            @Override
            public void run() {
                while(getControl()){
                    try {
                        Socket socket = new Socket(getBridgeAddress(), getBridgePORT());
                        socket.setSoTimeout(100);
                        InputStream is = socket.getInputStream();
                        OutputStream os = socket.getOutputStream();
                        String message = node.getMessage();
                        byte data[] = message.getBytes();
                        os.write(data, 0, data.length);
                        os.flush();
                        data = new byte[2048];
                        is.read(data);
                        message = new String(data, "UTF8");
                        node.processInstructions(message);
                        Thread.sleep(50);
                        os.close();
                        is.close();
                        socket.close();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    private String getBridgeAddress(){
        return this.bridgeAddress;
    }

    private int getBridgePORT(){
        return this.bridgePORT;
    }

    public void setControl(boolean value){
        synchronized(lock){
            this.control= value;
        }
    }

    public boolean getControl(){
        boolean value= false;
        synchronized(lock){
            value = this.control;
        }
        return value;
    }

    public boolean startHandler(){
        if(handler != null){
            handler.start();
            return true;
        }else
            return false;
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

}