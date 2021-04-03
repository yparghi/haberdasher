package com.haberdashervcs.server.operations.change;

public final class ParsedChangeTree {

    public static ParsedChangeTree fromChangeset(Changeset changeset) {
        return new ParsedChangeTree(changeset);
    }

    private final Changeset changeset;

    private ParsedChangeTree(Changeset changeset) {
         this.changeset = changeset;
    }
}
