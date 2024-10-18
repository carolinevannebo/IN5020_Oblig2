package com.in5020.group4;

import com.in5020.group4.client.Client;
import com.in5020.group4.utils.TxtFileReader;
import spread.SpreadGroup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

/** For this assignment you have to use the Spread toolkit to build a replicated banking system.
 *  The system architecture will consist of
        (a) the standard Spread server and
        (b) a client that you need to develop and link with the Spread library.
 *  The application only needs to support a single bank account with the sequentially consistent replication semantics.
 *  Each running instance of the client will represent a replica of this account.
 */

public class Main {
    private static final AtomicInteger outstandingCounter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        String serverAddress = "127.0.0.1";
        String accountName = "replicaGroup";
        int numberOfReplicas = 3;

        Listener listener = new Listener(numberOfReplicas);
        SpreadGroup group = new SpreadGroup();
        for (int i = 1; i <= numberOfReplicas; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    File inputFile = new File(System.getProperty("user.dir")+"/src/main/java/com/in5020/group4/utils/Rep"+finalI+".txt");

                    Client client = new Client(serverAddress, accountName, listener, group, finalI);
                    client.connect();

                    BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile));
                    String line = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        Transaction transaction = new Transaction(outstandingCounter.incrementAndGet(), line);
                        client.addOutStandingCollection(transaction);
                    }
                } catch (/*Interrupted*/Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        sleep(1000000);
    }

    private static void runInput(String clientName, String input) throws InterruptedException {
        /// I think the input file will give us a client ID, meaning we should not pass a client params, but an ID, to establish which client to use
        System.out.println("\nInput:" + input);
        if (input.equalsIgnoreCase("getQuickBalance")) {
            System.out.println("Quick Balance: " + client.getBalance());

        } else if (input.equalsIgnoreCase("getSyncedBalance")) {
            // Naive
            if (client.getOutstandingCounter() == 0) {
                //client.getSyncedBalance(transaction);
                System.out.println("Quick Balance: " + client.getBalance());
            }
            // Corrected
            Transaction transaction = new Transaction();
            transaction.setCommand(input);
            transaction.setUniqueId(clientName + " " + client.getOutstandingCounter());
            client.addOutStandingCollection(transaction);

        } else if (input.matches("deposit \\d+(\\.\\d+)?")) {
            String[] args = input.split(" ");
            int amount = Integer.parseInt(args[1]);
            Transaction transaction = new Transaction();
            transaction.setCommand(input);
            transaction.setUniqueId(clientName + " " + client.getOutstandingCounter());
            client.addOutStandingCollection(transaction);
            client.setOutstandingCounter(client.getOutstandingCounter + 1);

        } else if (input.matches("addInterest \\d+(\\.\\d+)?")) {
            String[] args = input.split(" ");
            int percent = Integer.parseInt(args[1]);
            Transaction transaction = new Transaction();
            transaction.setCommand(input);
            transaction.setUniqueId(clientName + " " + client.getOutstandingCounter());
            client.addOutStandingCollection(transaction);
            client.setOutstandingCounter(client.getOutstandingCounter + 1);

        } else if (input.equalsIgnoreCase("getHistory")) {
            System.out.println("\nExecuted List:");
            for (Transaction transaction : client.getExecutedList()) {
                System.out.println(transaction.getUniqueId() + ":" + transaction.getCommand());
            }
            System.out.println("\nOutstanding collection:");
            for (Transaction transaction : client.getOutstandingCollection()) {
                System.out.println(transaction.getUniqueId() + ":" + transaction.getCommand());
            }

        } else if (input.matches("checkTxStatus \\w+ \\d+")) {
            String[] args = input.split(" ");
            String transactionId = args[1] + " " + args[2];
            Transaction transactionExecuted = client.getExecutedList().stream()
                    .filter(it -> it.getUniqueId().equals(transactionId)).findFirst().orElse(null);
            if (transactionExecuted != null) {
                System.out.println(transactionExecuted.getCommand() + " is executed");
            } else {
                System.out.println(transactionExecuted.getCommand() + " is not executed");
            }

        } else if (input.equalsIgnoreCase("cleanHistory")) {
            client.setExecutedList(new ArrayList<>());

        } else if (input.equalsIgnoreCase("memberInfo")) {
            System.out.println("\nMember Info");
            for (int i = 0; i < numberOfReplicas; i++) {
                // TODO: get member info
                System.out.println(" ");
            }

        } else if (input.matches("sleep \\d+")) {
            String[] args = input.split(" ");
            int time = Integer.parseInt(args[1]);
            System.out.println("\nSleep: " + time + " seconds");
            sleep(time);

        } else if (input.equalsIgnoreCase("exit")) {
            client.exit();
        }
    }
}