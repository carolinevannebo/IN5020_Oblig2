package com.in5020.group4.client;

import com.in5020.group4.utils.Transaction;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// todo: thread for each in outstandingCollection being executed, will be added to executedList, with orderCounter
public class Client {
    private double balance;
    private List<Transaction> executedList;
    private final Collection<Transaction> outstandingCollection;
    private final AtomicInteger orderCounter;
    private final AtomicInteger outstandingCounter;

    public Client(double balance, List<Transaction> executedList, Collection<Transaction> outstandingCollection, AtomicInteger orderCounter, AtomicInteger outstandingCounter) {
        this.balance = balance;
        this.executedList = executedList;
        this.outstandingCollection = outstandingCollection;
        this.orderCounter = orderCounter;
        this.outstandingCounter = outstandingCounter;
    }

    public void addOutstandingCollection(Transaction transaction) {
        this.outstandingCollection.add(transaction);
        this.outstandingCounter.incrementAndGet();
    }

    public void getSyncedBalance(Transaction transaction) {
        boolean alreadyExecuted = executedList.stream()
                .anyMatch(executedTransaction -> executedTransaction.getUniqueId()
                        .equals(transaction.getUniqueId()));

        if (alreadyExecuted) return;

        this.outstandingCollection.stream()
                .filter(it -> it.getUniqueId().equals(transaction.getUniqueId()))
                .findFirst().ifPresent(outstandingCollection::remove);

        print("Synced Balance: " + this.balance);
    }

    // todo: only need transaction param
    public void deposit(Transaction transaction, double amount) {
        boolean alreadyExecuted = executedList.stream()
                .anyMatch(executedTransaction -> executedTransaction.getUniqueId()
                        .equals(transaction.getUniqueId()));

        if (alreadyExecuted) return;

        this.outstandingCollection.stream()
                .filter(it -> it.getUniqueId().equals(transaction.getUniqueId()))
                .findFirst().ifPresent(outstandingCollection::remove);

        this.balance += amount;
        this.executedList.add(transaction);
        this.orderCounter.incrementAndGet();
    }

    // todo: only need transaction param - ALSO: check math, might be reason numbers are too high
    public void addInterest(Transaction transaction, double percent) {
        boolean alreadyExecuted = executedList.stream()
                .anyMatch(executedTransaction -> executedTransaction.getUniqueId()
                        .equals(transaction.getUniqueId()));

        if (alreadyExecuted) return;

        this.outstandingCollection.stream()
                .filter(it -> it.getUniqueId().equals(transaction.getUniqueId()))
                .findFirst().ifPresent(outstandingCollection::remove);

        this.balance *= (1.0 + percent / 100.0);
        this.executedList.add(transaction);
        this.orderCounter.incrementAndGet();
    }

    public double getQuickBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public List<Transaction> getExecutedTransactions() {
        return this.executedList;
    }

    public void setExecutedTransactions(List<Transaction> executedList) {
        this.executedList = executedList;
    }

    public Collection<Transaction> getOutstandingCollection() {
        return this.outstandingCollection;
    }

    public AtomicInteger getOutstandingCounter() {
        return this.outstandingCounter;
    }

    public void print(String message) {
        System.out.println("[Client]: " + message);
    }

    public void sayHello(String replicaName) {
        System.out.println("Hello, my name is " + replicaName + " I have " + this.balance);
    }
}