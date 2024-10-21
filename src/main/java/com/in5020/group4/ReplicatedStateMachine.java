package com.in5020.group4;

import com.in5020.group4.client.Client;
import com.in5020.group4.listener.AdvancedListener;
import spread.*;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ReplicatedStateMachine {
    private static String fileName;
    private static String serverAddress;
    private static String accountName;
    public static int numberOfReplicas;

    public static SpreadGroup[] replicas;
    private static Client replica;
    private static String replicaName;

    public static AdvancedListener advancedListener;
    public static final SpreadConnection connection = new SpreadConnection();
    public static final SpreadGroup group = new SpreadGroup();

    private static ScheduledExecutorService scheduledExecutor;

    public ReplicatedStateMachine(String[] args) {
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

        print("fileName: " + fileName);
        print("serverAddress: " + serverAddress);
        print("accountName: " + accountName);
        print("numberOfReplicas: " + numberOfReplicas);

        connect();
        createExecutor();
        startExecutor();
        readInput();
        //after outstandingCollection is empty: stopExecutor();
    }

    public static void main(String[] args) {
        new ReplicatedStateMachine(args);
    }

    private void connect() {
        Random rand = new Random();
        int id = rand.nextInt();
        try {
            print("adding listener");
            advancedListener = new AdvancedListener();
            connection.add(advancedListener);
            print("listener added");

            print("connecting");
            connection.connect(InetAddress.getByName(serverAddress),
                    4803, String.valueOf(id), false, true);
            print("connected");

            print("joining group");
            joinGroup();
            print("joined group");

            SpreadMessage message = new SpreadMessage();
            message.addGroup(group);
            message.setFifo();
            message.setObject("first message");
            connection.multicast(message);
            replicas = message.getGroups();

            print("waiting, current replicas length: " + replicas.length);
            synchronized (group) {
                if (replicas.length < numberOfReplicas) {
                    group.wait();
                }
            }

            print("done waiting, current replicas length: " + replicas.length);
        } catch (SpreadException | UnknownHostException | InterruptedException | InterruptedIOException e) {
            e.printStackTrace();
        }
    }

    private void joinGroup() throws SpreadException, InterruptedIOException {
        group.join(connection, accountName);
        replicaName = connection.getPrivateGroup().toString();
        replica.sayHello(replicaName);
    }

    private synchronized static void createExecutor() {
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    private synchronized void startExecutor() {
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

    private synchronized void sendMessage(Transaction transaction) throws SpreadException {
        SpreadMessage message = new SpreadMessage();
        message.addGroup(group);
        message.setFifo();
        message.setObject(transaction);
        connection.multicast(message);
    }

    private void exit() {
        try {
            print("Leaving spread group...");
            group.leave();
            print("Removing listener...");
            connection.remove(advancedListener);
            print("Disconnecting spread server...");
            connection.disconnect();
        } catch (SpreadException e) {
            print("Exiting environment...");
            System.exit(0);
        }
    }

    private static void print(String message) {
        System.out.println("[ReplicatedStateMachine]: " + message);
    }
}
