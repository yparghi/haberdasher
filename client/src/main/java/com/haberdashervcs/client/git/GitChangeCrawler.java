package com.haberdashervcs.client.git;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.objects.LocalFileState;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDbRowKeyer;
import com.haberdashervcs.common.diff.DmpDiffResult;
import com.haberdashervcs.common.diff.DmpDiffer;
import com.haberdashervcs.common.diff.TextVsBinaryChecker;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;


// TODO?: Refactor this to share code with LocalChangeCrawler?
final class GitChangeCrawler {

    private static final HdLogger LOG = HdLoggers.create(GitChangeCrawler.class);


    private final LocalDb db;
    private final RevCommit gitCommit;
    private final Repository gitRepo;

    GitChangeCrawler(LocalDb db, RevCommit gitCommit, Repository gitRepo) {
        this.db = db;
        this.gitCommit = gitCommit;
        this.gitRepo = gitRepo;
    }

    // TODO... Returns empty if the folders are the same? Hmm, no...
    // - We're building a folder listing...
    // - If there's *any* difference from the corresponding commit folder, we should add a new folder obj to the db.
    // - But if they're the same... ???
    // - How can this be correct RECURSIVELY????....????????....
    //   **?? Would it be weird to have separate folder LISTINGS vs. folder ENTRIES? does that even make sense?
    //     - TODO sketch this out...
    void compare() {
        try {
            compareInternal();
        } catch (IOException ioEx) {
            throw new RuntimeException(ioEx);
        }
    }

    private String genId() {
        return UUID.randomUUID().toString();
    }

    private static class WalkEntry {
        private final @Nullable FolderListing baseFolder;
        private final AnyObjectId gitTree;
        private final String generatedFolderId;

        private WalkEntry(FolderListing baseFolder, AnyObjectId gitTree, String generatedFolderId) {
            this.baseFolder = baseFolder;
            this.gitTree = gitTree;
            this.generatedFolderId = generatedFolderId;
        }
    }

    void compareInternal() throws IOException {
        final BranchAndCommit currentBranch = db.getCurrentBranch();
        final FolderListing rootListing = db.getFolder(
                SqliteLocalDbRowKeyer.getInstance().forFolder(
                        currentBranch.getBranchName(), "/", currentBranch.getCommitId()));
        final String rootFolderId = genId();
        LinkedList<WalkEntry> walkEntries = new LinkedList<>();

        TreeWalk treeWalk = new TreeWalk(gitRepo);
        treeWalk.setRecursive(false);
        treeWalk.reset(gitCommit.getTree());
        if (!treeWalk.next()) {
            LOG.info("Skipping this commit -- empty tree.");
            return;
        }
        treeWalk.reset();
        WalkEntry rootWalkEntry = new WalkEntry(rootListing, gitCommit.getTree(), rootFolderId);
        walkEntries.add(rootWalkEntry);


        while (!walkEntries.isEmpty()) {
            WalkEntry thisEntry = walkEntries.pop();
            treeWalk.reset(thisEntry.gitTree);
            ArrayList<FolderListing.Entry> folderEntries = new ArrayList<>();
            LOG.info("This walk entry: %s", thisEntry.generatedFolderId);

            while (treeWalk.next()) {
                // TODO What if the folder is unchanged? How do I mark/handle that?
                final String name = treeWalk.getNameString();
                LOG.debug("TEMP: Tree entry: %s", name);
                final String idForThisEntry = genId();

                Optional<FolderListing.Entry> baseEntry;
                if (thisEntry.baseFolder == null) {
                    baseEntry = Optional.empty();
                } else {
                    baseEntry = thisEntry.baseFolder.getEntryForName(name);
                }


                FolderListing.Entry entry;
                if (treeWalk.isSubtree()) {
                    entry = FolderListing.Entry.forSubFolder(name, idForThisEntry);
                } else {
                    entry = FolderListing.Entry.forFile(name, idForThisEntry);
                }

                if (treeWalk.isSubtree()) {
                    if (baseEntry.isPresent()) {
                        FolderListing baseFolder = db.getFolder(baseEntry.get().getId());
                        WalkEntry walkEntry = new WalkEntry(baseFolder, treeWalk.getObjectId(0), idForThisEntry);
                        LOG.debug("Added walk entry with base: %s", walkEntry.generatedFolderId);
                        walkEntries.add(walkEntry);
                    } else {
                        WalkEntry walkEntry = new WalkEntry(null, treeWalk.getObjectId(0), idForThisEntry);
                        LOG.debug("Added walk entry without base: %s", walkEntry.generatedFolderId);
                        walkEntries.add(walkEntry);
                    }

                } else {
                    LOG.debug(
                            "File: comparing to base entry: %s",
                            (baseEntry.isPresent() ? baseEntry.get() : "(none)"));
                    ObjectLoader loader = gitRepo.open(treeWalk.getObjectId(0));
                    byte[] blobBytes = loader.getBytes();

                    // New
                    if (baseEntry.isEmpty() || baseEntry.get().getType() == FolderListing.Entry.Type.FOLDER) {
                        FileEntry newFile = FileEntry.forNewContents(idForThisEntry, blobBytes);
                        LocalFileState state = LocalFileState.of(false);
                        db.putFile(idForThisEntry, newFile, state);

                    // Modified or unchanged
                    } else {
                        FileEntry oldFile = db.getFile(baseEntry.get().getId());

                        Optional<String> oldText;
                        if (oldFile.getContentsType() == FileEntry.ContentsType.DIFF_DMP) {
                            oldText = Optional.of(db.resolveDiffs(oldFile));
                        } else {
                            oldText = TextVsBinaryChecker.convertToString(oldFile.getContents().getRawBytes());
                        }

                        Optional<String> newText = TextVsBinaryChecker.convertToString(blobBytes);

                        FileEntry newFile = null;
                        if (!oldText.isPresent()) {
                            LOG.debug("Old (left) file is binary.");
                            newFile = FileEntry.forNewContents(idForThisEntry, blobBytes);
                        } else if (!newText.isPresent()) {
                            LOG.debug("New (right) file is binary.");
                            newFile = FileEntry.forNewContents(idForThisEntry, blobBytes);
                        } else {
                            DmpDiffer differ = new DmpDiffer(oldText.get(), newText.get());
                            DmpDiffResult diffResult = differ.compare();  // TEMP! this is the only call to compare()
                            if (diffResult.getType() == DmpDiffResult.Type.SAME) {
                                entry = baseEntry.get();

                            } else {
                                newFile = FileEntry.forDiff(idForThisEntry, diffResult.getPatchesAsBytes(), baseEntry.get().getId());
                            }
                        }

                        if (newFile != null) {
                            LocalFileState state = LocalFileState.of(false);
                            db.putFile(idForThisEntry, newFile, state);
                        }
                    }
                }

                folderEntries.add(entry);
            }

            FolderListing newListing = FolderListing.withoutMergeLock(folderEntries);
            LOG.info("TEMP: Built folder: %s", newListing.getDebugString());
            db.putFolder(thisEntry.generatedFolderId, newListing);
        }


        final String newCommitId = genId();
        final CommitEntry newCommit = CommitEntry.forRootFolderId(rootFolderId);
        LOG.info("Adding new commit: %s / %s", newCommitId, newCommit.getDebugString());

        db.putCommit(newCommitId, newCommit);
        // TODO! db.putBranchState(...);
        //db.setCurrentCommit(newCommitId);
    }
}
