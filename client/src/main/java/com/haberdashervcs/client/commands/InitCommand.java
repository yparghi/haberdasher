package com.haberdashervcs.client.commands;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDb;


class InitCommand implements Command {

    // TODO move these constants somewhere common?
    private static final String INIT_FILENAME = "hdlocal";

    private static final String INIT_FILE_TEMPLATE = Joiner.on('\n').join(
            "---",
            "url: %s",
            "");


    private final List<String> otherArgs;
    private final LocalDb db;

    InitCommand(List<String> otherArgs) {
        Preconditions.checkArgument(otherArgs.size() >= 1);
        this.otherArgs = ImmutableList.copyOf(otherArgs);
        this.db = SqliteLocalDb.getInstance();
    }

    @Override
    public void perform() {
        String serverAddress = otherArgs.get(0);
        final File initFile = new File(INIT_FILENAME);
        if (initFile.exists()) {
            throw new IllegalStateException(
                    String.format("The file `%s` already exists!", INIT_FILENAME));
        }

        try {
            String initFileContents = initFileContents(serverAddress);
            Files.write(initFileContents.getBytes(StandardCharsets.UTF_8), initFile);
            initDb();
        } catch (IOException ioEx) {
            throw new RuntimeException(ioEx);
        }
    }

    private String initFileContents(String serverAddress) {
        return String.format(INIT_FILE_TEMPLATE, serverAddress);
    }

    private void initDb() {
        db.create();
    }
}
