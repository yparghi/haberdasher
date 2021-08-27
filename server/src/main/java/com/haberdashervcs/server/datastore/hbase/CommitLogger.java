package com.haberdashervcs.server.datastore.hbase;

import java.util.List;

import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.HdFolderPath;


// TODO! tests, then the http endpoint and client call
final class CommitLogger {

    private static final HdLogger LOG = HdLoggers.create(CommitLogger.class);


    static CommitLogger withMergeStates(MergeStates mergeStates, HBaseRawHelper helper, HBaseRowKeyer rowKeyer) {
        return new CommitLogger(mergeStates, helper, rowKeyer);
    }


    private final MergeStates mergeStates;
    private final HBaseRawHelper helper;
    private final HBaseRowKeyer rowKeyer;

    private CommitLogger(MergeStates mergeStates, HBaseRawHelper helper, HBaseRowKeyer rowKeyer) {
        this.mergeStates = mergeStates;
        this.helper = helper;
        this.rowKeyer = rowKeyer;
    }

    List<CommitEntry> getLog(HdFolderPath path) {
        // TODO!
        // The idea:
        // - For now, we won't special-case the log for '/' though maybe we should later.
        // - TODO! Make notes on ways we might better crawl a range of commits and/or subtrees...
    }
}
