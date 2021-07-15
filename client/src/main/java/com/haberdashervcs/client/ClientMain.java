package com.haberdashervcs.client;

import java.util.Arrays;

import com.haberdashervcs.client.commands.Command;
import com.haberdashervcs.client.commands.Commands;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;


public class ClientMain {

    private static final HdLogger LOG = HdLoggers.create(ClientMain.class);

    public static void main(String[] args) {
        try {
            Command command = Commands.parseFromArgs(args);
            command.perform();
        } catch (Throwable ex) {
            LOG.exception(ex, "Command failed: %s", Arrays.toString(args));
        }
    }
}
