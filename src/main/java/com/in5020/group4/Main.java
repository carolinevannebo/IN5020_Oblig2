package com.in5020.group4;

import com.in5020.group4.client.Client;
import com.in5020.group4.utils.TxtFileReader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

/** For this assignment you have to use the Spread toolkit to build a replicated banking system.
 *  The system architecture will consist of
        (a) the standard Spread server and
        (b) a client that you need to develop and link with the Spread library.
 *  The application only needs to support a single bank account with the sequentially consistent replication semantics.
 *  Each running instance of the client will represent a replica of this account.
 */

public class Main {

    public static void main(String[] args) throws InterruptedException {
        String serverAddress = "127.0.0.1";
        String accountName = "groupXX";
        int numberOfReplicas = 3;

        Listener listener = new Listener(numberOfReplicas);
        for (int i = 1; i <= numberOfReplicas; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    Client client = new Client(serverAddress, accountName, listener, finalI);
                    client.connect();

                    List<String> testingQueries = TxtFileReader.getQueries();
                    for (String query : testingQueries) { // todo: each client should not run the whole file by itself
                        runInput(client, query);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            sleep(1000); // just to test concurrency
        }
    }

    private static void runInput(Client client, String input) throws InterruptedException {
        /// I think the input file will give us a client ID, meaning we should not pass a client params, but an ID, to establish which client to use
        System.out.println("\nInput:" + input);
        if (input.equalsIgnoreCase("getQuickBalance")) {
            System.out.println("Quick Balance: " + client.getQuickBalance());

        } else if (input.equalsIgnoreCase("getSyncedBalance")) {
            Transaction transaction = new Transaction();
            transaction.setCommand(input);
            transaction.setUniqueId(input);
            client.getSyncedBalance(transaction);
            client.addOutStandingCollection(transaction);

        } else if (input.matches("deposit \\d+(\\.\\d+)?")) {
            String[] args = input.split(" ");
            int amount = Integer.parseInt(args[1]);
            Transaction transaction = new Transaction();
            transaction.setCommand(input);
            transaction.setUniqueId(input);
            client.deposit(transaction, amount);
            client.addOutStandingCollection(transaction);

        } else if (input.matches("addInterest \\d+(\\.\\d+)?")) {
            String[] args = input.split(" ");
            int percent = Integer.parseInt(args[1]);
            Transaction transaction = new Transaction();
            transaction.setCommand(input);
            transaction.setUniqueId(input);
            client.addInterest(transaction, percent);
            client.addOutStandingCollection(transaction);

        } else if (input.equalsIgnoreCase("getHistory")) {
            System.out.println("\nExecuted List:");
            for (Transaction transaction : client.getExecutedList()) {
                System.out.println(transaction.getUniqueId() + ":" + transaction.getCommand());
            }

            System.out.println("\nOutstanding collection:");
            for (Transaction transaction : client.getOutstandingCollection()) {
                System.out.println(transaction.getUniqueId() + ":" + transaction.getCommand());
            }

        } else if (input.matches("checkTxStatus <.*>")) {
            System.out.println("\nCheck Tx Status");

        } else if (input.equalsIgnoreCase("cleanHistory")) {
            System.out.println("Executing clean history");
            client.setExecutedList(new ArrayList<>());
            client.setOrderCounter(new AtomicInteger(0));

        } else if (input.equalsIgnoreCase("memberInfo")) {
            System.out.println("\nMember Info");

        } else if (input.matches("sleep \\d+")) {
            String[] args = input.split(" ");
            int time = Integer.parseInt(args[1]);
            System.out.println("\nSleep: " + time + " seconds");
            sleep(time);

        } else if (input.equalsIgnoreCase("exit")) {
            System.out.println("\n" + input);
            System.exit(0);
        }
    }
}