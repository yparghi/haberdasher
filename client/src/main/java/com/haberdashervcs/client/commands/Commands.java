package com.haberdashervcs.client.commands;

import java.util.ArrayList;
import java.util.Arrays;

import com.haberdashervcs.client.checkout.CheckoutCommand;
import com.haberdashervcs.client.push.PushCommand;


public class Commands {

    public static Command parseFromArgs(String[] args) {
        final String commandWord = args[0];
        ArrayList<String> otherArgs = new ArrayList<>(Arrays.asList(args));
        otherArgs.remove(0);

        if (commandWord.equals("init")) {
            return new InitCommand(otherArgs);

        } else if (commandWord.equals("checkout")) {
            return new CheckoutCommand(otherArgs);

        } else if (commandWord.equals("push")) {
            return new PushCommand(otherArgs);

        } else if (commandWord.equals("commit")) {
            // TODO: This should really be broken up with an add/scan command first, for staging changes.
            return new CommitCommand(otherArgs);
        }

        throw new IllegalArgumentException("Unknown command: " + commandWord);
    }
}
