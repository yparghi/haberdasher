package com.haberdashervcs.common.io;

import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;


public interface HdObjectOutputStream {

    void writeFolder(FolderListing folder);

    void writeFile(FileEntry file);

    void writeCommit(CommitEntry commit);
}
