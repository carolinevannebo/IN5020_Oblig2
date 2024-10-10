package com.in5020.group4.client;

import com.in5020.group4.Transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Client implements ClientInterface {
    public String serverAddress;
    public String accountName;
    public int numberOfReplicas;
    public List<Transaction> executedList = new ArrayList<>();
    public Collection<Transaction> outstandingCollection = new ArrayList<>();
    public AtomicInteger orderCounter = new AtomicInteger(0);
    public AtomicInteger outstandingCounter = new AtomicInteger(0);

    @Override
    public int getQuickBalance() throws Exception {
        return 0;
    }

    @Override
    public int getSyncedBalance() throws Exception {
        return 0;
    }

    @Override
    public void deposit(int amount) throws Exception {

    }

    @Override
    public void addInterest(int percent) throws Exception {

    }

    @Override
    public void getHistory() throws Exception {

    }

    @Override
    public String checkTxStatus(int transactionId) throws Exception {
        return "";
    }

    @Override
    public void cleanHistory() throws Exception {

    }

    @Override
    public List<String> memberInfo() throws Exception {
        return List.of();
    }

    @Override
    public void sleep(int duration) throws Exception {

    }

    @Override
    public void exit() throws Exception {

    }
}