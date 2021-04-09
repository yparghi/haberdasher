package com.haberdashervcs.server.operations;

import java.io.IOException;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.haberdashervcs.common.protobuf.FoldersProto;


public final class FolderListing {

    // TODO Factory method from POJOs, for testing?

    // TODO: Some iface for the bytes-from-datastore -> object converter? (so that protobuf isn't hardcoded here)
    public static FolderListing fromBytes(byte[] listingBytes) throws IOException {
        FoldersProto.FolderListing listingProto = FoldersProto.FolderListing.parseFrom(listingBytes);
        ImmutableList.Builder<FolderEntry> entries = ImmutableList.builder();

        for (FoldersProto.FolderListingEntry protoEntry : listingProto.getEntriesList()) {
            FolderEntry entry = new FolderEntry(
                    (protoEntry.getType() == FoldersProto.FolderListingEntry.Type.FILE)
                            ? FolderEntry.Type.FILE : FolderEntry.Type.FOLDER,
                    protoEntry.getName(),
                    protoEntry.getFileId());
            entries.add(entry);
        }
        return new FolderListing(entries.build());
    }

    public static class FolderEntry {
        public enum Type {
            FILE,
            FOLDER
        }

        private final Type type;
        private final String name;
        private final String fileId;

        private FolderEntry(Type type, String name, String fileId) {
            this.type = type;
            this.name = name;
            this.fileId = fileId;
        }

        public Type getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getFileId() {
            Preconditions.checkState(type == Type.FILE);
            return fileId;
        }
    }

    private final ImmutableList<FolderEntry> entries;

    private FolderListing(List<FolderEntry> entries) {
        this.entries = ImmutableList.copyOf(entries);
    }

    public List<FolderEntry> getEntries() {
        return entries;
    }

    public String getSubfolderId(String subfolderName) {
        for (FolderEntry entry : entries) {
            if (entry.getType() == FolderEntry.Type.FOLDER && entry.getName().equals(subfolderName)) {
                return entry.getFileId();
            }
        }
        throw new IllegalArgumentException("Subfolder not found: " + subfolderName);
    }
}
