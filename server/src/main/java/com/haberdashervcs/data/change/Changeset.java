package com.haberdashervcs.data.change;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public final class Changeset {

    public static final class Builder {

        private final ArrayList<SingleChange> changesSoFar;

        private Builder() {
            changesSoFar = new ArrayList<>();
        }

        public Builder withChange(SingleChange change) {
            changesSoFar.add(change);
            return this;
        }

        public Changeset build() {
            return new Changeset(changesSoFar);
        }
    }


    private final ImmutableList<SingleChange> changes;

    private Changeset(List<SingleChange> changes) {
        checkNotNull(changes);
        checkArgument(changes.size() > 0);
        this.changes = ImmutableList.copyOf(changes);
    }

    public List<SingleChange> getChanges() {
        return changes;
    }
}
