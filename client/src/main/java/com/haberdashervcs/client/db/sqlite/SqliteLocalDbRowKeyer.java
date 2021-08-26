package com.haberdashervcs.client.db.sqlite;

import com.haberdashervcs.client.db.LocalDbRowKeyer;
import com.haberdashervcs.common.objects.CommitEntry;


public class SqliteLocalDbRowKeyer implements LocalDbRowKeyer {

    private static final SqliteLocalDbRowKeyer INSTANCE = new SqliteLocalDbRowKeyer();

    public static SqliteLocalDbRowKeyer getInstance() {
        return INSTANCE;
    }


    @Override
    public String forFile(String fileId) {
        return String.format("%s", fileId);
    }

    @Override
    public String forFolder(String branchName, String path, long commitId) {
        return String.format("%s:%s:%020d", branchName, path, commitId);
    }

    @Override
    public String forCommit(CommitEntry newCommit) {
        return String.format("%s:%020d", newCommit.getBranchName(), newCommit.getCommitId());
    }
}
