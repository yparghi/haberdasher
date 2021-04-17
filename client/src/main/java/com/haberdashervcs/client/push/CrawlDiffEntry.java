package com.haberdashervcs.client.push;

import com.google.common.base.Preconditions;
import com.haberdashervcs.common.objects.FolderListing;


class CrawlDiffEntry {

    private final FolderListing oldL;
    private final FolderListing newL;

    CrawlDiffEntry(FolderListing oldL, FolderListing newL) {
        Preconditions.checkArgument(!(oldL == null && newL == null));
        this.oldL = oldL;
        this.newL = newL;
    }

    FolderListing getOld() {
        return oldL;
    }

    FolderListing getNew() {
        return newL;
    }
}
