package com.in5020.group4.listener;

import com.in5020.group4.ReplicatedStateMachine;
import com.in5020.group4.client.Client;
import spread.AdvancedMessageListener;
import spread.*;

import java.util.ArrayList;
import java.util.List;

public class AdvancedListener implements AdvancedMessageListener {
    private SpreadGroup[] members;
    private int numberOfReplicas = 2;
    public boolean allReplicasJoined = false;
    private final List<String> messages = new ArrayList<>();

    public AdvancedListener() {}
    public AdvancedListener(int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    @Override
    public synchronized void regularMessageReceived(SpreadMessage message) {
        print("Regular Message Received");
    }

    @Override
    public synchronized void membershipMessageReceived(SpreadMessage spreadMessage) {
        print("Membership Message Received");
        ReplicatedStateMachine.replicas = spreadMessage.getMembershipInfo().getMembers();
        //synchronized (ReplicatedStateMachine.group) {
            if (ReplicatedStateMachine.replicas.length >= ReplicatedStateMachine.numberOfReplicas) {
                ReplicatedStateMachine.group.notifyAll();
            }
        //}
        //notifyAll();

        /*String msg = null;
        try {
            msg = (String) message.getObject();
            print("Membership Message Received: " + msg);
            messages.add(msg);
            members = message.getGroups();
            //members = message.getMembershipInfo().getMembers();
            //Client.members = message.getGroups();
            //print("[Listener] client members: " + Client.members.length);
            //Client.members = message.getMembershipInfo().getMembers();
            if (messages.size() >= numberOfReplicas) {
                allReplicasJoined = true;
                print("Notifying waiting clients");
                notifyAll();
            }
        } catch (SpreadException e) {
            e.printStackTrace();
        }*/
    }

    public SpreadGroup[] getMembers() {
        return members;
    }

    private void print(String message) {
        System.out.println("[Listener] " + message);
    }
}
