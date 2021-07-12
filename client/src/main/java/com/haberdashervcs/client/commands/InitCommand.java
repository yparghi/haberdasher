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
import com.haberdashervcs.client.talker.JettyServerTalker;
import com.haberdashervcs.client.talker.ServerTalker;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;


class InitCommand implements Command {

    private static final HdLogger LOG = HdLoggers.create(InitCommand.class);

    // TODO move these constants somewhere common?
    private static final String INIT_FILENAME = "hdlocal";

    private static final String INIT_FILE_TEMPLATE = Joiner.on('\n').join(
            "---",
            "host: %s",
            "org: %s",
            "repo: %s",
            "");


    private final List<String> otherArgs;
    private final LocalDb db;

    InitCommand(List<String> otherArgs) {
        Preconditions.checkArgument(otherArgs.size() >= 1);
        this.otherArgs = ImmutableList.copyOf(otherArgs);
        this.db = SqliteLocalDb.getInstance();
    }

    @Override
    public void perform() throws Exception {
        final String host = otherArgs.get(0);
        final String org = otherArgs.get(1);
        final String repo = otherArgs.get(2);

        // TODO configure
        ServerTalker server = JettyServerTalker.forHost(host);

        final File initFile = new File(INIT_FILENAME);
        if (initFile.exists()) {
            throw new IllegalStateException(
                    String.format("The file `%s` already exists!", INIT_FILENAME));
        }

        try {
            String initFileContents = initFileContents(host, org, repo);
            Files.write(initFileContents.getBytes(StandardCharsets.UTF_8), initFile);
            initDb(server);
        } catch (IOException ioEx) {
            throw new RuntimeException(ioEx);
        }
    }

    private String initFileContents(String host, String org, String repo) {
        return String.format(
                INIT_FILE_TEMPLATE,
                host, org, repo);
    }

    private void initDb(ServerTalker server) throws Exception {
        BranchAndCommit mainFromServer = server.headOnBranch("main");
        db.init(mainFromServer);

        LOG.info(
                "You are synced to commit %d on branch main.\n\n"
                + "Use the 'checkout' command to add a path to your local copy.",
                mainFromServer.getCommitId());
    }
}
