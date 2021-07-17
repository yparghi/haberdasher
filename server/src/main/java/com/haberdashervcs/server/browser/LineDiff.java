package com.haberdashervcs.server.browser;

import java.util.Optional;

import com.google.common.base.Preconditions;


public final class LineDiff {

    public static LineDiff forSame(String line) {
        Optional<String> oLine = Optional.of(line);
        return new LineDiff(Type.SAME, oLine, oLine);
    }

    public static LineDiff forDiff(String original, String modified) {
        return new LineDiff(Type.DIFF, Optional.of(original), Optional.of(modified));
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

    public String getOriginalLine() {
        Preconditions.checkState(type == Type.SAME || type == Type.DIFF || type == Type.DELETED);
        return originalLine.get();
    }

    public String getNewLine() {
        Preconditions.checkState(type == Type.SAME || type == Type.DIFF || type == Type.ADDED);
        return newLine.get();
    }
}
