package com.haberdashervcs.client.db;


public interface LocalDbRowKeyer {

    String forFile(String fileId);

    String forFolder(String branchName, String path, long commitId);
}
