package com.haberdashervcs.client.db;

import java.util.List;
import java.util.Optional;

import com.haberdashervcs.client.db.objects.LocalBranchState;
import com.haberdashervcs.client.db.objects.LocalFileState;
import com.haberdashervcs.client.db.objects.LocalRepoState;
import com.haberdashervcs.common.objects.BranchAndCommit;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;


public interface LocalDb {

    void init(BranchAndCommit branchAndCommit);

    BranchAndCommit getCurrentBranch();
    BranchAndCommit switchToBranch(String branchName);
    void createNewBranch(String branchName);
    LocalBranchState getBranchState(String branchName);
    void putBranchState(String branchName, LocalBranchState newState);

    LocalRepoState getRepoState();
    void updateRepoState(LocalRepoState newState);

    CommitEntry getCommit(String key);

    // TODO obsolete this?
    FolderListing getFolder(String key);

    Optional<FolderListing> findFolderAt(String branchName, String path, long commitId);

    List<FolderListing> getListingsSinceCommit(
            String branchName, String path, long commitIdExclusive);

    FileEntry getFile(String key);

    LocalFileState getFileState(String fileId);

    void putFileState(String fileId, LocalFileState newState);

    void putCommit(String key, CommitEntry commit);

    void putFolder(String key, FolderListing folder);

    void putFile(String key, FileEntry file, LocalFileState state);

    String resolveDiffsToString(FileEntry file);
    byte[] resolveDiffsToBytes(final FileEntry file);

    // TODO: Do the same checked-out paths apply across all branches in the local repo?
    List<String> getCheckedOutPaths();

    List<FolderListing> getAllBranchHeadsSince(String branchName, long baseCommitId);

    Optional<FolderListing> getMostRecentListingForPath(
            long maxCommitId, String branchName, String path);

    void addCheckedOutPath(String path);
}
