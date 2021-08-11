package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import com.haberdashervcs.common.io.HdObjectByteConverter;
import com.haberdashervcs.common.io.ProtobufObjectByteConverter;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.user.AuthResult;
import com.haberdashervcs.common.objects.user.HdAuthenticator;
import com.haberdashervcs.common.objects.user.HdUser;
import com.haberdashervcs.common.objects.user.HdUserStore;
import com.haberdashervcs.common.objects.user.UserAuthToken;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.util.Bytes;


public final class HBaseAuthenticator implements HdAuthenticator {

    private static final HdLogger LOG = HdLoggers.create(HBaseAuthenticator.class);


    public static HdAuthenticator forConnection(Connection conn, HdUserStore userStore) {
        return new HBaseAuthenticator(conn, userStore);
    }


    private final Connection conn;
    private final HdUserStore userStore;
    private final HdObjectByteConverter byteConv;

    private HBaseAuthenticator(Connection conn, HdUserStore userStore) {
        this.conn = conn;
        this.userStore = userStore;
        this.byteConv = ProtobufObjectByteConverter.getInstance();
    }


    @Override
    public void start() throws Exception {
        Admin admin = conn.getAdmin();
        TableName name = TableName.valueOf("Tokens");
        if (!admin.tableExists(name)) {
            // Maps token id to token proto.
            TableDescriptor desc = TableDescriptorBuilder
                    .newBuilder(name)
                    .setColumnFamily(ColumnFamilyDescriptorBuilder.of("cfMain"))
                    .build();
            admin.createTable(desc);
            LOG.info("Created the Tokens table.");
        }
    }


    @Override
    public Optional<UserAuthToken> loginToWeb(String email, String password) throws IOException {
        HdUser user = userStore.getByEmail(email);
        // TODO bcrypt
        if (!user.getPassword().equals(password)) {
            return Optional.empty();
        }

        String tokenId = UUID.randomUUID().toString();
        UserAuthToken token = UserAuthToken.forWeb(tokenId, user);

        Table tokensTable = conn.getTable(TableName.valueOf("Tokens"));
        byte[] rowKey = tokenId.getBytes(StandardCharsets.UTF_8);
        Put tokenPut = new Put(rowKey);
        tokenPut.addColumn(
                Bytes.toBytes("cfMain"),
                Bytes.toBytes("token"),
                byteConv.userAuthTokenToBytes(token));
        tokensTable.put(tokenPut);

        return Optional.of(token);
    }

    @Override
    public UserAuthToken webTokenForId(String tokenId) {
        return null;
    }

    @Override
    // TODO: Create a CLI token through the web UI.
    public UserAuthToken cliTokenForId(String tokenId) {
        return null;
    }

    @Override
    // TODO: Real auth
    public AuthResult canAccessRepo(UserAuthToken authToken, String org, String repo) {
        return null;
    }
}
