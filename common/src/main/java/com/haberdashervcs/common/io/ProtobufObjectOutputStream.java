package com.haberdashervcs.common.io;

import java.io.OutputStream;

import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;


// TODO: basic idea is to write e.g. "commit:47\n" then a commit proto of 47 bytes.
public final class ProtobufObjectOutputStream implements HdObjectOutputStream {

    public ProtobufObjectOutputStream forOutputStream(OutputStream out) {
        return new ProtobufObjectOutputStream(out);
    }


    private final OutputStream out;

    private ProtobufObjectOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void writeFolder(FolderListing folder) {

    }

    @Override
    public void writeFile(FileEntry file) {

    }

    @Override
    public void writeCommit(CommitEntry commit) {

    }
}
