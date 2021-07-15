package com.haberdashervcs.server.browser;

import java.util.List;

import com.google.common.collect.ImmutableList;


public final class FileDiff {

    private final String path;
    private final List<LineDiff> diffs;

    FileDiff(String path, List<LineDiff> diffs) {
        this.path = path;
        this.diffs = ImmutableList.copyOf(diffs);
    }

    public String getPath() {
        return path;
    }

    public List<LineDiff> getDiffs() {
        return diffs;
    }
}
