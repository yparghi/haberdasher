package com.haberdashervcs.client.db.sqlite;


final class Schemas {

    // TODO store the version in Meta? And add a migration mechanism?
    static final int VERSION = 1;

    static final String META_SCHEMA =
        "CREATE TABLE Meta (" +
        "metaKey VARCHAR(255) PRIMARY KEY, " +
        "metaValue VARCHAR(255) NOT NULL " +
        ");";
}
