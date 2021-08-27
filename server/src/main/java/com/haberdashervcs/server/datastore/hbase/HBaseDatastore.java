package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.haberdashervcs.common.diff.HdHasher;
import com.haberdashervcs.common.io.HdObjectId;
import com.haberdashervcs.common.io.HdObjectInputStream;
import com.haberdashervcs.common.io.HdObjectOutputStream;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;
import com.haberdashervcs.common.objects.BranchEntry;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.server.browser.RepoBrowser;
import com.haberdashervcs.server.datastore.HdDatastore;
import com.haberdashervcs.server.operations.checkout.CheckoutResult;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;


public final class HBaseDatastore implements HdDatastore {

    private static HdLogger LOG = HdLoggers.create(HBaseDatastore.class);


    public static HBaseDatastore forConnection(Connection conn) {
        return new HBaseDatastore(conn);
    }


    private final Connection conn;
    private final HBaseRawHelper helper;

    private HBaseDatastore(Connection conn) {
        this.conn = conn;
        this.helper = HBaseRawHelper.forConnection(conn);
    }


    @Override
    public void writeObjectsFromPush(
            String org,
            String repo,
            String branchName,
            long baseCommitId,
            long newHeadCommitId,
            HdObjectInputStream objectsIn) {
        try {
            writeObjectsFromPushInternal(
                    org, repo, branchName, baseCommitId, newHeadCommitId, objectsIn);
        } catch (IOException ioEx) {
            LOG.exception(ioEx, "Error in writeObjectsFromPush");
            throw new RuntimeException(ioEx);
        }
    }


    private void writeObjectsFromPushInternal(
            String org,
            String repo,
            String branchName,
            long baseCommitId,
            long newHeadCommitId,
            HdObjectInputStream objectsIn)
            throws IOException {
        HBaseRowKeyer rowKeyer = HBaseRowKeyer.forRepo(org, repo);
        long highestSeenCommitId = -1;

        Optional<HdObjectId> nextOpt;
        while ((nextOpt = objectsIn.next()).isPresent()) {
            HdObjectId next = nextOpt.get();
            switch (next.getType()) {
                case FILE:
                    FileEntry file = objectsIn.getFile();
                    LOG.debug("Got file: %s", file.getDebugString());
                    helper.putFile(rowKeyer.forFile(file.getId()), file);
                    break;

                case FOLDER:
                    FolderListing folder = objectsIn.getFolder();
                    LOG.debug("Got folder: %s", folder.getDebugString());
                    if (folder.getCommitId() > highestSeenCommitId) {
                        highestSeenCommitId = folder.getCommitId();
                    }
                    helper.putFolderIfNotExists(
                            rowKeyer.forFolderAt(
                                    branchName, folder.getPath(), folder.getCommitId()),
                            folder);
                    break;

                case COMMIT:
                    CommitEntry commit = objectsIn.getCommit();
                    LOG.debug("Got commit: %s", commit.getDebugString());
                    byte[] rowKey = rowKeyer.forCommit(commit);
                    // TODO: How do I make this transactionally safe, if this push ultimately fails?
                    helper.putCommit(rowKey, commit);
                    break;

                default:
                    throw new UnsupportedOperationException(
                            "Unexpected object: " + next.getType());
            }
        }

        if (newHeadCommitId != highestSeenCommitId) {
            throw new AssertionError(String.format(
                    "The new head commit (%d) didn't match the newest folder commit (%d)",
                    newHeadCommitId, highestSeenCommitId));
        }

        // TODO: Consider branch overwriting, because of an accidental name collision. Should we
        //     use randomized alphanumeric branch id's for the actual name on the server?
        BranchEntry currentBranchState;
        Optional<BranchEntry> oBranch = helper.getBranch(rowKeyer.forBranch(branchName));
        if (oBranch.isPresent()) {
            currentBranchState = oBranch.get();
        } else {
            BranchEntry newBranch = BranchEntry.of(branchName, baseCommitId, 0);
            LOG.info("Creating new branch: %s", newBranch.getDebugString());
            helper.putBranch(rowKeyer.forBranch(branchName), newBranch);
            currentBranchState = newBranch;
        }

        LOG.debug("Push: Current branch state is %s", currentBranchState.getDebugString());
        if (currentBranchState.getBaseCommitId() != baseCommitId) {
            throw new AssertionError(String.format(
                    "Server branch base commit (%d) doesn't match pushed branch's base commit (%d)",
                    currentBranchState.getBaseCommitId(), baseCommitId));
        }

        helper.putBranch(
                rowKeyer.forBranch(branchName),
                BranchEntry.of(branchName, currentBranchState.getBaseCommitId(), newHeadCommitId));
    }


    @Override
    public CheckoutResult checkout(String org, String repo, String branch, long commitId, String folderToCheckout, HdObjectOutputStream out) {
        HBaseRowKeyer rowKeyer = HBaseRowKeyer.forRepo(org, repo);
        try {
            return checkoutInternal(rowKeyer, branch, commitId, folderToCheckout, out);
        } catch (IOException ioEx) {
            LOG.exception(ioEx, "Error checking out path: " + folderToCheckout);
            return CheckoutResult.failed(ioEx.getMessage());
        }
    }

