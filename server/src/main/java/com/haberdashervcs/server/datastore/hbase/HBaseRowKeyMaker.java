package com.haberdashervcs.server.datastore.hbase;

import java.nio.charset.StandardCharsets;


public final class HBaseRowKeyMaker {

    public static HBaseRowKeyMaker of(String org, String repo) {
        return new HBaseRowKeyMaker(org, repo);
    }


    private final String org;
    private final String repo;

    private HBaseRowKeyMaker(String org, String repo) {
        this.org = org;
        this.repo = repo;
    }

    public byte[] forCommit(String commitId) {
        return String.format("%s_%s_%s", org, repo, commitId).getBytes(StandardCharsets.UTF_8);
    }

    public byte[] forFolder(String folderId) {
        return String.format("%s_%s_%s", org, repo, folderId).getBytes(StandardCharsets.UTF_8);
    }

    public byte[] forFile(String fileId) {
        return String.format("%s_%s_%s", org, repo, fileId).getBytes(StandardCharsets.UTF_8);
    }
}
