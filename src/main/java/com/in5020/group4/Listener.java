package com.in5020.group4;

import spread.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Listener implements AdvancedMessageListener {
    private final int numberOfReplicas;
    public boolean allReplicasJoined = false;
    private final List<String> messages = new ArrayList<>();

    public Listener(int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
        /*new Thread(() -> {
            if (!messages.isEmpty()) {
                for (String message : messages) {
                    print(message);
                }
            }
        }).start();*/
    }

    @Override
    public synchronized void regularMessageReceived(SpreadMessage message) {
        String msg = null;
        try {
            msg = (String) message.getObject();

            if (message.isRegular()) {
                //RegularMsgType regMsg = (RegularMsgType) msg;
                print("Regular message: " + message.getType());
                /*switch (message.getType()) {
                    case ACTION:
                        event = Event.ACTION;
                        break;
                    case STATE:
                        log.debug(">>> got state msg " + regMsg.getSender());
                        event = Event.STATE_MESS;
                        break;
                    case CPC:
                        log.debug(">>> got cpc msg " + regMsg.getSender());
                        event = Event.CPC_MESS;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown message type: " + regMsg.getMsgType());
                }*/
            }

        } catch (SpreadException e) {
            throw new RuntimeException(e);
        }
        messages.add(msg);
        print(msg);
    }

    @Override
    public synchronized void membershipMessageReceived(SpreadMessage spreadMessage) {
        String msg = null;
        try {
            msg = (String) spreadMessage.getObject();

            if(spreadMessage.isRegular())
                print("Something wrong happened, membershipMessageReceived got a regular message: " + msg);
            else
                print("New membership message: " + msg);
            messages.add(msg);
        } catch (SpreadException e) {
            throw new RuntimeException(e);
        }

        if (messages.size() >= numberOfReplicas) {
            allReplicasJoined = true;
            print("All replicas joined. Notifying waiting clients...\n");
            notifyAll();
        }
    }

    private void print(String message) {
        System.out.println("[Listener] " + message);
    }
}