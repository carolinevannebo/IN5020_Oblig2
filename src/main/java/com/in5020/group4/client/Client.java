package com.in5020.group4.client;

import com.in5020.group4.Listener;
import com.in5020.group4.Transaction;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.InterruptedIOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// todo: thread for each in outstandingCollection being executed, will be added to executedList, with orderCounter
public class Client {
    private final String serverAddress;
    private final String accountName;
    private final Listener listener;
    private final int clientNumber;

    private final SpreadConnection connection = new SpreadConnection();
    private final SpreadGroup group = new SpreadGroup();

    private static AtomicInteger orderCounter = new AtomicInteger(0);
    private List<Transaction> executedList = new ArrayList<>();
    private Collection<Transaction> outstandingCollection = new ArrayList<>();
    //private AtomicInteger orderCounter = new AtomicInteger(0);
    //private AtomicInteger outstandingCounter = new AtomicInteger(0);
    public double balance;
    private Thread receiverThread;
    private Thread senderThread;

    public Client(String serverAddress, String accountName, Listener listener, SpreadGroup group, int clientNumber) {
        this.serverAddress = serverAddress;
        this.accountName = accountName;
        this.listener = listener;
        /*this.group = group;*/
        this.clientNumber = clientNumber;

        this.receiverThread = new Thread(() -> {
            System.out.println("[Client " + clientNumber + "] receiverThread running");
            while (true) {
                try {
                    if (connection.poll()) {
                        String receivedMsg = (String) connection.receive().getObject();
                        System.out.println("[Client " + clientNumber + "] received message: " + receivedMsg);
                    }
                } catch (SpreadException | InterruptedIOException e) {
                    throw new RuntimeException(e); // ??
                }
            }
        });

        this.senderThread = new Thread(() -> {
            System.out.println("[Client " + clientNumber + "] senderThread running");
            outstandingCollection.forEach(transaction -> orderCounter.incrementAndGet());

            while (orderCounter.get() > 0) {
                try {
                    if (!outstandingCollection.isEmpty()) {
                        // todo: call the method that will do the logic, then broadcast
                        for (Transaction transaction : outstandingCollection) {
                            try {
                                sendMessage(transaction.command);
                                executedList.add(transaction);
                                //outstandingCollection.remove(transaction);
                            } catch (SpreadException e) {
                                System.out.println("[Client " + clientNumber + "] error sending message: " + e.getMessage());
                            }
                        }
                        outstandingCollection.clear();
                        orderCounter.set(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void connect() {
        Random rand = new Random();
        int id = rand.nextInt();//this.clientNumber;

        try {
            connection.add(listener);
            // for the local machine (172.18.0.1 is the loop-back address in this machine)
            connection.connect(InetAddress.getByName(serverAddress), 4803, String.valueOf(id), false, true);
            System.out.println("[Client " + clientNumber + "] Connected to " + serverAddress + ":" + 4803 + " " + connection.getPrivateGroup());
            group.join(connection, accountName);

            SpreadMessage message = new SpreadMessage();
            message.addGroup(group);
            message.setFifo();
            message.setObject("client name: " + id);
            message.setServiceType(16128); // to make it a membership message

            connection.multicast(message);
            listener.membershipMessageReceived(message);

            synchronized (listener) {
                while (!listener.allReplicasJoined) {
                    System.out.println("[Client " + clientNumber + "] Waiting for all replicas to join...\n");
                    listener.wait();
                }
            }

            System.out.println("[Client " + clientNumber + "] starting receiverThread");
            receiverThread.start();
            senderThread.start();
        } catch (SpreadException |
                 UnknownHostException |
                 InterruptedException e
        ) {
            if (e instanceof SpreadException) {
                if (e.getCause() instanceof SocketException && Objects.equals(((SpreadException) e).getMessage(), "Broken pipe")) {
                    try {
                        connection.disconnect();
                        connect();
                    } catch (SpreadException spreadException) {
                        throw new RuntimeException();
                    }
                }
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    public AtomicInteger getOrderCounter() {
        return this.orderCounter;
    }

    public void setOrderCounter(AtomicInteger orderCounter) {
        this.orderCounter = orderCounter;
    }

    public AtomicInteger getOutstandingCounter() {
        return this.outstandingCounter;
    }

    public void setOutstandingCounter(AtomicInteger outstandingCounter) {
        this.outstandingCounter = outstandingCounter;
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

    private void sendMessage(Serializable object) throws SpreadException {
        SpreadMessage message = new SpreadMessage();
        message.addGroup(group);
        message.setFifo();
        message.setObject(object);

        if (message.isMembership()) {
            message.setServiceType(16128); // membership message
            System.out.println("[Client " + clientNumber + "] this is a membership message");
            connection.multicast(message);
            listener.membershipMessageReceived(message);
        } else {
            message.setServiceType(4194304); // regular message
            //System.out.println("[Client " + clientNumber + "] this is NOT a membership message");
            //connection.multicast(message);
            listener.regularMessageReceived(message);
        }
    }

    public void print(String message) {
        try {
            sendMessage("--> [Client " + clientNumber + "] " + message);
        } catch (SpreadException e) {
            System.out.println("Something went wrong, could not send message: ");
            e.printStackTrace();
            //System.out.println("[Client " + clientNumber + "] " + message);
        }
    }

}