package com.in5020.group4.listener;

import com.in5020.group4.Transaction;
import com.in5020.group4.client.Client;
import com.in5020.group4.utils.MessageType;
import spread.*;

public class BasicListener implements BasicMessageListener {
    private final SpreadConnection connection;
    private final String accountName;

    public BasicListener(SpreadConnection connection, String accountName) {
        this.connection = connection;
        this.accountName = accountName;
    }

    @Override
    public void messageReceived(SpreadMessage message) {
        print("message received");
        if (message.isMembership()) {
            print("membership message");
            MembershipInfo membershipInfo = message.getMembershipInfo();
            if (membershipInfo.isCausedByJoin()) {
                print("message caused by join");
                if (!membershipInfo.getJoined().equals(connection.getPrivateGroup())) {
                    print("someone new joined group");
                    SpreadMessage msg = new SpreadMessage();
                    Transaction transaction = new Transaction();
                    transaction.setBalance(1000); // test
                    transaction.setType(MessageType.INITIALIZE_BALANCE);
                    try {
                        msg.setObject(transaction);
                        //msg.setFifo();
                        msg.setReliable();
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
}