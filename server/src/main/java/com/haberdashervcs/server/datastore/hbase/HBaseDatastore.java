package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;
import java.util.LinkedList;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.haberdashervcs.common.io.HdObjectOutputStream;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.objects.FolderWithPath;
import com.haberdashervcs.server.datastore.HdDatastore;
import com.haberdashervcs.server.operations.change.AddChange;
import com.haberdashervcs.server.operations.change.ApplyChangesetResult;
import com.haberdashervcs.server.operations.change.Changeset;
import com.haberdashervcs.server.operations.checkout.CheckoutResult;
import org.apache.hadoop.hbase.client.Connection;


public final class HBaseDatastore implements HdDatastore {

    private static HdLogger LOG = HdLoggers.create(HBaseDatastore.class);


    public static HBaseDatastore forConnection (Connection conn) {
        return new HBaseDatastore(conn);
    }


    private final Connection conn;
    private final HBaseRawHelper helper;

    private HBaseDatastore(Connection conn) {
        this.conn = conn;
        this.helper = HBaseRawHelper.forConnection(conn);
    }

    @Override
    public ApplyChangesetResult applyChangeset(Changeset changeset) {
        try {
            return applyChangesetInternal(changeset);
        } catch (IOException ioEx) {
            LOG.exception(ioEx, "Error applying Changeset");
            return ApplyChangesetResult.failed();
        }
    }


    @Override
    public CheckoutResult checkout(String org, String repo, String commitId, String folderToCheckout, HdObjectOutputStream out) {
        HBaseRowKeyMaker rowKeyer = HBaseRowKeyMaker.of(org, repo);
        try {
            return checkoutInternal(rowKeyer, commitId, folderToCheckout, out);
        } catch (IOException ioEx) {
            LOG.exception(ioEx, "Error checking out path: " + folderToCheckout);
            return CheckoutResult.failed(ioEx.getMessage());
        }
    }

    private CheckoutResult checkoutInternal(
            HBaseRowKeyMaker rowKeyer, String commitId, String folderPath, HdObjectOutputStream out)
            throws IOException {
        // TODO How do I generalize this check?
        Preconditions.checkArgument(folderPath.startsWith("/"));

        final CommitEntry commitEntry = helper.getCommit(rowKeyer.forCommit(commitId));
        FolderListing parentFolder = helper.getFolder(
                rowKeyer.forFolder(commitEntry.getRootFolderId()));

        String[] pathParts = folderPath.split(Pattern.quote("/"));
        for (String nextFolderName : pathParts) {
            parentFolder = helper.getFolder(
                    rowKeyer.forFolder(parentFolder.getSubfolderId(nextFolderName)));
        }

        final FolderListing checkoutRoot = parentFolder;
        crawlFiles(folderPath, checkoutRoot, out);
        return CheckoutResult.ok();
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

    private void crawlFiles(String rootPath, FolderListing rootListing, HdObjectOutputStream out) throws IOException {
        // TODO DFS or BFS?
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
                    FileEntry fileEntry = helper.getFile(entryInFolder.getFileId());
                    out.writeFile(entryInFolder.getFileId(), fileEntry);
                }
            }
        }
    }


    private ApplyChangesetResult applyChangesetInternal(Changeset changeset) throws IOException {
        // TODO: Some Transaction (or TransactionManager from the config) should do this, maybe by using the datastore
        // instance.
        //
        // TODO Set up branches (just mapping to a head commit id? proto BranchEntry?)

        // For atomicity, apply the change from the bottom up:
        // - Create/modify files.
        // - Create new folder listings.
        // - Create the new commit with the new root folder listing.
        // - Update the branch to point to the new commit (TODO).

        // TODO: Should file ids be based on (hashed from?) the file contents? Or is that the client's problem to solve,
        // in tracking/storing/sending which files are changed? In other words, where do id's come from?
        for (AddChange addChange : changeset.getAddChanges()) {
            helper.putFileAdd(addChange.getId(), addChange.getFile());
        }

        for (FolderWithPath changedFolder : changeset.getChangedFolders()) {
            String folderId;
            if (changedFolder.getPath().equals("/")) {
                folderId = changeset.getProposedRootFolderId();
            } else {
                folderId = UUID.randomUUID().toString();
            }
            helper.putFolder(folderId, changedFolder.getListing());
        }

        helper.putCommit(changeset.getProposedCommitId(), changeset.getProposedRootFolderId());

        return ApplyChangesetResult.successful(changeset.getProposedCommitId());
    }
}
