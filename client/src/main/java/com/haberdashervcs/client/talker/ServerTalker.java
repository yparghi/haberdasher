package com.haberdashervcs.client.talker;

import java.io.OutputStream;

import com.haberdashervcs.common.io.HdObjectInputStream;
import com.haberdashervcs.common.objects.BranchAndCommit;


public interface ServerTalker {

    BranchAndCommit headOnBranch(String branchName) throws Exception;

    interface CheckoutContext {
        HdObjectInputStream getInputStream();

        void finish() throws Exception;
    }

    CheckoutContext checkout(String branchName,
            String path,
            long commitId)
            throws Exception;


    interface PushContext {
        OutputStream getOutputStream();

        void finish() throws Exception;
    }

    PushContext push(
            String branchName,
            long baseCommitId,
            long newHeadCommitId)
            throws Exception;
}
