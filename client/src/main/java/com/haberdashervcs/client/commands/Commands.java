package com.haberdashervcs.client.commands;

import java.util.ArrayList;
import java.util.Arrays;


public class Commands {

    public static Command parseFromArgs(String[] args) {
        final String commandWord = args[0];
        ArrayList<String> otherArgs = new ArrayList<>(Arrays.asList(args));
        otherArgs.remove(0);

        if (commandWord.equals("init")) {
            return new InitCommand(otherArgs);
        }

        throw new IllegalArgumentException("Unknown command: " + commandWord);
    }
}
