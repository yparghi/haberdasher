package com.haberdashervcs.common.objects;


public final class BranchAndCommit {

    public static BranchAndCommit of(String branchName, long commitId) {
        return new BranchAndCommit(branchName, commitId);
    }

    private final String branchName;
    private final long commitId;

    private BranchAndCommit(String currentBranch, long currentCommit) {
        this.branchName = currentBranch;
        this.commitId = currentCommit;
    }

    public String getBranchName() {
        return branchName;
    }

    public long getCommitId() {
        return commitId;
    }
}
