package com.haberdashervcs.client.db.objects;

import com.google.common.base.MoreObjects;

public final class LocalBranchState {

    public static LocalBranchState of(long baseCommitId, long headCommitId, long lastPushedCommitId) {
        return new LocalBranchState(baseCommitId, headCommitId, lastPushedCommitId);
    }


    private final long baseCommitId;
    private final long headCommitId;
    private final long lastPushedCommitId;

    private LocalBranchState(long baseCommitId, long headCommitId, long lastPushedCommitId) {
        this.baseCommitId = baseCommitId;
        this.headCommitId = headCommitId;
        this.lastPushedCommitId = lastPushedCommitId;
    }

    public long getBaseCommitId() {
        return baseCommitId;
    }

    public long getHeadCommitId() {
        return headCommitId;
    }

    public long getLastPushedCommitId() {
        return lastPushedCommitId;
    }

    public String getDebugString() {
        return MoreObjects.toStringHelper(this)
                .add("baseCommitId", baseCommitId)
                .add("headCommitId", headCommitId)
                .add("lastPushedCommitId", lastPushedCommitId)
                .toString();
    }
}
