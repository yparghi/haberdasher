package com.haberdashervcs.hbasetestserver;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.MiniHBaseCluster;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
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

        Path zkPath = Files.createTempDirectory("hdtest-zk");
        Path hBaseRootPath = Files.createTempDirectory("hdtest-hbase");
        Path hBaseTmpPath = Files.createTempDirectory("hdtest-hbasetmp");

        Configuration conf = HBaseConfiguration.create();
        conf.clear();
        conf.set(HConstants.ZOOKEEPER_CLIENT_PORT, "2181");
        //conf.set("hbase.wal.dir", "/tmp/yashhbase/wal");
        conf.set("hbase.rootdir", hBaseRootPath.toAbsolutePath().toString());
        conf.set("hbase.tmp.dir", hBaseTmpPath.toAbsolutePath().toString());
        conf.set("hbase.cluster.distributed", "false");
        // This prevents error messages with the WAL -- by disabling it, I think.
        conf.set("hbase.unsafe.stream.capability.enforce", "false");

        HBaseTestingUtility testUtil = new HBaseTestingUtility();  // TEMP

        MiniZooKeeperCluster zk = new MiniZooKeeperCluster(conf);
        zk.setDefaultClientPort(2181);
        zk.startup(zkPath.toFile());

        // Current bug:
        // 2021-04-10 15:41:49,942 ERROR [master/192.168.1.2:16000:becomeActiveMaster] wal.AsyncFSWALProvider (AsyncFSWALProvider.java:createAsyncWriter(117)) - The RegionServer async write ahead log provider relies on the ability to call hflush for proper operation during component failures, but the current FileSystem does not support doing so. Please check the config value of 'hbase.wal.dir' and ensure it points to a FileSystem mount that has suitable capabilities for output streams.
        // See: https://hbase.apache.org/2.0/book.html
        MiniHBaseCluster hBase = new MiniHBaseCluster(conf, 1);

        System.out.println("Done with cluster setup...");

        //Connection conn = testUtil.getConnection();
        //createTables(conn);
        System.out.println("Done with test data.");
    }

    private static void createTables(Connection conn) throws Exception {
        LOG.info("Creating test tables.");

        Admin admin = conn.getAdmin();

        TableDescriptor filesTableDesc = TableDescriptorBuilder
                .newBuilder(TableName.valueOf("Files"))
                .setColumnFamily(ColumnFamilyDescriptorBuilder.of("cfMain"))
                .build();
        admin.createTable(filesTableDesc);

        TableDescriptor foldersTableDesc = TableDescriptorBuilder
                .newBuilder(TableName.valueOf("Folders"))
                .setColumnFamily(ColumnFamilyDescriptorBuilder.of("cfMain"))
                .build();
        admin.createTable(foldersTableDesc);

        TableDescriptor commitsTableDesc = TableDescriptorBuilder
                .newBuilder(TableName.valueOf("Commits"))
                .setColumnFamily(ColumnFamilyDescriptorBuilder.of("cfMain"))
                .build();
        admin.createTable(commitsTableDesc);
    }
}
