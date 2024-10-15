package com.in5020.group4;

import com.in5020.group4.client.Client;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

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
        Client.run();
        Thread.sleep(100000000);
    }

    private static void runInput(String input) throws InterruptedException {
        if (input.equalsIgnoreCase("getQuickBalance")) {
            System.out.println("\n" + input);
            System.out.println("Quick Balance: " + client.getQuickBalance());

        } else if (input.equalsIgnoreCase("getSyncedBalance")) {
            System.out.println("\n" + input);

        } else if (input.matches("deposit \\d+(\\.\\d+)?")) {
            System.out.println("\n" + input);
            String[] args = input.split(" ");
            int amount = Integer.parseInt(args[1]);
            Transaction transaction = new Transaction();

        } else if (input.matches("addInterest \\d+(\\.\\d+)?")) {
            System.out.println("\n" + input);
            String[] args = input.split(" ");
            int percent = Integer.parseInt(args[1]);
            Transaction transaction = new Transaction();

        } else if (input.equalsIgnoreCase("getHistory")) {
            System.out.println("\n" + input);

        } else if (input.matches("checkTxStatus <.*>")) {
            System.out.println("\n" + input);

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