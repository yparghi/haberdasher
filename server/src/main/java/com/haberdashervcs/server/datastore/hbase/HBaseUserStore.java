package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;

import com.haberdashervcs.server.user.HdUser;
import com.haberdashervcs.server.user.HdUserStore;
import org.apache.hadoop.hbase.client.Connection;


public final class HBaseUserStore implements HdUserStore {

    public static HdUserStore forConnection(Connection conn) {
        return new HBaseUserStore(conn);
    }


    private final Connection conn;

    public HBaseUserStore(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public HdUser getByEmail(String email) throws IOException {
        return null;
    }

    @Override
    public HdUser getById(String userId) throws IOException {
        return null;
    }
}
