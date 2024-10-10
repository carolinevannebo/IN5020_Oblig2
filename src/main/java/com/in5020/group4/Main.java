package com.in5020.group4;

import com.in5020.group4.client.Client;
/** For this assignment you have to use the Spread toolkit to build a replicated banking system.
 *  The system architecture will consist of
        (a) the standard Spread server and
        (b) a client that you need to develop and link with the Spread library.
 *  The application only needs to support a single bank account with the sequentially consistent replication semantics.
 *  Each running instance of the client will represent a replica of this account.
 */

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Client.run();
        Thread.sleep(100000000);
    }
}