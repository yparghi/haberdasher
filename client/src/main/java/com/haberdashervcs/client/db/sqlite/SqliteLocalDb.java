package com.haberdashervcs.client.db.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.haberdashervcs.client.checkout.CheckoutInputStream;
import com.haberdashervcs.client.db.LocalDb;


// TODO! sqlite upsert to add objects?: https://www.sqlite.org/draft/lang_UPSERT.html
public final class SqliteLocalDb implements LocalDb {

    private static final SqliteLocalDb INSTANCE = new SqliteLocalDb();
    private static final String DB_FILENAME = "hdlocal.db";

    public static SqliteLocalDb getInstance() {
        return INSTANCE;
    }

    private Supplier<Connection> conn = Suppliers.memoize(new Supplier<Connection>() {
        @Override public Connection get() {
            try {
                return DriverManager.getConnection("jdbc:sqlite:" + DB_FILENAME);
            } catch (SQLException sqlEx) {
                throw new RuntimeException(sqlEx);
            }
        }
    });

    private SqliteLocalDb() {}

    @Override
    public void create() {
        final File dbFile = new File(DB_FILENAME);
        if (dbFile.exists()) {
            throw new IllegalStateException(
                    String.format("The file `%s` already exists!", DB_FILENAME));
        }

        createTable(Schemas.META_SCHEMA);
    }

    private void createTable(String sql) {
        try {
            Statement stmt = conn.get().createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException sqlEx) {
            throw new RuntimeException(sqlEx);
        }
    }

    @Override
    // TODO maybe the checkout operation/stream needs to be built here, to use the db's current commit
    public void addCheckout(CheckoutInputStream checkout) {

    }

    @Override
    public String getCurrentCommit() {
        return getMetaValue("currentCommit");
    }

    private String getMetaValue(String key) {
        try {
            PreparedStatement getStmt = conn.get().prepareStatement(
                    "SELECT value FROM Meta WHERE key = ?");
            getStmt.setString(1, key);
            ResultSet rs = getStmt.executeQuery();
            rs.next();
            return rs.getString("value");
        } catch (SQLException sqlEx) {
            throw new RuntimeException(sqlEx);
        }
}

    @Override
    public void setNewCommit(String newCommitId) {
        throw new UnsupportedOperationException();
    }
}
