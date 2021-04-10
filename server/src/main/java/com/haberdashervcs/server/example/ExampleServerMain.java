package com.haberdashervcs.server.example;

import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.protobuf.FilesProto;
import com.haberdashervcs.common.protobuf.FoldersProto;
import com.haberdashervcs.server.config.HaberdasherServer;
import com.haberdashervcs.server.datastore.hbase.HBaseDatastore;
import com.haberdashervcs.server.datastore.hbase.HBaseRawHelper;
import com.haberdashervcs.server.datastore.hbase.HBaseTestClusterManager;
import com.haberdashervcs.server.frontend.JettyHttpFrontend;
import com.haberdashervcs.server.operations.change.AddChange;
import com.haberdashervcs.server.operations.change.ApplyChangesetResult;
import com.haberdashervcs.server.operations.change.Changeset;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;


/**
 * TODO
 */
public class ExampleServerMain {

    private static HdLogger LOG = HdLoggers.create(ExampleServerMain.class);

    public static void main(String[] args) throws Exception {
        System.out.println( "Hello Haberdasher!" );

        Configuration conf = HBaseConfiguration.create();
        conf.clear();
        conf.set("hbase.master", "localhost:60000");
        Connection conn = ConnectionFactory.createConnection(conf);

        HBaseDatastore datastore = HBaseDatastore.forConnection(conn);

        HaberdasherServer server = HaberdasherServer.builder()
                .withDatastore(datastore)
                .withFrontend(new JettyHttpFrontend())
                .build();

        server.start();

        System.out.println("Serving...");

        // TEMP!
        loadTestData(conn, datastore);
    }

    // TEMP!
    private static void loadTestData(Connection conn, HBaseDatastore datastore) throws Exception {
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

        final Changeset changeset = changesetBuilder.build();

        ApplyChangesetResult result = datastore.applyChangeset(changeset);

    }
}
