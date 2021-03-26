package com.haberdashervcs.server.data.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;

import static com.google.common.base.Preconditions.checkState;


/**
 * Manages the once-per-test-run setup and teardown of a MiniHBaseCluster via HBaseTestingUtility.
 */
public final class HBaseTestClusterManager {

    private static final HBaseTestClusterManager INSTANCE = new HBaseTestClusterManager();

    public static HBaseTestClusterManager getInstance() {
        return INSTANCE;
    }


    private Configuration config;
    private HBaseTestingUtility testUtil;
    private Admin admin;
    private Connection conn;
    private boolean initialized = false;

    private HBaseTestClusterManager() {}

    // Re. teardown, the HBaseTestingUtility has its own shutdown hook.
    public synchronized void setUp() throws Exception {
        if (initialized) {
            return;
        } else {
            initialized = true;
        }

        config = HBaseConfiguration.create();
        testUtil = new HBaseTestingUtility(config);
        testUtil.startMiniCluster();
        conn = testUtil.getConnection();
        admin = conn.getAdmin();

        createTables();
    }

    // NOTE from http://hbase.apache.org/book.html#number.of.cfs:
    // "HBase currently does not do well with anything above two or three column families so keep the number of column
    // families in your schema low."
    private void createTables() throws Exception {
        TableDescriptor filesTableDesc = TableDescriptorBuilder
                .newBuilder(TableName.valueOf("Files"))
                .setColumnFamily(ColumnFamilyDescriptorBuilder.of("cfMain"))
                .build();
        admin.createTable(filesTableDesc);
    }

    public Connection getConn() {
        checkState(initialized);
        return conn;
    }
}
