package com.haberdashervcs.server.browser;

import java.util.Objects;
import java.util.Optional;

import com.google.common.base.MoreObjects;


public final class LineDiff {

    public static LineDiff forSame(String line) {
        Optional<String> oLine = Optional.of(line);
        return new LineDiff(Type.SAME, oLine, oLine);
    }

    // A diff algorithm may work in hunks, where e.g. a diff hunk may replace 2 lines in the
    // original with 4 lines in the new string. So a LineDiff in a diff hunk doesn't necessarily
    // have a line in both original and modified.
    public static LineDiff forDiff(Optional<String> original, Optional<String> modified) {
        return new LineDiff(Type.DIFF, original, modified);
    }

    public static LineDiff forAdded(String added) {
        return new LineDiff(Type.ADDED, Optional.empty(), Optional.of(added));
    }

    public static LineDiff forDeleted(String original) {
        return new LineDiff(Type.DELETED, Optional.of(original), Optional.empty());
    }


    enum Type {
        SAME,
        DIFF,
        ADDED,
        DELETED
    }


    private final Type type;
    private final Optional<String> originalLine;
    private final Optional<String> newLine;

    private LineDiff(Type type, Optional<String> originalLine, Optional<String> newLine) {
        this.type = type;
        this.originalLine = originalLine;
        this.newLine = newLine;
    }

    public Type getType() {
        return type;
    }

    public Optional<String> getOriginalLine() {
        return originalLine;
    }

    public Optional<String> getNewLine() {
        return newLine;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LineDiff lineDiff = (LineDiff) o;
        return type == lineDiff.type && originalLine.equals(lineDiff.originalLine) && newLine.equals(lineDiff.newLine);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, originalLine, newLine);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", type)
                .add("originalLine", originalLine)
                .add("newline", newLine)
                .toString();
    }

}
