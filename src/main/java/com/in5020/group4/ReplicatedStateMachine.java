package com.in5020.group4;

import com.in5020.group4.listener.AdvancedListener;
import com.in5020.group4.listener.BasicListener;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ReplicatedStateMachine {
    private static String fileName;
    private static String serverAddress;
    private static String accountName;
    public static int numberOfReplicas;

    public static SpreadGroup[] replicas;
    private static Client replica;
    private static String replicaName;

    public static final SpreadConnection connection = new SpreadConnection();
    private static BasicListener basicListener;
    private static final AdvancedListener advancedListener = new AdvancedListener();
    public static final SpreadGroup group = new SpreadGroup();

    private static ScheduledExecutorService scheduledExecutor;

    public synchronized static void main(String[] args) {
        replicas =  new SpreadGroup[0];
        fileName = null; // remember to handle filename by coding clients or terminal
        replica = new Client(0.0,
                new ArrayList<>(),
                new ArrayList<>(),
                new AtomicInteger(0),
                new AtomicInteger(0)
        );

        if (args.length > 0) {
            serverAddress = args[0];
            accountName = args[1];
            numberOfReplicas = Integer.parseInt(args[2]);
            if (args.length > 3) fileName = args[3];
        } else {
            serverAddress = "127.0.0.1";
            accountName = "replicaGroup";
            numberOfReplicas = 2;
        }

        connect();
        createExecutor();
        startExecutor();
        readInput();
        //after outstandingcollection is empty: stopExecutor();
    }

    private static void connect() {
        //connection = new SpreadConnection();
        basicListener = new BasicListener(connection, accountName);

        Random rand = new Random();
        int id = rand.nextInt();
        try {
            connection.add(advancedListener);
            connection.add(basicListener);
            connection.connect(InetAddress.getByName(serverAddress),
                    8000, String.valueOf(id), false, true);

//            joinGroup();
//            SpreadMessage message = new SpreadMessage();
//            message.addGroup(group);
//            message.setFifo();
//            message.setObject("testing");
//            connection.multicast(message);
//            advancedListener.membershipMessageReceived(message);

            if (connection.poll()) {
                print("there are messages waiting on this connection");
            } else {
                print("there are NOT messages waiting on this connection");
            }
            synchronized (advancedListener) {
                while (replicas.length < numberOfReplicas) {
                    print("Connection waiting for " + numberOfReplicas + " total replicas to join");
                    advancedListener.wait();
                }
            }
            /*synchronized (group) {
                if (replicas.length < numberOfReplicas) {
                    print("Group waiting for " + numberOfReplicas + " total replicas to join");
                    group.wait();
                }
            }*/
            /*synchronized (advancedListener) { // commented out because wait made while holding two locks
                while (replicas.length < numberOfReplicas) {
                    //replicas = connection.receive().getGroups(); // TESTING
                    advancedListener.wait();
                }
            }*/
            print("Done waiting");
        } catch (SpreadException | UnknownHostException | InterruptedException /*| InterruptedIOException*/ e) {
            e.printStackTrace();
        }
    }

    private static void joinGroup() throws SpreadException, InterruptedIOException {
        group.join(connection, accountName);
        replicaName = connection.getPrivateGroup().toString();
        replica.sayHello(replicaName);
    }

    private synchronized static void createExecutor() {
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    private synchronized static void startExecutor() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            Collection<Transaction> outStandingCollection = replica.getOutstandingCollection();
            if (!outStandingCollection.isEmpty()) {
                print("Client " + replicaName + " has " + outStandingCollection.size() + " outstanding transactions");

                for (Transaction transaction : outStandingCollection) {
                    try {
                        sendMessage(transaction);
                    } catch (SpreadException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    private synchronized static void stopExecutor() {
        if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutor.shutdownNow();
            }
        }
    }

    private synchronized static void readInput() {
        try {
            if (fileName == null) {
                // read terminal input
                while (true) {
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.nextLine();
                    print(input); // todo: parse
                }
            } else {
                // read file input
                File inputFile = new File(System.getProperty("user.dir") + "/src/main/java/com/in5020/group4/utils/" + fileName);
                BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile));
                String line = "";

                while ((line = bufferedReader.readLine()) != null) {
                    print(line); // todo: parse
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized static void sendMessage(Transaction transaction) throws SpreadException {
        SpreadMessage message = new SpreadMessage();
        message.addGroup(group);
        message.setFifo();
        message.setObject(transaction);
        connection.multicast(message);
    }

    private static void print(String message) {
        System.out.println("[ReplicatedStateMachine]: " + message);
    }
}
