import java.io.*;
import java.net.*;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
 
public class Client extends Thread {
    
    private Socket           socket;
    private BufferedReader   input;
    private DataInputStream  inputFromServer;
    private DataOutputStream output;
    private PublicKey        publicKey;
    private HashMap<String, PublicKey> pubKeyCollection = new HashMap<>();
    public static final String    BLUE = "\u001B[34m";
    public static final String    RED = "\u001B[31m";
    Encryptor rsa;
    boolean choosingName = false;
    
    public Client(String address, int portNum) {
        try {
            socket = new Socket(address, portNum);
            System.out.println("Connected to Server");

            input = new BufferedReader(new InputStreamReader(System.in));
            inputFromServer = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            output = new DataOutputStream(socket.getOutputStream());
        }
        catch(Exception e) {
            System.out.println(e);
        }

        String msg = "";
        new Thread(new IncomingMessages()).start();
        try {
            rsa = new Encryptor();
            publicKey = rsa.getPublicKey();
            String encodedKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            output.writeUTF(encodedKey);
            msg = input.readLine();
            output.writeUTF(msg);
            while (true) {
                try {    
                    msg = input.readLine();
                    if (msg.equalsIgnoreCase("bye")) {
                        output.writeUTF(msg);
                        String response = input.readLine();
                        if (response.equalsIgnoreCase("no")) {
                            break;
                        }
                        output.writeUTF(response);
                    }
                    else {
                        if (pubKeyCollection.size() > 0) {
                            StringBuilder messages = new StringBuilder();
                            for (HashMap.Entry<String, PublicKey> entry : pubKeyCollection.entrySet()) {
                                messages.append(entry.getKey() + " " + rsa.encrypt(msg, entry.getValue()) + "\n");
                            }
                            output.writeUTF(messages.toString());
                        }
                        else if (choosingName = true) {
                            output.writeUTF(msg);
                            choosingName = false;
                        }
                        else {
                            output.writeUTF(rsa.encrypt(msg, publicKey));
                        }
                    }
                }
                catch (Exception e) {System.out.println(e);}

            }
        }
        catch (Exception e) {System.out.println(e);}

        try {
            socket.close();
            input.close();
            output.close();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String args[]) throws IllegalArgumentException {
        Client client = new Client("127.0.0.1", 5060); 
    }

    private class IncomingMessages implements Runnable {
        @Override
        public void run() {
            String msgFromServer;
            try {
                while ((msgFromServer = inputFromServer.readUTF()) != null) {
                    if (msgFromServer.contains("Who would you like to talk to (list separated by commas)? Your options include...")) {
                        System.out.println(msgFromServer);
                        choosingName = true;
                    }
                    else if (msgFromServer.contains(BLUE)){
                        String[] words = msgFromServer.split("\\s");
                        String name = "";
                        String message = "";
                        for (int i = 0; i < words.length; i++) {
                            if (words[i].contains(BLUE)) {
                                name += words[i];
                            }
                            else {
                                message += words[i];
                            }
                        }
                        System.out.println(name + rsa.decrypt(message));
                    }
                    else if (msgFromServer.contains(RED)) {
                        String[] namesAndKeys = msgFromServer.split(" ");
                        pubKeyCollection.clear();
                        for (int i = 1; i < namesAndKeys.length - 1; i += 2) {
                            pubKeyCollection.put(namesAndKeys[i], decodePublicKey(namesAndKeys[i + 1]));
                        }
                    }
                    else {
                        System.out.println(msgFromServer);
                    }
                }
            } catch (IOException e) {
                System.out.println("\nYou have exited the chat application");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private PublicKey decodePublicKey(String encodedKey) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }
}