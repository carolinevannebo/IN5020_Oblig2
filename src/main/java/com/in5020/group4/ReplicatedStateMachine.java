package com.in5020.group4;

import com.in5020.group4.client.Client;
import com.in5020.group4.listener.AdvancedListener;
import com.in5020.group4.utils.Transaction;
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
    private static ScheduledExecutorService exitExecutor;

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

        String outputFile = System.getProperty("user.dir") + "/src/main/java/com/in5020/group4/utils/output_" + replicaName + ".txt";
        try (FileWriter fileWriter = new FileWriter(outputFile, false)) {
            // Opening the file in write mode without appending clears the file
        } catch (IOException e) {
            e.printStackTrace();
        }

        connect();
        startBroadcastingExecutor();
        readInput();
    }

    public static void main(String[] args) {
        new ReplicatedStateMachine(args);
    }

    private void connect() {
        Random rand = new Random();
        int id = rand.nextInt();
        try {
            if (replicaName.equals("Rep3")) Thread.sleep(15000);
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
        broadcastingExecutor = Executors.newScheduledThreadPool(1);//.newSingleThreadScheduledExecutor();
        broadcastingExecutor.scheduleWithFixedDelay(() -> {//.scheduleAtFixedRate(() -> {
            if (!replica.getOutstandingCollection().isEmpty()) {
                print("Client " + replicaName + " has " + replica.getOutstandingCollection().size() + " outstanding transactions");
                sendMessages(replica.getOutstandingCollection());
            }
        }, 10, 10, TimeUnit.SECONDS);
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
        inputExecutor = Executors.newScheduledThreadPool(1);
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
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.matches("sleep \\d+")) {
                        String[] args = line.split(" ");
                        int time = Integer.parseInt(args[1]);
                        inputExecutor.schedule(() -> ("Sleep " + time + " seconds"), time, TimeUnit.SECONDS);
                        try {
                            TimeUnit.SECONDS.sleep(time);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        parseInput(line);
                        int T = (int) (Math.random() * 1000) + 500;
                        inputExecutor.schedule(() -> print("T delay"), T, TimeUnit.MILLISECONDS);
                        try {
                            TimeUnit.MICROSECONDS.sleep(T);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
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

    public static void sendMessages(Collection<Transaction> transaction) {
        try {
            List<SpreadMessage> messages = new ArrayList<>();
            SpreadMessage message;
            for (Transaction t : transaction) {
                message = new SpreadMessage();
                message.addGroup(group);
                message.setFifo();
                message.setObject(t);
                messages.add(message);
            }
            SpreadMessage[] spreadMessages = messages.toArray(new SpreadMessage[0]);
            connection.multicast(spreadMessages);
        } catch (SpreadException e) {
            System.out.println("[Error]: " + e.getMessage());
        }
    }

    private static void parseInput(String input) throws InterruptedException {
        String command = input.split(" ")[0];  // Extract command
        Collection<Transaction> outstandingCollection = replica.getOutstandingCollection();

        switch (command.toLowerCase()) {
            case "getquickbalance": {
                print("Quick Balance: " + replica.getQuickBalance());
                writeOutput("Quick Balance: " + replica.getQuickBalance());
                break;
            }
            case "getsyncedbalance": {
                print(input);
                // Naive
//                while (!replica.getOutstandingCollection().isEmpty()) { continue; }
//                print("Synced Balance Naive: " + replica.getQuickBalance());
//                writeOutput("Synced Balance Naive: " + replica.getQuickBalance());

                // Correct
                if (replica.getOutstandingCollection().isEmpty()) {
                    Transaction transaction = new Transaction();
                    transaction.setCommand(input);
                    transaction.setUniqueId(replicaName + " " + replica.getOutstandingCounter());
                    transaction.setType(TransactionType.SYNCED_BALANCE);

                    replica.addOutstandingCollection(transaction);
                } else {
                    print("Synced Balance Correct: " + replica.getQuickBalance());
                    writeOutput("Synced Balance Correct: " + replica.getQuickBalance());
                }
                break;
            }
            case "deposit": {
                if (input.matches("deposit -?\\d+(\\.\\d+)?")) {
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

                //Collection<Transaction> outstandingCollection = replica.getOutstandingCollection();
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
                            .replace("-", " ")
                            .replace("  ", "-");
                    System.out.print(newReplicaName + " ");
                    writeOutput(newReplicaName);
                }
                System.out.println();
                break;
            }
            case "exit": {
                writeOutput("Exit");
                exit();
                System.exit(0);
                break;
            }
            default: {
                System.out.println("[Feedback]: command not recognized, try again");
                break;
            }
        }
    }

    private synchronized static void exit() {
        try {
            exitExecutor = Executors.newScheduledThreadPool(1);
            while (!replica.getOutstandingCollection().isEmpty()) {
                exitExecutor.schedule(() -> print("Have to wait for all outstanding transactions to complete"), 1, TimeUnit.SECONDS);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            print("All transactions complete");

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
        } finally {
            print("BALANCE: " + replica.getQuickBalance());
            writeOutput("BALANCE: " + replica.getQuickBalance());
        }
    }

    private static void print(String message) {
        System.out.println("[ReplicatedStateMachine]: " + message);
    }
}
