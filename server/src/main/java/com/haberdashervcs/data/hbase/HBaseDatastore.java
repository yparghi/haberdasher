package com.haberdashervcs.data.hbase;

import com.haberdashervcs.data.HdDatastore;
import com.haberdashervcs.data.change.ApplyChangesetResult;
import com.haberdashervcs.data.change.Changeset;
import org.apache.hadoop.hbase.client.Connection;


public final class HBaseDatastore implements HdDatastore {

    private final Connection conn;

    public HBaseDatastore(Connection conn) {
        this.conn = conn;
    }

    @Override
    public ApplyChangesetResult applyChangeset(Changeset changeset) {
        return null;
    }
}
