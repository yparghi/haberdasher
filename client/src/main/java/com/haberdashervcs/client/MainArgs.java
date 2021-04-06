package com.haberdashervcs.client;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

public class MainArgs {

    static MainArgs parseFromArgs(String[] args) {
        return new MainArgs(args);
    }

    static class UnsupportedArgsException extends RuntimeException {
        private UnsupportedArgsException(String message) {
            super(message);
        }
    }


    private String command = null;
    private List<String> commandSpecificArgs = new ArrayList<>();

    private MainArgs(String[] args) {
        parseArgs(args);
    }

    private void parseArgs(String[] args) {
        int i = 0;

        for (i = 0; i < args.length; ++i) {
            final String arg = args[i];

            if (arg.startsWith("--")) {  // TODO filter out top-level/universal flags here.
                //
            } else if (command == null) {
                command = arg;
            } else {
                commandSpecificArgs.add(arg);
            }
        }
    }

    void perform() {
        if (command == null) {
            throw new UnsupportedArgsException("No command given. Choices are: init");
        }

        if (command.equals("init")) {

        } else {
            throw new UnsupportedArgsException("No such command: " + command);
        }
    }
}
