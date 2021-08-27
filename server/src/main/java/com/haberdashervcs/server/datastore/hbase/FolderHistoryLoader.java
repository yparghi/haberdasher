package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.FolderListing;


final class FolderHistoryLoader {

    private static final HdLogger LOG = HdLoggers.create(FolderHistoryLoader.class);


    static FolderHistoryLoader forBranch(HBaseRowKeyer rowKeyer, String branch, HBaseRawHelper helper, long nowTs) {
        return new FolderHistoryLoader(rowKeyer, branch, helper, nowTs);
    }


    private final HBaseRowKeyer rowKeyer;
    private final String branchName;
    private final HBaseRawHelper helper;
    private final long nowTs;

    private FolderHistoryLoader(HBaseRowKeyer rowKeyer, String branchName, HBaseRawHelper helper, long nowTs) {
        this.rowKeyer = rowKeyer;
        this.branchName = branchName;
        this.helper = helper;
        this.nowTs = nowTs;
    }

    Optional<FolderListing> getFolderAtCommit(long commitId, String path) {
        try {
            LOG.debug("getFolderAtCommit: %s", commitId);

            // TODO! move this up / pass it in, so that the merge state is a consistent, single snapshot across all of these
            //     calls?
            MergeStates mergeStates = MergeStates.fromPastSeconds(TimeUnit.HOURS.toSeconds(1), helper, rowKeyer);

            return helper.getHeadAtCommit(
                    rowKeyer.getOrg(), rowKeyer.getRepo(), branchName, commitId, path, mergeStates, nowTs);

        } catch (IOException ioEx) {
            throw new RuntimeException(ioEx);
        }
    }
}
