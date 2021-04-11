package com.haberdashervcs.common.io;

import java.io.IOException;

import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;


public interface HdObjectByteConverter {

    byte[] fileToBytes(FileEntry file) throws IOException;
    byte[] folderToBytes(FolderListing folder) throws IOException;
    byte[] commitToBytes(CommitEntry commit) throws IOException;

    FileEntry fileFromBytes(byte[] fileBytes) throws IOException;
    FolderListing folderFromBytes(byte[] folderBytes) throws IOException;
    CommitEntry commitFromBytes(byte[] commitBytes) throws IOException;
}
