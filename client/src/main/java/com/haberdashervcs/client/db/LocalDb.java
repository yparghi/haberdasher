package com.haberdashervcs.client.db;

import com.haberdashervcs.client.checkout.CheckoutInputStream;
import com.haberdashervcs.common.objects.CommitEntry;

// Notes on the DB:
// - Always synced to one commit, no matter how many folders are checked out.
public interface LocalDb {

    void create();

    void addCheckout(CheckoutInputStream checkout);

    String getCurrentCommit();

    void setNewCommit(String newCommitId);

    String getBaseRemoteCommit();

    CommitEntry getCommit(String commitId);
}
