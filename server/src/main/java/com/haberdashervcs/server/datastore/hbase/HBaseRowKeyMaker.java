package com.haberdashervcs.server.datastore.hbase;

import java.nio.charset.StandardCharsets;

import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.MergeLock;


public final class HBaseRowKeyMaker {

    private static final HdLogger LOG = HdLoggers.create(HBaseRowKeyMaker.class);


    public static HBaseRowKeyMaker of(String org, String repo) {
        return new HBaseRowKeyMaker(org, repo);
    }


    private final String org;
    private final String repo;

    private HBaseRowKeyMaker(String org, String repo) {
        this.org = org;
        this.repo = repo;
    }

    public String getOrg() {
        return org;
    }

    public String getRepo() {
        return repo;
    }

    public byte[] forCommit(String commitId) {
        return String.format("%s:%s:%s", org, repo, commitId).getBytes(StandardCharsets.UTF_8);
    }

    // TODO: Should the start/stop row prefixes for folder history scans also be implemented in this class?
    public byte[] forFolderAt(String branchName, String path, long commitId) {
        // TODO: Should I trim the leading and trailing slash, just to save a little space?
        String key = String.format(
                "%s:%s:%s:%s:%020d", org, repo, branchName, path, commitId);
        return key.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] forFile(String fileId) {
        return String.format("%s:%s:%s", org, repo, fileId).getBytes(StandardCharsets.UTF_8);
    }

    public byte[] prefixForMergeLocksAtTimestamp(long timestampMillis) {
        return String.format("%s:%s:%d", org, repo, timestampMillis).getBytes(StandardCharsets.UTF_8);

    }

    public byte[] forMerge(MergeLock lock) {
        return String.format("%s:%s:%d:%s", org, repo, lock.getTimestampMillis(), lock.getId())
                .getBytes(StandardCharsets.UTF_8);
    }

    public byte[] forBranch(String branchName) {
        return String.format("%s:%s:%s", org, repo, branchName).getBytes(StandardCharsets.UTF_8);
    }
}
