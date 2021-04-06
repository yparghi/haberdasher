package com.haberdashervcs.client;

import java.util.Arrays;


public class ClientMain {

    public static void main(String[] args) {
        // TODO Logging using a 'common' module with HdLogger

        System.out.println("Haberdasher client: " + Arrays.toString(args));

        MainArgs mainArgs = MainArgs.parseFromArgs(args);
        mainArgs.perform();
    }
}
