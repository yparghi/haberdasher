package com.haberdashervcs.server.example;

import com.haberdashervcs.config.HaberdasherServer;
import com.haberdashervcs.data.hbase.HBaseDatastore;


/**
 * TODO
 */
public class ExampleMain {

    public static void main(String[] args) {
        System.out.println( "Hello Haberdasher!" );

        // TODO: Figure out the right way to set up an example cluster -- with some fake URL?
        /*HaberdasherServer server = HaberdasherServer.builder()
                .withDatastore(new HBaseDatastore())
                .build();*/

        System.out.println("Done!");
    }

}
