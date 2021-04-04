package com.haberdashervcs.server.datastore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.haberdashervcs.server.datastore.hbase.HBaseDatastore;
import com.haberdashervcs.server.operations.CheckoutResult;
import com.haberdashervcs.server.operations.CheckoutStream;
import com.haberdashervcs.server.operations.FolderListing;
import com.haberdashervcs.server.operations.change.AddChange;
import com.haberdashervcs.server.operations.change.ApplyChangesetResult;
import com.haberdashervcs.server.operations.change.Changeset;
import com.haberdashervcs.server.protobuf.CommitsProto;
import com.haberdashervcs.server.protobuf.FilesProto;
import com.haberdashervcs.server.protobuf.FoldersProto;
import com.haberdashervcs.server.server.data.hbase.HBaseTestClusterManager;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class HBaseDatastoreTest {

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


    private String putFileRaw(String contents) throws IOException {
        final String fileId = UUID.randomUUID().toString();

        Table filesTable = conn.getTable(TableName.valueOf("Files"));
        final String rowKey = fileId;
        final String columnFamilyName = "cfMain";
        final String columnName = "contents";

        FilesProto.FileEntry fileProto = FilesProto.FileEntry.newBuilder()
                .setContents(ByteString.copyFrom(contents, Charsets.UTF_8))
                .build();

        Put put = new Put(Bytes.toBytes(rowKey));
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                fileProto.toByteArray());
        filesTable.put(put);

        return fileId;
    }

    // TODO! Raw interface/helper to HBase, that does these puts and gets?
    private String putFolderRaw(List<String> fileIds, List<String> fileNames) throws IOException {
        Preconditions.checkArgument(fileIds.size() == fileNames.size());

        final String folderId = UUID.randomUUID().toString();

        Table filesTable = conn.getTable(TableName.valueOf("Folders"));
        final String rowKey = folderId;
        final String columnFamilyName = "cfMain";
        final String columnName = "listing";

        FoldersProto.FolderListing.Builder folderProto = FoldersProto.FolderListing.newBuilder();
        for (int i = 0; i < fileIds.size(); i++) {
            FoldersProto.FolderListingEntry entry = FoldersProto.FolderListingEntry.newBuilder()
                    .setType(FoldersProto.FolderListingEntry.Type.FILE)
                    .setName(fileNames.get(i))
                    .setFileId(fileIds.get(i))
                    .build();
            folderProto.addEntries(entry);
        }

        Put put = new Put(Bytes.toBytes(rowKey));
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                folderProto.build().toByteArray());
        filesTable.put(put);

        return folderId;
    }

    private String putCommitRaw(String rootFolderId) throws IOException {
        final String commitId = UUID.randomUUID().toString();

        Table commitsTable = conn.getTable(TableName.valueOf("Commits"));
        final String rowKey = commitId;
        final String columnFamilyName = "cfMain";
        final String columnName = "entry";

        CommitsProto.CommitEntry commitProto = CommitsProto.CommitEntry.newBuilder()
                .setRootFolderId(rootFolderId)
                .build();

        Put put = new Put(Bytes.toBytes(rowKey));
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                commitProto.toByteArray());
        commitsTable.put(put);

        return commitId;
    }

    @Test
    public void basicRootFolderCheckout() throws Exception {
        String firstFileId = putFileRaw("apple");
        String secondFileId = putFileRaw("banana");

        String folderId = putFolderRaw(
                Arrays.asList(firstFileId, secondFileId),
                Arrays.asList("apple.txt", "banana.txt"));

        String commitId = putCommitRaw(folderId);

        HBaseDatastore datastore = HBaseDatastore.forConnection(conn);
        CheckoutResult result = datastore.checkout(commitId, "/");

        assertEquals(CheckoutResult.Status.OK, result.getStatus());

        ArrayList<CheckoutStream.CheckoutFile> resultFiles = new ArrayList<>();
        for (CheckoutStream.CheckoutFile file : result.getStream()) {
            resultFiles.add(file);
        }

        // TODO: unordered check, and check contents
        assertEquals(2, resultFiles.size());
        assertEquals("/apple.txt", resultFiles.get(0).getPath());
        assertEquals("/banana.txt", resultFiles.get(1).getPath());
    }


    @Test
    public void basicInnerFolderCheckout() throws Exception {
        // TODO
        assertTrue(true);
    }


    @Test
    public void basicApplyChangeset() throws Exception {
        Changeset.Builder changeset = Changeset.builder();

        AddChange fileA = AddChange.forContents("fileA_id", "apple".getBytes(StandardCharsets.UTF_8));
        AddChange fileB = AddChange.forContents("fileB_id", "banana".getBytes(StandardCharsets.UTF_8));
        changeset.withAddChange(fileA);
        changeset.withAddChange(fileB);

        FoldersProto.FolderListing.Builder folderProto = FoldersProto.FolderListing.newBuilder()
                .addEntries(FoldersProto.FolderListingEntry.newBuilder()
                        .setType(FoldersProto.FolderListingEntry.Type.FILE)
                        .setName("apple.txt")
                        .setFileId(fileA.getId()))
                .addEntries(FoldersProto.FolderListingEntry.newBuilder()
                        .setType(FoldersProto.FolderListingEntry.Type.FILE)
                        .setName("banana.txt")
                        .setFileId(fileB.getId()));
        FolderListing folder = FolderListing.fromBytes(folderProto.build().toByteArray());
        changeset.withFolderAndPath("/", folder);

        final HdDatastore datastore = HBaseDatastore.forConnection(conn);
        ApplyChangesetResult result = datastore.applyChangeset(changeset.build());

        assertEquals(ApplyChangesetResult.Status.OK, result.getStatus());

        // TODO! Check the state of the data here -- do that "raw helper" TODO for puts/gets
    }
}
