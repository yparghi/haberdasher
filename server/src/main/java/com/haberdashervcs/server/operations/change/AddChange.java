package com.haberdashervcs.server.operations.change;

import java.util.Arrays;

import com.haberdashervcs.server.core.HdBytes;


public final class AddChange {

    public static AddChange forContents(byte[] contents) {
        return new AddChange(contents);
    }


    private final HdBytes contents;

    private AddChange(byte[] contents) {
        this.contents = HdBytes.of(Arrays.copyOf(contents, contents.length));
    }

    public HdBytes getContents() {
        return contents;
    }
}
