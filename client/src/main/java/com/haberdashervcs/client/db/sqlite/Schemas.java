package com.haberdashervcs.client.db.sqlite;


final class Schemas {

    private Schemas () {}


    // TODO store the version in Meta? And add a migration mechanism?
    static final int VERSION = 1;

    static final String META_SCHEMA =
        "CREATE TABLE Meta (" +
        "metaKey VARCHAR(255) PRIMARY KEY, " +
        "metaValue VARCHAR(255) NOT NULL " +
        ");";

    static final String COMMITS_SCHEMA =
            "CREATE TABLE Commits (" +
                    "id VARCHAR(255) PRIMARY KEY, " +
                    "contents BLOB NOT NULL " +
                    ");";

    static final String FOLDERS_SCHEMA =
            "CREATE TABLE Folders (" +
                    "id VARCHAR(255) PRIMARY KEY, " +
                    "contents BLOB NOT NULL " +
                    ");";

    static final String FILES_SCHEMA =
            "CREATE TABLE Files (" +
                    "id VARCHAR(255) PRIMARY KEY, " +
                    "contents BLOB NOT NULL " +
                    ");";
}
