import java.net.*;
import java.util.*;
import java.io.*;

public class Server {
    private ServerSocket            server;
    private Socket                  socket;
    private ArrayList<HandleClient> clients = new ArrayList<HandleClient>();
    public static final String    RED = "\u001B[31m";
    public static final String    RESET = "\u001B[0m";
    
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
            System.out.println(e);
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
        public static final String    RESET = "\u001B[0m";
        private static final String   GREEN = "\u001B[32m";
        public static final String    BLUE = "\u001B[34m";

        
    
        public HandleClient(Socket socket) {
            try {
                this.socket = socket;
                input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                output = new DataOutputStream(socket.getOutputStream());
            }
            catch (Exception e) {
                System.out.println(e);
            }
        }
    
        public void run() {
            String msgReceived = "";
            String [] list = new String[100];
            try {
                output.writeUTF(RED + "What is your name?" + RESET);
                name = input.readUTF();
                synchronized(clients) {
                    clients.add(this);
                }
                new Thread(() -> alertClients(this)).start();
                list = this.addToChat(true);
                this.output.writeUTF("Begin chatting whenever you like! (type 'bye' to view chat options)");
                while (true) {
                    msgReceived = input.readUTF();

                    if (!msgReceived.equalsIgnoreCase("Bye"))
                        whoSentTo(list, this);
                        
                    System.out.println("Client " + socket.getRemoteSocketAddress() + " (" + name + "): " + msgReceived);
                    if (msgReceived.equalsIgnoreCase("Bye")) {
                        this.output.writeUTF(RED + "Would you like to talk to someone else? (yes/no)" + RESET);
                        String decision = input.readUTF().toString();
                        if (!decision.equalsIgnoreCase("yes")) {
                            break;
                        }
                        else {
                            list = talkToSomeoneElse(list);
                        }
                    }
                    else {
                        sendToClients(msgReceived);
                    }
                }
                input.close();
                socket.close();
                output.close();
            } 
            catch (IOException e) {
                System.out.println(socket.getRemoteSocketAddress().toString() + " has exited the conversation.");
            }
            clients.remove(this);
        }

        public String[] talkToSomeoneElse(String[] list) {
            try {
                synchronized (clients) {
                    for (HandleClient client : clients) {
                        if (client != this) {
                            client.availability = false;
                        }
                    }
                }
                list = this.addToChat(false);
                this.output.writeUTF("Begin chatting!");
            }
            catch (Exception e) {System.out.println(e);}

            return list;
        }

        public void sendToClients(String msgReceived) {
            try {
                synchronized (clients) {
                    for (HandleClient client : clients) {
                        if (client != this && client.availability == true) { 
                            client.output.writeUTF(BLUE + "From " + name + ": " + RESET + msgReceived);
                            client.output.flush();
                        }
                    }
                }
            }
            catch (Exception e) {System.out.println(e);}
        }

        public String[] addToChat(boolean userCreatingName) {
            String[] list = new String[clients.size()];
            try {
                if (clients.size() > 1) { 
                    this.output.writeUTF(RED + "Who would you like to talk to (list separated by commas)? Your options include..." + RESET);
                    for (HandleClient client : clients) {
                        if (this != client) {
                            this.output.writeUTF(RED + client.name + RESET);
                        }
                    }
                    boolean cont = true;
                    do {
                        list = input.readUTF().split(",");
                        synchronized (clients) {
                            for (HandleClient client : clients) {
                                int count = 0;
                                for (String person : list) {
                                    if (client.name.equalsIgnoreCase(person.replaceAll("\\s", ""))) {
                                        count++;
                                        client.availability = true;
                                    }
                                }
                                if (count == 0 && client != this) {
                                    this.output.writeUTF(RED + "A name you typed was not found, try again." + RESET);
                                    cont = false;
                                    break;
                                }
                                else {
                                    cont = true;
                                }
                            }
                        }
                    } while (cont == false);
                }
                else if (userCreatingName == false) {
                    this.output.writeUTF("There is nobody to talk to at this time :(");
                }
            }
            catch (Exception e) {System.out.println(e);}

            return list;
        }

        public static void whoSentTo(String[] sendList, HandleClient client) {
            StringBuilder newString = new StringBuilder("Sent to --> ");
            if (sendList.length > 0 && sendList[0] != null) {
                for (String person : sendList) {
                    if (client.name != person && person != null) {
                        newString.append(person.replaceAll("\\s", "")).append(", ");
                    }
                }
                try {
                    client.output.writeUTF(GREEN + newString.substring(0, newString.length() - 2).toString() + RESET);
                }
                catch (Exception e) {System.out.println(e);}
            }
            else {
                try {
                    client.output.writeUTF(GREEN + "Sent to server only" + RESET);
                }
                catch (Exception e) {System.out.println(e);}
            }
        }
    }
}