    @Override
    public Optional<BranchAndCommit> getHeadCommitForBranch(String org, String repo, String branchName) {
        HBaseRowKeyer rowKeyer = HBaseRowKeyer.forRepo(org, repo);
        try {
            // TODO Rewrite this to use the returned optional.
            BranchEntry branchEntry = helper.getBranch(rowKeyer.forBranch(branchName)).get();
            return Optional.of(BranchAndCommit.of(branchName, branchEntry.getHeadCommitId()));
        } catch (IOException ioEx) {
            LOG.warn("No branch entry found for: %s", branchName);
            return Optional.empty();
        }
    }


    @Override
    public RepoBrowser getBrowser(String org, String repo) {
        return HBaseRepoBrowser.forRepo(org, repo, helper);
    }


    // TODO! tests
    private CheckoutResult checkoutInternal(
            HBaseRowKeyer rowKeyer, String branchName, long commitId, String path, HdObjectOutputStream out)
            throws IOException {
        // TODO How do I generalize this check?
        Preconditions.checkArgument(path.startsWith("/"));
        Preconditions.checkArgument(path.endsWith("/"));

        FolderListing rootListing = helper.getFolder(
                rowKeyer.forFolderAt(branchName, path, commitId));
        out.writeFolder("TODO do folders still have ids?", rootListing);

        LinkedList<CheckoutCrawlEntry> foldersToBrowse = new LinkedList<>();
        foldersToBrowse.add(new CheckoutCrawlEntry(path, rootListing));

        while (!foldersToBrowse.isEmpty()) {
            CheckoutCrawlEntry thisCrawlEntry = foldersToBrowse.pop();

            LOG.debug("TEMP: Checkout folder is: %s", thisCrawlEntry.listing.getDebugString());

            for (FolderListing.Entry entryInFolder : thisCrawlEntry.listing.getEntries()) {
                if (entryInFolder.getType() == FolderListing.Entry.Type.FOLDER) {
                    String subfolderPath = thisCrawlEntry.path + entryInFolder.getName() + "/";
                    FolderListing thisEntryFolderListing = helper.getFolder(
                            rowKeyer.forFolderAt(branchName, subfolderPath, commitId));
                    LOG.debug("TEMP: Writing folder: %s", thisEntryFolderListing.getPath());
                    out.writeFolder("TODO folder ids?", thisEntryFolderListing);
                    // BFS to stay within a folder, for row locality.
                    foldersToBrowse.add(
                            new CheckoutCrawlEntry(subfolderPath, thisEntryFolderListing));

                } else {
                    FileEntry fileEntry = helper.getFile(rowKeyer.forFile(entryInFolder.getId()));
                    out.writeFile(entryInFolder.getId(), fileEntry);
                }
            }
        }
        return CheckoutResult.ok();
    }


    // For tracking built-up paths like "/some/dir/in/the/tree/<filename goes here>"
    // TODO break out this crawling stuff
    private static class CheckoutCrawlEntry {

        private String path;
        private FolderListing listing;

        public CheckoutCrawlEntry(String path, FolderListing listing) {
            this.path = path;
            this.listing = listing;
        }
    }


    // TODO! This is incredibly temporary.
    public void loadTestData() throws IOException {
        HBaseRawHelper helper = HBaseRawHelper.forConnection(conn);
        HBaseRowKeyer rowKeyer = HBaseRowKeyer.forRepo("some_org", "some_repo");
        Admin admin = conn.getAdmin();

        for (String nameStr : Arrays.asList("Branches", "Files", "Folders", "Merges")) {
            TableName name = TableName.valueOf(nameStr);
            if (admin.tableExists(name)) {
                if (admin.isTableEnabled(name)) {
                    admin.disableTable(name);
                }
                admin.deleteTable(name);
            }
        }

        for (String nameStr : Arrays.asList("Branches", "Files", "Folders", "Merges")) {
            TableDescriptor desc = TableDescriptorBuilder
                    .newBuilder(TableName.valueOf(nameStr))
                    .setColumnFamily(ColumnFamilyDescriptorBuilder.of("cfMain"))
                    .build();
            admin.createTable(desc);
        }

        BranchEntry main = BranchEntry.of("main", 1, 1);
        helper.putBranch(rowKeyer.forBranch("main"), main);

        List<FolderListing.Entry> rootEntries = Arrays.asList(
                FolderListing.Entry.forSubFolder("subfolder", "TODO folder ids?"));
        FolderListing rootFolder = FolderListing.withoutMergeLock(
                rootEntries, "/", 1);
        helper.putFolderIfNotExists(
                rowKeyer.forFolderAt("main", rootFolder.getPath(), rootFolder.getCommitId()),
                rootFolder);

        final String fileContents = "hello contents";
        final String fileId = HdHasher.readBytes(fileContents.getBytes(StandardCharsets.UTF_8))
                .hashString();

        List<FolderListing.Entry> subfolderEntries = Arrays.asList(
                FolderListing.Entry.forFile("hello.txt", fileId));
        FolderListing subfolder = FolderListing.withoutMergeLock(
                subfolderEntries, "/subfolder/", 1);
        helper.putFolderIfNotExists(
                rowKeyer.forFolderAt("main", subfolder.getPath(), subfolder.getCommitId()),
                subfolder);

        FileEntry helloFile = FileEntry.forNewContents(
                fileId, fileContents.getBytes(StandardCharsets.UTF_8));
        helper.putFile(rowKeyer.forFile(fileId), helloFile);
    }
}
