package com.haberdashervcs.client.db.objects;

import java.io.IOException;


public interface LocalDbObjectByteConverter {

    LocalBranchState branchStateFromBytes(byte[] bytes) throws IOException;
    LocalFileState fileStateFromBytes(byte[] bytes) throws IOException;
    LocalRepoState repoStateFromBytes(byte[] bytes) throws IOException;

    byte[] branchStateToBytes(LocalBranchState branchState);
    byte[] fileStateToBytes(LocalFileState fileState);
    byte[] repoStateToBytes(LocalRepoState repoState);

}
