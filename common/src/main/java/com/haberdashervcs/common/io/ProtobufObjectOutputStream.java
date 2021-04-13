package com.haberdashervcs.common.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;


// TODO write this javadoc: basic idea is to write e.g. "commit:47\n" then a commit proto of 47 bytes.
public final class ProtobufObjectOutputStream implements HdObjectOutputStream {

    public static ProtobufObjectOutputStream forOutputStream(OutputStream out) {
        return new ProtobufObjectOutputStream(out);
    }


    private final OutputStream out;
    private final HdObjectByteConverter toBytes;

    private ProtobufObjectOutputStream(OutputStream out) {
        this.out = out;
        this.toBytes = ProtobufObjectByteConverter.getInstance();
    }

    @Override
    public void writeFolder(String folderId, FolderListing folder) throws IOException {
        byte[] converted = toBytes.folderToBytes(folder);
        out.write(idToString(new HdObjectId(HdObjectId.ObjectType.FOLDER, folderId), converted.length));
        out.write(converted);
    }

    @Override
    public void writeFile(String fileId, FileEntry file) throws IOException {
        byte[] converted = toBytes.fileToBytes(file);
        out.write(idToString(new HdObjectId(HdObjectId.ObjectType.FILE, fileId), converted.length));
        out.write(converted);
    }

    @Override
    public void writeCommit(String commitId, CommitEntry commit) throws IOException {
        byte[] converted = toBytes.commitToBytes(commit);
        out.write(idToString(new HdObjectId(HdObjectId.ObjectType.COMMIT, commitId), converted.length));
        out.write(converted);
    }

    private byte[] idToString(HdObjectId id, int numBytes) {
        return String.format("%s:%s:%d\n", id.getType(), id.getId(), numBytes)
                .getBytes(StandardCharsets.UTF_8);
    }
}
