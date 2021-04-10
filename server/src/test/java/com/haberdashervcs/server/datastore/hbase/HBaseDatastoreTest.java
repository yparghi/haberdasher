package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.protobuf.CommitsProto;
import com.haberdashervcs.common.protobuf.FilesProto;
import com.haberdashervcs.common.protobuf.FoldersProto;
import com.haberdashervcs.server.datastore.HdDatastore;
import com.haberdashervcs.server.operations.CheckoutResult;
import com.haberdashervcs.server.operations.CheckoutStream;
import com.haberdashervcs.server.operations.change.AddChange;
import com.haberdashervcs.server.operations.change.ApplyChangesetResult;
import com.haberdashervcs.server.operations.change.Changeset;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


// Assumes an HBase test cluster is already running -- see HBaseTestServerMain in the hbase-test-server module for a
// simple Java app you can start/stop.
//
// NOTE: I'd like to set up the test cluster right here in these JUnit tests, but the HBase test lib has all kinds of
// dependencies I don't want on my classpath. I think for convenience, HBase has to be running separately.
public class HBaseDatastoreTest {

    private static final HdLogger LOG = HdLoggers.create(HBaseDatastoreTest.class);

    private Connection conn;


    @Before
    public void setUp() throws Exception {
        Configuration conf = HBaseConfiguration.create();
        conf.clear();
        conf.set("dfs.datanode.ipc.address", "0.0.0.0:50020");
        conf.set("hadoop.registry.zk.quorum", "localhost:2181");
        conf.set("hbase.zookeeper.peerport", "2888");
        conf.set("dfs.namenode.http-address", "localhost:52460");
        conf.set("dfs.datanode.https.address", "0.0.0.0:50475");
        conf.set("hbase.status.multicast.address.ip", "226.1.1.3");
        conf.set("yarn.router.webapp.https.address", "0.0.0.0:8091");
        conf.set("dfs.balancer.address", "0.0.0.0:0");
        conf.set("hbase.master.info.bindAddress", "0.0.0.0");
        conf.set("hbase.regionserver.dns.interface", "default");
        conf.set("dfs.journalnode.https-address", "0.0.0.0:8481");
        conf.set("dfs.journalnode.rpc-address", "0.0.0.0:8485");
        conf.set( "dfs.namenode.secondary.https-address", "0.0.0.0:50091");
        conf.set( "hbase.zookeeper.property.clientPort", "62826");
        conf.set( "hbase.localcluster.assign.random.ports", "false");
        conf.set( "hbase.zookeeper.leaderport", "3888");
        conf.set( "hbase.zookeeper.dns.interface", "default");
        conf.set( "dfs.namenode.rpc-address", "localhost:52464");
        conf.set( "dfs.namenode.backup.address", "0.0.0.0:50100");
        conf.set( "fs.defaultFS", "hdfs://localhost:52464");
        conf.set( "dfs.namenode.secondary.http-address", "0.0.0.0:50090");
        conf.set( "hbase.zookeeper.property.dataDir", "${hbase.tmp.dir}/zookeeper");
        conf.set( "hbase.regionserver.info.bindAddress", "0.0.0.0");
        conf.set( "dfs.datanode.http.address", "0.0.0.0:50075");
        conf.set( "dfs.namenode.backup.http-address", "0.0.0.0:50105");
        conf.set( "dfs.journalnode.http-address", "0.0.0.0:8480");
        conf.set( "hbase.zookeeper.quorum", "127.0.0.1");
        conf.set( "dfs.namenode.https-address", "0.0.0.0:50470");
        conf.set("dfs.datanode.address", "0.0.0.0:50010");
        conf.set( "hbase.cluster.distributed", "false");

        conf.set("hbase.masters", "192.168.1.2:16000");

        conn = ConnectionFactory.createConnection(conf);

        createTables();
    }

    private void createTables() throws Exception {
        LOG.info("Creating test tables.");

        Admin admin = conn.getAdmin();

        TableDescriptor filesTableDesc = TableDescriptorBuilder
                .newBuilder(TableName.valueOf("Files"))
                .setColumnFamily(ColumnFamilyDescriptorBuilder.of("cfMain"))
                .build();
        admin.createTable(filesTableDesc);

        TableDescriptor foldersTableDesc = TableDescriptorBuilder
                .newBuilder(TableName.valueOf("Folders"))
                .setColumnFamily(ColumnFamilyDescriptorBuilder.of("cfMain"))
                .build();
        admin.createTable(foldersTableDesc);

        TableDescriptor commitsTableDesc = TableDescriptorBuilder
                .newBuilder(TableName.valueOf("Commits"))
                .setColumnFamily(ColumnFamilyDescriptorBuilder.of("cfMain"))
                .build();
        admin.createTable(commitsTableDesc);
    }

    @After
    public void tearDown() throws Exception {
        /*Admin admin = conn.getAdmin();
        for (String tableName : Arrays.asList("Files", "Folders", "Commits")) {
            admin.disableTable(TableName.valueOf(tableName));
            admin.deleteTable(TableName.valueOf(tableName));
        }*/
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

    // TODO! Move to raw helper, here and in putFile()
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
        HBaseRawHelper helper = HBaseRawHelper.forConnection(conn);

        Changeset.Builder changesetBuilder = Changeset.builder();

        AddChange fileA = AddChange.forContents(
                "fileA_id", helper.fileEntryForText("apple", FilesProto.ChangeType.ADD).toByteArray());
        AddChange fileB = AddChange.forContents(
                "fileB_id", helper.fileEntryForText("banana", FilesProto.ChangeType.ADD).toByteArray());
        changesetBuilder = changesetBuilder.withAddChange(fileA);
        changesetBuilder = changesetBuilder.withAddChange(fileB);

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
        changesetBuilder = changesetBuilder.withFolderAndPath("/", folder);


        final HdDatastore datastore = HBaseDatastore.forConnection(conn);
        final Changeset changeset = changesetBuilder.build();

        ApplyChangesetResult result = datastore.applyChangeset(changeset);
        assertEquals(ApplyChangesetResult.Status.OK, result.getStatus());
        assertEquals(changeset.getProposedCommitId(), result.getCommitId());

        CommitEntry commitEntry = helper.getCommit(result.getCommitId());
        assertEquals(changeset.getProposedRootFolderId(), commitEntry.getRootFolderId());

        FolderListing rootFolder = helper.getFolder(commitEntry.getRootFolderId());
        assertEquals(2, rootFolder.getEntries().size());
        assertEquals("apple.txt", rootFolder.getEntries().get(0).getName());
        assertEquals("banana.txt", rootFolder.getEntries().get(1).getName());
    }
}
