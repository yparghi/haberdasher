package com.haberdashervcs.client.db;

import com.haberdashervcs.client.checkout.CheckoutInputStream;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;


// Notes on the DB:
// - Always synced to one commit, no matter how many folders are checked out.
public interface LocalDb {

    void create();

    void addCheckout(CheckoutInputStream checkout);

    String getCurrentCommit();

    void setCurrentCommit(String newCommitId);

    String getBaseRemoteCommit();

    void setBaseRemoteCommit(String newRemoteCommitId);

    CommitEntry getCommit(String commitId);

    FolderListing getFolder(String folderId);

    FileEntry getFile(String fileId);

    void putCommit(String commitId, CommitEntry commit);

    void putFolder(String folderId, FolderListing folder);

    void putFile(String fileId, FileEntry file);
}
