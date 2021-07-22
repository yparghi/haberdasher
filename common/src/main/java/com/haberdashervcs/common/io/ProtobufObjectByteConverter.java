package com.haberdashervcs.common.io;

import java.io.IOException;
import java.util.ArrayList;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;
import com.haberdashervcs.common.objects.BranchEntry;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderHistory;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.objects.MergeLock;
import com.haberdashervcs.common.objects.user.HdUser;
import com.haberdashervcs.common.protobuf.BranchesProto;
import com.haberdashervcs.common.protobuf.CommitsProto;
import com.haberdashervcs.common.protobuf.FilesProto;
import com.haberdashervcs.common.protobuf.FoldersProto;
import com.haberdashervcs.common.protobuf.MergesProto;
import com.haberdashervcs.common.protobuf.ServerProto;
import com.haberdashervcs.common.protobuf.UsersProto;


public final class ProtobufObjectByteConverter implements HdObjectByteConverter {

    private static final HdLogger LOG = HdLoggers.create(ProtobufObjectByteConverter.class);


    private static final ProtobufObjectByteConverter INSTANCE = new ProtobufObjectByteConverter();

    public static ProtobufObjectByteConverter getInstance() {
        return INSTANCE;
    }


    private ProtobufObjectByteConverter() {}

    @Override
    public byte[] fileToBytes(FileEntry file) {
        FilesProto.FileEntry.Builder proto = FilesProto.FileEntry.newBuilder();
        proto.setId(file.getId());
        proto.setContents(ByteString.copyFrom(file.getContents().getRawBytes()));
        final FilesProto.FileEntry.ContentsType contentsType;
        switch (file.getContentsType()) {
            case DIFF_DMP:
                contentsType = FilesProto.FileEntry.ContentsType.DIFF_DMP;
                break;
            case FULL:
                contentsType = FilesProto.FileEntry.ContentsType.FULL;
                break;
            default:
                throw new IllegalArgumentException("Unknown contents type: " + file.getContentsType());
        }
        proto.setContentsType(contentsType);
        if (file.getBaseEntryId().isPresent()) {
            proto.setDiffBaseEntryId(file.getBaseEntryId().get());
        }
        return proto.build().toByteArray();
    }

