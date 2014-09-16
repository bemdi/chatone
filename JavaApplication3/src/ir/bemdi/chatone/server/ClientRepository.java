/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.bemdi.chatone.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author b.momeni
 */
public class ClientRepository {

    public ArrayList<ClientConnection> connections;

    HashMap<String, ClientConnection> hashMap;

    public ClientRepository() {
//        this.connections = new ArrayList<>();
        this.hashMap = new HashMap<>();
        this.hashMap.put("AKBAR", null);
        this.hashMap.put("ASGHAR", null);
        this.hashMap.put("ALI_ASGHAR", null);
        this.hashMap.put("GHOLAM", null);
    }

//    private synchronized int addConnection(ClientConnection cc)
//    {
//        
//        this.connections.add(cc);
//        return this.connections.size();
//    }
//    private synchronized int removeConnection(String user,ClientConnection cc)
//    {
//        if(hashMap.containsKey(user))
//        {
//            this.hashMap.remove(user);
//            this.connections.remove(cc);
//        }
//        
//        return this.connections.size();
//    }
    public synchronized boolean linkUserToClientConnection(String user, ClientConnection connection) {
        System.out.println("USER IS: " + user);
        if (!this.hashMap.containsKey(user)) {
            System.out.println("USER NOT FOUND");
            this.hashMap.put(user, connection);
//            this.addConnection(connection);
            return true;
        } else {
            System.out.println("USER FOUND");
            return false;
        }

    }

    public int getConnectedClientsSize() {
        return hashMap.size();
    }

    public synchronized String generateOnlineUsersLine() {
        String line = ClientConnection.COMMAND_SEND_ONLINE_USERS;
        Set<String> users = hashMap.keySet();
        Iterator<String> it = users.iterator();

        while (it.hasNext()) {

            line = line + it.next() + ",";

        }
        System.out.println("ONLINE USERS ARE: " + line);
        return line + "\n";
    }

    public ClientConnection getUsersConnection(String user) {
        if (hashMap.containsKey(user)) {
            return hashMap.get(user);
        } else {
            return null;
        }
    }

    public void removeClientConnection(ClientConnection cc) {
//        Collection<ClientConnection>  cons =  this.hashMap.values();
        this.hashMap.remove(cc.userkey);

//        for (int i = 0; i < cons.size(); i++) {
//            if(cons.remove(cc))
//            {
        ChatOneServer.instance.getConnectedClientsLabel().setText(ChatOneServer.repo.getConnectedClientsSize() + "");
//            }

//        }
        String line = generateOnlineUsersLine();
        Collection<String> users = this.hashMap.keySet();
        Iterator<String> it = users.iterator();

        while (it.hasNext()) {
            try {
                ClientConnection c = this.hashMap.get(it.next());
                if(c!=null)
                    c.send(ChatOneServer.repo.generateOnlineUsersLine());
            } catch (IOException ex) {
                Logger.getLogger(ClientRepository.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

}
