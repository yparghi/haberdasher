package com.haberdashervcs.hbasetestserver;

import org.apache.log4j.Logger;


/**
 * TODO
 */
public class HBaseTestServerMain {

    private static final Logger LOG =Logger.getLogger(HBaseTestServerMain.class);

    public static void main(String[] args) throws Exception {
        LOG.info("Starting the HBase test server...");

        HBaseTestClusterManager testCluster = HBaseTestClusterManager.getInstance();
        testCluster.setUp();
    }
}
