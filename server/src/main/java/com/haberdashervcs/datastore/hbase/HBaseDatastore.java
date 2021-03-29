package com.haberdashervcs.datastore.hbase;

import com.haberdashervcs.datastore.HdDatastore;
import com.haberdashervcs.operations.change.AddChange;
import com.haberdashervcs.operations.change.ApplyChangesetResult;
import com.haberdashervcs.operations.change.Changeset;
import com.haberdashervcs.operations.CheckoutStream;
import com.haberdashervcs.util.logging.HdLogger;
import com.haberdashervcs.util.logging.HdLoggers;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.UUID;


public final class HBaseDatastore implements HdDatastore {

    private static HdLogger LOG = HdLoggers.create(HBaseDatastore.class);


    private final Connection conn;

    public HBaseDatastore(Connection conn) {
        this.conn = conn;
    }

    @Override
    public ApplyChangesetResult applyChangeset(Changeset changeset) {
        try {
            return applyChangesetInternal(changeset);
        } catch (IOException ioEx) {
            LOG.exception(ioEx, "Error applying Changeset");
            return ApplyChangesetResult.forStatus(ApplyChangesetResult.Status.FAILED);
        }
    }


    @Override
    public CheckoutStream checkout(String branchName, String path) {
        // TODO
        return null;
    }


    private ApplyChangesetResult applyChangesetInternal(Changeset changeset) throws IOException {
        // TODO: Some Transaction (or TransactionManager from the config) should do this, maybe by using the datastore
        // instance.
        final String branch = "main";
        final String branchHeadCommit = "head_commit_0x123"; // TODO: getHeadCommit(branchName) or something?
        final String thisCommitId = UUID.randomUUID().toString();

        final Table filesTable = conn.getTable(TableName.valueOf("Files"));
        final String columnFamilyName = "cfMain";

        for (AddChange addChange : changeset.getAddChanges()) {
            final String rowKey = "someRow";
            Put put = new Put(Bytes.toBytes(rowKey));

            put.addColumn(
                    Bytes.toBytes(columnFamilyName),
                    Bytes.toBytes("commit_id"),
                    Bytes.toBytes(thisCommitId));

            // TODO: Make change_type some enum like add / modify_keyframe / modify_diff / modify_binarydiff?
            put.addColumn(
                    Bytes.toBytes(columnFamilyName),
                    Bytes.toBytes("change_type"),
                    Bytes.toBytes("add"));

            put.addColumn(
                    Bytes.toBytes(columnFamilyName),
                    Bytes.toBytes("fileContents"),
                    addChange.getContents());

            filesTable.put(put);
        }

        return ApplyChangesetResult.forStatus(ApplyChangesetResult.Status.OK);
    }
}
