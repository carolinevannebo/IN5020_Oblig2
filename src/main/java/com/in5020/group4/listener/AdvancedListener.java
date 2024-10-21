package com.in5020.group4.listener;

import com.in5020.group4.ReplicatedStateMachine;
import com.in5020.group4.Transaction;
import com.in5020.group4.utils.TransactionType;
import spread.AdvancedMessageListener;
import spread.*;

public class AdvancedListener implements AdvancedMessageListener {

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {
        try {
            Transaction transaction = (Transaction) spreadMessage.getObject();

            print("Regular message received, Transaction: " + transaction);
            switch (transaction.getType()) {
                case DEPOSIT -> {
                    ReplicatedStateMachine.replica.deposit(transaction, transaction.getBalance());
                    print("got notified to deposit from other replica");
                }
                case INTEREST -> print("got notified to interest from other replica");
                default -> print("Regular message received: " + transaction.toString());
            }

            if (transaction.getType() == TransactionType.DEPOSIT) {
                print("got notified to deposit from other replica");
                ReplicatedStateMachine.replica.deposit(transaction, transaction.getBalance());
            }
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

        if (ReplicatedStateMachine.allReplicasPresent) {
            try {
                Transaction transaction = new Transaction();
                String msg = (String) spreadMessage.getObject();
                String[] stringParts = msg.split(" ");
                transaction.setUniqueId(stringParts[0] + stringParts[1]);
                transaction.setCommand(stringParts[2]);
                transaction.setBalance(Double.parseDouble(stringParts[3]));
                transaction.setType(TransactionType.valueOf(stringParts[4]));
                // todo: string array, str[0...3], use setters

                print("Regular message received, Transaction: " + transaction);
                switch (transaction.getType()) {
                    case DEPOSIT -> {
                        ReplicatedStateMachine.replica.deposit(transaction, transaction.getBalance());
                        print("got notified to deposit from other replica");
                    }
                    case INTEREST -> print("got notified to interest from other replica");
                    default -> print("Regular message received: " + transaction.toString());
                }

                if (transaction.getType() == TransactionType.DEPOSIT) {
                    print("got notified to deposit from other replica");
                    ReplicatedStateMachine.replica.deposit(transaction, transaction.getBalance());
                }
            } catch (SpreadException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void print(String message) {
        System.out.println("[AdvancedListener] " + message);
    }
}
