package com.haberdashervcs.common.io;

import java.io.InputStream;
import java.util.Optional;

import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;


public class ProtobufObjectInputStream implements HdObjectInputStream {

    public static ProtobufObjectInputStream forInputStream(InputStream in) {
        return new ProtobufObjectInputStream(in);
    }


    private final InputStream in;

    private ProtobufObjectInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public Optional<Type> next() {
        return Optional.empty();
    }

    @Override
    public FolderListing getFolder() {
        return null;
    }

    @Override
    public FileEntry getFile() {
        return null;
    }

    @Override
    public CommitEntry getCommit() {
        return null;
    }
}
