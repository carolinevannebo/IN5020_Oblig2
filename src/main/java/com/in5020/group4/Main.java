package com.in5020.group4;

import com.in5020.group4.client.Client;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Scanner;

/** For this assignment you have to use the Spread toolkit to build a replicated banking system.
 *  The system architecture will consist of
        (a) the standard Spread server and
        (b) a client that you need to develop and link with the Spread library.
 *  The application only needs to support a single bank account with the sequentially consistent replication semantics.
 *  Each running instance of the client will represent a replica of this account.
 */

public class Main {
    static Client client = new Client();

    public static void main(String[] args) throws InterruptedException {
        String serverAddress = "127.0.0.1";
        String accountName = "groupXX";
<<<<<<< Updated upstream
        int numberOfReplicas = 2;
=======
        int numberOfReplicas = 1;
        String fileName = "input.txt";
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
>>>>>>> Stashed changes

        Listener listener = new Listener(numberOfReplicas);
        for (int i = 1; i <= numberOfReplicas; i++) {
            new Thread(() -> {
<<<<<<< Updated upstream
                Client client = new Client(serverAddress, accountName, listener);
                client.connect();
=======
                try {
                    Client client = new Client(serverAddress, accountName, listener);
                    client.connect();

                    if (fileName != null) {
                        List<String> testingQueries = TxtFileReader.getQueries(fileName);
                        for (String query : testingQueries) { // todo: each client should not run the whole file by itself
                            //System.out.println("query: " + query);
                            runInput(client, query);
                        }
                    }
                    else {
                        while (running) {
                            String query = scanner.nextLine().trim();
                            runInput(client, query);
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
>>>>>>> Stashed changes
            }).start();
        }
    }

    private static void runInput(String input) throws InterruptedException {
        if (input.equalsIgnoreCase("getQuickBalance")) {
            System.out.println("\n" + input);
            System.out.println("Quick Balance: " + client.getQuickBalance());

        } else if (input.equalsIgnoreCase("getSyncedBalance")) {
            System.out.println("\n" + input);
            Transaction transaction = new Transaction();
            transaction.setCommand(input);
            transaction.setUniqueId(input);
            client.getSyncedBalance(transaction);
            client.addOutStandingCollection(transaction);

        } else if (input.matches("deposit \\d+(\\.\\d+)?")) {
            System.out.println("\n" + input);
            String[] args = input.split(" ");
            int amount = Integer.parseInt(args[1]);
            Transaction transaction = new Transaction();
            transaction.setCommand(input);
            transaction.setUniqueId(input);
            client.deposit(transaction, amount);
            client.addOutStandingCollection(transaction);

        } else if (input.matches("addInterest \\d+(\\.\\d+)?")) {
            System.out.println("\n" + input);
            String[] args = input.split(" ");
            int percent = Integer.parseInt(args[1]);
            Transaction transaction = new Transaction();
            transaction.setCommand(input);
            transaction.setUniqueId(input);
            client.addInterest(transaction, percent);
            client.addOutStandingCollection(transaction);

        } else if (input.equalsIgnoreCase("getHistory")) {
            System.out.println("\n" + input);
            System.out.println("\nExecuted List:");
            for (Transaction transaction : client.getExecutedList()) {
                System.out.println(transaction.getUniqueId() + ":" + transaction.getCommand());
            }

            System.out.println("\nOutstanding collection:");
            for (Transaction transaction : client.getOutstandingCollection()) {
                System.out.println(transaction.getUniqueId() + ":" + transaction.getCommand());
            }

        } else if (input.matches("checkTxStatus <.*>")) {
<<<<<<< Updated upstream
            System.out.println("\n" + input);
=======
            System.out.println("\nCheck Tx Status");
            String firstPart = input.substring(1, input.indexOf(" ", 1));
            String secondPart = input.substring(firstPart.length() + 2);
            //generate transaction id if it is from the file
            if (secondPart.contains("<")) {
                // ToDO: get client id
                secondPart = clientName + " " + (client.getOutstandingCounter().decrementAndGet());
            }
            System.out.println("\nTransaction status of " + secondPart);
            String finalTransactionId = secondPart;
            Transaction transactionInExecuted = client.getExecutedList().stream()
                    .filter(it -> it.getUniqueId().equals(finalTransactionId)).findFirst().orElse(null);
            if (transactionInExecuted != null) {
                System.out.println(transactionInExecuted.getCommand() + " is executed.");
                return;
            }
            Transaction transactionInOutstanding = client.getOutstandingCollection().stream()
                    .filter(it -> it.getUniqueId().equals(finalTransactionId)).findFirst().orElse(null);
            if (transactionInOutstanding != null) {
                System.out.println(transactionInOutstanding.getCommand() + " is not executed yet.");
            } else {
                System.out.println("Transaction not found");
            }
>>>>>>> Stashed changes

        } else if (input.equalsIgnoreCase("cleanHistory")) {
            System.out.println("Executing clean history");
            client.setExecutedList(new ArrayList<>());
            client.setOrderCounter(new AtomicInteger(0));

        } else if (input.equalsIgnoreCase("memberInfo")) {
            System.out.println("\n" + input);

        } else if (input.matches("sleep \\d+")) {
            String[] args = input.split(" ");
            int time = Integer.parseInt(args[1]);
            System.out.println("\nSleep: " + time + " seconds");
            Thread.sleep(time);

        } else if (input.equalsIgnoreCase("exit")) {
            System.out.println("\n" + input);
            System.exit(0);
        }
    }
}