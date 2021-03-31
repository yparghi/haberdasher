package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import com.haberdashervcs.server.core.logging.HdLogger;
import com.haberdashervcs.server.core.logging.HdLoggers;
import com.haberdashervcs.server.datastore.HdDatastore;
import com.haberdashervcs.server.operations.CheckoutResult;
import com.haberdashervcs.server.operations.FolderListing;
import com.haberdashervcs.server.operations.change.AddChange;
import com.haberdashervcs.server.operations.change.ApplyChangesetResult;
import com.haberdashervcs.server.operations.change.Changeset;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;


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
    public CheckoutResult checkout(String branchName, String folderPath) {
        try {
            return checkoutInternal(branchName, folderPath);
        } catch (IOException ioEx) {
            LOG.exception(ioEx, "Error checking out path: " + folderPath);
            return CheckoutResult.failed(ioEx.getMessage());
        }
    }

    private CheckoutResult checkoutInternal(String branchName, String folderPath) throws IOException {
        String[] pathParts = folderPath.split(Pattern.quote("/"));
        String pathSoFar = "";
        FolderListing currentFolderListing = null;
        int currentPathIndex = 0;

        while (!pathSoFar.equals(folderPath)) {
            final String nextFolderName = pathParts[currentPathIndex];
            currentFolderListing = getFolderListing(currentFolderListing, nextFolderName);

            pathSoFar += "/" + nextFolderName;
            ++currentPathIndex;
        }

        final FolderListing checkoutRoot = currentFolderListing;
        HBaseCheckoutStream result = crawlFiles(pathSoFar, checkoutRoot);
        return CheckoutResult.forStream(result);
    }
    
    private HBaseCheckoutStream crawlFiles(String rootPath, FolderListing rootListing) {
        HBaseCheckoutStream.Builder out = HBaseCheckoutStream.Builder.atRoot(rootPath, rootListing);
        return out.build();
    }

    private FolderListing getFolderListing(FolderListing parentFolderListing, String nextFolderName) throws IOException {
        final Table foldersTable = conn.getTable(TableName.valueOf("Folders"));
        final String columnFamilyName = "cfMain";


        final String rowKey = nextFolderName; // TODO commits/refs in the row key?

        // TODO if not exists, throw an exception?
        Get get = new Get(Bytes.toBytes(rowKey));
        Result result = foldersTable.get(get);
        byte[] folderValue = result.getValue(
                Bytes.toBytes(columnFamilyName), Bytes.toBytes("listing"));

        return FolderListing.fromBytes(folderValue);
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
