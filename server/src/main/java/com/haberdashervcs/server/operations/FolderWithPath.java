package com.haberdashervcs.server.operations;

public final class FolderWithPath {

    public static FolderWithPath forPathAndListing(String id, String path, FolderListing listing) {
        return new FolderWithPath(id, path, listing);
    }


    private final String id;
    private final String path;
    private final FolderListing listing;

    private FolderWithPath(String id, String path, FolderListing listing) {
        this.id = id;
        this.path = path;
        this.listing = listing;
    }

    public String getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public FolderListing getListing() {
        return listing;
    }
}
