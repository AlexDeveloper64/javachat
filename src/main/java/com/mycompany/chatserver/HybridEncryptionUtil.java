package com.mycompany.chatserver;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class HybridEncryptionUtil {

    private static final String rsaTransform = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String aesTransform = "AES/GCM/NoPadding";
    
    public String getRSATransform(){
        return rsaTransform;
    }
    
    public String getAESTransform(){
        return aesTransform;
    }
    
    public class Pckg {

        public byte[] aesKey;
        public byte[] iv;
        public byte[] data;

        //Constructor for byte versions
        public Pckg(byte[] aesKey, byte[] iv, byte[] data) {
            this.aesKey = aesKey;
            this.iv = iv;
            this.data = data;
        }

        //Constructor for raw data (will split into stuff)
        public Pckg(byte[] rawBytes) {
            int arr = 0;
            //Make lengths to split raw to arrays later
            int aesLength = 0;
            int ivLength = 0;
            int dataLength = 0;

            for (int i = 0; i < rawBytes.length; i++) {
                switch (arr) {
                    case 0 -> {
                        if (rawBytes[i] != (byte) ',') {
                            aesLength++;
                        } else {
                            arr++;
                        }
                    } //xxxx,xxxx,xxxx -> length will be position of comma each time.
                    case 1 -> {
                        if (rawBytes[i] != (byte) ',') {
                            ivLength++;
                        } else {
                            arr++;
                        }
                    }
                    case 2 -> {
                        if (rawBytes[i] != (byte) ',') {
                            dataLength++;
                        } else {
                            arr++;
                        }
                    }
                    default -> {
                        break;
                    }
                }//end switch
            }//end for loop
            
            aesKey = Arrays.copyOfRange(rawBytes, 0, aesLength);
            iv = Arrays.copyOfRange(rawBytes, aesLength+1, aesLength+ivLength+1);
            data = Arrays.copyOfRange(rawBytes, aesLength+ivLength+2, rawBytes.length);
            
          
        }

        //make into one byte array seperated by ,
        public byte[] pack() {
            byte delimiter = (byte) ',';

            byte[] result = new byte[aesKey.length + iv.length + data.length + 2];

            System.arraycopy(aesKey, 0, result, 0, aesKey.length);
            result[aesKey.length] = delimiter;
            System.arraycopy(iv, 0, result, aesKey.length + 1, iv.length);
            result[aesKey.length + iv.length + 1] = delimiter;
            System.arraycopy(data, 0, result, aesKey.length + iv.length + 2, data.length);

            return result;
        }

        public byte[] decKey() {
            return Base64.getDecoder().decode(aesKey);
        }

        public byte[] decIV() {
            return Base64.getDecoder().decode(iv);
        }

        public byte[] decData() {
            return Base64.getDecoder().decode(data);
        }
    }

    public Pckg createPackage(byte[] aesKey, byte[] iv, byte[] data){
        return new Pckg(aesKey,iv,data);
    }
    
    public Pckg createPackage(byte[] raw){
        return new Pckg(raw);
    }

    //Make AES Key and encrypt it with RSA
    public Pckg encrypt(PublicKey rsaKey, byte[] data) throws Exception {

        //Make key (256 cuz idk)
        KeyGenerator aesKeyGen = KeyGenerator.getInstance("AES");
        aesKeyGen.init(256);
        SecretKey aesKey = aesKeyGen.generateKey();

        //Make IV
        byte[] iv = new byte[12];
        SecureRandom.getInstanceStrong().nextBytes(iv);
        byte[] iv64 = Base64.getEncoder().encode(iv);

        //Make GCM Parameters
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

        //Cipher
        Cipher aesCipher = Cipher.getInstance(aesTransform);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey,gcmSpec);
        byte[] encryptedData = aesCipher.doFinal(data);
        byte[] encryptedData64 = Base64.getEncoder().encode(encryptedData);

        //encrypt aesKey
        byte[] aesKeyEncoded = aesKey.getEncoded();
        PublicKey publicKey = rsaKey;

        Cipher rsaCipher = Cipher.getInstance(rsaTransform);
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedAesKey = rsaCipher.doFinal(aesKeyEncoded);
        byte[] encryptedAesKey64 = Base64.getEncoder().encode(encryptedAesKey);

        Pckg pckg = new Pckg(encryptedAesKey64, iv64, encryptedData64);

        return pckg;
    }

    //do the opposite of what i just said the other thing did
    public String decrypt(PrivateKey rsaKey, Pckg securePackage) throws Exception {
        byte[] encryptedAesKey = securePackage.decKey();
        byte[] iv = securePackage.decIV();
        byte[] encryptedData = securePackage.decData();
        
        //RSA cipher to decrupt AES key
        Cipher rsaCipher = Cipher.getInstance(rsaTransform);
        rsaCipher.init(Cipher.DECRYPT_MODE, rsaKey);
        SecretKey aesKey = new SecretKeySpec(rsaCipher.doFinal(encryptedAesKey),"AES");
        
        //Decrypt data with AES key
        GCMParameterSpec gcm = new GCMParameterSpec(128,iv);
        
        Cipher aesCipher = Cipher.getInstance(aesTransform);
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcm);
        byte[] dataBytes = aesCipher.doFinal(encryptedData);
        
        return new String(dataBytes, StandardCharsets.UTF_8);
    }

}
