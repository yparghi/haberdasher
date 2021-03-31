package com.haberdashervcs.server.example;

import com.haberdashervcs.server.core.logging.HdLogger;
import com.haberdashervcs.server.core.logging.HdLoggers;


/**
 * TODO
 */
public class ExampleMain {

    private static HdLogger LOG = HdLoggers.create(ExampleMain.class);

    public static void main(String[] args) {
        System.out.println( "Hello Haberdasher!" );

        // TODO: Figure out the right way to set up an example cluster -- with some fake URL?
        /*HaberdasherServer server = HaberdasherServer.builder()
                .withDatastore(new HBaseDatastore())
                .build();*/

        System.out.println("Done!");
    }

}
