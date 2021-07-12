package com.haberdashervcs.client.push;

import com.google.common.base.Preconditions;
import com.haberdashervcs.common.objects.FolderListing;


class CrawlDiffEntry {

    private final String path;
    private final FolderListing oldL;
    private final FolderListing newL;

    CrawlDiffEntry(String path, FolderListing oldL, FolderListing newL) {
        Preconditions.checkArgument(!(oldL == null && newL == null));
        this.path = path;
        this.oldL = oldL;
        this.newL = newL;
    }

    FolderListing getOld() {
        return oldL;
    }

    FolderListing getNew() {
        return newL;
    }

    public String getPath() {
        return path;
    }
}
