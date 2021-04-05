package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;
import java.util.LinkedList;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.haberdashervcs.server.core.HdBytes;
import com.haberdashervcs.server.core.logging.HdLogger;
import com.haberdashervcs.server.core.logging.HdLoggers;
import com.haberdashervcs.server.datastore.HdDatastore;
import com.haberdashervcs.server.operations.CheckoutResult;
import com.haberdashervcs.server.operations.CommitEntry;
import com.haberdashervcs.server.operations.FileEntry;
import com.haberdashervcs.server.operations.FolderListing;
import com.haberdashervcs.server.operations.FolderWithPath;
import com.haberdashervcs.server.operations.change.ApplyChangesetResult;
import com.haberdashervcs.server.operations.change.Changeset;
import com.haberdashervcs.server.operations.change.ParsedChangeTree;
import com.haberdashervcs.server.protobuf.CommitsProto;
import com.haberdashervcs.server.protobuf.FilesProto;
import com.haberdashervcs.server.protobuf.FoldersProto;
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
        // TODO How do I generalize this check?
        Preconditions.checkArgument(folderPath.startsWith("/"));
        final CommitEntry commitEntry = getCommit(commitId);
        FolderListing parentFolder = getFolder(commitEntry.getRootFolderId());

        String[] pathParts = folderPath.split(Pattern.quote("/"));
        for (String nextFolderName : pathParts) {
            parentFolder = getFolder(parentFolder.getSubfolderId(nextFolderName));
        }

        final FolderListing checkoutRoot = parentFolder;
        HBaseCheckoutStream result = crawlFiles(folderPath, checkoutRoot);
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
                    FileEntry fileEntry = getFile(entryInFolder.getFileId());
                    out.addFile(thisCrawlEntry.path + entryInFolder.getName(), fileEntry.getContents());
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

    private FileEntry getFile(String fileId) throws IOException {
        final Table filesTable = conn.getTable(TableName.valueOf("Files"));
        final String columnFamilyName = "cfMain";

        Get get = new Get(Bytes.toBytes(fileId));
        Result result = filesTable.get(get);
        byte[] fileValue = result.getValue(
                Bytes.toBytes(columnFamilyName), Bytes.toBytes("contents"));

        return FileEntry.fromBytes(fileId, HdBytes.of(fileValue));
    }


    private FolderListing getFolder(String folderId) throws IOException {
        final Table foldersTable = conn.getTable(TableName.valueOf("Folders"));
        final String columnFamilyName = "cfMain";

        final String rowKey = folderId; // TODO commits/refs in the row key?

        Get get = new Get(Bytes.toBytes(rowKey));
        Result result = foldersTable.get(get);
        byte[] folderValue = result.getValue(
                Bytes.toBytes(columnFamilyName), Bytes.toBytes("listing"));

        return FolderListing.fromBytes(folderValue);
    }


    private String putFileAdd(final String fileId, FileEntry fileEntry) throws IOException {
        final Table filesTable = conn.getTable(TableName.valueOf("Files"));
        final String columnFamilyName = "cfMain";
        final String columnName = "contents";

        final String rowKey = fileId;

        // TODO compress the commit id and other string fields to bytes? Is that necessary?
        FilesProto.FileEntry fileProto = FilesProto.FileEntry.newBuilder()
                .setContents(ByteString.copyFrom(fileEntry.getContents().getRawBytes()))
                .setChangeType(FilesProto.ChangeType.ADD)
                .build();

        Put put = new Put(Bytes.toBytes(rowKey));
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                fileProto.toByteArray());
        filesTable.put(put);

        return fileId;
    }

    private FoldersProto.FolderListing convertFolderToProto(FolderListing folderListing) {
        FoldersProto.FolderListing.Builder out = FoldersProto.FolderListing.newBuilder();

        for (FolderListing.FolderEntry entry : folderListing.getEntries()) {
            FoldersProto.FolderListingEntry protoEntry = FoldersProto.FolderListingEntry.newBuilder()
                    .setFileId(entry.getFileId())
                    .setName(entry.getName())
                    .setType(entry.getType() == FolderListing.FolderEntry.Type.FILE
                            ? FoldersProto.FolderListingEntry.Type.FILE
                            : FoldersProto.FolderListingEntry.Type.FOLDER)
                    .build();
            out.addEntries(protoEntry);
        }

        return out.build();
    }

    private String putFolder(final String folderId, FolderListing folderListing) throws IOException {
        final Table filesTable = conn.getTable(TableName.valueOf("Folders"));
        final String columnFamilyName = "cfMain";
        final String columnName = "listing";

        final String rowKey = folderId;

        // TODO compress the commit id and other string fields to bytes? Is that necessary?
        FoldersProto.FolderListing folderProto = convertFolderToProto(folderListing);

        Put put = new Put(Bytes.toBytes(rowKey));
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                folderProto.toByteArray());
        filesTable.put(put);

        return folderId;
    }

    private void putCommit(String commitId, String rootFolderId) throws IOException {
        final Table commitsTable = conn.getTable(TableName.valueOf("Commits"));
        final String columnFamilyName = "cfMain";
        final String columnName = "entry";

        final String rowKey = commitId;

        CommitsProto.CommitEntry commitProto = CommitsProto.CommitEntry.newBuilder()
                .setRootFolderId(rootFolderId)
                .build();

        Put put = new Put(Bytes.toBytes(rowKey));
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                commitProto.toByteArray());
        commitsTable.put(put);
    }


    private ApplyChangesetResult applyChangesetInternal(Changeset changeset) throws IOException {
        // TODO: Some Transaction (or TransactionManager from the config) should do this, maybe by using the datastore
        // instance.

        // TODO Set up branches (just mapping to a head commit id? proto BranchEntry?)
        final String thisCommitId = UUID.randomUUID().toString();
        final String rootFolderId = UUID.randomUUID().toString();

        ParsedChangeTree parsed = ParsedChangeTree.fromChangeset(changeset);

        // For atomicity, apply the change from the bottom up:
        // - Create/modify files.
        // - Create new folder listings.
        // - Create the new commit with the new root folder listing.
        // - Update the branch to point to the new commit (TODO).

        // TODO: Should file ids be based on (hashed from?) the file contents? Or is that the client's problem to solve,
        // in tracking/storing/sending which files are changed? In other words, where do id's come from?
        for (FileEntry addedFile : parsed.getAddedFiles()) {
            putFileAdd(addedFile.getId(), addedFile);
        }

        for (FolderWithPath changedFolder : parsed.getChangedFolders()) {
            String folderId;
            if (changedFolder.getPath().equals("")) {
                folderId = rootFolderId;
            } else {
                folderId = UUID.randomUUID().toString();
            }
            putFolder(folderId, changedFolder.getListing());
        }

        putCommit(thisCommitId, rootFolderId);

        return ApplyChangesetResult.forStatus(ApplyChangesetResult.Status.OK);
    }
}
