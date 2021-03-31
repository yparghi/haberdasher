package com.haberdashervcs.server.operations;

import com.haberdashervcs.server.protobuf.FoldersProto;

import java.io.IOException;


public final class FolderListing {

    public static FolderListing fromBytes(byte[] listingBytes) throws IOException {
        FoldersProto.FolderListing listingProto = FoldersProto.FolderListing.parseFrom(listingBytes);
        return new FolderListing();
    }

    private FolderListing() {}
}
