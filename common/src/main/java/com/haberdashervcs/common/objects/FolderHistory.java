package com.haberdashervcs.common.objects;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;


public final class FolderHistory {

    public static FolderHistory ofMerged(
            List<Entry> entries, String path, String commitLow, String commitHigh) {
        return new FolderHistory(entries, path, commitLow, commitHigh, null);
    }

    public static FolderHistory withMergeLock(
            List<Entry> entries, String path, String commitLow, String commitHigh, String mergeLockId) {
        return new FolderHistory(entries, path, commitLow, commitHigh, mergeLockId);
    }

    public static final class Entry {
        public static Entry of(String commitId, String folderId) {
            return new Entry(commitId, folderId);
        }

        private final String commitId;
        private final String folderId;

        private Entry(String commitId, String folderId) {
            this.commitId = commitId;
            this.folderId = folderId;
        }

        public String getCommitId() {
            return commitId;
        }

        public String getFolderId() {
            return folderId;
        }
    }


    private final ImmutableList<Entry> entries;
    private final String path;
    private final String commitRangeLow;
    private final String commitRangeHigh;
    private final @Nullable String mergeLockId;

    private FolderHistory(
            List<Entry> entries, String path, String commitRangeLow, String commitRangeHigh, String mergeLock) {
        this.entries = ImmutableList.copyOf(entries);
        this.path = path;
        this.commitRangeLow = Preconditions.checkNotNull(commitRangeLow);
        this.commitRangeHigh = Preconditions.checkNotNull(commitRangeHigh);
        this.mergeLockId = mergeLock;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public String getPath() {
        return path;
    }

    public String getCommitRangeLow() {
        return commitRangeLow;
    }

    public String getCommitRangeHigh() {
        return commitRangeHigh;
    }

    public Optional<String> getMergeLockId() {
        return Optional.ofNullable(mergeLockId);
    }
}
