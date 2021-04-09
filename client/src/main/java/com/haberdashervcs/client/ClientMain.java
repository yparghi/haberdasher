package com.haberdashervcs.client;

import java.util.Arrays;

import com.haberdashervcs.client.commands.Command;
import com.haberdashervcs.client.commands.Commands;


public class ClientMain {

    public static void main(String[] args) {
        // TODO Logging using a 'common' module with HdLogger

        System.out.println("Haberdasher client: " + Arrays.toString(args));

        Command command = Commands.parseFromArgs(args);
        command.perform();
    }
}
