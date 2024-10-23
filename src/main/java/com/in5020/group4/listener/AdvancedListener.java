package com.in5020.group4.listener;

import com.in5020.group4.ReplicatedStateMachine;
import com.in5020.group4.Transaction;
import com.in5020.group4.utils.TransactionType;
import spread.AdvancedMessageListener;
import spread.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdvancedListener implements AdvancedMessageListener {
    private final AtomicBoolean alreadyInitializedBalance = new AtomicBoolean(false);

    public AdvancedListener() {
        String outputFile = System.getProperty("user.dir") + "/src/main/java/com/in5020/group4/utils/broadcast_output_" + ReplicatedStateMachine.replicaName + ".txt";
        try (FileWriter fileWriter = new FileWriter(outputFile, false)) {
            // Opening the file in write mode without appending clears the file
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {
        try {
            Transaction transaction = (Transaction) spreadMessage.getObject();

            List<Transaction> executedTransactions = ReplicatedStateMachine.replica.getExecutedTransactions();
            for (Transaction executedTransaction : executedTransactions) {
                if (executedTransaction.getUniqueId().equals(transaction.getUniqueId())) {
                    // Command already executed
                    return;
                }
            }

            String output = "";
            // receive broadcast messages and execute
            switch (transaction.getType()) {
                case DEPOSIT -> {
                    ReplicatedStateMachine.replica.deposit(transaction, transaction.getBalance());
                    print("got notified to deposit " + transaction.getBalance() + " , transaction id: " + transaction.uniqueId); // todo: remove print statement
                    output = transaction.getUniqueId() + " " + transaction.getCommand() + " " + transaction.getBalance();
                }
                case INTEREST -> {
                    ReplicatedStateMachine.replica.addInterest(transaction, transaction.getPercent());
                    print("got notified to add " + transaction.getPercent() + " % interest, transaction id: " + transaction.uniqueId); // todo: remove print statement
                    output = transaction.getUniqueId() + " " + transaction.getCommand() + " " + transaction.getPercent();
                }
                case SYNCED_BALANCE -> {
                    ReplicatedStateMachine.replica.getSyncedBalance(transaction);
                    print("got notified to get synced balance, transaction id: " + transaction.uniqueId); // todo: remove print statement
                    output = transaction.getUniqueId() + " " + transaction.getCommand();
                }
                case UPDATE_BALANCE -> {
                    if (alreadyInitializedBalance.get()) return;

                    ReplicatedStateMachine.replica.setBalance(transaction.getBalance());
                    print("got notified to update balance to " + transaction.getBalance() + ", transaction id: " + transaction.uniqueId + ", previous balance: " + ReplicatedStateMachine.replica.getQuickBalance());
                    output = transaction.getUniqueId() + " " + transaction.getCommand() + " " + transaction.getBalance();
                    alreadyInitializedBalance.set(true);
                }
                default -> print("Regular message received: " + transaction.command);
            }
            writeOutput(output);
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
                transaction.setCommand("updateBalance " + ReplicatedStateMachine.replicaName);
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

    private static void writeOutput(String output) {
        BufferedWriter writer = null;
        String fileName = System.getProperty("user.dir") + "/src/main/java/com/in5020/group4/utils/broadcast_output_" + ReplicatedStateMachine.replicaName + ".txt";
        try {
            writer = new BufferedWriter(new FileWriter(fileName, true));
            writer.write(output);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void print(String message) {
        System.out.println("[AdvancedListener]: " + message);
    }
}
