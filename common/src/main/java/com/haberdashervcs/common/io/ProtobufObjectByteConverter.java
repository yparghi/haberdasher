package com.haberdashervcs.common.io;

import java.io.IOException;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.protobuf.CommitsProto;
import com.haberdashervcs.common.protobuf.FilesProto;
import com.haberdashervcs.common.protobuf.FoldersProto;


public final class ProtobufObjectByteConverter implements HdObjectByteConverter {

    private static final ProtobufObjectByteConverter INSTANCE = new ProtobufObjectByteConverter();

    public static ProtobufObjectByteConverter getInstance() {
        return INSTANCE;
    }


    private ProtobufObjectByteConverter() {}

    @Override
    public byte[] fileToBytes(FileEntry file) {
        FilesProto.FileEntry.Builder proto = FilesProto.FileEntry.newBuilder();
        proto.setContents(ByteString.copyFrom(file.getContents().getRawBytes()));
        return proto.build().toByteArray();
    }

    @Override
    public byte[] folderToBytes(FolderListing folder) {
        FoldersProto.FolderListing.Builder proto = FoldersProto.FolderListing.newBuilder();
        for (FolderListing.FolderEntry entry : folder.getEntries()) {
            FoldersProto.FolderListingEntry.Builder entryProto = FoldersProto.FolderListingEntry.newBuilder();
            entryProto.setName(entry.getName());
            entryProto.setFileId(entry.getFileId());
            entryProto.setType(
                    (entry.getType() == FolderListing.FolderEntry.Type.FILE)
                    ? FoldersProto.FolderListingEntry.Type.FILE : FoldersProto.FolderListingEntry.Type.FOLDER);
            proto.addEntries(entryProto.build());
        }
        return proto.build().toByteArray();
    }

    @Override
    public byte[] commitToBytes(CommitEntry commit) {
        CommitsProto.CommitEntry.Builder proto = CommitsProto.CommitEntry.newBuilder();
        proto.setRootFolderId(commit.getRootFolderId());
        return proto.build().toByteArray();
    }

    @Override
    public FileEntry fileFromBytes(byte[] fileBytes) throws IOException {
        FilesProto.FileEntry proto = FilesProto.FileEntry.parseFrom(fileBytes);
        return FileEntry.forContents(proto.getContents().toByteArray());
    }

    @Override
    public FolderListing folderFromBytes(byte[] folderBytes) throws IOException {
        FoldersProto.FolderListing listingProto = FoldersProto.FolderListing.parseFrom(folderBytes);
        ImmutableList.Builder<FolderListing.FolderEntry> entries = ImmutableList.builder();

        for (FoldersProto.FolderListingEntry protoEntry : listingProto.getEntriesList()) {
            if (protoEntry.getType() == FoldersProto.FolderListingEntry.Type.FILE) {
                entries.add(FolderListing.FolderEntry.forFile(protoEntry.getName(), protoEntry.getFileId()));
            } else {
                entries.add(FolderListing.FolderEntry.forSubFolder(protoEntry.getName(), protoEntry.getFileId()));
            }
        }

        return FolderListing.forEntries(entries.build());
    }

    @Override
    public CommitEntry commitFromBytes(byte[] commitBytes) throws IOException {
        CommitsProto.CommitEntry proto = CommitsProto.CommitEntry.parseFrom(commitBytes);
        return CommitEntry.forRootFolderId(proto.getRootFolderId());
    }
}
