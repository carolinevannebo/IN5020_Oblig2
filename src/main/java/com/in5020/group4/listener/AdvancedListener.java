package com.in5020.group4.listener;

import com.in5020.group4.ReplicatedStateMachine;
import spread.AdvancedMessageListener;
import spread.*;

public class AdvancedListener implements AdvancedMessageListener {
    private static AdvancedListener instance;

    public static AdvancedListener getInstance() {
        if (instance == null) {
            instance = new AdvancedListener();
        }
        return instance;
    }

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {
        System.out.println("[Listener]: regular message received");
//        MembershipInfo membershipInfo = spreadMessage.getMembershipInfo();
//        ReplicatedStateMachine.replicas = membershipInfo.getMembers();
//        if (ReplicatedStateMachine.replicas.length >= ReplicatedStateMachine.numberOfReplicas) {
//            notifyAll();
//        }
        synchronized (ReplicatedStateMachine.group) {
            if (ReplicatedStateMachine.replicas.length >= ReplicatedStateMachine.numberOfReplicas) {
                ReplicatedStateMachine.group.notifyAll();
            }
        }
    }

    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {
        System.out.println("[Listener]: Membership message received");
        MembershipInfo membershipInfo = spreadMessage.getMembershipInfo();
        ReplicatedStateMachine.replicas = membershipInfo.getMembers();
//        if (ReplicatedStateMachine.replicas.length >= ReplicatedStateMachine.numberOfReplicas) {
//            notifyAll();
//        }
        synchronized (ReplicatedStateMachine.group) {
            if (ReplicatedStateMachine.replicas.length >= ReplicatedStateMachine.numberOfReplicas) {
                ReplicatedStateMachine.group.notifyAll();
            }
        }
    }
//    private SpreadGroup[] members;
//    private int numberOfReplicas = 2;
//    public boolean allReplicasJoined = false;
//    private final List<String> memberShipMessages = new ArrayList<>();
//    private final ReplicatedStateMachine replicatedStateMachine;
//
//    public AdvancedListener(ReplicatedStateMachine replicatedStateMachine) {
//        this.replicatedStateMachine = replicatedStateMachine;
//    }
//    public AdvancedListener(int numberOfReplicas, ReplicatedStateMachine replicatedStateMachine) {
//        this.numberOfReplicas = numberOfReplicas;
//        this.replicatedStateMachine = replicatedStateMachine;
//    }
//
//    @Override
//    public synchronized void regularMessageReceived(SpreadMessage spreadMessage) {
//        print("Regular Message Received: " + spreadMessage);
//    }
//
//    @Override
//    public synchronized void membershipMessageReceived(SpreadMessage spreadMessage) {
//        print("Membership Message Received: " + spreadMessage);
//        SpreadGroup[] replicas = spreadMessage.getMembershipInfo().getMembers();
//        replicatedStateMachine.handleMembershipChange(replicas);
//        print("Membership Message Received, old length: " + memberShipMessages.size());
//        try {
//            memberShipMessages.add((String) spreadMessage.getObject());
//            print("new: " + memberShipMessages.size());
//        } catch (SpreadException e) {
//            throw new RuntimeException(e);
//        }
//
//        ReplicatedStateMachine.replicas = spreadMessage.getMembershipInfo().getMembers();
//        synchronized (ReplicatedStateMachine.connection){
//            if (ReplicatedStateMachine.replicas.length >= ReplicatedStateMachine.numberOfReplicas) {
//            if (memberShipMessages.size() >= ReplicatedStateMachine.numberOfReplicas) {
//                print("Got remaining replicas, notifying...");
//                notifyAll();
//            }
//        }
//
//        synchronized (ReplicatedStateMachine.group) {
//            if (ReplicatedStateMachine.replicas.length >= ReplicatedStateMachine.numberOfReplicas) {
//                ReplicatedStateMachine.group.notifyAll();
//            }
//        }
//
//        if (ReplicatedStateMachine.replicas.length > ReplicatedStateMachine.numberOfReplicas) {
//            SpreadMessage message = new SpreadMessage();
//            message.addGroup(ReplicatedStateMachine.group);
//            message.setFifo();
//
//            try {
//                message.setObject("Joined: " + spreadMessage.getMembershipInfo().getJoined());
//                ReplicatedStateMachine.connection.multicast(message);
//            } catch (SpreadException e) {
//                e.printStackTrace();
//            }
//        }
//
//        notifyAll();
//
//        String msg = null;
//        try {
//            msg = (String) message.getObject();
//            print("Membership Message Received: " + msg);
//            messages.add(msg);
//            members = message.getGroups();
//            //members = message.getMembershipInfo().getMembers();
//            //Client.members = message.getGroups();
//            //print("[Listener] client members: " + Client.members.length);
//            //Client.members = message.getMembershipInfo().getMembers();
//            if (messages.size() >= numberOfReplicas) {
//                allReplicasJoined = true;
//                print("Notifying waiting clients");
//                notifyAll();
//            }
//        } catch (SpreadException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public SpreadGroup[] getMembers() {
//        return members;
//    }

    private void print(String message) {
        System.out.println("[Listener] " + message);
    }
}
