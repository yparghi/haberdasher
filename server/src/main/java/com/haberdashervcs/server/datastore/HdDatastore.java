package com.haberdashervcs.server.datastore;

import com.haberdashervcs.server.operations.CheckoutResult;
import com.haberdashervcs.server.operations.change.ApplyChangesetResult;
import com.haberdashervcs.server.operations.change.Changeset;


public interface HdDatastore {

    ApplyChangesetResult applyChangeset(Changeset changeset);

    // TODO: Will I need to use some kind of general ref instead of a branch?
    CheckoutResult checkout(String branchName, String folderPath);
}
