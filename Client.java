import java.io.*;
import java.net.*;
import javax.crypto.SecretKey;
 
public class Client extends Thread {
    
    private Socket           socket;
    private BufferedReader   input;
    private DataInputStream  inputFromServer;
    private DataOutputStream output;
    SecretKey secretKey;
    String msgFromServer;
    public static final String    RED = "\u001B[31m";
    public static final String    BLUE = "\u001B[34m";
    public static final String    RESET = "\u001B[0m";
    Encryptor en;

    
    public Client(String address, int portNum) {
        try {
            socket = new Socket(address, portNum);
            System.out.println("Connected to Server");

            input = new BufferedReader(new InputStreamReader(System.in));
            inputFromServer = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            output = new DataOutputStream(socket.getOutputStream());
            en = new Encryptor();
            secretKey = en.generateKey();
        }
        catch(Exception e) {
            System.out.println(e);
        }

        String msg = "";
        new Thread(new IncomingMessages()).start();
        Encryptor en = new Encryptor();

        while (true) {
            try {    
                msg = input.readLine();
                if (msg.equalsIgnoreCase("bye")) {
                    String response = input.readLine();
                    if (response.equalsIgnoreCase("no")) {
                        break;
                    }
                    output.writeUTF(response);
                }
                else {
                    if (!msgFromServer.contains(RED)) {
                        String encrypted = en.encrypt(msg, secretKey);
                        output.writeUTF(encrypted);
                    }
                    else {
                        output.writeUTF(msg);
                    }
                }
            }
            catch (Exception e) {System.out.println(e);}

        }
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
            try {
                while ((msgFromServer = inputFromServer.readUTF()) != null) {
                    if (msgFromServer.contains(BLUE)) {
                        StringBuilder msg = new StringBuilder("");
                        String[] words = msgFromServer.split("\\s+");
                        int count = 0;
                        for (String word : words) {
                            if (count != 0) {
                                msg.append(" ");
                            }
                            if (!word.contains(BLUE)) {
                                msg.append(word);
                            }
                            count++;
                        }
                        try {
                            String decryptedMessage = en.decrypt(msg.toString(), secretKey);
                            System.out.println(decryptedMessage);
                        }
                        catch (Exception e) {System.out.println(e);}
                    }
                    else {
                        System.out.println(msgFromServer);
                    }
                }
            } catch (IOException e) {
                System.out.println("\nYou have exited the chat application");
            }
        }
    }
}