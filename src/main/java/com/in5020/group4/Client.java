package com.in5020.group4;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

    public synchronized void addExecutedTransaction(Transaction transaction) {
        this.executedList.add(transaction);
    }

    public synchronized void addOutstandingTransaction(Transaction transaction) {
        this.outstandingCollection.add(transaction);
        this.outstandingCounter.incrementAndGet();
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public List<Transaction> getExecutedList() {
        return executedList;
    }

    public void setExecutedList(List<Transaction> executedList) {
        this.executedList = executedList;
    }

    public Collection<Transaction> getOutstandingCollection() {
        return outstandingCollection;
    }

    public void setOutstandingCollection(Collection<Transaction> outstandingCollection) {
        this.outstandingCollection = outstandingCollection;
    }

    public AtomicInteger getOrderCounter() {
        return orderCounter;
    }

    public void setOrderCounter(AtomicInteger orderCounter) {
        this.orderCounter = orderCounter;
    }

    public AtomicInteger getOutstandingCounter() {
        return outstandingCounter;
    }

    public void setOutstandingCounter(AtomicInteger outstandingCounter) {
        this.outstandingCounter = outstandingCounter;
    }

    public void print(String message) {
        System.out.println("[Client]: " + message);
    }

    public void sayHello(String replicaName) {
        System.out.println("Hello, my name is " + replicaName);
    }
}
