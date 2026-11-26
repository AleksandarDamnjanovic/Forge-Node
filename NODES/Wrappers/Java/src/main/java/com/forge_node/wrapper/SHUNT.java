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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

/*
Shunt is a special mode in FN system. It receives instructions through MQTT and then uses only those instructions 
meant for itself, while all other passes towards its bridge and slaves. Communication with bridge is through tcp/ip
but other mediums can be used... like uard or bluetooth.
*/
public class SHUNT {

    private Client client;
    private shuntNode node;
    private int master;
    private int bridge;

    public SHUNT(NODE node, int master, int bridge, int bridgePort){
        this(node, master, bridge, bridgePort, "");
    }

    public SHUNT(NODE node, int master, int bridge, int bridgePort, String aesSecretKey){
        this.node = new shuntNode(node, bridgePort, aesSecretKey);
        this.master = master;
        this.bridge = bridge;
    }

    public void setupClient(String name, String password, String ip, int port, String keystorePath,
            String keystorePassword){
        this.client = new Client(this.node, name, password, ip, port, keystorePath, keystorePassword);
    }

    public Client getClient(){
        return this.client;
    }

    public shuntNode getNode(){
        return this.node;
    }

    public int getMasterIndex(){
        return this.master;
    }

    public int getBridgeIndex(){
        return  this.bridge;
    }

    public class shuntNode extends NODE{

        private List<NODE_INSTRUCTION> instructions;
        private List<NODE_INSTRUCTION> towardsBridge;
        private List<NODE_INSTRUCTION> confirmations;

        private boolean bridgingControl = false;
        private final Object lock = new Object();

        private int bridgePort = 10001;
        private String aesKey = "";

        public shuntNode(NODE node, int bridgePort){
            this(node, bridgePort, "");
        }

        public shuntNode(NODE node, int bridgePort, String aesSecretKey){
            super(node.getNodeIndex(), node.getNumOfSwitches(), node.getNumOfSensors(), node.getNumOfTransmitters(),
            node.getClientNodeSwitches(), node.getClientNodeSensors(), node.getClientNodeTransmitters());
            instructions = new ArrayList<>();
            confirmations = new ArrayList<>();
            towardsBridge = new ArrayList<>();
            this.bridgePort = bridgePort;
            this.aesKey = aesSecretKey == null ? "" : aesSecretKey;
        }

        public void setAesKey(String secretKey){
            synchronized(lock){
                this.aesKey = secretKey == null ? "" : secretKey;
            }
        }

        public String getAesKey(){
            synchronized(lock){
                return this.aesKey;
            }
        }

        public String exchangeBridgePayload(String encryptedIncomingFromBridge){
            String key = getAesKey();
            String decrypted = CRYPTO.decrypt(encryptedIncomingFromBridge, key).trim();
            if(decrypted.startsWith("Cr"))
                appendBridgeConfirmations(decrypted);
            String plainForBridge = generateMessageForTheBridge();
            return CRYPTO.encrypt(plainForBridge, key);
        }

        @Override 
        public String getMessage(){
            String message = super.getMessage();
            message = message.replace("\n", "");
            synchronized(lock){
                for(NODE_INSTRUCTION n : this.confirmations){
                    message = message + "Cr_" + n.getNodeIndex() + "_" + n.getType() + "_" + 
                    n.getElementIndex() + "_";
                    if(n.getType()=='C')
                        message = message + n.getIntValue() + " ";
                    else if (n.getType()=='R')
                        message = message + String.format(java.util.Locale.US, "%.2f", n.getFloatValue()) + " ";
                }
                this.confirmations = new ArrayList<>();
            }
            message = message + "\n";
            return message;
        }

        @Override 
        public void processInstructions(String rawMessage){
            if(rawMessage == null || rawMessage.startsWith("Cr"))
                return;
            this.instructions= NODE_INSTRUCTION.getListOfInstructions(rawMessage);
            synchronized(lock){
                this.towardsBridge.clear();
            }
            for(int i = 0; i < instructions.size(); i++){
                if(this.instructions.get(i).getNodeIndex() != this.getNodeIndex()){
                    synchronized(lock){
                        this.towardsBridge.add(this.instructions.get(i));
                    }
                }else{
                    if(this.instructions.get(i).getType()=='C')
                        this.writeSwitch(this.instructions.get(i).getElementIndex(), (this.instructions.get(i).getIntValue()==1? true : false));
                    else if(this.instructions.get(i).getType()=='T')
                        this.writeTransmitter(this.instructions.get(i).getElementIndex(), this.instructions.get(i).getStringValue());
                }
            }
        }

        public void startBridging(){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ServerSocket server = new ServerSocket(bridgePort);
                        while(getBridgingControl()){
                            
                            try{
                                Socket client = server.accept();
                                client.setSoTimeout(60000);
                                InputStream is = client.getInputStream();
                                OutputStream os = client.getOutputStream();
                                byte data[] = new byte[4096];
                                int bytesRead = is.read(data);
                                String incoming = bytesRead > 0 ? new String(data, 0, bytesRead, "UTF8").trim() : "";

                                String encryptedReply = exchangeBridgePayload(incoming);

                                byte bm[] = encryptedReply.getBytes("UTF8");
                                if(bm.length > 0){
                                    os.write(bm, 0, bm.length);
                                    os.flush();
                                }
                                Thread.sleep(200);
                                
                                os.close();
                                is.close();
                                client.close();
                            }catch(SocketTimeoutException | InterruptedException t){
                                t.printStackTrace();
                            }
                            
                        }

                        server.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    
                }
                
            }).start();
        }

        public String generateMessageForTheBridge(){
            String message = "";
            
            synchronized(lock){
                for(NODE_INSTRUCTION n : this.towardsBridge){
                    message = message + "I_" + n.getNodeIndex() + "_" + n.getType() + "_" + 
                    n.getElementIndex();
                    
                    if(n.getType()!='S')
                        message = message + "_";
                    else
                        message = message + " ";

                    if(n.getType()=='C')
                        message = message + n.getIntValue() + " ";
                    else if (n.getType()=='T')
                        message = message + n.getStringValue() + " ";
                }
                this.towardsBridge.clear();
                message = message + "\n";
            }
           
            return message;
        }

        public void appendBridgeConfirmations(String rawMessage){
            List<NODE_INSTRUCTION>lista = NODE_INSTRUCTION.getListOfInstructions(rawMessage);
            synchronized(lock){
                for(NODE_INSTRUCTION incoming : lista){
                    this.confirmations.removeIf(existing ->
                        existing.getNodeIndex() == incoming.getNodeIndex()
                        && existing.getType() == incoming.getType()
                        && existing.getElementIndex() == incoming.getElementIndex()
                    );
                    this.confirmations.add(incoming);
                }
            }
        }

        public boolean getBridgingControl(){
            boolean toReturn = false;
            synchronized(lock){
                toReturn = this.bridgingControl;
            }
            return toReturn;
        }

        public void setBridgingControl(boolean value){
            synchronized(lock){
                this.bridgingControl = value;
            }
        }

    }

}