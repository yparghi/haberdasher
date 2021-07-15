package com.haberdashervcs.server.browser;

import java.util.List;

import com.google.common.collect.ImmutableList;


public final class BranchDiff {

    private final List<FileDiff> diffs;

    BranchDiff(List<FileDiff> diffs) {
        this.diffs = ImmutableList.copyOf(diffs);
    }

    public List<FileDiff> getFileDiffs() {
        return diffs;
    }
}
