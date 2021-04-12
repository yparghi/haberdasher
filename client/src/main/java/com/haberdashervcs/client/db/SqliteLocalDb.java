package com.haberdashervcs.client.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.haberdashervcs.client.checkout.CheckoutInputStream;


public final class SqliteLocalDb implements LocalDb {

    private static final SqliteLocalDb INSTANCE = new SqliteLocalDb();
    private static final String DB_FILENAME = "hdlocal.db";

    public static SqliteLocalDb getInstance() {
        return INSTANCE;
    }

    private Supplier<Connection> conn = Suppliers.memoize(new Supplier<Connection>() {
        @Override public Connection get() {
            try {
                return DriverManager.getConnection("jdbc:sqlite:sample.db");
            } catch (SQLException sqlEx) {
                throw new RuntimeException(sqlEx);
            }
        }
    });

    private SqliteLocalDb() {}

    @Override public void create() {
        final File dbFile = new File(DB_FILENAME);
        if (dbFile.exists()) {
            throw new IllegalStateException(
                    String.format("The file `%s` already exists!", DB_FILENAME));
        }

        conn.get();
    }


    @Override
    public void addCheckout(CheckoutInputStream checkout) {

    }
}
