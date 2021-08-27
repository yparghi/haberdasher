package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.objects.MergeLock;


public final class Merger {

    private static final HdLogger LOG = HdLoggers.create(Merger.class);


    public static final class MergeResult {
        public enum State {
            FAILED,
            SUCCESSFUL
        }

        private final State state;
        private final String message;
        private final long commitOnMain;

        private MergeResult(State state, String message, long commitOnMain) {
            this.state = state;
            this.message = message;
            this.commitOnMain = commitOnMain;
        }

        public State getState() {
            return state;
        }

        public String getMessage() {
            return message;
        }

        public long getNewCommitOnMain() {
            return commitOnMain;
        }
    }


    // The idea:
    // - Gather *every* folder history for this branch
    // - For each path in the histories, add the new folder & a lock, using `checkAndPut` with the value pulled
    //     - Bail on this whole thing if there's already an *active* lock on one of the histories.
    // - Make it official: if you get through that whole list, mark the branch as merged.
    // - Clean-up: Go through the histories again (with their updated values). Use checkAndPut to remove the locks.
    public MergeResult merge(String org, String repo, String branchName, HBaseRawHelper helper) throws IOException {
        HBaseRowKeyer rowKeyer = HBaseRowKeyer.forRepo(org, repo);
        BranchEntry branchEntry = helper.getBranch(rowKeyer.forBranch(branchName)).get();
        MergeStates mergeStates = MergeStates.fromPastSeconds(TimeUnit.HOURS.toSeconds(1), helper, rowKeyer);
        final long nowTs = System.currentTimeMillis();

        final List<HBaseRawHelper.FolderListingWithOriginalBytes> branchHeadHistories =
                helper.getAllHeadBranchHistories(org, repo, branchName);
        final List<Optional<HBaseRawHelper.FolderListingWithOriginalBytes>> mainHeadHistories = new ArrayList<>();

        for (HBaseRawHelper.FolderListingWithOriginalBytes headOnBranch : branchHeadHistories) {

            Optional<HBaseRawHelper.FolderListingWithOriginalBytes> fromMain = helper.getMostRecentListingForPath(
                    org, repo, "main", branchEntry.getHeadCommitId(), headOnBranch.listing.getPath());
            mainHeadHistories.add(fromMain);

            if (fromMain.isPresent()) {
                if (fromMain.get().listing.getMergeLockId().isPresent()) {
                    Optional<MergeLock> existingLock = mergeStates.forMergeLockId(fromMain.get().listing.getMergeLockId().get());
                    if (existingLock.isPresent() && existingLock.get().isActive(nowTs)) {
                        return new MergeResult(
                                MergeResult.State.FAILED,
                                "Failed to acquire locks for changed folders in this merge.",
                                -1);
                    }
                }

                boolean newerCommitHasHappenedOnMain = (fromMain.get().listing.getCommitId() > branchEntry.getBaseCommitId());
                if (newerCommitHasHappenedOnMain) {
                    return new MergeResult(
                            MergeResult.State.FAILED,
                            "The main-branch history for this folder has changed since branching -- the branch should be rebased before trying to merge again.",
                            -1);
                }

            } else {
                // TODO! What about a race condition where another concurrent/competing merge has written a history?
                //     Some kind of write IF NOT PRESENT?
            }
        }


        // Now, lock the folders.

        if (branchHeadHistories.size() != mainHeadHistories.size()) {
            throw new AssertionError("Mismatched history entries!");
        }

        MergeLock newLock = MergeLock.of(
                UUID.randomUUID().toString(),
                branchName,
                MergeLock.State.IN_PROGRESS,
                nowTs);


        final HeadCommitNumberTaker numberTaker = HeadCommitNumberTaker.forDb(helper, rowKeyer);
        final long wouldBeNewCommitIdOnMain = numberTaker.getNewHeadCommitId();

        for (int i = 0; i < branchHeadHistories.size(); ++i) {
            HBaseRawHelper.FolderListingWithOriginalBytes headOnBranch = branchHeadHistories.get(i);

            FolderListing newListing = FolderListing.withMergeLock(
                    headOnBranch.listing.getEntries(),
                    headOnBranch.listing.getPath(),
                    wouldBeNewCommitIdOnMain,
                    newLock.getId());

            helper.putFolderIfNotExists(
                    rowKeyer.forFolderAt("main", newListing.getPath(), newListing.getCommitId()),
                    newListing);
        }


        // Mark the merge as complete to officially make it part of main's history.

        MergeLock completed = MergeLock.of(
                newLock.getId(), newLock.getBranchName(), MergeLock.State.COMPLETED, newLock.getTimestampMillis());
        helper.putMerge(rowKeyer.forMerge(completed), completed);


        // TODO: Clean-up: Go through the histories again (with their updated values). Use checkAndMutate to remove the
        //     locks.

        return new MergeResult(
                MergeResult.State.SUCCESSFUL,
                "Successfully merged into main.",
                wouldBeNewCommitIdOnMain);
    }
}
