package com.haberdashervcs.common.io;

import java.util.Optional;

import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;


public interface HdObjectInputStream {

    Optional<HdObjectId> next();

    FolderListing getFolder();

    FileEntry getFile();

    CommitEntry getCommit();
}
