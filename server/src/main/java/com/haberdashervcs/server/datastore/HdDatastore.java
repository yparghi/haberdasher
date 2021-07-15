package com.haberdashervcs.server.datastore;

import java.util.Optional;

import com.haberdashervcs.common.io.HdObjectInputStream;
import com.haberdashervcs.common.io.HdObjectOutputStream;
import com.haberdashervcs.common.objects.BranchAndCommit;
import com.haberdashervcs.server.browser.RepoBrowser;
import com.haberdashervcs.server.operations.checkout.CheckoutResult;


public interface HdDatastore {

    void writeObjectsFromPush(
            String org,
            String repo,
            String branchName,
            long baseCommitId,
            long newHeadCommitId,
            HdObjectInputStream objectsIn);

    CheckoutResult checkout(
            String org,
            String repo,
            String branchName,
            long commitId,
            String folderToCheckout,
            HdObjectOutputStream out);

    Optional<BranchAndCommit> getHeadCommitForBranch(
            String org,
            String repo,
            String branchName);

    RepoBrowser getBrowser(
            String org,
            String repo);
}
