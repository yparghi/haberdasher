package com.haberdashervcs.server.example;

import com.haberdashervcs.server.data.hbase.HBaseTestClusterManager;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ExampleMainTest {

    private HBaseTestClusterManager clusterManager;
    private Connection conn;


    @Before
    public void setUp() throws Exception {
        clusterManager = HBaseTestClusterManager.getInstance();
        clusterManager.setUp();
        conn = clusterManager.getConn();
    }

    @After
    public void tearDown() throws Exception {
        clusterManager.tearDownBetweenTests();
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


    @Test
    public void basicDatastoreOperations() throws Exception {
        // TODO
        assertTrue(true);
    }
}
