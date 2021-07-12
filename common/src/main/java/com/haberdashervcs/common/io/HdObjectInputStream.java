package com.haberdashervcs.common.io;

import java.io.IOException;
import java.util.Optional;

import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;


public interface HdObjectInputStream {

    Optional<HdObjectId> next() throws IOException;

    FolderListing getFolder() throws IOException;

    FileEntry getFile() throws IOException;

    CommitEntry getCommit() throws IOException;
}
