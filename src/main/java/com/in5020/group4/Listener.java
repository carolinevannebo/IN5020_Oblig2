package com.in5020.group4;

import spread.*;

public class Listener implements AdvancedMessageListener {
    private final int numberOfReplicas;
    public boolean allReplicasJoined = false;

    public Listener(int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    public synchronized void regularMessageReceived(SpreadMessage message) {
        String msg = null;
        try {
            msg = (String) message.getObject();
        } catch (SpreadException e) {
            throw new RuntimeException(e);
        }
        System.out.println(msg);
    }

    @Override
    public synchronized void membershipMessageReceived(SpreadMessage spreadMessage) {
        MembershipInfo membershipInfo = spreadMessage.getMembershipInfo();
        int currentMembers = membershipInfo.getMembers().length;
        System.out.println("Current members in group: " + currentMembers);

        if (currentMembers == numberOfReplicas) {
            allReplicasJoined = true;
            notifyAll();
        }
    }

    public synchronized void waitForAllReplicas() throws InterruptedException {
        while (!allReplicasJoined) {
            wait();
        }
    }
}