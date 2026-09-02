package com.mycompany.chatserver;
//saving to onedrive!!

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class DihCordClient {

    private KeyPair keypair;
    private Socket s;
    //private HashMap<String, String> messageList = new HashMap<>(); //address and message
    private ArrayList<String> messageDataList = new ArrayList<>();
    private ArrayList<String> messageIPList = new ArrayList<>();
    
    private HashMap<String, PublicKey> publicKeys = new HashMap<>(); //address and public key

    private HybridEncryptionUtil heu = new HybridEncryptionUtil();

    public DihCordClient(int port, String address) throws IOException, InterruptedException, NoSuchAlgorithmException, Exception {
        //make 3072 bit RSA key pair for stuff (cuz apparently that gives 128 bits of security which matches the 256 bit
        //AES moree.
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
        keygen.initialize(3072);
        keypair = keygen.generateKeyPair();

        //Make username (up to 8 bytes) using tuff predicates :3
        String username = JOptionPane.showInputDialog("Enter Username\n[A-z],[_-] MAX 30 CHARACTERS");
        while (!username.matches("^[A-Za-z0-9_ -]+$") || username.length() > 30) {
            username = JOptionPane.showInputDialog("Invalid Username, try again\n[A-z],[_- ] MAX 30 CHARACTERS");
        }

        //Convert name to bytes (and pad to 8 bytes if too short)
        byte[] user_bytes = username.getBytes(StandardCharsets.UTF_8);
        user_bytes = Arrays.copyOf(user_bytes, 30);

        //Getting public key bytes
        byte[] public_key_bytes = keypair.getPublic().getEncoded();

        //Writing Connection Data
        s = new Socket(address, port);
        DataOutputStream os = new DataOutputStream(s.getOutputStream());
        DataInputStream is = new DataInputStream(s.getInputStream());

        //write name
        os.write(user_bytes);
        //write key length
        os.writeInt(public_key_bytes.length);
        //write key
        os.write(public_key_bytes);

        os.flush();

        class MessageListener extends Thread {

            @Override
            public void run() {
                while (true) {
                    try {
                        if (is.available() <= 0) { //skip if theres no message
                            continue;
                        }

                        int messageType = is.readInt();
                        String senderAddress = is.readUTF();

                        System.out.println("Message Type: " + messageType + " Sender Address: " + senderAddress);

                        if (messageType == 1) { //public key recieved.
                            int keyLength = is.readInt();
                            byte[] keyBytes = new byte[keyLength];
                            is.readFully(keyBytes);
                            PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
                            publicKeys.put(senderAddress, key);
                        } else if (messageType == 0) {
                            int dataLength = is.readInt();
                            byte[] data = new byte[dataLength];
                            is.readFully(data);

                            String result = heu.decrypt(keypair.getPrivate(), heu.createPackage(data));
                            //messageList.put(senderAddress, result); not used
                            messageIPList.add(senderAddress);
                            messageDataList.add(result);
                        }

                    } catch (IOException | InterruptedException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
                        Logger.getLogger(DihCordClient.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
                        Logger.getLogger(DihCordClient.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

        new MessageListener().start();//start listener thread

        //makeMessage(s.getInetAddress() + "", "I'm going to collar you and walk you around");
    }

    //add code to get username too
    public PublicKey requestPublicKey(String address) throws IOException, InterruptedException {
        if (publicKeys.containsKey(address)) { //check if public key is saved first
            return publicKeys.get(address);
        }

        DataOutputStream os = new DataOutputStream(s.getOutputStream());

        os.writeInt(1); //type 1 for public key request
        os.writeUTF(address); //address matched to public key
        os.flush();

        while (!publicKeys.containsKey(address)) {
            //System.out.println("Still Waiting for key...");
            //System.out.println(publicKeys.toString());
            Thread.sleep(100);
        } //block until public key is recieved

        System.out.println("Public Key Fetched Succesfully");
        return publicKeys.get(address);
    }

    public void makeMessage(String address, String message) throws IOException, Exception {
        DataOutputStream os = new DataOutputStream(s.getOutputStream());
        System.out.println("Message Created!");
        byte[] encryptedData = heu.encrypt(requestPublicKey(address), message.getBytes(StandardCharsets.UTF_8)).pack();

        os.writeInt(0); //type 0 for message
        os.writeUTF(address);
        os.writeInt(encryptedData.length);
        os.write(encryptedData);
    }
    
    public ArrayList[] getMessages(){
        ArrayList[] messageList = {messageIPList,messageDataList};
        return messageList;
    }

    public Socket getSocket() {
        return s;
    }
    
    public static void main(String[] args) throws Exception {
        DihCordClient c = new DihCordClient(5000, "127.0.0.1");
    }

}
