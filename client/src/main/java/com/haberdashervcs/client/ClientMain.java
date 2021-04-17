package com.haberdashervcs.client;

import java.util.Arrays;

import com.haberdashervcs.client.commands.Command;
import com.haberdashervcs.client.commands.Commands;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;


// $ ~/src/haberdasher/client/hd.sh init "localhost:15367" some_org some_repo
public class ClientMain {

    private static final HdLogger LOG = HdLoggers.create(ClientMain.class);

    public static void main(String[] args) {
        System.out.println("Haberdasher client: " + Arrays.toString(args));

        try {
            Command command = Commands.parseFromArgs(args);
            command.perform();
        } catch (Exception ex) {
            // TODO
            System.out.println("Command exception!: " + ex);
            ex.printStackTrace();
        }
    }
}
