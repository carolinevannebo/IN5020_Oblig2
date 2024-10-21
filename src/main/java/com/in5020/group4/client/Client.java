package com.in5020.group4.client;

import com.in5020.group4.Transaction;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// todo: thread for each in outstandingCollection being executed, will be added to executedList, with orderCounter
public class Client {
    private double balance;
    private List<Transaction> executedList;
    private Collection<Transaction> outstandingCollection;
    private AtomicInteger orderCounter;
    private AtomicInteger outstandingCounter;

    public Client(double balance, List<Transaction> executedList, Collection<Transaction> outstandingCollection, AtomicInteger orderCounter, AtomicInteger outstandingCounter) {
        this.balance = balance;
        this.executedList = executedList;
        this.outstandingCollection = outstandingCollection;
        this.orderCounter = orderCounter;
        this.outstandingCounter = outstandingCounter;
    }

    public void addExecutedTransaction(Transaction transaction) {
        this.executedList.add(transaction);
    }
    public void addOutstandingCollection(Transaction transaction) {
        this.outstandingCollection.add(transaction);
        this.outstandingCounter.incrementAndGet();
    }

    public void getSyncedBalance(Transaction transaction) {
        this.outstandingCollection.stream()
                .filter(it -> it.getUniqueId().equals(transaction.getUniqueId()))
                .findFirst()
                .ifPresent(this.outstandingCollection::remove);  // Remove if transaction is found

        System.out.println("Synced Balance: " + this.balance);
    }

    public void deposit(Transaction transaction, int amount) {
        this.outstandingCollection.stream()
                .filter(it -> it.getUniqueId().equals(transaction.getUniqueId()))
                .findFirst()
                .ifPresent(this.outstandingCollection::remove);

        this.balance += amount;
        this.executedList.add(transaction);
        this.orderCounter.incrementAndGet();
    }

    public void addInterest(Transaction transaction, int percent) {
        this.outstandingCollection.stream()
                .filter(it -> it.getUniqueId().equals(transaction.getUniqueId()))
                .findFirst()
                .ifPresent(this.outstandingCollection::remove);

        this.balance *= (1.0 + percent / 100.0);
        this.executedList.add(transaction);
        this.orderCounter.incrementAndGet();
    }


    public String checkTxStatus(int transactionId) throws Exception {
        return "";
    }

    public List<String> memberInfo() throws Exception {
        return List.of();
    }

    public void getHistory() { // don't think this is right
        /*int start = this.orderCounter.get() - this.executedList.size();
        for (int i = this.orderCounter.get(); i < this.executedList.size(); i++) {
            print(i + ". " + this.executedList.get(i).command);
        }*/
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

    public void setOutstandingCollection(Collection<Transaction> outstandingCollection) {
        this.outstandingCollection = outstandingCollection;
    }

    public AtomicInteger getOrderCounter() {
        return this.orderCounter;
    }

    public void setOrderCounter(AtomicInteger orderCounter) {
        this.orderCounter = orderCounter;
    }

    public AtomicInteger getOutstandingCounter() {
        return this.outstandingCounter;
    }

    public void setOutstandingCounter(AtomicInteger outstandingCounter) {
        this.outstandingCounter = outstandingCounter;
    }

    public void print(String message) {
        System.out.println("[Client] " + message);
    }

    public void sayHello(String replicaName) {
        System.out.println("Hello, my name is " + replicaName);
    }
}