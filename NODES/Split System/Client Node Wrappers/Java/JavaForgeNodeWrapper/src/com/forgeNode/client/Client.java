package com.forgeNode.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.forgeNode.base.NODE;

public class Client {

    NODE node = null;
    private Thread process;
    private boolean control;
    private final Object lock = new Object();

    /* 
        Node can be accessed directly only if outside of the process.
        When process is running, node can be accessed through the process, otherwise
        you are risking synchronization issue
    */
    public Client(NODE n){
        node = n;
        this.control = false;
        createProcess();
    }

    private void createProcess(){
        process = new Thread(new Runnable() {
            @Override 
            public void run(){
                while(control){
                    try (Socket socket = new Socket(node.getServerIP(), node.getPort());
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()))) {

                        out.println(node.getMessage());



                        out.close();
                        in.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void processStart(){
        this.process.start();
    }

    public void setControl(boolean value){
        synchronized(lock){
            this.control = value;
        }
    }

    public int getNumOfSwitches(){
        int ret = -1;
        synchronized(lock){
            if(this.node!=null)
                ret = node.getNumOfSwitches();
        }
        return ret;
    }

    public int getNumOfSensors(){
        int ret = -1;
        synchronized(lock){
            if(this.node!=null)
                ret = node.getNumOfSensors();
        }
        return ret;
    }

    public int getNumOfTransmitters(){
        int ret = -1;
        synchronized(lock){
            if(this.node!=null)
                ret = node.getNumOfTransmitters();
        }
        return ret;
    }

    public boolean getSwitchValue(int index){
        boolean s = false;
        synchronized(lock){
            if(this.node!=null)
                s = node.getSwitchValue(index);
        }
        return s;
    }

    public float getSensorValue(int index){
        float f = -1;
        synchronized(lock){
            if(this.node!=null)
                f = node.getSensorValue(index);
        }
        return f;
    }

    public String getTransmitterValue(int index){
        String t = "";
        synchronized(lock){
            if(this.node!=null)
                t = node.getTransmitterValue(index);
        }
        return t;
    }

    public void writeSwitch(int index, boolean value){
        synchronized(lock){
            if(this.node!=null)
                node.writeSwitch(index, value);
        }
    }

    public void writeSensor(int index, float value){
        synchronized(lock){
            if(this.node!=null)
                node.writeSensor(index, value);
        }
    }

    public void writeTransmitter(int index, String value){
        synchronized(lock){
            if(this.node!=null)
                node.writeTransmitter(index, value);
        }
    }

}