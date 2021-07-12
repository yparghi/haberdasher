package com.haberdashervcs.client.commands;

import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;

import java.util.ArrayList;


public final class StatusCommand implements Command {

    private static final HdLogger LOG = HdLoggers.create(StatusCommand.class);


    private final LocalDb db;

    public StatusCommand(RepoConfig repoConfig, LocalDb db, ArrayList<String> otherArgs) {
        this.db = db;
    }

    @Override
    public void perform() throws Exception {
        BranchAndCommit currentBranch = db.getCurrentBranch();
        LOG.info(
                "On branch %s and commit %d",
                currentBranch.getBranchName(),
                currentBranch.getCommitId());
    }
}
