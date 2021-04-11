package com.haberdashervcs.common.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;


// TODO write this javadoc: basic idea is to write e.g. "commit:47\n" then a commit proto of 47 bytes.
public final class ProtobufObjectOutputStream implements HdObjectOutputStream {

    public ProtobufObjectOutputStream forOutputStream(OutputStream out) {
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
        out.write(idToString(new HdObjectId(HdObjectId.ObjectType.FOLDER, folderId)));
        out.write(toBytes.folderToBytes(folder));
    }

    @Override
    public void writeFile(String fileId, FileEntry file) throws IOException {
        out.write(idToString(new HdObjectId(HdObjectId.ObjectType.FILE, fileId)));
        out.write(toBytes.fileToBytes(file));
    }

    @Override
    public void writeCommit(String commitId, CommitEntry commit) throws IOException {
        out.write(idToString(new HdObjectId(HdObjectId.ObjectType.COMMIT, commitId)));
        out.write(toBytes.commitToBytes(commit));
    }

    private byte[] idToString(HdObjectId id) {
        return String.format("%s:%s\n", id.getType(), id.getId()).getBytes(StandardCharsets.UTF_8);
    }
}
