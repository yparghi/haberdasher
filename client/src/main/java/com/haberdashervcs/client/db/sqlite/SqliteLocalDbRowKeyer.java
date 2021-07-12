package com.haberdashervcs.client.db.sqlite;

import com.haberdashervcs.client.db.LocalDbRowKeyer;


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
}
