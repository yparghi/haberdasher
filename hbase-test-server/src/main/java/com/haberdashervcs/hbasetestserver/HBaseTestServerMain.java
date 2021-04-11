package com.haberdashervcs.hbasetestserver;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.MiniHBaseCluster;
import org.apache.hadoop.hbase.zookeeper.MiniZooKeeperCluster;
import org.apache.log4j.Logger;


/**
 * TODO
 */
public class HBaseTestServerMain {

    private static final Logger LOG =Logger.getLogger(HBaseTestServerMain.class);

    public static void main(String[] args) throws Exception {
        LOG.info("Starting the HBase test server...");

        Path zkPath = Files.createTempDirectory("hdtest-zk");
        Path hBaseRootPath = Files.createTempDirectory("hdtest-hbase");
        Path hBaseTmpPath = Files.createTempDirectory("hdtest-hbasetmp");

        Configuration conf = HBaseConfiguration.create();
        conf.clear();
        conf.set(HConstants.ZOOKEEPER_CLIENT_PORT, "2181");
        conf.set("hbase.rootdir", hBaseRootPath.toAbsolutePath().toString());
        conf.set("hbase.tmp.dir", hBaseTmpPath.toAbsolutePath().toString());
        conf.set("hbase.cluster.distributed", "false");
        // This prevents error messages with the WAL -- by disabling it, I think.
        conf.set("hbase.unsafe.stream.capability.enforce", "false");

        HBaseTestingUtility testUtil = new HBaseTestingUtility();  // TEMP

        MiniZooKeeperCluster zk = new MiniZooKeeperCluster(conf);
        zk.setDefaultClientPort(2181);
        zk.startup(zkPath.toFile());

        // This constructor call apparently starts the cluster??
        MiniHBaseCluster hBase = new MiniHBaseCluster(conf, 1);

        System.out.println("Done with cluster setup.");
        hBase.join();
    }
}
