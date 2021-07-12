package com.haberdashervcs.client.rebase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.MoreObjects;
import com.haberdashervcs.client.commands.Command;
import com.haberdashervcs.client.commands.RepoConfig;
import com.haberdashervcs.client.commit.CommitCommand;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.objects.LocalBranchState;
import com.haberdashervcs.client.db.objects.LocalRepoState;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDb;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.objects.HdFolderPath;


public final class RebaseCommand implements Command {

    private static final HdLogger LOG = HdLoggers.create(RebaseCommand.class);


    private static class ChangedFile {
        private final String path;
        private final String fileId;
        private final Optional<String> mainBaseFileId;

        private ChangedFile(String path, String fileId, Optional<String> mainBaseFileId) {
            this.path = path;
            this.fileId = fileId;
            this.mainBaseFileId = mainBaseFileId;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("path", path)
                    .add("fileId", fileId)
                    .add("mainBaseFileId", mainBaseFileId)
                    .toString();
        }
    }


    private static class ChangedFileComparison {
        private final ChangedFile onMain;
        private final ChangedFile onBranch;

        private ChangedFileComparison(ChangedFile onMain, ChangedFile onBranch) {
            this.onMain = onMain;
            this.onBranch = onBranch;
        }
    }


    private final RepoConfig config;
    private final LocalDb db;
    private final List<String> otherArgs;

    public RebaseCommand(RepoConfig config, List<String> otherArgs) {
        this.config = config;
        this.db = SqliteLocalDb.getInstance();
        this.otherArgs = otherArgs;
    }


    @Override
    public void perform() throws Exception {
        // Notes, 6/3/2021:
        // - Say the local branch is on base commit 100, with +10 commits.
        // - Say main (on the server) has moved forward to base commit 200.
        // - We want to rebase onto main, ASSUMING (for now) that we always rebase onto the current
        //     head commit, in this case 200.
        // - First, download main up to commit 200 and save it into the db.
        // - Like the merging code on the server, gather all folder histories on the branch for
        //     commits 100+1 through 100+10.
        // - For each of those folders, gather all the histories on main for 101 through 200.
        // - If there are any OVERLAPPING (conflicting) FILES, and add them to a list.
        // - Update the LOCAL branch entry to 200, with some indication in its state that it's
        //     mid-rebase, with these conflicting files.
        // - Use the diff-match-patch library to:
        //     1) Compare the files b/w main-200 and main-100 to get a patch.
        //     2) Apply that patch to the file at branch-100+10 (now updated to branch-200+10 maybe)
        // - Print out the conflicting file paths, and that they're written with a merge attempt
        //     that you should review.
        //
        // - After conflicts have been resolved, the user can run:
        //     `hd rebase [apply/finish/something]`
        // - This will call out to the server with all the branch changes (200 through
        //     200+10+conflict-resolutions), and the server will update the branch's history and
        //     entry.

        LocalRepoState repoState = db.getRepoState();
        // Assume main is already downloaded, and we'll rebase onto its head commit.
        LocalBranchState mainState = db.getBranchState("main");
        BranchAndCommit currentBranch = db.getCurrentBranch();
        if (currentBranch.getBranchName().equals("main")) {
            throw new IllegalArgumentException("You can only rebase a non-main branch.");
        }
        String branchName = currentBranch.getBranchName();
        LocalBranchState branchState = db.getBranchState(branchName);

        if (otherArgs.size() > 0) {
            String subcommand = otherArgs.get(0);
            if (!subcommand.equals("commit")) {
                throw new IllegalArgumentException("Unknown subcommand: " + subcommand);
            } else if (repoState.getState() != LocalRepoState.State.REBASE_IN_PROGRESS) {
                throw new IllegalStateException("No rebase is in progress.");
            } else {
                commitLocal(mainState, branchState, currentBranch);
            }
            return;
        }

        if (repoState.getState() != LocalRepoState.State.NORMAL) {
            throw new IllegalStateException("A repo operation is already in progress.");
        }

        // We don't go commit by commit looking for conflicts (maybe one day we will). Instead, just
        // compare heads for all changed paths on the branch.
        List<FolderListing> branchHeads = db.getAllBranchHeadsSince(
                branchName, branchState.getBaseCommitId());
        LOG.debug("TEMP: Got branch heads: %s", branchHeads);

        List<Optional<FolderListing>> mainHeads = getHeadsOnMainAtCommit(
                mainState.getHeadCommitId(), branchHeads, branchState.getBaseCommitId());
        List<Optional<FolderListing>> mainBases = getHeadsOnMainAtCommit(
                branchState.getBaseCommitId(), branchHeads, -1);

        if (branchHeads.size() != mainHeads.size()
                || mainHeads.size() != mainBases.size()) {
            throw new AssertionError("Mismatched history lists!");
        }

        LOG.debug("TEMP: Got main heads: %s", mainHeads);
        LOG.debug("TEMP: Got main bases: %s", mainBases);

        // TEMP! Next: Look for conflicts, i.e. a path in the main listings whose file id DOES NOT
        //     MATCH the corresponding path -> file id in the branch head (if the path exists
        //     there).
        // Also, how do I handle deletions? It's a conflict if a file is changed in the branch but
        //     deleted in main (or vice versa).
        // - Should I leave some placeholder value in a FolderListing to indicate a file was
        //     deleted?
        //
        // Since this is a 3-way thing, should I also collect the folder listings on main at the
        //     BASE commit, as well as the most recent listing? Should I just make a list of all
        //     changed file paths b/w main-base and main-head, vs. changes b/w branch-base and
        //     branch-head?
        //
        // How this works:
        // - We have file id's at head folders, and we want to know what's changed...
        // - So for each folder, compare it to that folder at main-100. You get a list of changed
        //     files (including absences/deletions).
        // - Do the same for the heads at main-200.
        // - Then compare the lists?
        List<FolderListing> flattenedBase = flatten(mainBases);
        Map<String, ChangedFile> changedPathsOnBranch = getChangedPathsBetween(branchHeads, flattenedBase);
        LOG.debug("TEMP: changedPathsOnBranch: %s", changedPathsOnBranch);
        Map<String, ChangedFile> changedPathsOnMain = getChangedPathsBetween(flatten(mainHeads), flattenedBase);
        LOG.debug("TEMP: changedPathsOnMain: %s", changedPathsOnMain);

        Map<String, ChangedFileComparison> conflictingFiles = getConflicts(
                changedPathsOnMain, changedPathsOnBranch);
        LOG.info("Got conflicting files: %s", conflictingFiles);


        // Thoughts on merging, 6/26/2021:
        // - For each conflicting file...
        // - Write it directly to disk, as filename.xxx.MERGE ?
        // - Print out the results/status?
        //
        // What about updating the final branch state, i.e. updating the base commit id?:
        // - Is this a separate rebase "finalize" command?...
        //     - If so, do I need to track the rebase state/process in the db?
        //
        // **?? Merge using DMP??? A library? Shelling out to 'merge' is too much, I guess.
        //
        // Putting it all together:
        // - No conflicting files? Fine, update the branch's base commit in the DB
        //     * TODO: In push, check if the branch's base commit has changed, to send that change
        //           along to the server.
        // - Conflicts?:
        //     - Set the DB state to locked/rebasing or something.
        //     - Let users handle the conflicts / *.MERGE files, and run 'rebase commit'?
        // - Or, you can abort: this runs a checkout at the current branch + commit to wipe away
        //   the *.MERGE files.

        // NOTE: Changes on both branches may merge without conflict, but even in that case we make
        //     users review the merge manually.
        if (conflictingFiles.isEmpty()) {
            commitLocal(mainState, branchState, currentBranch);

        } else {
            handleConflictingFiles(conflictingFiles);

            LocalRepoState newState = LocalRepoState.forState(LocalRepoState.State.REBASE_IN_PROGRESS);
            db.updateRepoState(newState);

            LOG.info("Rebase done. When you've resolved merge conflicts, run 'hd rebase commit'.");
        }
    }


