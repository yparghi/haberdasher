package com.haberdashervcs.server.operations.change;

import java.util.Arrays;

import com.haberdashervcs.common.io.HdBytes;


// TEMP NOTES on where ids come from:
// - Let's assume the client hashes stuff to an id (and the server confirms that hashing)
// - Then a changeset has (and/or computes) the hash for a file/folder.
public final class AddChange {

    // TODO Should this take a FileEntry instead of a byte[] ?
    public static AddChange forContents(String id, byte[] contents) {
        return new AddChange(id, contents);
    }


    private final String id;
    private final HdBytes contents;

    private AddChange(String id, byte[] contents) {
        this.id = id;
        this.contents = HdBytes.of(Arrays.copyOf(contents, contents.length));
    }

    public HdBytes getContents() {
        return contents;
    }

    public String getId() {
        return id;
    }
}
