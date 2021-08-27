package com.haberdashervcs.common.objects;


import com.google.common.base.MoreObjects;

public final class CommitEntry {

    public static CommitEntry of(String branchName, long commitId, String author, String message) {
        return new CommitEntry(branchName, commitId, author, message);
    }


    private final String branchName;
    private final long commitId;
    private final String author;
    private final String message;

    private CommitEntry(String branchName, long commitId, String author, String message) {
        this.branchName = branchName;
        this.commitId = commitId;
        this.author = author;
        this.message = message;
    }

    public String getBranchName() {
        return branchName;
    }

    public long getCommitId() {
        return commitId;
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }

    public String getDebugString() {
        return MoreObjects.toStringHelper(this)
                .add("branchName", branchName)
                .add("commitId", commitId)
                .add("author", author)
                .add("message", message)
                .toString();
    }
}
