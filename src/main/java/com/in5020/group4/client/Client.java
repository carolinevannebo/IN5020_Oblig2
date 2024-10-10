package com.in5020.group4.client;

import com.in5020.group4.Listener;
import com.in5020.group4.Transaction;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Client implements ClientInterface {
    public String serverAddress;
    public String accountName;
    public int numberOfReplicas;
    public List<Transaction> executedList = new ArrayList<>();
    public Collection<Transaction> outstandingCollection = new ArrayList<>();
    public AtomicInteger orderCounter = new AtomicInteger(0);
    public AtomicInteger outstandingCounter = new AtomicInteger(0);

    public static void run() {
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
    }

    @Override
    public int getQuickBalance() throws Exception {
        return 0;
    }

    @Override
    public int getSyncedBalance() throws Exception {
        return 0;
    }

    @Override
    public void deposit(int amount) throws Exception {

    }

    @Override
    public void addInterest(int percent) throws Exception {

    }

    @Override
    public void getHistory() throws Exception {

    }

    @Override
    public String checkTxStatus(int transactionId) throws Exception {
        return "";
    }

    @Override
    public void cleanHistory() throws Exception {

    }

    @Override
    public List<String> memberInfo() throws Exception {
        return List.of();
    }

    @Override
    public void sleep(int duration) throws Exception {

    }

    @Override
    public void exit() throws Exception {

    }
}