package com.haberdashervcs.config;

import com.haberdashervcs.datastore.HdDatastore;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;


public final class HaberdasherServer {

    public static final class Builder {
        private HdDatastore datastore = null;

        private Builder() {}

        public Builder withDatastore(HdDatastore datastore) {
            checkState(datastore == null);
            this.datastore = checkNotNull(datastore);
            return this;
        }

        public HaberdasherServer build() {
            return new HaberdasherServer(datastore);
        }
    }

    public static Builder builder() {
        return new Builder();
    }


    private final HdDatastore datastore;

    private HaberdasherServer(HdDatastore datastore) {
        this.datastore = checkNotNull(datastore);
    }
}
