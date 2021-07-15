package com.haberdashervcs.server.browser;


public final class LineDiff {

    enum Type {
        SAME,
        DIFF,
        ADDED,
        DELETED
    }


    private final Type type;
    private final int lineNumber;
    private final String lineContents;

    LineDiff(Type type, int lineNumber, String lineContents) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.lineContents = lineContents;
    }

    public Type getType() {
        return type;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getLineContents() {
        return lineContents;
    }
}
