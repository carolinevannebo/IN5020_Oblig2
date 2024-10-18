package com.in5020.group4;

import spread.*;

import java.util.ArrayList;
import java.util.List;

public class Listener implements AdvancedMessageListener {
    private final int numberOfReplicas;
    public boolean allReplicasJoined = false;
    private final List<String> messages = new ArrayList<>();

    public Listener(int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    @Override
    public synchronized void regularMessageReceived(SpreadMessage message) {
        String msg = null;
        try {
            msg = (String) message.getObject();
        } catch (SpreadException e) {
            throw new RuntimeException(e);
        }
        print(msg);
        //print("message type: " + message.getServiceType() + "message:" + msg);
        //print("message type: " + message.getType());
    }

    @Override
    public synchronized void membershipMessageReceived(SpreadMessage spreadMessage) {
        String msg = null;
        try {
            msg = (String) spreadMessage.getObject();
            print("Received replica '" + msg + "'");
            messages.add(msg);
        } catch (SpreadException e) {
            throw new RuntimeException(e);
        }

        if (messages.size() >= numberOfReplicas) {
            allReplicasJoined = true;
            print("All replicas joined. Notifying waiting clients...\n");
            notifyAll();
        }
    }

    private void print(String message) {
        System.out.println("[Listener] " + message);
    }
}