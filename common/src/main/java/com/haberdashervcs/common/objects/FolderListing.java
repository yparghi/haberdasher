package com.haberdashervcs.common.objects;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;


public final class FolderListing {

    public static FolderListing forEntries(List<FolderEntry> entries) {
        return new FolderListing(entries);
    }

    public static class FolderEntry {
        public enum Type {
            FILE,
            FOLDER
        }

        public static FolderEntry forFile(String name, String fileId) {
            return new FolderEntry(Type.FILE, name, fileId);
        }

        public static FolderEntry forSubFolder(String name, String folderId) {
            return new FolderEntry(Type.FOLDER, name, folderId);
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

        public String getId() {
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

    public Optional<FolderEntry> getEntryForName(String name) {
        for(FolderEntry entry : entries) {
            if (entry.getName().equals(name)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public String getSubfolderId(String subfolderName) {
        for (FolderEntry entry : entries) {
            if (entry.getType() == FolderEntry.Type.FOLDER && entry.getName().equals(subfolderName)) {
                return entry.getId();
            }
        }
        throw new IllegalArgumentException("Subfolder not found: " + subfolderName);
    }
}
