/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.bemdi.chatone.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 *
 * @author b.momeni
 */
public class DiscoveryMulticastListener extends Thread {

//    MulticastSocket multicastSocket;
    ReceiverWorker child;
    public boolean continueWorking;
    BlockingQueue<A> queue;

    DatagramSocket dgSocket;

    public DiscoveryMulticastListener() {
        try {
            // IP is 203.0.113.0
            // PORT 5222
            this.dgSocket = new DatagramSocket(5222);
            this.continueWorking = true;
            this.child = new ReceiverWorker(this);
            queue = new ArrayBlockingQueue<>(1024, true);
            this.child.start();

        } catch (SocketException ex) {
            System.out.println("NO");
        }

    }

    @Override
    public void run() {

        while (continueWorking) {

            try {
                A a = queue.take();
                System.out.println("RECEIVED " + a.toString());
                ClientConnection cc = new ClientConnection(a);
                cc.start();
            } catch (InterruptedException ex) {

                this.dgSocket.close();
                this.continueWorking = false;
                this.child.valid = false;
                this.child.interrupt();
                this.interrupt();
            }

        }

    }

    public void multicastReceived(A a) {
        queue.offer(a);
    }

    public static class ReceiverWorker extends Thread {

        DiscoveryMulticastListener parent;
        public boolean valid;

        public byte[] buffer;

        public ReceiverWorker(DiscoveryMulticastListener parent) {
            this.parent = parent;
            this.valid = true;
            this.buffer = new byte[1024];

        }

        @Override
        public void run() {
            while (valid) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    parent.dgSocket.receive(packet);
                    parent.multicastReceived(new A(packet.getAddress(), packet.getData()));
                } catch (SocketException ex) {
                    this.valid = false;
                    this.parent.continueWorking = false;
                    this.parent.interrupt();
                    this.interrupt();
                } catch (IOException ex) {
                    this.valid = false;
                    this.parent.continueWorking = false;
                    this.parent.interrupt();
                    this.interrupt();
                }
            }
        }

    }

    public static class A {

        public InetAddress address;
        public byte[] message;

        public A(InetAddress a, byte[] message) {
            this.address = a;
            this.message = message;
        }

        @Override
        public String toString() {
            return "[" + address.toString() + "," + new String(this.message).trim() + "]";
        }

    }

}
