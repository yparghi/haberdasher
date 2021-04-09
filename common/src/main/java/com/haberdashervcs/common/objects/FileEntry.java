package com.haberdashervcs.common.objects;

import java.io.IOException;

import com.haberdashervcs.common.io.HdBytes;
import com.haberdashervcs.common.protobuf.FilesProto;


public class FileEntry {

    public static FileEntry fromBytes(String id, HdBytes bytes) throws IOException {
        FilesProto.FileEntry proto = FilesProto.FileEntry.parseFrom(bytes.getRawBytes());
        return new FileEntry(id, proto.getContents().toByteArray());
    }


    private final String id;
    private HdBytes contents;

    private FileEntry(String id, byte[] contents) {
        this.id = id;
        this.contents = HdBytes.of(contents);
    }

    public HdBytes getContents() {
        return contents;
    }

    public String getId() {
        return id;
    }
}
