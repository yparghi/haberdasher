package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;
import java.util.Optional;

import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.user.AuthResult;
import com.haberdashervcs.common.objects.user.AuthToken;
import com.haberdashervcs.common.objects.user.HdAuthenticator;
import com.haberdashervcs.common.objects.user.HdUser;
import com.haberdashervcs.common.objects.user.HdUserStore;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;


public final class HBaseAuthenticator implements HdAuthenticator {

    private static final HdLogger LOG = HdLoggers.create(HBaseAuthenticator.class);


    public static HdAuthenticator forConnection(Connection conn, HdUserStore userStore) {
        return new HBaseAuthenticator(conn, userStore);
    }


    private final Connection conn;
    private final HdUserStore userStore;

    private HBaseAuthenticator(Connection conn, HdUserStore userStore) {
        this.conn = conn;
        this.userStore = userStore;
    }


    @Override
    public void start() throws Exception {
        Admin admin = conn.getAdmin();
        TableName name = TableName.valueOf("Tokens");
        if (!admin.tableExists(name)) {
            TableDescriptor desc = TableDescriptorBuilder
                    .newBuilder(name)
                    .setColumnFamily(ColumnFamilyDescriptorBuilder.of("token"))
                    .build();
            admin.createTable(desc);
            LOG.info("Created the Tokens table.");
        }
    }


    @Override
    public Optional<AuthToken> loginToWeb(String email, String password) throws IOException {
        HdUser user = userStore.getByEmail(email);
        // TODO bcrypt
        if (!user.getPassword().equals(password)) {
            return Optional.empty();
        }

        return null;
    }

    @Override
    public AuthToken webTokenForId(String tokenId) {
        return null;
    }

    @Override
    // TODO: Create a CLI token
    public AuthToken cliTokenForId(String tokenId) {
        return null;
    }

    @Override
    public AuthResult canAccessRepo(AuthToken authToken, String org, String repo) {
        return null;
    }
}
