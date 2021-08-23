package com.haberdashervcs.client.commit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.haberdashervcs.client.crawl.EntryComparisonThisFolder;
import com.haberdashervcs.client.crawl.LocalChangeHandler;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.objects.LocalFileState;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDbRowKeyer;
import com.haberdashervcs.common.diff.HdHasher;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.objects.HdFolderPath;


final class CommitChangeHandler implements LocalChangeHandler {

    private static final HdLogger LOG = HdLoggers.create(CommitChangeHandler.class);


    private final LocalDb db;
    private final BranchAndCommit currentBranch;

    CommitChangeHandler(LocalDb db, BranchAndCommit currentBranch) {
        this.db = db;
        this.currentBranch = currentBranch;
    }


    @Override
    public void handleComparisons(HdFolderPath path, List<EntryComparisonThisFolder> comparisons)
            throws IOException {

        ArrayList<FolderListing.Entry> seenEntries = new ArrayList<>();
        boolean newFolderShouldBeWritten = false;

        for (EntryComparisonThisFolder comparison : comparisons) {
            LOG.debug("Handling comparison in %s: %s", path, comparison);

            if (comparison.getEntryInCommit() == null
                    && comparison.getPathInLocalRepo().toFile().isFile()) {
                newFolderShouldBeWritten = true;
                HdHasher.ContentsAndHash ch = putNewFile(comparison);
                LOG.debug("Adding new file: %s", path.forFolderListing() + comparison.getName());
                seenEntries.add(
                        FolderListing.Entry.forFile(comparison.getName(), ch.hashString()));


            } else if (comparison.getEntryInCommit() == null
                    && comparison.getPathInLocalRepo().toFile().isDirectory()) {
                newFolderShouldBeWritten = true;
                LOG.debug("Adding new folder: %s", path.forFolderListing() + comparison.getName());
                seenEntries.add(FolderListing.Entry.forSubFolder(
                        comparison.getName(), "TODO folder ids?"));


            } else if (comparison.getEntryInCommit() != null
                    && comparison.getPathInLocalRepo() == null) {
                // NOTE: here's were a deletion "marker" would be written.
                newFolderShouldBeWritten = true;
                LOG.debug("Skipping/deleting: %s", path.forFolderListing() + comparison.getName());


            } else if (comparison.getEntryInCommit() != null
                    && comparison.getPathInLocalRepo() != null
                    && comparison.getPathInLocalRepo().toFile().isFile()) {
                if (comparison.getEntryInCommit().getType() == FolderListing.Entry.Type.FOLDER) {
                    newFolderShouldBeWritten = true;
                    HdHasher.ContentsAndHash ch = putNewFile(comparison);
                    LOG.debug("Replacing folder with new file: %s", path.forFolderListing() + comparison.getName());
                    seenEntries.add(
                            FolderListing.Entry.forFile(comparison.getName(), ch.hashString()));

                } else {
                    HdHasher.ContentsAndHash localCH = HdHasher.readLocalFile(comparison.getPathInLocalRepo());
                    FileEntry commitFile = db.getFile(comparison.getEntryInCommit().getId());
                    if (!localCH.hashString().equals(commitFile.getId())) {
                        newFolderShouldBeWritten = true;
                        LOG.debug("Adding diff for file %s: hashes %s / %s",
                                path.forFolderListing() + comparison.getName(),
                                commitFile.getId(),
                                localCH.hashString());

                        // TODO! Here is where I should check whether the local file contents are binary or not.
                        // Also: What if the old file is non-binary? Then it's a new file add...
                        byte[] localContents = localCH.getContents();
                        byte[] commitContents = db.resolveDiffsToBytes(commitFile);
                        FileEntry newFile = FileEntry.forNewContents(localCH.hashString(), localCH.getContents());






                        LocalFileState state = LocalFileState.withPushedToServerState(false);
                        db.putFile(localCH.hashString(), newFile, state);
                    } else {
                        LOG.debug("Unchanged file: %s", path.forFolderListing() + comparison.getName());
                    }
                    seenEntries.add(
                            FolderListing.Entry.forFile(comparison.getName(), localCH.hashString()));
                }


            } else if (comparison.getEntryInCommit() != null
                    && comparison.getPathInLocalRepo() != null
                    && comparison.getPathInLocalRepo().toFile().isDirectory()) {

                if (comparison.getEntryInCommit().getType() == FolderListing.Entry.Type.FILE) {
                    newFolderShouldBeWritten = true;
                    LOG.debug("File replaced with folder: %s", path.forFolderListing() + comparison.getName());
                } else {
                    LOG.debug("Existing folder: %s", path.forFolderListing() + comparison.getName());
                }
                seenEntries.add(FolderListing.Entry.forSubFolder(
                        comparison.getName(), "TODO folder ids?"));
            }
        }

        if (newFolderShouldBeWritten) {
            String rowKey = SqliteLocalDbRowKeyer.getInstance().forFolder(
                    currentBranch.getBranchName(),
                    path.forFolderListing(),
                    currentBranch.getCommitId() + 1);
            FolderListing newFolder = FolderListing.withoutMergeLock(
                    seenEntries, path.forFolderListing(), currentBranch.getCommitId() + 1);
            LOG.debug("Writing folder: %s", newFolder.getDebugString());
            db.putFolder(rowKey, newFolder);
        }
    }


    private HdHasher.ContentsAndHash putNewFile(
            EntryComparisonThisFolder comparison)
            throws IOException {
        HdHasher.ContentsAndHash ch = HdHasher.readLocalFile(comparison.getPathInLocalRepo());
        FileEntry newFile = FileEntry.forNewContents(ch.hashString(), ch.getContents());
        LocalFileState state = LocalFileState.withPushedToServerState(false);
        db.putFile(ch.hashString(), newFile, state);
        return ch;
    }
}
