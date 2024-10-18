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

public class Client {
    private final String serverAddress;
    private final String accountName;
    private final Listener listener;
    public int numberOfReplicas;
    public double balance;
    public List<Transaction> executedList = new ArrayList<>();
    public Collection<Transaction> outstandingCollection = new ArrayList<>();
    public AtomicInteger orderCounter = new AtomicInteger(0);
    public AtomicInteger outstandingCounter = new AtomicInteger(0);

    public Client(String serverAddress, String accountName, Listener listener) {
        this.serverAddress = serverAddress;
        this.accountName = accountName;
        this.listener = listener;
        //connect();
    }

    public void connect() {
        SpreadConnection connection = new SpreadConnection();

        Random rand = new Random();
        int id = rand.nextInt();
        try {
            connection.add(listener);
            // if the ifi machine is used <use the ifi machine ip address>
            //connection.connect(InetAddress.getByName("129.240.65.59"), 4803, "test connection", false, true);

            System.out.println("Testing connection on localhost");
            // for the local machine (172.18.0.1 is the loop-back address in this machine)
            connection.connect(InetAddress.getByName(serverAddress), 4803, String.valueOf(id), false, true);
            System.out.println("Connection established");

            SpreadGroup group = new SpreadGroup();
            group.join(connection, accountName);

            SpreadMessage message = new SpreadMessage();
            message.addGroup(group);
            message.setFifo();
            message.setObject("client name : " + id);

            connection.multicast(message);

            //listener.membershipMessageReceived(message);
            System.out.println("Waiting for replicas to join");
            listener.waitForAllReplicas();
            System.out.println("All replicas joined");
        } catch (SpreadException | UnknownHostException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public AtomicInteger getOrderCounter() {
        return this.orderCounter;
    }

    public void setOrderCounter(AtomicInteger orderCounter) {
        this.orderCounter = orderCounter;
    }

    public List<Transaction> getExecutedList() {
        return this.executedList;
    }

    public void setExecutedList(List<Transaction> executedList) {
        this.executedList = executedList;
    }

    public Collection<Transaction> getOutstandingCollection() {
        return this.outstandingCollection;
    }

    public void setOutstandingCollection(List<Transaction> outstandingCollection) {
        this.outstandingCollection = outstandingCollection;
    }

    public void addExecutedList(Transaction transaction) {
        this.executedList.add(transaction);
    }

    public void addOutStandingCollection(Transaction transaction) {
        this.outstandingCollection.add(transaction);
    }

    public double getQuickBalance() {
        return balance;
    }

    public void getSyncedBalance(Transaction transaction) {
        this.outstandingCollection.stream()
                .filter(it -> it.getUniqueId().equals(transaction.getUniqueId()))
                .findFirst()
                .ifPresent(this.outstandingCollection::remove);  // Remove if transaction is found

        System.out.println("Synced Balance: " + this.balance);
    }

    public void deposit(Transaction transaction, int amount) {
        this.outstandingCollection.stream()
                .filter(it -> it.getUniqueId().equals(transaction.getUniqueId()))
                .findFirst()
                .ifPresent(this.outstandingCollection::remove);

        this.balance += amount;
        this.executedList.add(transaction);
        this.orderCounter.incrementAndGet();
    }

    public void addInterest(Transaction transaction, int percent) {
        this.outstandingCollection.stream()
                .filter(it -> it.getUniqueId().equals(transaction.getUniqueId()))
                .findFirst()
                .ifPresent(this.outstandingCollection::remove);

        this.balance *= (1.0 + percent / 100.0);
        this.executedList.add(transaction);
        this.orderCounter.incrementAndGet();
    }


    public String checkTxStatus(int transactionId) throws Exception {
        return "";
    }


    public List<String> memberInfo() throws Exception {
        return List.of();
    }


}