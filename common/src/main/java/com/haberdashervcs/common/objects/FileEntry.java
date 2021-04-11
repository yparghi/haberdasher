package com.haberdashervcs.common.objects;

import com.haberdashervcs.common.io.HdBytes;


public class FileEntry {

    public static FileEntry forContents(byte[] bytes) {
        return new FileEntry(bytes);
    }


    private HdBytes contents;

    private FileEntry(byte[] contents) {
        this.contents = HdBytes.of(contents);
    }

    public HdBytes getContents() {
        return contents;
    }
}
