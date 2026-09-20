/*
*************** Name: Forge Node
*************** Part of: Java Wrapper libraries for nodes of split system
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 20.8.2026.
*************** Location: Kragujevac, Serbia
*/

package com.forgeNode.main;

import com.forgeNode.base.NODE;
import com.forgeNode.client.Client;
import com.forgeNode.control.Control;

/*
With functions testClient and testControl, you can see the working example of java wrapper libraries
This app demonstrate wrapper functionalities.
*/

public class App {
    public static void main(String[] args) throws Exception {
        new Thread(new Runnable() {

            @Override
            public void run() {
                testClient();
            }
            
        }).start();
        
        new Thread(new Runnable() {

            @Override
            public void run() {
                testControl();
            }
            
        }).start();

        Thread.sleep(5000);
    }

    private static void testClient(){
        boolean switches[]={false, false};
        float sensors[] = {14,28.2f};
        String transmitters[]={"hey", "ho"};
        NODE node = new NODE(1, 2, 2, 2, "192.168.0.13", 10003,switches,sensors,transmitters);
        Client client = new Client(node);
        client.setControl(true);
        client.processStart();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        client.setControl(false);
    }

    private static void testControl(){
        boolean switches[]={false, false};
        float sensors[] = {0,0};
        String transmitters[]={"message1", "message2"};
        NODE node = new NODE(1, 2, 2, 2, "", 10003, switches,sensors,transmitters);
        Control control = new Control(node);
        control.setControl(true);
        control.processStart();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        control.setControl(false);
    }

}