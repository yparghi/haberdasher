package com.haberdashervcs.client.checkout;

import java.util.List;

import com.haberdashervcs.client.commands.Command;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDb;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;


public class SyncCommand implements Command {

    private static final HdLogger LOG = HdLoggers.create(SyncCommand.class);


    private final BranchAndCommit branchAndCommit;
    private final LocalDb db;

    public SyncCommand(List<String> otherArgs) {
        this(BranchAndCommit.of(otherArgs.get(0), Long.parseLong(otherArgs.get(1))));
    }

    // TODO! Do I need this still?
    SyncCommand(BranchAndCommit branchAndCommit) {
        this.branchAndCommit = branchAndCommit;
        this.db = SqliteLocalDb.getInstance();
    }


    @Override
    // TODO! What if the new commit is greater than the downloaded commit? Download it from the server?
    // Then, considerations for the download:
    // - Conflicts? Do I need to track local changes separately from remote ones, even on the same branch?
    public void perform() throws Exception {
        /* TODO use this...
        final String commitIdToCheckout = otherArgs.get(0);
        final String currentCommit = db.getCurrentCommit();
        CommitEntry commitToCheckout = db.getCommit(commitIdToCheckout);
        final Path localRoot = Paths.get("");

        CheckoutLocalCrawler crawler = new CheckoutLocalCrawler(db, commitToCheckout, localRoot);
        crawler.compare();
         */
    }
}
