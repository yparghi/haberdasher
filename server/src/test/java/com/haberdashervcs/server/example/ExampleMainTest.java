package com.haberdashervcs.server.example;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class ExampleMainTest {

    private Configuration config;
    private HBaseTestingUtility testUtil;
    private Admin admin;
    private Connection conn;


    @Before
    // TODO: Figure out how to set this up correctly in Java.
    public void setUp() throws Exception {
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

    @After
    public void tearDown() throws Exception {
        admin.close();
        testUtil.shutdownMiniCluster();
    }


    // TODO: Move this somewhere sensible.
    @Test
    public void hBaseTestInstanceWorks() throws Exception {
        Table filesTable = conn.getTable(TableName.valueOf("Files"));

        final String rowKey = "someRow";
        final String columnFamilyName = "cfMain";
        final String columnName = "path";

        Put put = new Put(Bytes.toBytes(rowKey));
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                Bytes.toBytes("cellContents"));

        filesTable.put(put);

        Get get = new Get(Bytes.toBytes(rowKey));
        Result result = filesTable.get(get);
        String cellContents = Bytes.toString(result.getValue(
                Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName)));
        assertEquals("cellContents", cellContents);
    }
}
