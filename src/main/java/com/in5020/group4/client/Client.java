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
    public String serverAddress;
    public String accountName;
    public int numberOfReplicas;
    public double balance;
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

    public double getQuickBalance() {
        return balance;
    }

    public void getSyncedBalance(Transaction transaction) throws Exception {
        // Remove the transaction from outstandingList if found
        this.outstandingCollection.stream()
                .filter(it -> it.getUniqueId().equals(transaction.getUniqueId()))
                .findFirst()
                .ifPresent(this.outstandingCollection::remove);  // Remove if transaction is found

        // Print the synced balance
        System.out.println("Synced Balance: " + this.balance);
    }

    public void deposit(Transaction transaction, int amount) throws Exception {
        this.outstandingCollection.stream()
                .filter(it -> it.getUniqueId().equals(transaction.getUniqueId()))
                .findFirst()
                .ifPresent(this.outstandingCollection::remove);  // Remove if transaction is found

        // Update the balance by adding the transaction amount
        this.balance += amount;

        // Add the transaction to the executed list
        this.executedList.add(transaction);

        // Increment the order counter using AtomicInteger
        this.orderCounter.incrementAndGet();
    }

    public void addInterest(Transaction transaction, int percent) throws Exception {
        this.outstandingCollection.stream()
                .filter(it -> it.getUniqueId().equals(transaction.getUniqueId()))
                .findFirst()
                .ifPresent(this.outstandingCollection::remove);  // Remove if transaction is found

        // Update the balance by applying interest
        this.balance *= (1.0 + percent / 100.0);

        // Add the transaction to the executed list
        this.executedList.add(transaction);

        // Increment the order counter using AtomicInteger
        this.orderCounter.incrementAndGet();
    }

    public void getHistory() throws Exception {

    }

    public String checkTxStatus(int transactionId) throws Exception {
        return "";
    }

    public void cleanHistory() throws Exception {

    }

    public List<String> memberInfo() throws Exception {
        return List.of();
    }

    public void sleep(int duration) throws Exception {

    }

    public void exit() throws Exception {

    }
}