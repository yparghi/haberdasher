package com.haberdashervcs.server.operations;

import java.io.IOException;

import com.haberdashervcs.server.core.HdBytes;
import com.haberdashervcs.server.protobuf.FilesProto;


public class FileEntry {

    public static FileEntry fromBytes(HdBytes bytes) throws IOException {
        FilesProto.FileEntry proto = FilesProto.FileEntry.parseFrom(bytes.getRawBytes());
        return new FileEntry(proto.getContents().toByteArray());
    }


    private HdBytes contents;

    private FileEntry(byte[] contents) {
        this.contents = HdBytes.of(contents);
    }

    public HdBytes getContents() {
        return contents;
    }
}
