package com.mycompany.chatserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DihCordServer {

    private ServerSocket ss;
    //private ArrayList<Socket> socketList;

    public DihCordServer(int port) throws Exception {
        ss = new ServerSocket(port);
        //Hasmap to match IP (as key) to socket (as value) for routing stuff
        HashMap<String, Socket> ipSocketMap = new HashMap<>();
        //Hashmap to match IP to public key
        HashMap<String, PublicKey> ipKeyMap = new HashMap<>();
        //Hashmap to match IP to name
        HashMap<String, String> ipNameMap = new HashMap<>();

        //Thread that accepts everything and adds it to the arraylist for like reading and stuff
        class AcceptThread extends Thread {

            private ServerSocket ss;
            HashMap<String, Socket> ipSocket;

            public AcceptThread(ServerSocket ss) {
                this.ss = ss;

                //start itself cuz i'll forget to do it myself
                this.start();
            }

            @Override
            //the run method of the thread (idk how i forgot that T-T)
            public void run() {
                while (true) {
                    try {
                        Socket s = ss.accept();

                        DataInputStream is = new DataInputStream(s.getInputStream());

                        //put IP
                        ipSocketMap.put(s.getInetAddress().getHostAddress() + "", s);

                        //put name
                        byte[] username = new byte[30];
                        is.readFully(username);
                        ipNameMap.put(s.getInetAddress().getHostAddress() + "", new String(username, StandardCharsets.UTF_8));

                        //put public key
                        int keyLength = is.readInt();
                        byte[] key = new byte[keyLength];
                        is.readFully(key);
                        PublicKey temp = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
                        ipKeyMap.put(s.getInetAddress().getHostAddress() + "", temp);

                        System.out.println("Address: " + s.getLocalAddress() + "\nName: " + ipNameMap.get(s.getLocalAddress() + "") + "\nKey: " + ipKeyMap.get(s.getLocalAddress() + ""));
                        Thread.sleep(100);
                    } catch (InterruptedException | IOException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
                        Logger.getLogger(DihCordServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        }

        new AcceptThread(ss); //accept sockets, get their Public Key and Username to store to the HashMap

        while (true) {
            for (Socket i : ipSocketMap.values()) {
                if (i.getInputStream().available() > 0) {
                    DataInputStream is = new DataInputStream(i.getInputStream());
                    DataOutputStream os = new DataOutputStream(i.getOutputStream());

                    int messageType = is.readInt();
                    System.out.println("Message Type: " + messageType);
                    if (messageType == 0) {
                        String senderAddress = i.getInetAddress().getHostAddress()+"";
                        String receiverAddress = is.readUTF();
                        int dataLength = is.readInt();
                        byte[] incomingData = new byte[dataLength];
                        is.readFully(incomingData);
                        
                        if (ipSocketMap.containsKey(receiverAddress)) {
                            Socket receiver = ipSocketMap.get(receiverAddress); //socket being sent to
                            DataOutputStream rOS = new DataOutputStream(receiver.getOutputStream()); //get stream of receiver
                            
                            System.out.println("Sending Message to: " + receiverAddress);
                            
                            rOS.writeInt(0); //message type message
                            rOS.writeUTF(senderAddress); //who sent the message
                            rOS.writeInt(incomingData.length); //length of data
                            rOS.write(incomingData); //data
                            rOS.flush();
                        }
                        
                        System.out.println("Incoming Message");
                    } else if (messageType == 1) {
                        String targetAddress = is.readUTF();
                        System.out.println("Requested public key for: " + targetAddress);
                        if (ipKeyMap.containsKey(targetAddress)) {
                            byte[] encodedKey = ipKeyMap.get(targetAddress).getEncoded();
                            os.writeInt(1); //return type is public key
                            os.writeUTF(targetAddress);
                            os.writeInt(encodedKey.length); //length of key
                            os.write(encodedKey);
                            System.out.println("Public key sent");
                        }

                    }
                }
            }
            Thread.sleep(100);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, Exception {

        DihCordServer sss = new DihCordServer(5000);

    }

}
