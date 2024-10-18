package com.in5020.group4;

public class Transaction {
    public int uniqueId;
    public String command;

    public Transaction() {}
    public Transaction(int uniqueId, String command) {
        this.uniqueId = uniqueId;
        this.command = command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public void setUniqueId(int uniqueId) {
        this.uniqueId = uniqueId;
    }

    public int getUniqueId() {
        return uniqueId;
    }
}