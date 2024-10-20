package com.in5020.group4;

import com.in5020.group4.listener.AdvancedListener;
import com.in5020.group4.listener.BasicListener;
import spread.*;

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

public class ReplicatedStateMachine /*implements AdvancedMessageListener*/ {
    private static String fileName;
    private static String serverAddress;
    private static String accountName;
    public static int numberOfReplicas;

    public static SpreadGroup[] replicas;
    private static Client replica;
    private static String replicaName;

    public static final SpreadConnection connection = new SpreadConnection();
    //private static BasicListener basicListener;
    //private static final AdvancedListener advancedListener = new AdvancedListener();
    //private AdvancedListener advancedListener;
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
        new Thread(() -> {
            new ReplicatedStateMachine(args);
        }).start();
    }

    private synchronized void connect() {
        Random rand = new Random();
        int id = rand.nextInt();
        try {
            print("connecting");
            connection.connect(InetAddress.getByName(serverAddress),
                    8000, String.valueOf(id), false, true);
            print("connected");

//            updateReplicas.start();

            print("adding listener");
            connection.add(AdvancedListener.getInstance());
            print("listener added");

            print("joining group");
            joinGroup();
            print("joined group");

            SpreadMessage message = new SpreadMessage();
            message.addGroup(group);
            message.setFifo();
            message.setObject("first message");
            connection.multicast(message);
            replicas = message.getGroups();
//            replicas = message.getMembershipInfo().getMembers();

            print("waiting, current replicas length: " + replicas.length);
            if (replicas.length < numberOfReplicas) {
                print("waiting");
                wait();
                print("Done waiting");
            }
        } catch (SpreadException | UnknownHostException | InterruptedException | InterruptedIOException e) {
            e.printStackTrace();
        }
    }

//    Thread updateReplicas = new Thread(() -> {
//        while (true) {
//            SpreadMessage message = null;
//            try {
//                message = connection.receive();
//                if (message.isIncoming()) {
//                    print("Incoming message");
//                }
//                replicas = connection.receive().getGroups();
//                print("replicas: " + replicas.length);
//                if (replicas.length >= numberOfReplicas) {
//                    notifyAll();
//                }
//            } catch (SpreadException | InterruptedIOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    });

    public void handleMembershipChange(SpreadGroup[] members) {
        print("incoming new membership");
        if (replicas != null && replicas.length >= numberOfReplicas) {
            replicas = members;
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

    private static void print(String message) {
        System.out.println("[ReplicatedStateMachine]: " + message);
    }

//    @Override
//    public void regularMessageReceived(SpreadMessage spreadMessage) {
//        print("Regular message received: " + spreadMessage);
//    }
//
//    @Override
//    public void membershipMessageReceived(SpreadMessage spreadMessage) {
//        print("Membership message received");
//        MembershipInfo membershipInfo = spreadMessage.getMembershipInfo();
//        if (membershipInfo.isCausedByJoin()) {
//            replicas = membershipInfo.getMembers();
//            //SpreadGroup groupMembers = membershipInfo.getJoined();
//            print("Amount of joined replicas: " + replicas.length);
//        }
//        if (replicas.length >= numberOfReplicas) {
//            print("All replicas joined, notifying");
//            notifyAll();
//        }
//    }
}