    @Override
    public byte[] folderToBytes(FolderListing folder) {
        FoldersProto.FolderListing.Builder proto = FoldersProto.FolderListing.newBuilder();
        proto.setPath(folder.getPath());
        proto.setCommitId(folder.getCommitId());
        if (folder.getMergeLockId().isPresent()) {
            proto.setMergeLockId(folder.getMergeLockId().get());
        }
        for (FolderListing.Entry entry : folder.getEntries()) {
            FoldersProto.FolderListingEntry.Builder entryProto = FoldersProto.FolderListingEntry.newBuilder();
            entryProto.setName(entry.getName());
            entryProto.setId(entry.getId());
            entryProto.setType(
                    (entry.getType() == FolderListing.Entry.Type.FILE)
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

        if (proto.getContentsType() == FilesProto.FileEntry.ContentsType.FULL) {
            return FileEntry.forNewContents(proto.getId(), proto.getContents().toByteArray());
        } else if (proto.getContentsType() == FilesProto.FileEntry.ContentsType.DIFF_DMP) {
            return FileEntry.forDiff(
                    proto.getId(), proto.getContents().toByteArray(), proto.getDiffBaseEntryId());

        } else {
            throw new IllegalArgumentException("Unknown contents type: " + proto.getContentsType());
        }
    }

    @Override
    public FolderListing folderFromBytes(byte[] folderBytes) throws IOException {
        FoldersProto.FolderListing proto = FoldersProto.FolderListing.parseFrom(folderBytes);
        ImmutableList.Builder<FolderListing.Entry> entries = ImmutableList.builder();

        for (FoldersProto.FolderListingEntry protoEntry : proto.getEntriesList()) {
            if (protoEntry.getType() == FoldersProto.FolderListingEntry.Type.FILE) {
                entries.add(FolderListing.Entry.forFile(protoEntry.getName(), protoEntry.getId()));
            } else {
                entries.add(FolderListing.Entry.forSubFolder(protoEntry.getName(), protoEntry.getId()));
            }
        }

        if (proto.getMergeLockId().isEmpty()) {
            return FolderListing.withoutMergeLock(entries.build(), proto.getPath(), proto.getCommitId());
        } else {
            return FolderListing.withMergeLock(
                    entries.build(), proto.getPath(), proto.getCommitId(), proto.getMergeLockId());
        }
    }

    @Override
    public CommitEntry commitFromBytes(byte[] commitBytes) throws IOException {
        CommitsProto.CommitEntry proto = CommitsProto.CommitEntry.parseFrom(commitBytes);
        return CommitEntry.forRootFolderId(proto.getRootFolderId());
    }

    @Override
    public byte[] folderHistoryToBytes(FolderHistory folderHistory) throws IOException {
        FoldersProto.FolderHistory.Builder out = FoldersProto.FolderHistory.newBuilder();
        out.setPath(folderHistory.getPath());
        out.setCommitLow(folderHistory.getCommitRangeLow());
        out.setCommitHigh(folderHistory.getCommitRangeHigh());
        if (folderHistory.getMergeLockId().isPresent()) {
            out.setMergeLockId(folderHistory.getMergeLockId().get());
        }

        for (FolderHistory.Entry entry : folderHistory.getEntries()) {
            out.addHistoryEntries(FoldersProto.FolderHistoryEntry.newBuilder()
                    .setCommitId(entry.getCommitId())
                    .setFolderId(entry.getFolderId())
                    .build());
        }

        return out.build().toByteArray();
    }

    @Override
    public FolderHistory folderHistoryFromBytes(byte[] folderHistoryBytes) throws IOException {
        FoldersProto.FolderHistory proto = FoldersProto.FolderHistory.parseFrom(folderHistoryBytes);
        ArrayList<FolderHistory.Entry> entries = new ArrayList<>();

        for (FoldersProto.FolderHistoryEntry protoEntry : proto.getHistoryEntriesList()) {
            entries.add(FolderHistory.Entry.of(protoEntry.getCommitId(), protoEntry.getFolderId()));
        }

        if (proto.getMergeLockId().isEmpty()) {
            return FolderHistory.ofMerged(entries, proto.getPath(), proto.getCommitLow(), proto.getCommitHigh());
        } else {
            return FolderHistory.withMergeLock(
                    entries, proto.getPath(), proto.getCommitLow(), proto.getCommitHigh(), proto.getMergeLockId());
        }
    }

    @Override
    public byte[] mergeLockToBytes(MergeLock mergeLock) throws IOException {
        MergesProto.MergeLock.Builder out = MergesProto.MergeLock.newBuilder();
        out.setId(mergeLock.getId());
        out.setBranchName(mergeLock.getBranchName());
        final MergesProto.MergeLock.State state;
        switch (mergeLock.getState()) {
            case COMPLETED:
                state = MergesProto.MergeLock.State.COMPLETED;
                break;
            case FAILED:
                state = MergesProto.MergeLock.State.FAILED;
                break;
            case IN_PROGRESS:
                state = MergesProto.MergeLock.State.IN_PROGRESS;
                break;
            default:
                throw new IllegalArgumentException("Unknown merge state: " + mergeLock.getState());
        }
        out.setState(state);
        out.setTimestampMillis(mergeLock.getTimestampMillis());
        return out.build().toByteArray();
    }

    @Override
    public MergeLock mergeLockFromBytes(byte[] mergeLockBytes) throws IOException {
        MergesProto.MergeLock proto = MergesProto.MergeLock.parseFrom(mergeLockBytes);
        final MergeLock.State state;
        switch (proto.getState()) {
            case COMPLETED:
                state = MergeLock.State.COMPLETED;
                break;
            case IN_PROGRESS:
                state = MergeLock.State.IN_PROGRESS;
                break;
            case FAILED:
                state = MergeLock.State.FAILED;
                break;
            default:
                throw new IllegalArgumentException("Unknown merge state: " + proto.getState());
        }
        return MergeLock.of(proto.getId(), proto.getBranchName(), state, proto.getTimestampMillis());
    }

    @Override
    public BranchEntry branchFromBytes(byte[] branchBytes) throws IOException {
        BranchesProto.BranchEntry proto = BranchesProto.BranchEntry.parseFrom(branchBytes);
        return BranchEntry.of(proto.getName(), proto.getBaseCommitId(), proto.getHeadCommitId());
    }

    @Override
    public byte[] branchToBytes(BranchEntry branch) throws IOException {
        BranchesProto.BranchEntry out = BranchesProto.BranchEntry.newBuilder()
                .setName(branch.getName())
                .setBaseCommitId(branch.getBaseCommitId())
                .setHeadCommitId(branch.getHeadCommitId())
                .build();
        return out.toByteArray();
    }

    @Override
    public byte[] branchAndCommitToBytes(BranchAndCommit branchAndCommit) throws IOException {
        return ServerProto.BranchAndCommit.newBuilder()
                .setBranchName(branchAndCommit.getBranchName())
                .setCommitId(branchAndCommit.getCommitId())
                .build()
                .toByteArray();
    }

    @Override
    public byte[] userToBytes(HdUser user) throws IOException {
        return UsersProto.HdUser.newBuilder()
                .setUserId(user.getUserId())
                .setEmail(user.getEmail())
                .setOrg(user.getOrg())
                .setPassword(user.getPassword())
                .build()
                .toByteArray();
    }

    @Override
    public BranchAndCommit branchAndCommitFromBytes(byte[] bytes) throws IOException {
        ServerProto.BranchAndCommit proto = ServerProto.BranchAndCommit.parseFrom(bytes);
        return BranchAndCommit.of(proto.getBranchName(), proto.getCommitId());
    }

    @Override
    public HdUser userFromBytes(byte[] bytes) throws IOException {
        UsersProto.HdUser proto = UsersProto.HdUser.parseFrom(bytes);
        return new HdUser(proto.getUserId(), proto.getEmail(), proto.getOrg(), proto.getPassword());
    }
}
