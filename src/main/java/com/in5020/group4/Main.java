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
    //private static AtomicInteger orderCounter = new AtomicInteger(0);

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

    // todo: move logic into client
    private static void runInput(Client client, String input) throws InterruptedException {
        /// I think the input file will give us a client ID, meaning we should not pass a client params, but an ID, to establish which client to use
        client.print("\nInput:" + input);
        if (input.equalsIgnoreCase("getQuickBalance")) {
            client.print("Quick Balance: " + client.getQuickBalance());

        } else if (input.equalsIgnoreCase("getSyncedBalance")) {
            Transaction transaction = new Transaction();
            transaction.setCommand(input);
            transaction.setUniqueId(outstandingCounter.incrementAndGet());
            client.getSyncedBalance(transaction);
            client.addOutStandingCollection(transaction);

        } else if (input.matches("deposit \\d+(\\.\\d+)?")) {
            String[] args = input.split(" ");
            int amount = Integer.parseInt(args[1]);
            Transaction transaction = new Transaction();
            transaction.setCommand(input);
            transaction.setUniqueId(outstandingCounter.incrementAndGet());
            client.deposit(transaction, amount);
            client.addOutStandingCollection(transaction);

            //orderCounter.incrementAndGet();
            outstandingCounter.incrementAndGet();

        } else if (input.matches("addInterest \\d+(\\.\\d+)?")) {
            String[] args = input.split(" ");
            int percent = Integer.parseInt(args[1]);
            Transaction transaction = new Transaction();
            transaction.setCommand(input);
            transaction.setUniqueId(outstandingCounter.incrementAndGet());
            client.addInterest(transaction, percent);
            client.addOutStandingCollection(transaction);

            //orderCounter.incrementAndGet();
            outstandingCounter.incrementAndGet();

        } else if (input.equalsIgnoreCase("getHistory")) {
            client.getHistory();
            /*client.print("\nExecuted List:");
            for (Transaction transaction : client.getExecutedList()) {
                client.print(transaction.getUniqueId() + ":" + transaction.getCommand());
            }
            client.print("\nOutstanding collection:");
            for (Transaction transaction : client.getOutstandingCollection()) {
                client.print(transaction.getUniqueId() + ":" + transaction.getCommand());
            }*/

        } else if (input.matches("checkTxStatus <.*>")) {
            client.print("\nCheck Tx Status");

        } else if (input.equalsIgnoreCase("cleanHistory")) {
            client.print("Executing clean history");
            //client.setExecutedList(new ArrayList<>());
            //client.setOrderCounter(new AtomicInteger(0));

        } else if (input.equalsIgnoreCase("memberInfo")) {
            client.print("\nMember Info");

        } else if (input.matches("sleep \\d+")) {
            String[] args = input.split(" ");
            int time = Integer.parseInt(args[1]);
            client.print("Sleep: " + time + " seconds");
            sleep(time);

        } else if (input.equalsIgnoreCase("exit")) {
            client.exit();
        }
    }
}