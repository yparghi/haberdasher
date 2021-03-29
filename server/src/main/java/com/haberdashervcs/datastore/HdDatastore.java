package com.haberdashervcs.datastore;

import com.haberdashervcs.operations.change.ApplyChangesetResult;
import com.haberdashervcs.operations.change.Changeset;
import com.haberdashervcs.operations.CheckoutStream;


public interface HdDatastore {

    ApplyChangesetResult applyChangeset(Changeset changeset);

    // TODO: Will I need to use some kind of general ref instead of a branch?
    CheckoutStream checkout(String branchName, String path);
}
