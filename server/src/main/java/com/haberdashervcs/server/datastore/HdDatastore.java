package com.haberdashervcs.server.datastore;

import com.haberdashervcs.server.operations.checkout.CheckoutResult;
import com.haberdashervcs.server.operations.change.ApplyChangesetResult;
import com.haberdashervcs.server.operations.change.Changeset;


public interface HdDatastore {

    ApplyChangesetResult applyChangeset(Changeset changeset);

    CheckoutResult checkout(String commitId, String folderToCheckout);
}
