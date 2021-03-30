package com.haberdashervcs.datastore;

import com.haberdashervcs.operations.CheckoutResult;
import com.haberdashervcs.operations.change.ApplyChangesetResult;
import com.haberdashervcs.operations.change.Changeset;


public interface HdDatastore {

    ApplyChangesetResult applyChangeset(Changeset changeset);

    // TODO: Will I need to use some kind of general ref instead of a branch?
    CheckoutResult checkout(String branchName, String folderPath);
}
