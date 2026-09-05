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
        //Hashmap to match name to the IP (and port)
        HashMap<String, String> nameIPMap = new HashMap<>();
        //Hashmap to match IP to name (like the opposite of the above one)
        HashMap<String, String> ipNameMap = new HashMap<>();
        //member list with index to return to thing + one to return only new members
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> newNames = new ArrayList<>();

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
                        DataOutputStream os = new DataOutputStream(s.getOutputStream());

                        //put IP
                        ipSocketMap.put(s.getInetAddress().getHostAddress() + ":" + s.getPort() + "", s);

                        //put name
                        byte[] username = new byte[30];
                        is.readFully(username);
                        String usernameUTF = new String(username, StandardCharsets.UTF_8).replace("\0", "");
                        nameIPMap.put(usernameUTF, s.getInetAddress().getHostAddress() + ":" + s.getPort() + ""); //both so you can get
                        ipNameMap.put(s.getInetAddress().getHostAddress() + ":" + s.getPort(), usernameUTF);//ip from name and name from ip
                        names.add(usernameUTF);//add to names
                        newNames.add(usernameUTF);//add to new names

                        //put public key
                        int keyLength = is.readInt();
                        byte[] key = new byte[keyLength];
                        is.readFully(key);
                        PublicKey temp = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
                        ipKeyMap.put(s.getInetAddress().getHostAddress() + ":" + s.getPort() + "", temp);

                        //return member list and other stuff later
                        os.writeInt(2);
                        os.writeUTF("SERVER");
                        StringBuilder sb = new StringBuilder();
                        //make list seperated by ,
                        for (int i = 0; i < names.size(); i++) {
                            sb.append(names.get(i));
                            if (i < names.size()) {
                                sb.append(",");
                            }
                        }
                        byte[] ret = sb.toString().getBytes(StandardCharsets.UTF_8);

                        os.writeInt(ret.length);
                        os.write(ret);

                        os.flush();

                        System.out.println("Address: " + s.getInetAddress().getHostAddress() + ":" + s.getPort() + "\nName: " + usernameUTF + "\nKey: " + ipKeyMap.get(s.getInetAddress().getHostAddress() + ":" + s.getPort() + ""));
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
                DataOutputStream os = new DataOutputStream(i.getOutputStream());
                if (i.getInputStream().available() > 0) {
                    DataInputStream is = new DataInputStream(i.getInputStream());

                    int messageType = is.readInt();
                    System.out.println("Message Type: " + messageType);
                    if (messageType == 0) {
                        String senderName = ipNameMap.get(i.getInetAddress().getHostAddress() + ":" + i.getPort() + ""); //person who sent message
                        String receiverName = is.readUTF(); //person who will be receiving message
                        String receiverAddress = nameIPMap.get(receiverName); //convert name to address (and port ffs)

                        int dataLength = is.readInt();
                        byte[] incomingData = new byte[dataLength];
                        is.readFully(incomingData);

                        if (ipSocketMap.containsKey(receiverAddress)) {
                            Socket receiver = ipSocketMap.get(receiverAddress); //socket being sent to
                            DataOutputStream rOS = new DataOutputStream(receiver.getOutputStream()); //get stream of receiver

                            System.out.println("Sending Message to: " + receiverAddress);

                            rOS.writeInt(0); //message type message
                            rOS.writeUTF(senderName); //who sent the message
                            rOS.writeInt(incomingData.length); //length of data
                            rOS.write(incomingData); //data
                            rOS.flush();
                        }

                        System.out.println("Incoming Message");
                    } else if (messageType == 1) {
                        String targetName = is.readUTF();
                        String targetAddress = nameIPMap.get(targetName);
                        System.out.println("Requested public key for: " + targetName);

                        if (ipKeyMap.containsKey(targetAddress)) {
                            byte[] encodedKey = ipKeyMap.get(targetAddress).getEncoded();
                            os.writeInt(1); //return type is public key
                            os.writeUTF(targetName);
                            os.writeInt(encodedKey.length); //length of key
                            os.write(encodedKey);
                            System.out.println("Public key sent");
                        }

                    }
                }
                //return member list and other stuff later to each socket IF there are any new members
                if (!newNames.isEmpty()) {
                    os.writeInt(2);
                    os.writeUTF("SERVER");
                    StringBuilder sb = new StringBuilder();
                    //make list seperated by ,
                    for (int ii = 0; ii < names.size(); ii++) {
                        sb.append(names.get(ii));
                        if (ii < names.size()) {
                            sb.append(",");
                        }
                    }
                    byte[] ret = sb.toString().getBytes(StandardCharsets.UTF_8);

                    os.writeInt(ret.length);
                    os.write(ret);

                    os.flush();
                }
            }
            newNames.clear();
            Thread.sleep(100);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, Exception {

        DihCordServer sss = new DihCordServer(5000);

    }

}
