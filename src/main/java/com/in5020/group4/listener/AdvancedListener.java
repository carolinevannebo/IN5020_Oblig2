package com.in5020.group4.listener;

import com.in5020.group4.ReplicatedStateMachine;
import com.in5020.group4.Transaction;
import com.in5020.group4.utils.TransactionType;
import spread.AdvancedMessageListener;
import spread.*;

import java.util.List;

public class AdvancedListener implements AdvancedMessageListener {

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {
        try {
            Transaction transaction = (Transaction) spreadMessage.getObject();

            List<Transaction> executedTransactions = ReplicatedStateMachine.replica.getExecutedTransactions();
            if (transaction.getType().equals(TransactionType.UPDATE_BALANCE)) { // make sure balance isn't updated twice
                for (Transaction executedTransaction : executedTransactions) {
                    if (executedTransaction.getCommand().equals(transaction.getCommand())) {
                        // Command already executed
                        return;
                    }
                }
                return;
            }

            // receive broadcast messages and execute
            switch (transaction.getType()) {
                case DEPOSIT -> {
                    ReplicatedStateMachine.replica.deposit(transaction, transaction.getBalance());
                    print("got notified to deposit " + transaction.getBalance() + " , transaction id: " + transaction.uniqueId); // todo: remove print statement
                }
                case INTEREST -> {
                    ReplicatedStateMachine.replica.addInterest(transaction, transaction.getPercent());
                    print("got notified to add " + transaction.getPercent() + " % interest, transaction id: " + transaction.uniqueId); // todo: remove print statement
                }
                case SYNCED_BALANCE -> {
                    ReplicatedStateMachine.replica.getSyncedBalance(transaction);
                    print("got notified to get synced balance, transaction id: " + transaction.uniqueId); // todo: remove print statement
                }
                case UPDATE_BALANCE -> {
                    print("got notified to update balance to " + transaction.getBalance() +", transaction id: " + transaction.uniqueId + ", previous balance: " + ReplicatedStateMachine.replica.getQuickBalance());
                    ReplicatedStateMachine.replica.setBalance(transaction.getBalance());
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
            synchronized (ReplicatedStateMachine.group) { // notify waiting clients when expected number of replicas are present
                if (ReplicatedStateMachine.replicas.length >= ReplicatedStateMachine.numberOfReplicas) {
                    ReplicatedStateMachine.group.notifyAll();
                }
            }

            SpreadGroup joined = membershipInfo.getJoined();
            if (!ReplicatedStateMachine.connection.getPrivateGroup().equals(joined)) { // Updating balance of newly joined replica
                Transaction transaction = new Transaction();
                transaction.setUniqueId(ReplicatedStateMachine.replicaName + " " + ReplicatedStateMachine.replica.getOutstandingCounter());
                transaction.setCommand("updateBalance");
                transaction.setType(TransactionType.UPDATE_BALANCE);
                transaction.setBalance(ReplicatedStateMachine.replica.getQuickBalance());

                ReplicatedStateMachine.sendMessage(transaction);

                // broadcast executed transactions to newly joined replica
                if (ReplicatedStateMachine.replicas.length > ReplicatedStateMachine.numberOfReplicas) {
                    ReplicatedStateMachine.numberOfReplicas = ReplicatedStateMachine.replicas.length;
                    List<Transaction> missedTransactions = ReplicatedStateMachine.replica.getExecutedTransactions();
                    ReplicatedStateMachine.sendMessages(missedTransactions);
                }
            }
        } else if (membershipInfo.isCausedByLeave() || membershipInfo.isCausedByDisconnect()) {
            ReplicatedStateMachine.replicas = membershipInfo.getMembers();
        }
    }

    private void print(String message) {
        System.out.println("[AdvancedListener]: " + message);
    }
}
