package com.forge_node.wrapper;

public class Main {
    public static void main(String[] args) {
       
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