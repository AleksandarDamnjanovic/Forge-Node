/*
*************** Name: Forge Node
*************** Part of: Java Wrapper libraries
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 20.8.2026.
*************** Location: Kragujevac, Serbia
*************** Purpose of Main.java:
                    In here you can see an exaple of how wrapper can be used in order to simulate
                    both shunt, bridge and slave nodes.
                    Shunt node deals with mqtt comunicattion and creates tcp ip server in order to establish
                    connection with the bridge. Bridge has two tcp communication threads. One that deals with
                    the shunt and another that deals with communication with slaves.
*/

package com.forge_node.wrapper;

public class Main {

    static final int shuntPort = 10023;
    static final int bridgePort = 10024;
    static final String aesSecretKey = "forge_node_shunt_bridge_secret";

    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                shuntExample();
            }
        }).start();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                bridgeExample();
            }
        }).start();
         
        new Thread(new Runnable() {
            @Override
            public void run() {
                slaveExample();
            }
        }).start();
    }

    public static void slaveExample(){
        boolean switches[] = {false, false};
        float sensors[]= {0f,0f};
        String transmitters[] = {"", ""};
        NODE node = new NODE(3, 2, 2, 2, switches, sensors, transmitters);
        SLAVE slave = new SLAVE(node, "192.168.0.17", bridgePort);
        slave.setControl(true);
        slave.startHandler();

        slave.writeSensor(0, 25.1f);
        slave.writeSensor(1, 1.51f);

        int counter = 0;
        while(counter < 100)
            try {
                Thread.sleep(1000);
                counter += 1;
                System.out.println(slave.getTransmitterValue(0)+ " " 
                + slave.getTransmitterValue(1));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        slave.setControl(false);
        System.exit(0);
    }

    public static void bridgeExample(){
        boolean switches[] = {false, false};
        float sensors[]= {0f,0f};
        String transmitters[] = {"", ""};
        NODE node = new NODE(2, 2, 2, 2, switches, sensors, transmitters);
        BRIDGE bridge = new BRIDGE(node, "192.168.0.17", shuntPort, bridgePort, aesSecretKey);
        bridge.setControl(true);
        bridge.initializeShuntHandler();
        bridge.initializeSlaveHandler();

        bridge.writeSensor(0, 5.13f);
        bridge.writeSensor(1, 799.51f);

        int counter = 0;
        while(counter < 100)
            try {
                Thread.sleep(1000);
                counter += 1;
                System.out.println(bridge.getTransmitterValue(0)+ " " 
                + bridge.getTransmitterValue(1));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        bridge.setControl(false);
        System.exit(0);
    }

    public static void shuntExample(){
        boolean switches[] = {false, false};
        float sensors[]= {0f,0f};
        String transmitters[] = {"", ""};
        NODE node = new NODE(1, 2, 2, 2, switches, sensors, transmitters);
        SHUNT shunt = new SHUNT(node, 1, 3, shuntPort, aesSecretKey);
        shunt.setupClient("1f", "00004444", "192.168.0.24", 8883, "./store.jks", "password");
        shunt.getClient().setControl(true);
        shunt.getClient().startListening();
        shunt.getClient().startPublishing();
        shunt.getNode().setBridgingControl(true);
        shunt.getNode().startBridging();

        shunt.getClient().writeSensor(0, 2.5f);
        shunt.getClient().writeSensor(1, 23.4f);

        int counter = 0;
        while(counter < 100)
            try {
                Thread.sleep(1000);
                counter += 1;
                System.out.println(shunt.getClient().getTransmitterValue(0)+ " " 
                + shunt.getClient().getTransmitterValue(1));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        shunt.getClient().setControl(false);
        System.exit(0);
    }


    public static void masterExample(){
        boolean switches[] = {false, false};
        float sensors[]= {0f,0f};
        String transmitters[] = {"", ""};

        NODE node = new NODE(1, 2, 2, 2, switches, sensors, transmitters);
        Client client = new Client(node, "1f", "00004444", "192.168.0.24", 8883, "./store.jks", "password");
        client.setControl(true);
        client.startListening();
        client.startPublishing();
        
        client.writeSensor(0, 2.5f);
        client.writeSensor(1, 23.4f);

        int counter = 0;
        while(counter < 100)
            try {
                Thread.sleep(1000);
                counter += 1;
                System.out.println(client.getTransmitterValue(0)+ " " + client.getTransmitterValue(1));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        client.setControl(false);
        System.exit(0);
    }
}