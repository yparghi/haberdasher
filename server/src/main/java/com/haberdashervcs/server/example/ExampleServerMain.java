package com.haberdashervcs.server.example;

import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.server.config.HaberdasherServer;
import com.haberdashervcs.server.datastore.hbase.HBaseDatastore;
import com.haberdashervcs.server.frontend.JettyHttpVcsFrontend;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;


/**
 * TODO
 */
public class ExampleServerMain {

    private static final HdLogger LOG = HdLoggers.create(ExampleServerMain.class);


    public static void main(String[] args) throws Exception {
        LOG.info("Hello Haberdasher!");

        Configuration conf = HBaseConfiguration.create();
        conf.clear();
        Connection conn = ConnectionFactory.createConnection(conf);

        HBaseDatastore datastore = HBaseDatastore.forConnection(conn);

        datastore.loadTestData();

        HaberdasherServer server = HaberdasherServer.builder()
                .withDatastore(datastore)
                .withVcsFrontend(JettyHttpVcsFrontend.forDatastore(datastore))
                .build();

        server.start();

        LOG.info("Serving...");
    }
}
