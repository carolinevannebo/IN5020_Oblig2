package com.in5020.group4;

import com.in5020.group4.client.Client;
import com.in5020.group4.listener.AdvancedListener;
import com.in5020.group4.utils.TransactionType;
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

    public static Client replica;
    public static SpreadGroup[] replicas;
    public static String replicaName;

    public static AdvancedListener advancedListener;
    public static SpreadConnection connection;// = new SpreadConnection();
    public static final SpreadGroup group = new SpreadGroup();

    private static ScheduledExecutorService scheduledExecutor;
    //public static boolean allReplicasPresent = false;

    public ReplicatedStateMachine(String[] args) {
        fileName = null; // remember to handle filename by coding clients or terminal
        replicas =  new SpreadGroup[0];
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
            replicaName = args[3];

            if (args.length > 4) fileName = args[4];
            // todo: else, set CLI mode
        } else {
            System.out.println(" ----- MISSING ARGUMENTS -----");
            System.out.println("Please provide the following arguments:");
            System.out.println("<serverAddress> <accountName> <numberOfReplicas> <replicaName> <fileName>");
            System.out.println("\nExample:");
            System.out.println("172.20.10.3 replicaGroup 3 Rep1 Rep1.txt");
            System.exit(0);
        }

        connect();
        readInput();
        //after outstandingCollection is empty: stopExecutor(); ??
        // print balance - should be equal to other replicas
        print("Balance: " + replica.getQuickBalance());
    }

    public static void main(String[] args) {
        new ReplicatedStateMachine(args);
    }

    private void connect() {
        Random rand = new Random();
        int id = rand.nextInt();
        try {
            advancedListener = new AdvancedListener();
            connection = new SpreadConnection();
            connection.add(advancedListener);
            print("listener added");

            connection.connect(InetAddress.getByName(serverAddress),
                    4803, replicaName+"-"+id, false, true);
            print("connected");

            joinGroup();
            print("joined group");

            print("waiting for amount of replicas: " + numberOfReplicas);
            synchronized (group) {
                if (replicas.length < numberOfReplicas) {
                    group.wait();
                }
            }
            //allReplicasPresent = true;
            print("done waiting, current replicas length: " + replicas.length);
        } catch (SpreadException | UnknownHostException | InterruptedException | InterruptedIOException e) {
            e.printStackTrace();
        }
    }

    private void joinGroup() throws SpreadException, InterruptedIOException {
        group.join(connection, accountName);
        replica.sayHello(replicaName);
    }

    private static void stopExecutor() {
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

    private static void readInput() {
        long initialDelay = 0;
        if (replicaName.equals("Rep3")) initialDelay = 15;
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        try {
            if (fileName == null) {
                // read terminal input
                while (true) { // todo: test
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.nextLine();
                    parseInput(input);
                }
            } else {
                // read file input
                File inputFile = new File(System.getProperty("user.dir") + "/src/main/java/com/in5020/group4/utils/" + fileName);
                BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile));
                String line = "";
                ArrayList<String> lines = new ArrayList<>();

                while ((line = bufferedReader.readLine()) != null) {
                    lines.add(line);
                }

                scheduledExecutor.scheduleAtFixedRate(() -> {
                    synchronized (lines) {
                        if (!lines.isEmpty()) {
                            String input = lines.remove(0);
                            try {
                                parseInput(input);
                                // todo: fix method to handle other commands as well
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            stopExecutor();
                        }
                    }
                }, initialDelay, 1, TimeUnit.SECONDS);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendMessage(Transaction transaction) {
        try {
            SpreadMessage message = new SpreadMessage();
            message.addGroup(group);
            message.setFifo();
            message.setObject(transaction);
            print("Sending message, transaction id: " + transaction.uniqueId);
            connection.multicast(message);
        } catch (SpreadException e) {
            System.out.println("[Error]: " + e.getMessage());
        }
    }

    // todo: update code to latest and refactor to switch case
    private static void parseInput(String input) throws InterruptedException {
        String command = input.split(" ")[0];  // Extract command
        switch (command.toLowerCase()) {
            case "getquickbalance": {
                print("Quick Balance: " + replica.getQuickBalance());
                break;
            }
            case "getsyncedbalance": {
                print(input);
                // Naive
                new Thread(() -> {
                    synchronized (replica.getOutstandingCollection()) {
                        while (!replica.getOutstandingCollection().isEmpty()) {
                            try {
                                replica.getOutstandingCollection().wait(); // ?
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        synchronized (replica.getOutstandingCollection()) {
                            print("Synced Balance Naive: " + replica.getQuickBalance());
                        }
                    }
                });//.start();
                // Correct
                if (replica.getOutstandingCollection().isEmpty()) {
                    Transaction transaction = new Transaction();
                    transaction.setCommand(input);
                    transaction.setUniqueId(replicaName + " " + replica.getOutstandingCounter());
                    transaction.setType(TransactionType.SYNCED_BALANCE);

                    replica.addOutstandingCollection(transaction);
                    sendMessage(transaction);
                } else {
                    print("Synced Balance Correct: " + replica.getQuickBalance());
                }
                break;
            }
            case "deposit": {
                if (input.matches("deposit \\d+(\\.\\d+)?")) {
                    print(input);
                    String[] args = input.split(" ");
                    double amount = Double.parseDouble(args[1]);

                    Transaction transaction = new Transaction();
                    transaction.setCommand(args[0]);
                    transaction.setUniqueId(replicaName + " " + replica.getOutstandingCounter());
                    transaction.setBalance(amount);
                    transaction.setType(TransactionType.DEPOSIT);

                    replica.addOutstandingCollection(transaction);
                    sendMessage(transaction);
                }
                break;
            }
            case "addinterest": {
                if (input.matches("addInterest \\d+(\\.\\d+)?")) {
                    print(input);
                    String[] args = input.split(" ");
                    double percent = Double.parseDouble(args[1]);

                    Transaction transaction = new Transaction();
                    transaction.setCommand(input);
                    transaction.setUniqueId(replicaName + " " + replica.getOutstandingCounter());
                    transaction.setPercent(percent);
                    transaction.setType(TransactionType.INTEREST);

                    replica.addOutstandingCollection(transaction);
                    sendMessage(transaction);
                }
                break;
            }
            case "gethistory": {
                print(input);

                List<Transaction> executedTransactions = replica.getExecutedTransactions();
                if (!executedTransactions.isEmpty()) {
                    print("Executed List:");
                    for (Transaction transaction : executedTransactions) {
                        print(transaction.getUniqueId() + ":" + transaction.getCommand());
                    }
                }

                Collection<Transaction> outstandingCollection = replica.getOutstandingCollection();
                if (!outstandingCollection.isEmpty()) {
                    print("Outstanding collection:");
                    for (Transaction transaction : outstandingCollection) {
                        print(transaction.getUniqueId() + ":" + transaction.getCommand());
                    }
                }
                break;
            }
            case "checktxstatus": {
                print(input);
                String[] args = input.split(" ");
                String transactionId = args[1] + " " + args[2];
                Transaction transactionExecuted = replica.getExecutedTransactions().stream()
                        .filter(it -> it.getUniqueId().equals(transactionId)).findFirst().orElse(null);
                if (transactionExecuted != null) {
                    print(transactionExecuted.getCommand() + " is executed");
                } else {
                    print(input + " has not been executed yet");
                }
                break;
            }
            case "cleanhistory": {
                print("Executing clean history");
                replica.setExecutedTransactions(new ArrayList<>());
                break;
            }
            case "memberinfo": {
                System.out.print("[ReplicatedStateMachine]: Member info: ");
                for (Object replicaName : Arrays.stream(replicas).toArray()) {
                    System.out.print(replicaName + " ");
                }
                System.out.println();
                break;
            }
            case "sleep": {
                if (input.matches("sleep \\d+")) {
                    String[] args = input.split(" ");
                    int time = Integer.parseInt(args[1]);

                    print("Sleep: " + time + " seconds");
                    Thread.sleep(time * 1000);
                }
                break;
            }
            case "exit": {
                print("would exit but not gonna na ah");
                // todo: check that all executions are done and messages has been broadcast
                //exit();
                break;
            }
            default: {
                System.out.println("[Feedback]: command not recognized, try again");
                break;
            }
        }
    }

    private static void exit() {
        try {
            if (!replica.getOutstandingCollection().isEmpty()) {
                //wait
                // not enough, although we remove from outstanding collection,
                // how do we know there's no incoming messages from other replicas that need to be executed?
            }

            print("Stopping executor");
            stopExecutor();

            print("Leaving spread group...");
            group.leave();

            try {
                print("Removing listener...");
                connection.remove(advancedListener);
            } finally {
                print("Disconnecting spread server...");
                connection.disconnect();
            }
        } catch (SpreadException e) {
            print("Exiting environment...");
            System.exit(0);
        }
    }

    private static void print(String message) {
        System.out.println("[ReplicatedStateMachine]: " + message);
    }
}
