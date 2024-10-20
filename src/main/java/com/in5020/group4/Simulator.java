//package com.in5020.group4;
//
//import spread.SpreadConnection;
//
//public class Simulator {
//    public static void main(String[] args) throws InterruptedException {
//        Thread test1 = new Thread(() -> {
//            args[3] = "Rep1.txt";
//            SpreadConnection connection = new SpreadConnection();
//            new ReplicatedStateMachine(args, connection);
//
//        });
//        Thread test2 = new Thread(() -> {
//            args[3] = "Rep2.txt";
//            SpreadConnection connection = new SpreadConnection();
//            new ReplicatedStateMachine(args, connection);
//        });
//
//        test1.start();
//        for (int i = 0; i < 2000; i++) {
//            continue;
//        }
//        test2.start();
//    }
//}
