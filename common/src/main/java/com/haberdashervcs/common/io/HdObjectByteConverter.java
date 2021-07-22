package com.haberdashervcs.common.io;

import java.io.IOException;

import com.haberdashervcs.common.objects.BranchAndCommit;
import com.haberdashervcs.common.objects.BranchEntry;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderHistory;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.objects.MergeLock;
import com.haberdashervcs.common.objects.user.HdUser;
import com.haberdashervcs.common.protobuf.ServerProto;


public interface HdObjectByteConverter {

    byte[] fileToBytes(FileEntry file) throws IOException;
    byte[] folderToBytes(FolderListing folder) throws IOException;
    byte[] commitToBytes(CommitEntry commit) throws IOException;
    byte[] folderHistoryToBytes(FolderHistory folderHistory) throws IOException;
    byte[] mergeLockToBytes(MergeLock mergeLock) throws IOException;
    byte[] branchToBytes(BranchEntry branch) throws IOException;
    byte[] branchAndCommitToBytes(BranchAndCommit branchAndCommit) throws IOException;
    byte[] userToBytes(HdUser user) throws IOException;

    FileEntry fileFromBytes(byte[] fileBytes) throws IOException;
    FolderListing folderFromBytes(byte[] folderBytes) throws IOException;
    CommitEntry commitFromBytes(byte[] commitBytes) throws IOException;
    FolderHistory folderHistoryFromBytes(byte[] folderHistoryBytes) throws IOException;
    MergeLock mergeLockFromBytes(byte[] mergeLockBytes) throws IOException;
    BranchEntry branchFromBytes(byte[] branchBytes) throws IOException;
    BranchAndCommit branchAndCommitFromBytes(byte[] bytes) throws IOException;
    HdUser userFromBytes(byte[] bytes) throws IOException;
}
