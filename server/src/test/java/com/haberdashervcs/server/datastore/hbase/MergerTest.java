package com.haberdashervcs.server.datastore.hbase;

import java.util.ArrayList;
import java.util.Arrays;

import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.objects.MergeLock;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class MergerTest {

    private static final HdLogger LOG = HdLoggers.create(MergerTest.class);

    private static final String ORG = "test_org";
    private static final String REPO = "test_repo";
    private static final String BRANCH = "test_branch";


    private Connection conn;
    private Admin admin;
    private HBaseRawHelper helper;
    private HBaseRowKeyMaker rowKeyer;
    private long nowTs;

    @Before
    public void setUp() throws Exception {
        nowTs = System.currentTimeMillis();

        Configuration conf = HBaseConfiguration.create();
        conf.clear();

        conn = ConnectionFactory.createConnection(conf);
        admin = conn.getAdmin();

        createTables();

        helper = HBaseRawHelper.forConnection(conn);
        rowKeyer = HBaseRowKeyMaker.forRepo(ORG, REPO);
    }

    // TODO: Commonize this stuff.
    private void createTables() throws Exception {
        LOG.info("Creating test tables.");

        clearTables();

        for (String nameStr : Arrays.asList("Branches", "Files", "Folders", "Merges")) {
            TableDescriptor desc = TableDescriptorBuilder
                    .newBuilder(TableName.valueOf(nameStr))
                    .setColumnFamily(ColumnFamilyDescriptorBuilder.of("cfMain"))
                    .build();
            admin.createTable(desc);
        }
    }

    private void clearTables() throws Exception {
        for (String nameStr : Arrays.asList("Branches", "Files", "Folders", "Merges")) {
            TableName name = TableName.valueOf(nameStr);
            if (admin.tableExists(name)) {
                if (admin.isTableEnabled(name)) {
                    admin.disableTable(name);
                }
                admin.deleteTable(name);
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        clearTables();
    }


    @Test
    public void mergeSimple() throws Exception {
        BranchEntry main = BranchEntry.of("main", -1, 100);
        helper.putBranch(rowKeyer.forBranch("main"), main);

        // TODO! Pushes should update the head commit id on the branch. I'm cheating and setting it here.
        BranchEntry branch = BranchEntry.of(BRANCH, 100, 101);
        helper.putBranch(rowKeyer.forBranch(BRANCH), branch);

        MergeLock lock = MergeLock.of("some-merge-id", "test-branch", MergeLock.State.IN_PROGRESS, nowTs);
        helper.putMerge(rowKeyer.forMerge(lock), lock);

        ArrayList<FolderListing.Entry> entries = new ArrayList<>();
        entries.add(FolderListing.Entry.forFile("older.txt", "fileId_older"));

        FolderListing older = FolderListing.withoutMergeLock(entries, "/some/path", 100);
        helper.putFolderIfNotExists(
                rowKeyer.forFolderAt("main", older.getPath(), older.getCommitId()),
                older);

        entries.add(FolderListing.Entry.forFile("newer.txt", "fileId_newer"));
        FolderListing newer = FolderListing.withMergeLock(entries, "/some/path", 101, lock.getId());
        helper.putFolderIfNotExists(
                rowKeyer.forFolderAt(BRANCH, newer.getPath(), newer.getCommitId()),
                newer);

        Merger merger = new Merger();
        Merger.MergeResult result = merger.merge(ORG, REPO, BRANCH, helper);

        assertEquals(Merger.MergeResult.State.SUCCESSFUL, result.getState());
        assertEquals(101, result.getNewCommitOnMain());

        FolderHistoryLoader historyLoader = FolderHistoryLoader.forBranch(rowKeyer, "main", helper, nowTs);

        FolderListing afterMerge = historyLoader.getFolderAtCommit(result.getNewCommitOnMain(), "/some/path").get();
        assertEquals(2, afterMerge.getEntries().size());
    }


    @Test
    public void mergeFailsWithConflict() throws Exception {
        BranchEntry main = BranchEntry.of("main", -1, 100);
        helper.putBranch(rowKeyer.forBranch("main"), main);

        // TODO! Pushes should update the head commit id on the branch. I'm cheating and setting it here.
        BranchEntry branch1 = BranchEntry.of("branch1", 100, 101);
        helper.putBranch(rowKeyer.forBranch("branch1"), branch1);

        BranchEntry branch2 = BranchEntry.of("branch2", 100, 101);
        helper.putBranch(rowKeyer.forBranch("branch2"), branch2);

        MergeLock lock1 = MergeLock.of("merge1", "branch1", MergeLock.State.IN_PROGRESS, nowTs);
        helper.putMerge(rowKeyer.forMerge(lock1), lock1);

        MergeLock lock2 = MergeLock.of("merge2", "branch2", MergeLock.State.IN_PROGRESS, nowTs);
        helper.putMerge(rowKeyer.forMerge(lock2), lock2);


        ArrayList<FolderListing.Entry> entries = new ArrayList<>();
        entries.add(FolderListing.Entry.forFile("base.txt", "fileId_base"));

        FolderListing base = FolderListing.withoutMergeLock(entries, "/some/path", 100);
        helper.putFolderIfNotExists(
                rowKeyer.forFolderAt("main", base.getPath(), base.getCommitId()),
                base);


        entries.add(FolderListing.Entry.forFile("newer1.txt", "fileId_newer1"));
        FolderListing newer1 = FolderListing.withMergeLock(entries, "/some/path", 101, lock1.getId());
        helper.putFolderIfNotExists(
                rowKeyer.forFolderAt("branch1", newer1.getPath(), newer1.getCommitId()),
                newer1);

        entries.add(FolderListing.Entry.forFile("newer2.txt", "fileId_newer2"));
        FolderListing newer2 = FolderListing.withMergeLock(entries, "/some/path", 101, lock2.getId());
        helper.putFolderIfNotExists(
                rowKeyer.forFolderAt("branch2", newer2.getPath(), newer2.getCommitId()),
                newer2);


        Merger merger1 = new Merger();
        Merger.MergeResult result1 = merger1.merge(ORG, REPO, "branch1", helper);
        assertEquals(Merger.MergeResult.State.SUCCESSFUL, result1.getState());
        assertEquals(101, result1.getNewCommitOnMain());

        Merger merger2 = new Merger();
        Merger.MergeResult result2 = merger2.merge(ORG, REPO, "branch2", helper);
        // NOTE: This *should* fail because the head commit on /some/path is now 101, not 100 (the base commit id for
        //     this merge).
        assertEquals(Merger.MergeResult.State.FAILED, result2.getState());


        FolderHistoryLoader historyLoader = FolderHistoryLoader.forBranch(rowKeyer, "main", helper, nowTs);
        FolderListing afterMerge = historyLoader.getFolderAtCommit(result1.getNewCommitOnMain(), "/some/path").get();
        assertEquals(2, afterMerge.getEntries().size());
        assertEquals("base.txt", afterMerge.getEntries().get(0).getName());
        assertEquals("newer1.txt", afterMerge.getEntries().get(1).getName());
    }
}
