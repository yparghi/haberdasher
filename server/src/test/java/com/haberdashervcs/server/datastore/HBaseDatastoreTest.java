package com.haberdashervcs.server.datastore;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.haberdashervcs.server.datastore.hbase.HBaseDatastore;
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


    private UUID putFile(String contents) throws IOException {
        final UUID uuid = UUID.randomUUID();

        Table filesTable = conn.getTable(TableName.valueOf("Files"));
        final String rowKey = uuid.toString();
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

        return uuid;
    }

    private UUID putFolder(List<UUID> files, List<String> fileNames) throws IOException {
        Preconditions.checkArgument(files.size() == fileNames.size());

        final UUID uuid = UUID.randomUUID();

        Table filesTable = conn.getTable(TableName.valueOf("Folders"));
        final String rowKey = uuid.toString();
        final String columnFamilyName = "cfMain";
        final String columnName = "listing";

        FoldersProto.FolderListing.Builder folderProto = FoldersProto.FolderListing.newBuilder();
        for (int i = 0; i < files.size(); i++) {
            FoldersProto.FolderListingEntry entry = FoldersProto.FolderListingEntry.newBuilder()
                    .setType(FoldersProto.FolderListingEntry.Type.FILE)
                    .setName(fileNames.get(i))
                    .setFileId(files.get(i).toString())
                    .build();
            folderProto.addEntries(entry);
        }

        Put put = new Put(Bytes.toBytes(rowKey));
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                folderProto.build().toByteArray());
        filesTable.put(put);

        return uuid;
    }

    @Test
    public void basicFolderCheckout() throws Exception {
        UUID firstFileId = putFile("apple");
        UUID secondFileId = putFile("banana");

        UUID folderId = putFolder(
                Arrays.asList(firstFileId, secondFileId),
                Arrays.asList("apple.txt", "banana.txt"));

        HBaseDatastore datastore = HBaseDatastore.forConnection(conn);
        // TODO figure this out -- do I need branches/commits now?
        datastore.checkout("someBranch", "someFolder");
    }


    @Test
    // TODO: Replace this with a real 2nd test.
    public void secondTestSetupAndTeardown() throws Exception {
        assertTrue(true);
    }
}
