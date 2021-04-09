package com.haberdashervcs.server.example;

import com.haberdashervcs.server.config.HaberdasherServer;
import com.haberdashervcs.server.core.logging.HdLogger;
import com.haberdashervcs.server.core.logging.HdLoggers;
import com.haberdashervcs.server.datastore.hbase.HBaseDatastore;
import com.haberdashervcs.server.frontend.JettyHttpFrontend;


/**
 * TODO
 */
public class ExampleServerMain {

    private static HdLogger LOG = HdLoggers.create(ExampleServerMain.class);

    public static void main(String[] args) throws Exception {
        System.out.println( "Hello Haberdasher!" );

        // TODO: Figure out the right way to set up an example cluster -- with some fake URL?
        HaberdasherServer server = HaberdasherServer.builder()
                .withDatastore(HBaseDatastore.forConnection(null /*TODO!*/))
                .withFrontend(new JettyHttpFrontend())
                .build();

        server.start();

        System.out.println("Serving...");
    }
}
