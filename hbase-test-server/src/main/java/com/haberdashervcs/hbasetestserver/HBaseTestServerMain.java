package com.haberdashervcs.hbasetestserver;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.log4j.Logger;


/**
 * TODO
 */
public class HBaseTestServerMain {

    private static final Logger LOG =Logger.getLogger(HBaseTestServerMain.class);

    public static void main(String[] args) throws Exception {
        LOG.info("Starting the HBase test server...");

        Configuration config = HBaseConfiguration.create();
        HBaseTestingUtility testUtil = new HBaseTestingUtility(config);
        testUtil.startMiniCluster();

        System.out.println("Master: " + testUtil.getConfiguration().get("hbase.master", "<none>"));
        System.out.println("Done.");
    }
}
