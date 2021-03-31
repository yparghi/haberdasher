package com.haberdashervcs.server.core;

import java.util.Arrays;


/**
 * Wraps an immutable byte array.
 */
public class HdBytes {

    public static HdBytes of(byte[] contents) {
        return new HdBytes(contents);
    }


    private final byte[] contents;

    private HdBytes(byte[] contents) {
        this.contents = Arrays.copyOf(contents, contents.length);
    }

    public byte[] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
