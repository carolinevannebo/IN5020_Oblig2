package com.in5020.group4.client;

import java.util.List;

public interface ClientInterface {
    int getQuickBalance() throws Exception;

    int getSyncedBalance() throws Exception;
    // You have to implement both of the mentioned ideas (the naive implementation and the
    // correct one), observe the differences in the response for the getSyncedBalance command,
    // and explain the reasons.

    void deposit(int amount) throws Exception;

    void addInterest(int percent) throws Exception;

    void getHistory() throws Exception; // might need different return type?

    String checkTxStatus(int transactionId) throws Exception;

    void cleanHistory() throws Exception;

    List<String> memberInfo() throws Exception;

    void sleep(int duration) throws Exception;

    void exit() throws Exception;
}
