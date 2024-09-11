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
                socket = server.accept(); //Listen for incoming clients
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
        new Server(5060); //Arbitrarily chosen port number
    }

    public void alertClients(HandleClient newClient) { //Wait for change in client listen to notify clients of a new user on server
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
        
        public HandleClient(Socket socket) { //Setup connection between server and client
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
            String [] list = new String[100]; //100 clients maximum
            try {
                output.writeUTF("What is your name?");
                name = input.readUTF();
                synchronized(clients) {
                    clients.add(this);
                }
                new Thread(() -> alertClients(this)).start();
                this.output.writeUTF("Begin chatting whenever you like! (type 'bye' to view chat options)");
                outerloop:
                while (true) {
                    msgReceived = input.readUTF();
                    if (!msgReceived.equalsIgnoreCase("Bye"))
                        whoSentTo(list, this);

                    System.out.println("Client " + socket.getRemoteSocketAddress() + " (" + name + "): " + "Hidden message"); //Don't show message to server
                    if (msgReceived.equalsIgnoreCase("Bye")) {
                        this.output.writeUTF("Would you like to talk to someone else? (yes/no)");
                        boolean decisionSpelling = false;
                        while (decisionSpelling == false) { //Ensure correct spelling
                            String decision = input.readUTF().toString();
                            if (decision.equalsIgnoreCase("no")) {
                                break outerloop;
                            }
                            else if (decision.equalsIgnoreCase("yes")) {
                                list = talkToSomeoneElse(list); //Allows user to talk to new people
                                decisionSpelling = true;
                            }
                            else {
                                output.writeUTF("Type 'yes' or 'no' please");
                                decisionSpelling = false;
                            }
                        }
                    }
                    else {
                        sendToClients(msgReceived); //Deliver message to chosen clients
                    }
                } //Close client connection
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
                synchronized (clients) { //Change user availabilities based on who client chose to talk to
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
                        if (client != this && client.availability == true) {  //Send available clients the message
                            client.output.writeUTF(BLUE + "From " + name + ": " + msgReceived + RESET);
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
                    this.output.writeUTF("Who would you like to talk to (list separated by commas)? Your options include...");
                    for (HandleClient client : clients) {
                        if (this != client) {
                            this.output.writeUTF(client.name);
                        }
                    }
                    boolean cont = true;
                    do {
                        list = input.readUTF().split(",");
                        synchronized (clients) {
                            for (String person : list) {
                                int count = 0;
                                for (HandleClient client : clients) {
                                    if (client.name.equalsIgnoreCase(person.replaceAll("\\s", ""))) {
                                        count++;
                                        client.availability = true;
                                    }
                                }
                                if (count == 0) {
                                    this.output.writeUTF("A name you typed was not found, try again.");
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

        public static void whoSentTo(String[] sendList, HandleClient client) { //Tell user who the message was sent to
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