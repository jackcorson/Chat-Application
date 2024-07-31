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

    public void alertClients(HandleClient newClient) {
        synchronized(clients) {
            for (HandleClient client : clients) {
                if (client != newClient) {
                    try {
                        client.output.writeUTF("a new client has joined, type 'bye' if you would like to see your new chat options!");
                    }
                    catch (Exception e) {System.out.println(e);}
                }
            }
        }
    }

    class HandleClient extends Thread {
        private Socket                socket;
        private DataInputStream       input;
        public DataOutputStream       output;
        private String                name;
        private boolean               availability = false;
    
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
            String [] list = new String[100];
            try {
                output.writeUTF("What is your name?");
                name = input.readUTF();
                clients.add(this);
                new Thread(() -> alertClients(this)).start();
                list = this.addToChat();
                this.output.writeUTF("Begin chatting whenever you like!");
                while (true) {
                    msgReceived = input.readUTF();
                    this.output.writeUTF("(...Sent to ->)");
                    whoSentTo(list, this);
                    System.out.println("Client " + socket.getRemoteSocketAddress() + " (" + name + "): " + msgReceived);
                    if (msgReceived.equalsIgnoreCase("Bye")) {
                        this.output.writeUTF("Would you like to talk to someone else? (yes/no)");
                        String decision = input.readUTF().toString();
                        if (!decision.equalsIgnoreCase("yes")) {
                            break;
                        }
                        else {
                            for (HandleClient client : clients) {
                                client.availability = false;
                            }
                            list = this.addToChat();
                            this.output.writeUTF("Begin chatting!");
                        }
                    }
                    else {
                        for (HandleClient client : clients) {
                            if (client != this && client.availability == true) {
                                client.output.writeUTF("From " + name + ": " + msgReceived);
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

        public String[] addToChat() {
            String[] list = new String[clients.size()];
            try {
                if (clients.size() > 1) { 
                    this.output.writeUTF("Who would you like to talk to (list separated by commas and no spaces)? Your options include...");
                    for (HandleClient client : clients) {
                        if (this != client) {
                            this.output.writeUTF(client.name);
                        }
                    }
                    list = input.readUTF().split(",");
                    for (HandleClient client : clients) {
                        for (String person : list) {
                            if (client.name.equalsIgnoreCase(person)) {
                                client.availability = true;
                            }
                        }
                    }
                }
            }
            catch (Exception e) {System.out.println(e);}

            return list;
        }

        public static void whoSentTo(String[] sendList, HandleClient client) {
            StringBuilder newString = new StringBuilder();
            if (sendList.length > 0 && sendList[0] != null) {
                for (String person : sendList) {
                    if (client.name != person && person != null) {
                        newString.append(person).append(" ");
                    }
                }
                try {
                    client.output.writeUTF(newString.toString());
                }
                catch (Exception e) {System.out.println(e);}
            }
            else {
                try {
                    client.output.writeUTF("Sent to server only");
                }
                catch (Exception e) {System.out.println(e);}
            }
        }
    }
}