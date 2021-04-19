package com.haberdashervcs.client.commands;

import java.util.List;

import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDb;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;


public class CommitCommand implements Command {

    private static final HdLogger LOG = HdLoggers.create(CommitCommand.class);


    private final List<String> otherArgs;
    private final LocalDb db;

    public CommitCommand(List<String> otherArgs) {
        this.otherArgs = otherArgs;
        this.db = SqliteLocalDb.getInstance();
    }

    @Override
    public void perform() throws Exception {
        // TODO crawling code:
        // - Start from the root.
        // - Get the folder entries there and compare them. (How? By hash? Manual for now?)
        // - Like the PushCommand crawling (which I should abstract/commonize), choose a left/right branch and add
        //   objects to the DB as needed from there.
        // - Finally set the current commit.
    }
}
