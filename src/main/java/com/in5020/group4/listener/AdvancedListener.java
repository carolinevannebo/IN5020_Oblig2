package com.in5020.group4.listener;

import com.in5020.group4.ReplicatedStateMachine;
import spread.AdvancedMessageListener;
import spread.*;

public class AdvancedListener implements AdvancedMessageListener {

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {
        try {
            String msg = (String) spreadMessage.getObject();
            print("Regular message received: " + msg);
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
