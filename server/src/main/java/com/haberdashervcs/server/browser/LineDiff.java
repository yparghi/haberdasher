package com.haberdashervcs.server.browser;


public final class LineDiff {

    enum Type {
        SAME,
        DIFF,
        ADDED,
        DELETED
    }


    private final Type type;
    private final String lineContents;

    LineDiff(Type type, String lineContents) {
        this.type = type;
        this.lineContents = lineContents;
    }

    public Type getType() {
        return type;
    }

    public String getLineContents() {
        return lineContents;
    }
}
