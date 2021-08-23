package com.haberdashervcs.client.crawl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.common.diff.DmpDiffResult;
import com.haberdashervcs.common.diff.DmpDiffer;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;


public final class EntryComparisonThisFolder {

    final String name;

    // TODO! Should I also store the commit's FolderListing as commitParentFolder?
    //     If I do, maybe this whole thing should be refactored for immutability with factory
    //     methods and field asserts.
    @Nullable
    FolderListing.Entry entryInCommit = null;

    @Nullable
    Path pathInLocalRepo = null;

    EntryComparisonThisFolder(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public FolderListing.Entry getEntryInCommit() {
        return entryInCommit;
    }

    @Nullable
    public Path getPathInLocalRepo() {
        return pathInLocalRepo;
    }


    // TODO! What does text vs. binary entail here?
    public boolean isDiffableText() {
        return (entryInCommit != null
                && entryInCommit.getType() == FolderListing.Entry.Type.FILE
                && pathInLocalRepo != null);
    }

    // TODO! What does text vs. binary entail here?
    public DmpDiffResult generateDiffs(LocalDb db) throws IOException {
        Preconditions.checkState(isDiffableText());

        FileEntry fromCommit = db.getFile(entryInCommit.getId());
        String fromCommitContents = db.resolveDiffsToString(fromCommit);
        String fromLocalFile = Files.readString(pathInLocalRepo, StandardCharsets.UTF_8);
        return new DmpDiffer(fromCommitContents, fromLocalFile).compare();
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("entryInCommit", entryInCommit)
                .add("pathInLocalRepo", pathInLocalRepo)
                .toString();
    }
}
