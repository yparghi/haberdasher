package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.MergeLock;


final class MergeStates {

    private static final HdLogger LOG = HdLoggers.create(MergeStates.class);

    // TODO! Don't use System.currentTimeMillis, just have callers pass in starting & ending timestamps
    static MergeStates fromPastSeconds(
            long secondsAgo, HBaseRawHelper helper, HBaseRowKeyMaker rowKeyer)
            throws IOException {
        long now = System.currentTimeMillis();
        long ago = now - (TimeUnit.SECONDS.toMillis(secondsAgo));
        byte[] rowNow = rowKeyer.prefixForMergeLocksAtTimestamp(now);
        byte[] rowAgo = rowKeyer.prefixForMergeLocksAtTimestamp(ago);
        List<MergeLock> currentMerges = helper.getMerges(rowAgo, rowNow);
        return new MergeStates(currentMerges);
    }


    private final ImmutableList<MergeLock> currentMerges;

    private MergeStates(List<MergeLock> currentMerges) {
        this.currentMerges = ImmutableList.copyOf(currentMerges);
        LOG.info("Temp: got %d merges", this.currentMerges.size());
    }

    // TODO! Consider if the lock's timestamp has expired?
    Optional<MergeLock> forMergeLockId(String mergeLockId) {
        LOG.info("TEMP: comparing merge lock id's in list: %s", currentMerges);
        for (MergeLock merge : currentMerges) {
            LOG.info("TEMP: comparing merge lock id's: %s / %s", mergeLockId, merge.getId());
            if (merge.getId().equals(mergeLockId)) {
                return Optional.of(merge);
            }
        }
        return Optional.empty();
    }
}