    private void commitLocal(
            LocalBranchState mainState,
            LocalBranchState branchState,
            BranchAndCommit currentBranch)
            throws Exception {
        CommitCommand commit = new CommitCommand(config, Collections.emptyList());
        commit.perform();

        // TODO: Fix this hack! This writes a new branch state to move up the base commit, after
        //     CommitCommand has just written a new branch state (with head +1).
        LocalBranchState committedState = db.getBranchState(currentBranch.getBranchName());
        LocalBranchState newState = LocalBranchState.of(
                mainState.getHeadCommitId(),
                committedState.getHeadCommitId(),
                committedState.getLastPushedCommitId());
        db.putBranchState(currentBranch.getBranchName(), newState);

        LocalRepoState newRepoState = LocalRepoState.forState(LocalRepoState.State.NORMAL);
        db.updateRepoState(newRepoState);
    }


    private Map<String, ChangedFileComparison> getConflicts(
            Map<String, ChangedFile> changedPathsOnMain,
            Map<String, ChangedFile> changedPathsOnBranch) {
        HashMap<String, ChangedFileComparison> out = new HashMap<>();
        for (String pathOnMain : changedPathsOnMain.keySet()) {
            if (changedPathsOnBranch.containsKey(pathOnMain)) {
                out.put(pathOnMain, new ChangedFileComparison(
                        changedPathsOnMain.get(pathOnMain),
                        changedPathsOnBranch.get(pathOnMain)));
            }
        }
        return out;
    }


