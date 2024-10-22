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
    public static SpreadConnection connection;
    public static final SpreadGroup group = new SpreadGroup();

    private static ScheduledExecutorService inputExecutor;
    private static ScheduledExecutorService broadcastingExecutor;

    public ReplicatedStateMachine(String[] args) {
        fileName = null;
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
        startBroadcastingExecutor();
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
            print("done waiting, current replicas length: " + replicas.length);
        } catch (SpreadException | UnknownHostException | InterruptedException | InterruptedIOException e) {
            e.printStackTrace();
        }
    }

    private void joinGroup() throws SpreadException, InterruptedIOException {
        group.join(connection, accountName);
        replica.sayHello(replicaName);
    }

    private static synchronized void startBroadcastingExecutor() {
        broadcastingExecutor = Executors.newSingleThreadScheduledExecutor();
        Collection<Transaction> outStandingCollection = replica.getOutstandingCollection();
        synchronized (outStandingCollection) {
            broadcastingExecutor.scheduleAtFixedRate(() -> {
                    if (!outStandingCollection.isEmpty()) {
                        print("Client " + replicaName + " has " + outStandingCollection.size() + " outstanding transactions");
                        for (Transaction transaction : outStandingCollection) {
                            sendMessage(transaction);
                        }
                    }
            }, 0, 10, TimeUnit.SECONDS);
        }
    }

    private static void stopExecutor(ScheduledExecutorService executor) {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }

    private static void readInput() {
        long initialDelay = 0;
        if (replicaName.equals("Rep3")) initialDelay = 15;
        inputExecutor = Executors.newSingleThreadScheduledExecutor();

        try {
            if (fileName == null) {
                // read command line input
                while (true) {
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.nextLine();
                    parseInput(input);
                }
            } else {
                // read file input
                BufferedReader bufferedReader;
                File inputFile = new File(System.getProperty("user.dir") + "/src/main/java/com/in5020/group4/utils/" + fileName);
                //File jarInputFile = new File(fileName);
                //try {
                    bufferedReader = new BufferedReader(new FileReader(inputFile));
                //} catch (FileNotFoundException e) {
                //    bufferedReader = new BufferedReader(new FileReader(jarInputFile));
                //}

                String line = "";
                ArrayList<String> lines = new ArrayList<>();

                while ((line = bufferedReader.readLine()) != null) {
                    lines.add(line);
                }

                inputExecutor.scheduleAtFixedRate(() -> {
                    synchronized (lines) {
                        if (!lines.isEmpty()) {
                            String input = lines.remove(0);
                            try {
                                parseInput(input);
                                float T = 0.5f + (float) Math.random();
                                Thread.sleep((long) T);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            stopExecutor(inputExecutor);
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

    public static void writeOutput(String output) {
        BufferedWriter writer = null;
        String fileName = System.getProperty("user.dir") + "/src/main/java/com/in5020/group4/utils/output_" + replicaName + ".txt";
        try {
            writer = new BufferedWriter(new FileWriter(fileName, true));
            writer.write(output);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
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

    private static void parseInput(String input) throws InterruptedException {
        String command = input.split(" ")[0];  // Extract command
        switch (command.toLowerCase()) {
            case "getquickbalance": {
                print("Quick Balance: " + replica.getQuickBalance());
                writeOutput("Quick Balance: " + replica.getQuickBalance());
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
                            writeOutput("Synced Balance Naive: " + replica.getQuickBalance());
                        }
                    }
                });//.start();

                // Correct
                if (replica.getOutstandingCollection().isEmpty()) {
                    Transaction transaction = new Transaction();
                    transaction.setCommand(input);
                    transaction.setUniqueId(replicaName + " " + replica.getOutstandingCounter());
                    transaction.setType(TransactionType.SYNCED_BALANCE);

                    replica.addOutstandingCollection(transaction); // todo: output
                } else {
                    print("Synced Balance Correct: " + replica.getQuickBalance());
                    writeOutput("Synced Balance Correct: " + replica.getQuickBalance());
                }
                break;
            }
            case "deposit": {
                if (input.matches("deposit \\d+(\\.\\d+)?")) {
                    String[] args = input.split(" ");
                    double amount = Double.parseDouble(args[1]);

                    Transaction transaction = new Transaction();
                    transaction.setCommand(args[0]);
                    transaction.setUniqueId(replicaName + " " + replica.getOutstandingCounter());
                    transaction.setBalance(amount);
                    transaction.setType(TransactionType.DEPOSIT);

                    replica.addOutstandingCollection(transaction);
                    print(input);
                    writeOutput(input);
                }
                break;
            }
            case "addinterest": {
                if (input.matches("addInterest \\d+(\\.\\d+)?")) {
                    String[] args = input.split(" ");
                    double percent = Double.parseDouble(args[1]);

                    Transaction transaction = new Transaction();
                    transaction.setCommand(input);
                    transaction.setUniqueId(replicaName + " " + replica.getOutstandingCounter());
                    transaction.setPercent(percent);
                    transaction.setType(TransactionType.INTEREST);

                    replica.addOutstandingCollection(transaction);
                    print(input);
                    writeOutput(input);
                }
                break;
            }
            case "gethistory": {
                print(input);

                List<Transaction> executedTransactions = replica.getExecutedTransactions();
                if (!executedTransactions.isEmpty()) {
                    print("Executed List:");
                    writeOutput("Executed List:");
                    for (Transaction transaction : executedTransactions) {
                        print(transaction.getUniqueId() + ":" + transaction.getCommand());
                        writeOutput(transaction.getUniqueId() + ":" + transaction.getCommand());
                    }
                }

                Collection<Transaction> outstandingCollection = replica.getOutstandingCollection();
                if (!outstandingCollection.isEmpty()) {
                    print("Outstanding collection:");
                    writeOutput("Outstanding collection:");
                    for (Transaction transaction : outstandingCollection) {
                        print(transaction.getUniqueId() + ":" + transaction.getCommand());
                        writeOutput(transaction.getUniqueId() + ":" + transaction.getCommand());
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
                    writeOutput(transactionExecuted.getCommand() + " is executed");
                } else {
                    print(input + " has not been executed yet");
                    writeOutput(input + " has not been executed yet");
                }
                break;
            }
            case "cleanhistory": {
                print("Executing clean history");
                writeOutput("Executing clean history");
                replica.setExecutedTransactions(new ArrayList<>());
                break;
            }
            case "memberinfo": {
                System.out.print("[ReplicatedStateMachine]: Member info: ");
                writeOutput("Member info: ");
                for (Object replicaName : Arrays.stream(replicas).toArray()) {
                    String newReplicaName = replicaName.toString()
                            .replace("#", "")
                            .replace("spreadserver", "")
                            .replace("-", " ");
                    System.out.print(newReplicaName + " ");
                    writeOutput(newReplicaName);
                }
                System.out.println();
                break;
            }
            case "sleep": {
                if (input.matches("sleep \\d+")) {
                    String[] args = input.split(" ");
                    int time = Integer.parseInt(args[1]);

                    print("Sleep: " + time + " seconds");
                    writeOutput("Sleep: " + time + " seconds");
                    Thread.sleep(time * 1000L);
                }
                break;
            }
            case "exit": {
                writeOutput("Exit");
                exit();
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
            print("Stopping executors...");
            stopExecutor(inputExecutor);
            stopExecutor(broadcastingExecutor);

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
            print("BALANCE: " + replica.getQuickBalance());
            writeOutput("BALANCE: " + replica.getQuickBalance());
            print("Exiting environment...");
            System.exit(0);
        } finally { // finally block can not complete normally
            print("BALANCE: " + replica.getQuickBalance());
            writeOutput("BALANCE: " + replica.getQuickBalance());
        }
    }

    private static void print(String message) {
        System.out.println("[ReplicatedStateMachine]: " + message);
    }
}
