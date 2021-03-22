package com.haberdashervcs.server.example;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.protobuf.generated.TableProtos;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;


public class ExampleMainTest {

    private Configuration config;
    private HBaseTestingUtility testUtil;
    private Admin admin;


    @Before
    public void setUp() throws Exception {
        config = HBaseConfiguration.create();
        testUtil = new HBaseTestingUtility(config);
        testUtil.startMiniCluster();
        admin = testUtil.getAdmin();

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
        //admin.createTable(filesTableDesc);
    }

    @After
    public void tearDown() throws Exception {
        admin.close();
        testUtil.shutdownMiniCluster();
    }


    // TODO: Move this somewhere sensible.
    @Test
    public void hBaseTestInstanceWorks() throws Exception {
        assertTrue(true);
    }
}
