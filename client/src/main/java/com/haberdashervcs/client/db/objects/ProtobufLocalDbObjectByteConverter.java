package com.haberdashervcs.client.db.objects;

import java.io.IOException;

import com.haberdashervcs.common.protobuf.LocalDbProto;


public final class ProtobufLocalDbObjectByteConverter implements LocalDbObjectByteConverter {

    public static ProtobufLocalDbObjectByteConverter getInstance() {
        return new ProtobufLocalDbObjectByteConverter();
    }


    private ProtobufLocalDbObjectByteConverter() {}

    @Override
    public LocalBranchState branchStateFromBytes(byte[] bytes) throws IOException {
        LocalDbProto.LocalBranchState proto = LocalDbProto.LocalBranchState.parseFrom(bytes);
        return LocalBranchState.of(
                proto.getBaseCommitId(), proto.getHeadCommitId(), proto.getLastPushedCommit());
    }

    @Override
    public LocalFileState fileStateFromBytes(byte[] bytes) throws IOException {
        LocalDbProto.LocalFileState proto = LocalDbProto.LocalFileState.parseFrom(bytes);
        return LocalFileState.of(proto.getPushedToServer());
    }

    @Override
    public LocalRepoState repoStateFromBytes(byte[] bytes) throws IOException {
        LocalDbProto.LocalRepoState proto = LocalDbProto.LocalRepoState.parseFrom(bytes);
        LocalRepoState.State state;
        switch (proto.getState()) {
            case REBASE_IN_PROGRESS:
                state = LocalRepoState.State.REBASE_IN_PROGRESS;
                break;
            case NORMAL:
            default:
                state = LocalRepoState.State.NORMAL;
                break;
        }
        return LocalRepoState.forState(state);
    }

    @Override
    public byte[] branchStateToBytes(LocalBranchState branchState) {
        LocalDbProto.LocalBranchState proto = LocalDbProto.LocalBranchState.newBuilder()
                .setBaseCommitId(branchState.getBaseCommitId())
                .setHeadCommitId(branchState.getHeadCommitId())
                .setLastPushedCommit(branchState.getLastPushedCommitId())
                .build();
        return proto.toByteArray();
    }

    @Override
    public byte[] fileStateToBytes(LocalFileState fileState) {
        LocalDbProto.LocalFileState proto = LocalDbProto.LocalFileState.newBuilder()
                .setPushedToServer(fileState.isPushedToServer())
                .build();
        return proto.toByteArray();
    }

    @Override
    public byte[] repoStateToBytes(LocalRepoState repoState) {
        LocalDbProto.LocalRepoState.RepoState state;
        switch (repoState.getState()) {
            case REBASE_IN_PROGRESS:
                state = LocalDbProto.LocalRepoState.RepoState.REBASE_IN_PROGRESS;
                break;
            case NORMAL:
            default:
                state = LocalDbProto.LocalRepoState.RepoState.NORMAL;
                break;
        }
        LocalDbProto.LocalRepoState proto = LocalDbProto.LocalRepoState.newBuilder()
                .setState(state)
                .build();
        return proto.toByteArray();
    }
}
