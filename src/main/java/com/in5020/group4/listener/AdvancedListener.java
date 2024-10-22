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
                    print("got notified to deposit " + transaction.getBalance() + " , transaction id: " + transaction.uniqueId);
                }
                case INTEREST -> {
                    ReplicatedStateMachine.replica.addInterest(transaction, transaction.getPercent());
                    print("got notified to add " + transaction.getPercent() + " % interest, transaction id: " + transaction.uniqueId);
                }
                case SYNCED_BALANCE -> {
                    ReplicatedStateMachine.replica.getSyncedBalance(transaction);
                    print("got notified to get synced balance, transaction id: " + transaction.uniqueId);
                }
                default -> print("Regular message received: " + transaction.command);
            }
        } catch (SpreadException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {
        print("Membership message received");
        MembershipInfo membershipInfo = spreadMessage.getMembershipInfo();

        if (membershipInfo.isCausedByJoin()) {
            ReplicatedStateMachine.replicas = membershipInfo.getMembers();
            synchronized (ReplicatedStateMachine.group) {
                if (ReplicatedStateMachine.replicas.length >= ReplicatedStateMachine.numberOfReplicas) {
                    ReplicatedStateMachine.group.notifyAll();
                }
            }
        }

        /* todo: All initial replicas will start with the same state: balance = 0.0. After that, the
            client should handle new joins by setting the state of the new replica, and the
            state should be consistent across all the replicas: the balance of all replicas
            should be the same.*/
    }

    private void print(String message) {
        System.out.println("[AdvancedListener]: " + message);
    }
}
