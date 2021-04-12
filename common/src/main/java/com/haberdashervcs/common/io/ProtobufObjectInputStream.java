package com.haberdashervcs.common.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.protobuf.CommitsProto;
import com.haberdashervcs.common.protobuf.FilesProto;
import com.haberdashervcs.common.protobuf.FoldersProto;


// TODO! TESTS!!!
public class ProtobufObjectInputStream implements HdObjectInputStream {

    public static ProtobufObjectInputStream forInputStream(InputStream in) {
        return new ProtobufObjectInputStream(in);
    }


    private final InputStream in;
    private final HdObjectByteConverter fromBytes;
    private final byte[] idBuf;
    private int bytesInIdBuf;
    private int bytesNextObj;

    private ProtobufObjectInputStream(InputStream in) {
        this.in = in;
        this.fromBytes = ProtobufObjectByteConverter.getInstance();
        this.idBuf = new byte[256];
        this.bytesInIdBuf = 0;
        this.bytesNextObj = -1;
    }

    @Override
    public Optional<HdObjectId> next() throws IOException {
        Preconditions.checkState(bytesNextObj == -1);
        Preconditions.checkState(bytesInIdBuf == 0);

        while (true) {
            int thisByte = in.read();

            if (thisByte == -1) {
                if (bytesInIdBuf != 0) {
                    throw new IOException("Unexpected EOF in object stream");
                } else {
                    return Optional.empty();
                }

            } else if (thisByte == '\n') {
                return Optional.of(idFromBytes());

            } else {
                idBuf[bytesInIdBuf] = (byte)thisByte;
                ++bytesInIdBuf;

                if (bytesInIdBuf >= idBuf.length) {
                    throw new IOException("Id line was longer than expected!");
                }
            }
        }
    }

    @Override
    public FolderListing getFolder() throws IOException {
        byte[] buf = new byte[bytesNextObj];
        int bytesRead = in.read(buf);
        if (bytesRead != buf.length) {
            throw new IOException("Not enough bytes to read a folder");
        }
        FoldersProto.FolderListing proto = FoldersProto.FolderListing.parseFrom(buf);
        bytesNextObj = -1;
        return fromBytes.folderFromBytes(proto.toByteArray());
    }

    @Override
    public FileEntry getFile() throws IOException {
        byte[] buf = new byte[bytesNextObj];
        int bytesRead = in.read(buf);
        if (bytesRead != buf.length) {
            throw new IOException("Not enough bytes to read a file");
        }
        FilesProto.FileEntry proto = FilesProto.FileEntry.parseFrom(buf);
        bytesNextObj = -1;
        return fromBytes.fileFromBytes(proto.toByteArray());
    }

    @Override
    public CommitEntry getCommit() throws IOException {
        byte[] buf = new byte[bytesNextObj];
        int bytesRead = in.read(buf);
        if (bytesRead != buf.length) {
            throw new IOException("Not enough bytes to read a commit");
        }
        CommitsProto.CommitEntry proto = CommitsProto.CommitEntry.parseFrom(buf);
        bytesNextObj = -1;
        return fromBytes.commitFromBytes(proto.toByteArray());
    }

    private HdObjectId idFromBytes() throws IOException {
        String idString = new String(idBuf, 0, bytesInIdBuf, StandardCharsets.UTF_8);
        String[] parts = idString.split(":", 3);
        if (parts.length != 3) {
            throw new IOException("Object id string isn't of the form <type>:<id>:<num bytes>");
        }

        bytesInIdBuf = 0;
        bytesNextObj = Integer.parseInt(parts[2]);
        return new HdObjectId(HdObjectId.ObjectType.valueOf(parts[0]), parts[1]);
    }
}
