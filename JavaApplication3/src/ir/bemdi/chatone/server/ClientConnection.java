/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.bemdi.chatone.server;

import ir.bemdi.chatone.server.DiscoveryMulticastListener.A;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author b.momeni
 */
public class ClientConnection extends Thread {

    private static final String COMMAND_INTRODUCE_USER = "00";
    private static final String COMMAND_CHAT_MESSAGE = "01";
    public static final String COMMAND_SEND_ONLINE_USERS = "02";
    boolean afterSleep = false;

    Socket socket;
    BlockingQueue<String> queue;
    BufferedReader br;
    ClientConnectionReceiverWorker child;
    boolean cw;

    public String userkey = "";

    public ClientConnection(A client) {

        System.out.println("Connecting to a new client at " + client.address.toString());
        try {
            queue = new ArrayBlockingQueue<>(16, true);
            socket = new Socket(client.address, 5225);
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            this.cw = true;

            this.child = new ClientConnectionReceiverWorker(this);
            this.child.start();

        } catch (IOException ex) {
            Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void run() {

        while (cw) {

            try {
                String line = queue.take();
                System.out.println("Line Taken: " + line);
                handleInput(line);

                if (!afterSleep) {
                    sleep(10000);
                    ChatOneServer.repo.hashMap.remove("ASGHAR");
                    ChatOneServer.repo.hashMap.remove("ALI_ASGHAR");
                    afterSleep = true;
                    this.send(ChatOneServer.repo.generateOnlineUsersLine());
                }
            } catch (InterruptedException ex) {
                System.out.println("Client Connection from " + socket.getInetAddress() + " Interrupted.");
            } catch (IOException ex) {
                Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void lineReceived(String line) {
        queue.offer(line);
    }

    public static class ClientConnectionReceiverWorker extends Thread {

        ClientConnection connection;
        boolean valid;

        public ClientConnectionReceiverWorker(ClientConnection connection) {
            this.connection = connection;
            this.valid = true;
        }

        @Override
        public void run() {
            while (valid) {
                try {
                    String line = connection.br.readLine();
                    if (line != null) {
                        System.out.println("Received line: " + line);
                        connection.lineReceived(line);
                    }

                } catch (SocketException ex) {
                    connection.cw = false;
                    this.valid = false;
                    connection.interrupt();
                    this.connection.interrupt();
                    ChatOneServer.repo.removeClientConnection(connection);
                } catch (IOException ex) {
                    connection.cw = false;
                    this.valid = false;
                    connection.interrupt();
                    this.connection.interrupt();
                    ChatOneServer.repo.removeClientConnection(connection);
                }
            }
        }
    }

    public void send(String line) throws IOException {
        socket.getOutputStream().write(line.getBytes());
        socket.getOutputStream().flush();
    }

    public void handleInput(String line) {
        /**
         * USER INTRODUCTION
         */

        System.out.println("User sent: " + line);

        String command = line.substring(0, 2);
        String paramPayload = line.substring(2);

        switch (command) {
            case COMMAND_INTRODUCE_USER:
                handleUserIndroduction(paramPayload);
                break;
            case COMMAND_CHAT_MESSAGE:
                String[] recipients = (paramPayload.substring(0, paramPayload.indexOf(":"))).split(",");
                String message = paramPayload.substring(paramPayload.indexOf(":"));
                handleDeliverChatMessage(recipients, message);
                break;
        }
    }

    private void handleUserIndroduction(String user) {

        if (!ChatOneServer.repo.linkUserToClientConnection(user, this)) {
            try {
                send("$$@@%%USERNAME_IN_USE\n");

            } catch (IOException ex) {
                Logger.getLogger(ClientConnection.class
                        .getName()).log(Level.SEVERE, null, ex);

                ChatOneServer.repo.removeClientConnection(
                        this);

                this.cw = false;

                this.child.valid = false;

                this.child.interrupt();

                this.interrupt();

            }
        } else {
            try {
                send(ChatOneServer.repo.generateOnlineUsersLine());
                ChatOneServer.instance.getConnectedClientsLabel().setText(ChatOneServer.repo.getConnectedClientsSize() + "");
                this.userkey = user;

            } catch (IOException ex) {
                Logger.getLogger(ClientConnection.class
                        .getName()).log(Level.SEVERE, null, ex);

                this.cw = false;

                this.child.valid = false;

                this.child.interrupt();

                this.interrupt();

                ChatOneServer.repo.removeClientConnection(
                        this);
            }
        }
    }

    private void handleDeliverChatMessage(String[] recipients, String message) {

        for (int i = 0; i < recipients.length; i++) {
            String key = recipients[i];

            ClientConnection c = ChatOneServer.repo.hashMap.get(key);

            try {
                c.send(message);
            } catch (IOException ex) {
                ChatOneServer.repo.removeClientConnection(c);
            }

        }
    }
}
