package com.haberdashervcs.server.operations;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.haberdashervcs.server.protobuf.FoldersProto;


public final class FolderListing {

    public static FolderListing fromBytes(byte[] listingBytes) throws IOException {
        FoldersProto.FolderListing listingProto = FoldersProto.FolderListing.parseFrom(listingBytes);
        ImmutableList.Builder<FolderEntry> entries = ImmutableList.builder();

        for (FoldersProto.FolderListingEntry protoEntry : listingProto.getEntriesList()) {
            FolderEntry entry = new FolderEntry(
                    (protoEntry.getType() == FoldersProto.FolderListingEntry.Type.FILE)
                            ? FolderEntry.Type.FILE : FolderEntry.Type.FOLDER,
                    protoEntry.getName());
        }
        return new FolderListing(entries.build());
    }

    public static class FolderEntry {
        enum Type {
            FILE,
            FOLDER
        }

        private final Type type;
        private final String name;

        private FolderEntry(Type type, String name) {
            this.type = type;
            this.name = name;
        }
    }

    private final ImmutableList<FolderEntry> entries;

    private FolderListing(List<FolderEntry> entries) {
        this.entries = ImmutableList.copyOf(entries);
    }
}
