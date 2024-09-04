import java.io.*;
import java.net.*;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;
 
public class Client extends Thread {
    
    private Socket           socket;
    private BufferedReader   input;
    private DataInputStream  inputFromServer;
    private DataOutputStream output;
    private PublicKey        publicKey;
    private ArrayList<String> list = new ArrayList<>();
    public static final String    BLUE = "\u001B[34m";
    Encryptor rsa;
    File file;
    Scanner myReader;
    
    public Client(String address, int portNum) {
        try {
            socket = new Socket(address, portNum);
            System.out.println("Connected to Server");
            file = new File("publicKeys.txt");
            myReader = new Scanner(file);

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
                        String whoTalkTo = input.readLine();
                        output.writeUTF(whoTalkTo);
                        whoTalkTo.replaceAll("\\s", "");
                        String[] who = whoTalkTo.split(",");
                        list.clear();
                        for (int i = 0; i < who.length; i++) {
                            list.add(who[i]);
                        }
                    }
                    else {
                        if (list.size() > 0) {
                            while (myReader.hasNextLine()) {
                                String[] nameAndKey = myReader.nextLine().split(" ");
                                for (int i = 0; i < list.size(); i++) {
                                    System.out.println("hi");
                                    if (nameAndKey[0].equals(list.get(i))) {
                                        System.out.println("hi");
                                        output.writeUTF(nameAndKey[0] + " " + rsa.encrypt(msg, decodePublicKey(nameAndKey[1])));
                                        break;
                                    }
                                }
                            }
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
        if (args.length == 1) {
            Client client = new Client(args[0], 5060);
        }
        else {
            throw new IllegalArgumentException("Please provide one argument which is an IP Address.");
        }
    }

    private class IncomingMessages implements Runnable {
        @Override
        public void run() {
            String msgFromServer;
            try {
                while ((msgFromServer = inputFromServer.readUTF()) != null) {
                    if (msgFromServer.contains("a new client has joined, type 'bye' if you would like to see your new chat options!")) {
                        System.out.println(msgFromServer);
                    }
                    else if (msgFromServer.contains("From")){
                        String[] words = msgFromServer.split("\\s+");
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