package com.haberdashervcs.server.operations.change;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.haberdashervcs.server.core.logging.HdLogger;
import com.haberdashervcs.server.core.logging.HdLoggers;
import com.haberdashervcs.server.operations.FileEntry;
import com.haberdashervcs.server.operations.FolderListing;


// TODO Do I really need this class, separate from Changeset? Maybe it will grow to do some real parsing.
public final class ParsedChangeTree {

    private static HdLogger LOG = HdLoggers.create(ParsedChangeTree.class);


    public static ParsedChangeTree fromChangeset(Changeset changeset) {
        return new ParsedChangeTree(changeset);
    }

    private final Changeset changeset;
    private final ImmutableList<FileEntry> addedFiles;

    private ParsedChangeTree(Changeset changeset) {
         this.changeset = changeset;

         this.addedFiles = parseAddedFiles();
    }

    private ImmutableList<FileEntry> parseAddedFiles() {
        ImmutableList.Builder<FileEntry> out = ImmutableList.builder();

        for (AddChange addChange : changeset.getAddChanges()) {
            try {
                out.add(FileEntry.fromBytes(addChange.getContents()));
            } catch(IOException ioEx) {
                throw new RuntimeException(ioEx);
            }
        }

        return out.build();
    }

    public List<FileEntry> getAddedFiles() {
        return addedFiles;
    }

    public List<FolderListing> getChangedFolders() {
        return changeset.getChangedFolders();
    }
}
