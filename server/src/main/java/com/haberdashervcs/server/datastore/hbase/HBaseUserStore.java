package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.haberdashervcs.common.io.HdObjectByteConverter;
import com.haberdashervcs.common.io.ProtobufObjectByteConverter;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.user.HdUser;
import com.haberdashervcs.common.objects.user.HdUserStore;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.util.Bytes;


public final class HBaseUserStore implements HdUserStore {

    private static HdLogger LOG = HdLoggers.create(HBaseUserStore.class);


    public static HdUserStore forConnection(Connection conn) {
        return new HBaseUserStore(conn);
    }


    private final Connection conn;
    private final HBaseRawHelper helper;
    private final HdObjectByteConverter byteConv;

    public HBaseUserStore(Connection conn) {
        this.conn = conn;
        this.helper = HBaseRawHelper.forConnection(conn);
        this.byteConv = ProtobufObjectByteConverter.getInstance();
    }

    @Override
    public void start() throws Exception {
        Admin admin = conn.getAdmin();
        TableName name = TableName.valueOf("Users");
        if (!admin.tableExists(name)) {
            TableDescriptor desc = TableDescriptorBuilder
                    .newBuilder(name)
                    .setColumnFamily(ColumnFamilyDescriptorBuilder.of("cfIdToUser"))
                    .setColumnFamily(ColumnFamilyDescriptorBuilder.of("cfEmailToId"))
                    .build();
            admin.createTable(desc);
            LOG.info("Created the Users table.");
        }
    }


    @Override
    public HdUser getByEmail(String email) throws IOException {
        final Table filesTable = conn.getTable(TableName.valueOf("Files"));
        final String columnFamilyName = "cfEmailToId";
        byte[] rowKey = email.getBytes(StandardCharsets.UTF_8);
        Get get = new Get(rowKey);
        Result result = filesTable.get(get);
        byte[] value = result.getValue(
                Bytes.toBytes(columnFamilyName), Bytes.toBytes("id"));

        String userId = new String(value, StandardCharsets.UTF_8);
        return getById(userId);
    }


    @Override
    public HdUser getById(String userId) throws IOException {
        final Table filesTable = conn.getTable(TableName.valueOf("Files"));
        final String columnFamilyName = "cfIdToUser";
        byte[] rowKey = userId.getBytes(StandardCharsets.UTF_8);
        Get get = new Get(rowKey);
        Result result = filesTable.get(get);
        byte[] value = result.getValue(
                Bytes.toBytes(columnFamilyName), Bytes.toBytes("id"));

        return byteConv.userFromBytes(value);
    }


    @Override
    public void putUser(HdUser user) throws IOException {
        // TODO! Put in both cf's
    }
}
