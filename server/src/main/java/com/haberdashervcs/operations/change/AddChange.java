package com.haberdashervcs.operations.change;

import java.util.Arrays;


public final class AddChange {

    public static AddChange forContents(byte[] contents) {
        return new AddChange(contents);
    }


    // TODO: Some immutable wrapper for the bytes?
    private final byte[] contents;

    private AddChange(byte[] contents) {
        this.contents = Arrays.copyOf(contents, contents.length);
    }

    public byte[] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
