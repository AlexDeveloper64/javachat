package com.mycompany.chatserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
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

        //Thread system, 1 thread per socket
        class SocketListener extends Thread {
            private static ArrayList<SocketListener> instances = new ArrayList<>();
            
            public ArrayList<String> newNames = new ArrayList<>();
            private Socket i;

            public SocketListener(Socket i) {
                this.i = i;
                instances.add(this);
            }

            @Override
            public void run() {
                try {
                    DataOutputStream os = new DataOutputStream(i.getOutputStream());
                    DataInputStream is = new DataInputStream(i.getInputStream());

                    //return member list and other stuff later to each socket IF there are any new members
                    new Thread() {
                        @Override
                        public void run() {
                            while (true) {
                                if (!newNames.isEmpty()) {
                                    try {
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

                                        newNames.clear(); //clear so it only updates if the list changes
                                    } catch (SocketException ex) {
                                        return;
                                    } catch (IOException ex) {
                                        Logger.getLogger(DihCordServer.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(DihCordServer.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }.start();

                    while (true) {
                        //check if theres anything to read to avoid blocking the username update.
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
                        Thread.sleep(100);
                    }
                } catch (SocketException | EOFException ex) { //detect when socket disconnects
                    System.out.println("Socket Disconnected!");
                    //remove socket from members list etc.
                    String socketIP = i.getInetAddress().getHostAddress() + ":" + i.getPort();
                    String socketName = ipNameMap.get(socketIP);

                    //remove socket from all arrays etc.
                    ipSocketMap.remove(socketIP);
                    ipKeyMap.remove(socketIP);
                    nameIPMap.remove(socketName);
                    ipNameMap.remove(socketIP);
                    names.remove(socketName);
                    
                    instances.forEach((instance)->{
                        names.forEach((x)->{
                            instance.newNames.add(x);
                        });
                    });
                    try {
                        i.close(); //close socket to free resources
                    } catch (IOException ex1) {
                    }

                    return;
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(DihCordServer.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }

        //Thread that accepts everything and adds it to the arraylist for reading and stuff
        class AcceptThread extends Thread {

            private ServerSocket ss;
            private final ArrayList<SocketListener> socketListeners = new ArrayList<>();

            public AcceptThread(ServerSocket ss) {
                this.ss = ss;
            }

            @Override
            //the run method of the thread
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

                        //Start listening for messages from socket on thread after initial handshake
                        SocketListener sL = new SocketListener(s);
                        socketListeners.add(sL);
                        sL.start();

                        //loops through all listeners, deleting dead ones (that died from socket closure)
                        socketListeners.forEach((listener) -> {
                            if (!listener.isAlive()) {
                                socketListeners.remove(listener);
                            } else {
                                names.forEach((name) -> {
                                    listener.newNames.add(name);
                                });
                            }
                        }); //add all names to new names on EVERY thread                        

                        System.out.println("Address: " + s.getInetAddress().getHostAddress() + ":" + s.getPort() + "\nName: " + usernameUTF + "\nKey: " + ipKeyMap.get(s.getInetAddress().getHostAddress() + ":" + s.getPort() + ""));
                        Thread.sleep(100);
                    } catch (InterruptedException | IOException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
                        Logger.getLogger(DihCordServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        }
        new AcceptThread(ss).start(); //accept sockets, get their Public Key and Username to store to the HashMap
    }

    public static void main(String[] args) throws IOException, InterruptedException, Exception {

        DihCordServer sss = new DihCordServer(5000);

    }

}
