package com.haberdashervcs.server.browser;

import java.util.List;

import com.google.common.collect.ImmutableList;


public final class BranchDiff {

    public static BranchDiff of(List<FileDiff> diffs) {
        return new BranchDiff(diffs);
    }


    private final List<FileDiff> diffs;

    private BranchDiff(List<FileDiff> diffs) {
        this.diffs = ImmutableList.copyOf(diffs);
    }

    public List<FileDiff> getFileDiffs() {
        return diffs;
    }
}
