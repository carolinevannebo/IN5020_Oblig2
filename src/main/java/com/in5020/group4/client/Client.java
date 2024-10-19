package com.in5020.group4.client;

import com.in5020.group4.listener.AdvancedListener;
import com.in5020.group4.listener.BasicListener;
import com.in5020.group4.Transaction;
import com.in5020.group4.utils.MessageType;
import spread.*;

import java.io.InterruptedIOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// todo: thread for each in outstandingCollection being executed, will be added to executedList, with orderCounter
public class Client {
    private final String serverAddress;// = "127.0.0.1";
    private final String accountName;// = "replicaGroup";
    private final int clientNumber;
    private String repName = "";

    private BasicListener basicListener;
    private final AdvancedListener advancedListener;
    private final SpreadConnection connection = new SpreadConnection();
    private final SpreadGroup group = new SpreadGroup();
    //public static SpreadGroup[] members;// = new SpreadGroup[0];

    private static AtomicInteger orderCounter = new AtomicInteger(0);
    private List<Transaction> executedList = new ArrayList<>();
    private Collection<Transaction> outstandingCollection = new ArrayList<>();
    //private AtomicInteger orderCounter = new AtomicInteger(0);
    //private AtomicInteger outstandingCounter = new AtomicInteger(0);
    public double balance;
    //private Thread receiverThread;
    private Thread senderThread;

    /*public synchronized static void main (String[] args) {
        String serverAddress = "127.0.0.1";
        String accountName = "replicaGroup";
        int numberOfReplicas = 2;
        int clientNumber = Integer.parseInt(args[0].trim());
        String repName = "";

        SpreadConnection connection = new SpreadConnection();
        AdvancedListener advancedListener = new AdvancedListener();
        BasicMessageListener basicListener = new BasicListener(connection, accountName);

        Random rand = new Random();
        int id = rand.nextInt();
        try {
            connection.add(advancedListener);
            connection.add(basicListener);

            connection.connect(InetAddress.getByName(serverAddress), 8000, String.valueOf(id), false, true);
            System.out.println("[Client " + clientNumber + "] Connected to " + serverAddress + ":" + 8000 + " " + connection.getPrivateGroup());

            SpreadGroup group = new SpreadGroup();
            group.join(connection, accountName);
            repName = connection.getPrivateGroup().toString();
            System.out.println("[Client " + clientNumber + "] group name: " + repName);

            synchronized (advancedListener) {
                if (members.length < numberOfReplicas) {
                    System.out.println("[Client " + clientNumber + "] waiting for other replicas to join");
                    System.out.println("[Client " + clientNumber + "] members: " + members.length);
                    advancedListener.wait();
                }
            }
            System.out.println("[Client " + clientNumber + "] done waiting, members: " + members.length);
        } catch (SpreadException | InterruptedException | UnknownHostException e) {
            System.out.println("[Client " + clientNumber + "] Connection Error: " + e.getMessage());
            e.printStackTrace();
        }
    }*/

    public Client(String serverAddress, String accountName, int clientNumber, int numberOfReplicas, AdvancedListener advancedListener) {
        this.serverAddress = serverAddress;
        this.accountName = accountName;
        this.clientNumber = clientNumber;
        String repName = "";

        this.basicListener = new BasicListener(connection, accountName);
        this.advancedListener = advancedListener;

        Random rand = new Random();
        int id = rand.nextInt();
        try {
            connection.add(advancedListener);
            connection.add(basicListener);

            connection.connect(InetAddress.getByName(serverAddress), 8000, String.valueOf(id), false, true);
            System.out.println("[Client " + clientNumber + "] Connected to " + serverAddress + ":" + 8000 + " " + connection.getPrivateGroup());

            SpreadGroup group = new SpreadGroup();
            group.join(connection, accountName);
            repName = connection.getPrivateGroup().toString();
            System.out.println("[Client " + clientNumber + "] group name: " + repName);

            synchronized (this.advancedListener) {
                while (!advancedListener.allReplicasJoined) {
                    System.out.println("[Client " + clientNumber + "] waiting for other replicas to join");
                    this.advancedListener.wait();
                }
            }

            System.out.println("[Client " + clientNumber + "] done waiting");
        } catch (SpreadException | InterruptedException | UnknownHostException e) {
            System.out.println("[Client " + clientNumber + "] Connection Error: " + e.getMessage());
            e.printStackTrace();
        }

        /*this.receiverThread = new Thread(() -> { // todo: not working - fix it
            while (true) {
                try {
                    //if (connection.poll()) {
                        String receivedMsg = (String) connection.receive().getObject();
                        System.out.println("[Client " + clientNumber + "] received message: " + receivedMsg);
                    //}
                } catch (SpreadException | InterruptedIOException e) {
                    System.out.println("[Client " + clientNumber + "] interrupting receiver thread due to error: " + e);
                    break;
                }
            }
            Thread.currentThread().interrupt();
        });*/

        this.senderThread = new Thread(() -> {
            System.out.println("[Client " + clientNumber + "] senderThread running");
            //if (!outstandingCollection.isEmpty())
                outstandingCollection.forEach(transaction -> orderCounter.incrementAndGet());

            while (orderCounter.get() > 0) {
                try {
                    if (!outstandingCollection.isEmpty()) {
                        // todo: call the method that will do the logic, then broadcast
                        for (Transaction transaction : outstandingCollection) {
                            try {
                                sendMessage(transaction);
                                executedList.add(transaction);
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
        int id = rand.nextInt();
        try {
            //connection.add(listener);
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
            /*listener.membershipMessageReceived(message);

            synchronized (listener) {
                while (!listener.allReplicasJoined) {
                    System.out.println("[Client " + clientNumber + "] Waiting for all replicas to join...\n");
                    listener.wait();
                }
            }*/

            //receiverThread.start();
            senderThread.start();
        } catch (SpreadException |
                 UnknownHostException /*|
                 /*InterruptedException*/ e//|
                 /*InterruptedIOException e*/
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

    private void handleMembershipChanges(int numReplicas) throws SpreadException, InterruptedIOException {
        SpreadMessage msg;
        while ((msg = connection.receive()) != null) {
            MembershipInfo membershipInfo = msg.getMembershipInfo();
            if (membershipInfo.isCausedByJoin() && membershipInfo.getMembers().length >= numReplicas) {
                System.out.println("All replicas have joined.");
                break;
            }
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
        orderCounter.incrementAndGet();
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

    public void getHistory() { // don't think this is right
        /*int start = this.orderCounter.get() - this.executedList.size();
        for (int i = this.orderCounter.get(); i < this.executedList.size(); i++) {
            print(i + ". " + this.executedList.get(i).command);
        }*/
    }

    public void exit() {
        try {
            print("Killing receiver and sender...");
            //receiverThread.interrupt();
            senderThread.interrupt();
            print("Leaving spread group...");
            group.leave();
            print("Disconnecting spread server...");
            connection.disconnect();
            print("Removing listener...");
            //connection.remove(listener);
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
        connection.multicast(message);

        /*if (message.isMembership()) {
            message.setServiceType(16128); // membership message
            System.out.println("[Client " + clientNumber + "] this is a membership message");
            connection.multicast(message);
            //listener.membershipMessageReceived(message);
        } else {
            message.setServiceType(4194304); // regular message
            message.setType((short) 0); // regular message has type 0
            //System.out.println("[Client " + clientNumber + "] this is NOT a membership message");
            //connection.multicast(message);
            //listener.regularMessageReceived(message);
        }*/
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