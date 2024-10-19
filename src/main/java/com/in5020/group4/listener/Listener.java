/*package com.in5020.group4.listener;

import spread.*;

public class Listener implements BasicMessageListener {
    private SpreadConnection connection;
    private String accountName;

    public Listener(SpreadConnection connection, String accountName) {
        this.connection = connection;
        this.accountName = accountName;
    }

    @Override
    public void messageReceived(SpreadMessage message) {
        if (message.isMembership()) {
            print("membership message");
            MembershipInfo membershipInfo = message.getMembershipInfo();
            if (membershipInfo.isCausedByJoin()) {
                print("message caused by join");
                if (!membershipInfo.getJoined().equals(connection.getPrivateGroup())) {
                    print("someone new joined group");
                    SpreadMessage msg = new SpreadMessage();
                    try {
                        msg.setObject("transaction will be sent here");
                        msg.setFifo();
                        //msg.setReliable();
                        msg.addGroup(accountName);
                        print("sending message");
                        connection.multicast(msg);
                        print("message sent");
                    } catch (SpreadException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void print(String message) {
        System.out.println("[Listener] " + message);
    }
}*/

/*public class Listener implements AdvancedMessageListener {
    private final int numberOfReplicas;
    public boolean allReplicasJoined = false;
    private final List<String> messages = new ArrayList<>();

    public Listener(int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    @Override
    public synchronized void regularMessageReceived(SpreadMessage message) {
        Event event = null;
        String msg = null;
        try {
            msg = (String) message.getObject();
            print("listener msg type: " + message.getType());

            print("Regular message: " + message.getType());
            messages.add(msg);
            print("membership info: " + message.getMembershipInfo());
            print(msg);

        } catch (SpreadException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void membershipMessageReceived(SpreadMessage spreadMessage) {
        String msg = null;
        try {
            msg = (String) spreadMessage.getObject();

            if (spreadMessage.isRegular())
                print("Something wrong happened, membershipMessageReceived got a regular message: " + msg);
            else
                print("New membership message: " + msg);
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
}*/