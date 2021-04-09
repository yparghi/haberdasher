package com.haberdashervcs.server.config;

import com.haberdashervcs.server.datastore.HdDatastore;
import com.haberdashervcs.server.frontend.Frontend;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;


public final class HaberdasherServer {

    public static final class Builder {
        private HdDatastore datastore = null;
        private Frontend frontend = null;

        private Builder() {}

        public Builder withDatastore(HdDatastore datastore) {
            checkState(this.datastore == null);
            this.datastore = checkNotNull(datastore);
            return this;
        }

        public Builder withFrontend(Frontend frontend) {
            checkState(this.frontend == null);
            this.frontend = checkNotNull(frontend);
            return this;
        }

        public HaberdasherServer build() {
            return new HaberdasherServer(datastore, frontend);
        }
    }

    public static Builder builder() {
        return new Builder();
    }


    private final HdDatastore datastore;
    private final Frontend frontend;

    private HaberdasherServer(HdDatastore datastore, Frontend frontend) {
        this.datastore = checkNotNull(datastore);
        this.frontend = checkNotNull(frontend);
    }

    // TODO should this be synch or asynch?
    public void start() throws Exception {
        frontend.startInBackground();
    }
}
