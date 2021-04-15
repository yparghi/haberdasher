package com.haberdashervcs.server.datastore;

import com.haberdashervcs.common.io.HdObjectOutputStream;
import com.haberdashervcs.server.operations.change.ApplyChangesetResult;
import com.haberdashervcs.server.operations.change.Changeset;
import com.haberdashervcs.server.operations.checkout.CheckoutResult;


public interface HdDatastore {

    ApplyChangesetResult applyChangeset(Changeset changeset);

    CheckoutResult checkout(
            String org, String repo, String commitId, String folderToCheckout, HdObjectOutputStream out);
}
