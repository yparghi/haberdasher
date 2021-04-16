package com.haberdashervcs.server.example;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.server.config.HaberdasherServer;
import com.haberdashervcs.server.datastore.hbase.HBaseDatastore;
import com.haberdashervcs.server.datastore.hbase.HBaseRawHelper;
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

    private static final HdLogger LOG = HdLoggers.create(ExampleServerMain.class);

    public static void main(String[] args) throws Exception {
        LOG.info("Hello Haberdasher!");

        Configuration conf = HBaseConfiguration.create();
        conf.clear();
        Connection conn = ConnectionFactory.createConnection(conf);

        HBaseDatastore datastore = HBaseDatastore.forConnection(conn);

        // TEMP!
        loadTestData(conn, datastore);

        HaberdasherServer server = HaberdasherServer.builder()
                .withDatastore(datastore)
                .withFrontend(JettyHttpFrontend.forDatastore(datastore))
                .build();

        server.start();

        LOG.info("Serving...");
    }

    // TEMP!
    private static void loadTestData(Connection conn, HBaseDatastore datastore) throws Exception {
        final String testOrg = "some_org";
        final String testRepo = "some_repo";

        HBaseRawHelper helper = HBaseRawHelper.forConnection(conn);
        Changeset.Builder changesetBuilder = Changeset.builder();

        AddChange fileA = AddChange.forContents(
                "fileA_id", FileEntry.forContents("apple".getBytes(StandardCharsets.UTF_8)));
        AddChange fileB = AddChange.forContents(
                "fileB_id", FileEntry.forContents("banana".getBytes(StandardCharsets.UTF_8)));
        changesetBuilder = changesetBuilder.withAddChange(fileA);
        changesetBuilder = changesetBuilder.withAddChange(fileB);

        FolderListing folder = FolderListing.forEntries(Arrays.asList(
            FolderListing.FolderEntry.forFile("apple.txt", fileA.getId()),
                FolderListing.FolderEntry.forFile("banana.txt", fileB.getId())));
        changesetBuilder = changesetBuilder.withFolderAndPath("/", folder);

        final Changeset changeset = changesetBuilder.build();
        ApplyChangesetResult result = datastore.applyChangeset(testOrg, testRepo, changeset);
        LOG.info("Loaded test data at commit: %s", result.getCommitId());
    }
}
