package com.haberdashervcs.common.objects;


import com.google.common.base.MoreObjects;

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

    public String getDebugString() {
        return "CommitEntry: " + MoreObjects.toStringHelper(this)
                .add("rootFolderId", rootFolderId)
                .toString();
    }
}
