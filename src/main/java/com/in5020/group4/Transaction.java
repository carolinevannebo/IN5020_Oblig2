package com.in5020.group4;

public class Transaction {
    public String uniqueId;
    public String command;

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
}
