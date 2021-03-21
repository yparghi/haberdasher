package com.haberdashervcs.server.example;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.apache.hadoop.hbase.HBaseTestingUtility;


public class ExampleMainTest  {

    @Test
    public void shouldAnswerWithTrue() {
        assertTrue( true );
    }

    // TODO: Move this somewhere sensible.
    @Test
    public void hBaseTestInstanceWorks() {
        HBaseTestingUtility testUtil = new HBaseTestingUtility();
        // HBaseTestingUtility.shutdownMiniCluster()
    }
}
