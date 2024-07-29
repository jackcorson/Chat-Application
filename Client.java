import java.io.*;
import java.net.*;
 
public class Client extends Thread {
    
    private Socket socket;
    private BufferedReader input;
    private DataInputStream inputFromServer;
    private DataOutputStream output;
    
    public Client(String address, int portNum) {
        try {
            socket = new Socket(address, portNum);
            System.out.println("Connected to Server");

            input = new BufferedReader(new InputStreamReader(System.in));
            inputFromServer = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            output = new DataOutputStream(socket.getOutputStream());
        }
        catch(Exception e) {
            System.out.println("a"+e);
        }

        String msg = "";
        new Thread(new IncomingMessages()).start();

        while (true) {
            try {    
                msg = input.readLine();
                output.writeUTF(msg);
                if (msg.equalsIgnoreCase("bye")) {
                    String response = input.readLine();
                    if (response.equalsIgnoreCase("no")) {
                        break;
                    }
                    output.writeUTF(response);
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
            System.out.println("d"+e);
        }
    }

 
    public static void main(String args[])
    {
        Client client = new Client("127.0.0.1", 5060);
    }

    private class IncomingMessages implements Runnable {
        @Override
        public void run() {
            String msgFromServer;
            try {
                while ((msgFromServer = inputFromServer.readUTF()) != null) {
                    System.out.println(msgFromServer);
                }
            } catch (IOException e) {
                System.out.println("Error reading from server: " + e.getMessage());
            }
        }
    }
}