package com.haberdashervcs.server.datastore.hbase;

import com.haberdashervcs.common.objects.user.AuthResult;
import com.haberdashervcs.common.objects.user.AuthToken;
import com.haberdashervcs.common.objects.user.HdAuthenticator;
import org.apache.hadoop.hbase.client.Connection;


public final class HBaseAuthenticator implements HdAuthenticator {

    public static HdAuthenticator forConnection(Connection conn) {
        return new HBaseAuthenticator(conn);
    }


    private final Connection conn;

    private HBaseAuthenticator(Connection conn) {
        this.conn = conn;
    }

    @Override
    public AuthToken login(String email, String password) {
        return null;
    }

    @Override
    public AuthToken webTokenForId(String tokenId) {
        return null;
    }

    @Override
    public AuthToken cliTokenForId(String tokenId) {
        return null;
    }

    @Override
    public AuthResult canAccessRepo(AuthToken authToken, String org, String repo) {
        return null;
    }
}
