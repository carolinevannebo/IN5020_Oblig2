package com.in5020.group4.utils;

import java.io.Serializable;

public class Transaction implements Serializable {
    // todo: make all private
    public String uniqueId;
    public String command;
    private double balance = 0.0;
    private double percent = 0.0;
    private TransactionType type;

    public Transaction() {}
    public Transaction(String uniqueId, String command) {
        this.uniqueId = uniqueId;
        this.command = command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double getBalance() {
        return balance;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }

    public double getPercent() {
        return percent;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionType getType() {
        return type;
    }
}
