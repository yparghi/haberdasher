package com.haberdashervcs.client.db.sqlite;


final class Schemas {
    static final String META_SCHEMA =
        "CREATE TABLE Meta (" +
        "key VARCHAR(255) NOT NULL, " +
        "value VARCHAR(255) NOT NULL, " +
        "PRIMARY KEY key);";
}
