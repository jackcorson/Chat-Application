import java.net.*;
import java.util.*;
import java.io.*;

public class Server {
    private ServerSocket            server;
    private Socket                  socket;
    private ArrayList<HandleClient> clients = new ArrayList<HandleClient>();
    
    public Server(int portNum) {
        try {
            server = new ServerSocket(portNum);
            System.out.println("Server created, waiting for a connection...");

            while (true) {
                socket = server.accept();
                System.err.println(socket.getRemoteSocketAddress() + " Connected!");
                HandleClient client = new HandleClient(socket);
                clients.add(client);
                client.start();
            }
        }
        catch (Exception e) {
            System.out.println("e"+e);
        }
    }
 
    public static void main(String args[])
    {
        Server server = new Server(5060);
    }

    class HandleClient extends Thread {
        private Socket           socket;
        private DataInputStream  input;
        private DataOutputStream output;
    
        public HandleClient(Socket socket) {
            try {
                this.socket = socket;
                input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                output = new DataOutputStream(socket.getOutputStream());
            }
            catch (Exception e) {
                System.out.println("f"+e);
            }
        }
    
        public void run() {
            String msgReceived = "";
            try {
                while (true) {
                    msgReceived = input.readUTF();
                    System.out.println("Client " + socket.getRemoteSocketAddress() + ": " + msgReceived);
                    if (msgReceived.equalsIgnoreCase("Bye")) {
                        this.output.writeUTF("Would you like to talk to someone else? (yes/no)");
                        String decision = input.readUTF().toString();
                        if (!decision.equalsIgnoreCase("yes")) {
                            break;
                        }
                        System.out.println("Client " + socket.getRemoteSocketAddress() + ": " + decision);
                    }
                    else {
                        for (HandleClient client : clients) {
                            if (client != this) {
                                client.output.writeUTF("From " + socket.getRemoteSocketAddress().toString() + ": " + msgReceived);
                                client.output.flush();
                            }
                        }
                    }
                }

                input.close();
                socket.close();
                output.close();
            } 
            catch (IOException e) {
                System.out.println(socket.getRemoteSocketAddress().toString()+" has exited the conversation.");
            }
            clients.remove(this);
        }
    }
}