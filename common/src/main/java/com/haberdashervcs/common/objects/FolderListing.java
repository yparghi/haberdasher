package com.haberdashervcs.common.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;


public final class FolderListing {

    public static FolderListing withoutMergeLock(List<Entry> entries, String path, long commitId) {
        return new FolderListing(entries, null, path, commitId);
    }

    public static FolderListing withMergeLock(List<Entry> entries, String path, long commitId, String mergeLockId) {
        return new FolderListing(entries, mergeLockId, path, commitId);
    }

    public static FolderListing withoutMergeLock(ArrayList<Entry> folderEntries) {
        throw new UnsupportedOperationException("TEMPBUILD");
    }

    public static class Entry {
        public enum Type {
            FILE,
            FOLDER
        }

        public static Entry forFile(String name, String fileId) {
            return new Entry(Type.FILE, name, fileId);
        }

        public static Entry forSubFolder(String name, String folderId) {
            return new Entry(Type.FOLDER, name, folderId);
        }


        private final Type type;
        private final String name;
        private final String objectId;

        private Entry(Type type, String name, String id) {
            this.type = type;
            this.name = name;
            this.objectId = id;
        }

        public Type getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getId() {
            return objectId;
        }

        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("type", type)
                    .add("name", name)
                    .add("id", objectId)
                    .toString();
        }
    }

    private final ImmutableList<Entry> entries;
    private final Optional<String> mergeLockId;
    private final String path;
    private final long commitId;

    private FolderListing(List<Entry> entries, @Nullable String mergeLockId, String path, long commitId) {
        this.entries = ImmutableList.copyOf(entries);
        this.mergeLockId = Optional.ofNullable(mergeLockId);
        this.path = path;
        this.commitId = commitId;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public Optional<Entry> getEntryForName(String name) {
        for(Entry entry : entries) {
            if (entry.getName().equals(name)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public String getSubfolderId(String subfolderName) {
        for (Entry entry : entries) {
            if (entry.getType() == Entry.Type.FOLDER && entry.getName().equals(subfolderName)) {
                return entry.getId();
            }
        }
        throw new IllegalArgumentException("Subfolder not found: " + subfolderName);
    }

    public Optional<String> getMergeLockId() {
        return mergeLockId;
    }

    public String getPath() {
        return path;
    }

    public long getCommitId() {
        return commitId;
    }

    @Override
    public String toString() {
        return getDebugString();
    }

    public String getDebugString() {
        return MoreObjects.toStringHelper(this)
                .add("entries", entries)
                .add("path", path)
                .add("commitId", commitId)
                .add("mergeLockId", mergeLockId)
                .toString();
    }
}
