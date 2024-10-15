package com.in5020.group4.client;

import com.in5020.group4.Listener;
import com.in5020.group4.Transaction;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Client {
    private final String serverAddress;
    private final String accountName;
    private final Listener listener;
    private final int clientNumber;

    private final SpreadConnection connection = new SpreadConnection();
    private final SpreadGroup group = new SpreadGroup();

    private List<Transaction> executedList = new ArrayList<>();
    private Collection<Transaction> outstandingCollection = new ArrayList<>();
    //private AtomicInteger orderCounter = new AtomicInteger(0);
    //private AtomicInteger outstandingCounter = new AtomicInteger(0);
    public double balance;

    public Client(String serverAddress, String accountName, Listener listener, int clientNumber) {
        this.serverAddress = serverAddress;
        this.accountName = accountName;
        this.listener = listener;
        this.clientNumber = clientNumber;
    }

    public void connect() {
        Random rand = new Random();
        int id = rand.nextInt();

        try {
            connection.add(listener);
            // for the local machine (172.18.0.1 is the loop-back address in this machine)
            connection.connect(InetAddress.getByName(serverAddress), 4803, String.valueOf(id), false, true);
            print("Connection established on " + serverAddress);
            group.join(connection, accountName);

            SpreadMessage message = new SpreadMessage();
            message.addGroup(group);
            message.setFifo();
            message.setObject("client name: " + id);

            connection.multicast(message);
            listener.membershipMessageReceived(message);

            synchronized (listener) {
                while (!listener.allReplicasJoined) {
                    print("Waiting for all replicas to join...\n");
                    listener.wait();
                }
            }
        } catch (SpreadException | UnknownHostException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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

        print("Synced Balance: " + this.balance);
    }

    public void deposit(Transaction transaction, int amount) {
        this.outstandingCollection.stream()
                .filter(it -> it.getUniqueId().equals(transaction.getUniqueId()))
                .findFirst()
                .ifPresent(this.outstandingCollection::remove);

        this.balance += amount;
        this.executedList.add(transaction);
        //this.orderCounter.incrementAndGet();
        //this.outstandingCounter.incrementAndGet();
    }

    public void addInterest(Transaction transaction, int percent) {
        this.outstandingCollection.stream()
                .filter(it -> it.getUniqueId().equals(transaction.getUniqueId()))
                .findFirst()
                .ifPresent(this.outstandingCollection::remove);

        this.balance *= (1.0 + percent / 100.0);
        this.executedList.add(transaction);
        //this.orderCounter.incrementAndGet();
        //this.outstandingCounter.incrementAndGet();
    }

    public String checkTxStatus(int transactionId) throws Exception {
        return "";
    }


    public List<String> memberInfo() throws Exception {
        return List.of();
    }

    public void getHistory() { // don't think this is right
        /*int start = this.orderCounter.get() - this.executedList.size();
        for (int i = this.orderCounter.get(); i < this.executedList.size(); i++) {
            print(i + ". " + this.executedList.get(i).command);
        }*/
    }

    public void exit() {
        try {
            print("Leaving spread group...");
            group.leave();
            print("Disconnecting spread server...");
            connection.disconnect();
        } catch (SpreadException e) {
            print("Exiting environment...");
            System.exit(0);
        }
    }

    private void sendMessage(Serializable object) throws SpreadException {
        SpreadMessage message = new SpreadMessage();
        message.addGroup(group);
        message.setFifo();
        message.setObject(object);

        listener.regularMessageReceived(message);
    }

    public void print(String message) {
        try {
            sendMessage("--> [Client " + clientNumber + "] " + message);
        } catch (SpreadException e) {
            System.out.println("[Client " + clientNumber + "] " + message);
        }
    }
}