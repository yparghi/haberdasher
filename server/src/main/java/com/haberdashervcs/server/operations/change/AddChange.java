package com.haberdashervcs.server.operations.change;

import java.util.Arrays;

import com.haberdashervcs.common.io.HdBytes;
import com.haberdashervcs.common.objects.FileEntry;


// TEMP NOTES on where ids come from:
// - Let's assume the client hashes stuff to an id (and the server confirms that hashing)
// - Then a changeset has (and/or computes) the hash for a file/folder.
public final class AddChange {

    public static AddChange forContents(String id, FileEntry file) {
        return new AddChange(id, file);
    }


    private final String id;
    private final FileEntry file;

    private AddChange(String id, FileEntry file) {
        this.id = id;
        this.file = file;
    }

    public String getId() {
        return id;
    }

    public FileEntry getFile() {
        return file;
    }
}
