package com.in5020.group4;

import spread.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

/** For this assignment you have to use the Spread toolkit to build a replicated banking system.
 *  The system architecture will consist of
        (a) the standard Spread server and
        (b) a client that you need to develop and link with the Spread library.
 *  The application only needs to support a single bank account with the sequentially consistent replication semantics.
 *  Each running instance of the client will represent a replica of this account.
 */

public class Main {

    public static void main(String[] args) throws InterruptedException {
        SpreadConnection connection = new SpreadConnection();
        Listener listener = new Listener();
        Random rand = new Random();
        int id = rand.nextInt();
        try {
            connection.add(listener);

            // if the ifi machine is used <use the ifi machine ip address>
            //connection.connect(InetAddress.getByName("129.240.65.59"), 4803, "test connection", false, true);

            System.out.println("Testing connection on localhost");
            // for the local machine (172.18.0.1 is the loopback address in this machine)
            connection.connect(InetAddress.getByName("127.0.0.1"), 4803, String.valueOf(id), false, true);
            System.out.println("Connection established");

            SpreadGroup group = new SpreadGroup();
            group.join(connection, "group");
            SpreadMessage message = new SpreadMessage();
            message.addGroup(group);
            message.setFifo();
            message.setObject("client name : "+id);
            connection.multicast(message);

        } catch (SpreadException e) {
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        //System.out.println("Hello world!");
        Thread.sleep(100000000);
    }
}