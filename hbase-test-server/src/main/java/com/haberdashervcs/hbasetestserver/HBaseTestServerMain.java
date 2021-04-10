package com.haberdashervcs.hbasetestserver;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.LocalHBaseCluster;
import org.apache.hadoop.hbase.MiniHBaseCluster;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.zookeeper.MiniZooKeeperCluster;
import org.apache.log4j.Logger;


/**
 * TODO
 */
// TEMP: 2021-04-10 14:21:11,318 INFO  [Listener at localhost/51706] hbase.HBaseCommonTestingUtility (HBaseTestingUtility.java:startMiniHBaseCluster(1192)) - Minicluster is up; activeMaster=192.168.1.2,51717,1618078840395
// 2nd run (still random): Minicluster is up; activeMaster=192.168.1.2,52026,1618079234864
// $ echo "hello" | nc 192.168.1.2 52026
// - this works, 'localhost' fails
//
// Debug run: 52479
// Conf: "hbase.masters"

public class HBaseTestServerMain {

    private static final Logger LOG =Logger.getLogger(HBaseTestServerMain.class);

    public static void main(String[] args) throws Exception {
        LOG.info("Starting the HBase test server...");

        Configuration conf = HBaseConfiguration.create();
        conf.clear();
        conf.set(HConstants.ZOOKEEPER_CLIENT_PORT, "2181");

        HBaseTestingUtility testUtil = new HBaseTestingUtility();  // TEMP

        MiniZooKeeperCluster zk = new MiniZooKeeperCluster(conf);
        zk.setDefaultClientPort(2181);
        Path zkPath = Files.createTempDirectory("yashzk");
        zk.startup(zkPath.toFile());

        MiniHBaseCluster hBase = new MiniHBaseCluster(conf, 1);
    }
}
