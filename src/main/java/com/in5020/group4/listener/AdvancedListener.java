package com.in5020.group4.listener;

import com.in5020.group4.ReplicatedStateMachine;
import com.in5020.group4.Transaction;
import spread.AdvancedMessageListener;
import spread.*;

public class AdvancedListener implements AdvancedMessageListener {

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {
        try {
            Transaction transaction = (Transaction) spreadMessage.getObject();

            switch (transaction.getType()) {
                case DEPOSIT -> {
                    ReplicatedStateMachine.replica.deposit(transaction, transaction.getBalance());
                    print("got notified to deposit from other replica");
                }
                default -> print("Regular message received: " + transaction.toString());
            }

            //print("Regular message received: " + transaction.toString());
        } catch (SpreadException e) {
            throw new RuntimeException(e);
        }


        // todo: remove, should only be for membership messages
        synchronized (ReplicatedStateMachine.group) {
            if (ReplicatedStateMachine.replicas.length >= ReplicatedStateMachine.numberOfReplicas) {
                ReplicatedStateMachine.group.notifyAll();
            }
        }
    }

    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {
        print("Membership message received");
        MembershipInfo membershipInfo = spreadMessage.getMembershipInfo();
        ReplicatedStateMachine.replicas = membershipInfo.getMembers();
        synchronized (ReplicatedStateMachine.group) {
            if (ReplicatedStateMachine.replicas.length >= ReplicatedStateMachine.numberOfReplicas) {
                ReplicatedStateMachine.group.notifyAll();
            }
        }
    }

    private void print(String message) {
        System.out.println("[AdvancedListener] " + message);
    }
}