    // TODO: Is there a way to do this on the fly, as we're calculating changed paths on the
    //     branch? Would that even save time?
    private void handleConflictingFiles(Map<String, ChangedFileComparison> conflictingFiles) throws IOException {
        for (Map.Entry<String, ChangedFileComparison> conflict : conflictingFiles.entrySet()) {
            ChangedFile branchChange = conflict.getValue().onBranch;
            ChangedFile mainChange = conflict.getValue().onMain;

            final String baseText;
            if (branchChange.mainBaseFileId.isEmpty() != mainChange.mainBaseFileId.isEmpty()) {
                throw new AssertionError("Mismatched main bases!");
            } else if (branchChange.mainBaseFileId.isEmpty()) {
                baseText = "";
            } else if (!branchChange.mainBaseFileId.get().equals(mainChange.mainBaseFileId.get())){
                throw new AssertionError("Unequal main bases!");
            } else {
                FileEntry baseOnMain = db.getFile(branchChange.mainBaseFileId.get());
                baseText = db.resolveDiffs(baseOnMain);
            }

            FileEntry changedOnMain = db.getFile(conflict.getValue().onMain.fileId);
            FileEntry changedOnBranch = db.getFile(conflict.getValue().onBranch.fileId);
            String changedMainText = db.resolveDiffs(changedOnMain);
            String changedBranchText = db.resolveDiffs(changedOnBranch);

            DmpMerger.DmpMergeResult mergeResult = new DmpMerger().mergeText(
                    baseText, changedMainText, changedBranchText);

            String path = conflict.getKey();
            // TODO! Figure out how to canonically handle file paths -- expand HdFolderPath? Add HdFilePath?
            if (!path.startsWith("/")) {
                throw new AssertionError("Unexpected path format: " + path);
            } else {
                path = path.substring(1);
            }
            Path outputPath = config.getRoot().resolve(path + ".MERGE");
            Files.write(outputPath, mergeResult.getMergedText().getBytes(StandardCharsets.UTF_8));

            if (mergeResult.isCleanMerge()) {
                LOG.info("Merged cleanly: %s", outputPath);
            } else {
                LOG.info("Merge CONFLICT: %s", outputPath);
            }
        }
    }


    // TODO! Test whether a new (added) file registers a conflict. Should base be optionals, and
    //     NOT flattened?
    private Map<String, ChangedFile> getChangedPathsBetween(
            List<FolderListing> branch, List<FolderListing> base) {
        Map<String, String> pathsToIdsInBranch = getAllFilePaths(branch);
        Map<String, String> pathsToIdsInBase = getAllFilePaths(base);

        Map<String, ChangedFile> out = new HashMap<>();
        for (Map.Entry<String, String> entry : pathsToIdsInBranch.entrySet()) {
            String path = entry.getKey();
            String fileId = entry.getValue();

            if (!pathsToIdsInBase.containsKey(entry.getKey())) {
                // Deletion
                out.put(path, new ChangedFile(path, fileId, Optional.empty()));

            } else if (!entry.getValue().equals(pathsToIdsInBase.get(entry.getKey()))) {
                // Different file ids
                out.put(path, new ChangedFile(
                        path,
                        fileId,
                        Optional.of(pathsToIdsInBase.get(entry.getKey()))));
            }
        }

        return out;
    }

    private Map<String, String> getAllFilePaths(List<FolderListing> folders) {
        HashMap<String, String> pathToFileId = new HashMap<>();
        for (FolderListing folder : folders) {
            for (FolderListing.Entry entry : folder.getEntries()) {
                if (entry.getType() == FolderListing.Entry.Type.FILE) {
                    String path = HdFolderPath.fromFolderListingFormat(folder.getPath())
                            .filePathForName(entry.getName());
                    if (pathToFileId.containsKey(path)) {
                        throw new AssertionError("Repeated path!: " + path);
                    }
                    pathToFileId.put(path, entry.getId());
                }
            }
        }
        return pathToFileId;
    }


    private <T> List<T> flatten(List<Optional<T>> optList) {
        ArrayList<T> out = new ArrayList<>();
        for (Optional<T> of : optList) {
            if (of.isPresent()) {
                out.add(of.get());
            }
        }
        return out;
    }


    private List<Optional<FolderListing>> getHeadsOnMainAtCommit(
            long maxCommitId,
            List<FolderListing> pathsOnBranch,
            long onlyNewerThanThisCommitId) {

        List<Optional<FolderListing>> headsOnMain = new ArrayList<>();
        for (FolderListing branchHead : pathsOnBranch) {
            Optional<FolderListing> maybeMainHead = db.getMostRecentListingForPath(
                    maxCommitId, "main", branchHead.getPath());
            if (maybeMainHead.isPresent()
                    && maybeMainHead.get().getCommitId() > onlyNewerThanThisCommitId) {
                headsOnMain.add(maybeMainHead);
            } else {
                headsOnMain.add(Optional.empty());
            }
        }
        return headsOnMain;
    }
}
