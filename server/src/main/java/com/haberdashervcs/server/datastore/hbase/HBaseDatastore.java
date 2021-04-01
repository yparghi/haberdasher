package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;
import java.util.LinkedList;
import java.util.UUID;
import java.util.regex.Pattern;

import com.haberdashervcs.server.core.logging.HdLogger;
import com.haberdashervcs.server.core.logging.HdLoggers;
import com.haberdashervcs.server.datastore.HdDatastore;
import com.haberdashervcs.server.operations.CheckoutResult;
import com.haberdashervcs.server.operations.CommitEntry;
import com.haberdashervcs.server.operations.FileEntry;
import com.haberdashervcs.server.operations.FolderListing;
import com.haberdashervcs.server.operations.change.AddChange;
import com.haberdashervcs.server.operations.change.ApplyChangesetResult;
import com.haberdashervcs.server.operations.change.Changeset;
import com.haberdashervcs.server.protobuf.CommitsProto;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;


public final class HBaseDatastore implements HdDatastore {

    private static HdLogger LOG = HdLoggers.create(HBaseDatastore.class);


    public static HBaseDatastore forConnection (Connection conn) {
        return new HBaseDatastore(conn);
    }


    private final Connection conn;

    private HBaseDatastore(Connection conn) {
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
    public CheckoutResult checkout(String commitId, String folderToCheckout) {
        try {
            return checkoutInternal(commitId, folderToCheckout);
        } catch (IOException ioEx) {
            LOG.exception(ioEx, "Error checking out path: " + folderToCheckout);
            return CheckoutResult.failed(ioEx.getMessage());
        }
    }

    private CheckoutResult checkoutInternal(String commitId, String folderPath) throws IOException {
        CommitEntry commitEntry = getCommit(commitId);

        String[] pathParts = folderPath.split(Pattern.quote("/"));
        for (String nextFolderName : pathParts) {

        }



        // TODO! toss below what I can
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


    // For tracking built-up paths like "/some/dir/in/the/tree/<filename goes here>"
    // TODO break out this crawling stuff
    private static class CrawlEntry {
        private String path;

        private FolderListing listing;

        public CrawlEntry(String path, FolderListing listing) {
            this.path = path;
            this.listing = listing;
        }
    }

    private HBaseCheckoutStream crawlFiles(String rootPath, FolderListing rootListing) throws IOException {
        HBaseCheckoutStream.Builder out = HBaseCheckoutStream.Builder.atRoot(rootPath, rootListing);
        LinkedList<CrawlEntry> foldersToBrowse = new LinkedList<>();
        foldersToBrowse.add(new CrawlEntry(rootPath, rootListing));

        while (!foldersToBrowse.isEmpty()) {
            CrawlEntry thisCrawlEntry = foldersToBrowse.pop();
            for (FolderListing.FolderEntry entryInFolder : thisCrawlEntry.listing.getEntries()) {
                if (entryInFolder.getType() == FolderListing.FolderEntry.Type.FOLDER) {
                    FolderListing thisEntryFolderListing = null;
                    foldersToBrowse.add(new CrawlEntry(
                            thisCrawlEntry.path + "/" + entryInFolder.getName(), thisEntryFolderListing));

                } else {
                    String fileRowKey = entryInFolder.getFileId();
                    FileEntry fileEntry = getFile(fileRowKey);
                    out.addFile(thisCrawlEntry.path, fileEntry.getContents());
                }
            }
        }

        return out.build();
    }

    private CommitEntry getCommit(String rowKey) throws IOException {
        final Table commitsTable = conn.getTable(TableName.valueOf("Commits"));
        final String columnFamilyName = "cfMain";

        Get get = new Get(Bytes.toBytes(rowKey));
        Result result = commitsTable.get(get);
        byte[] commitEntryBytes = result.getValue(
                Bytes.toBytes(columnFamilyName), Bytes.toBytes("entry"));

        return CommitEntry.fromBytes(commitEntryBytes);
    }

    private FileEntry getFile(String rowKey) throws IOException {
        final Table filesTable = conn.getTable(TableName.valueOf("Files"));
        final String columnFamilyName = "cfMain";

        Get get = new Get(Bytes.toBytes(rowKey));
        Result result = filesTable.get(get);
        byte[] fileValue = result.getValue(
                Bytes.toBytes(columnFamilyName), Bytes.toBytes("contents"));

        return FileEntry.fromBytes(fileValue);
    }


    // TODO! keep this? refactor it?
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
