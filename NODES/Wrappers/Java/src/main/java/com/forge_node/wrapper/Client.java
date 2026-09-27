package com.forge_node.wrapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Client {

    NODE node;
    String name;
    String password;
    String ip;
    int port;
    String address;
    MqttClient client;
    public final Object lock = new Object();

    private boolean control = true;

    public Client(NODE node, String name, String password, String ip, int port, String keystorePath, String keystorePassword){
        this.node = node;
        this.name = name;
        this.password = password;
        this.ip = ip;
        this.port= port;
        this.address = "ssl://" + ip + ":" + Integer.valueOf(port);

        try {
            client = new MqttClient(this.address, this.name, new MemoryPersistence());
            SSLContext context = SSLContext.getInstance("SSL");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(keystorePath),keystorePassword.toCharArray());
            tmf.init(keyStore);
            context.init(null, tmf.getTrustManagers(), new SecureRandom());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setSocketFactory(context.getSocketFactory());
            options.setUserName(this.name);
            options.setPassword(this.password.toCharArray());
            options.setCleanSession(true);

            MqttCallback cb = new MqttCallback() {

                @Override
                public void connectionLost(Throwable arg0) {
                   
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken arg0) {
                    ;
                }

                @Override
                public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
                    String message = new String(arg1.getPayload(), "UTF8");
                    synchronized(lock){
                        node.processInstructions(message);
                    }
                }
                
            };

            client.setCallback(cb);
            client.connect(options);

        } catch (MqttException | NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException | KeyManagementException e) {
            e.printStackTrace();
        }
            
    }

    public void startListening(){
        new Thread(new Runnable() {

            @Override
            public void run() {
                boolean test = false;

                synchronized(lock){
                    test = control;
                }

                try {
                    synchronized(lock){
                       client.subscribe(name, 0); 
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }

                while(test){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    synchronized(lock){
                        test = control;
                    }
                }

                synchronized(lock){
                    try {
                        client.unsubscribe(name);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    } 
                }
            }
            
        }).start();
    }

    public void startPublishing(){
        new Thread(new Runnable() {

            @Override
            public void run() {
                boolean test = false;

                synchronized(lock){
                    test = control;
                }

                while (test) {
                    String text="";
                    synchronized(lock){
                        text= node.getMessage();
                    }
                    MqttMessage message = new MqttMessage(text.getBytes());
                    message.setQos(0);
                    
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    synchronized(lock){
                        try {
                            client.publish(name, message);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        test = control;
                    }
                }
            }
            
        }).start();
    }

    public void setControl(boolean value){
        synchronized(lock){
            this.control = value;
        }
    }

    public boolean getControl(){
        boolean value = false;
        synchronized(lock){
            value = this.control;
        }
        return value;
    }

    /* 
    This list of functions must exist in Client class too, for the sake of syncronization
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

}