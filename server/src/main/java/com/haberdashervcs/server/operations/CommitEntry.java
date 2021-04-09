package com.haberdashervcs.server.operations;

import java.io.IOException;

import com.haberdashervcs.common.protobuf.CommitsProto;


public class CommitEntry {

    public static CommitEntry fromBytes(byte[] bytes) throws IOException {
        CommitsProto.CommitEntry proto = CommitsProto.CommitEntry.parseFrom(bytes);
        return new CommitEntry(proto.getRootFolderId());
    }


    private final String rootFolderId;

    private CommitEntry(String rootFolderId) {
        this.rootFolderId = rootFolderId;
    }

    public String getRootFolderId() {
        return rootFolderId;
    }
}
