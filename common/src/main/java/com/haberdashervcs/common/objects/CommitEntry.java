package com.haberdashervcs.common.objects;


public class CommitEntry {

    public static CommitEntry forRootFolderId(String rootFolderId) {
        return new CommitEntry(rootFolderId);
    }


    private final String rootFolderId;

    private CommitEntry(String rootFolderId) {
        this.rootFolderId = rootFolderId;
    }

    public String getRootFolderId() {
        return rootFolderId;
    }
}
