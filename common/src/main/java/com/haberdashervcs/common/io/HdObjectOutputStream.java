package com.haberdashervcs.common.io;

import java.io.IOException;

import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;


public interface HdObjectOutputStream {

    void writeFolder(String folderId, FolderListing folder) throws IOException;

    void writeFile(String fileId, FileEntry file) throws IOException;

    void writeCommit(String commitId, CommitEntry commit) throws IOException;
}
