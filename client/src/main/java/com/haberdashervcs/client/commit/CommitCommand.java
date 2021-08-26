package com.haberdashervcs.client.commit;

import java.util.List;

import com.haberdashervcs.client.commands.Command;
import com.haberdashervcs.client.commands.RepoConfig;
import com.haberdashervcs.client.crawl.LocalChangeCrawler;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.objects.LocalBranchState;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDb;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDbRowKeyer;
import com.haberdashervcs.common.exceptions.HdException;
import com.haberdashervcs.common.exceptions.HdRuntimeException;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.HdFolderPath;


public class CommitCommand implements Command {

    private static final HdLogger LOG = HdLoggers.create(CommitCommand.class);


    private final RepoConfig config;
    private final LocalDb db;
    private final List<String> otherArgs;

    public CommitCommand(RepoConfig config, List<String> otherArgs) {
        this.config = config;
        this.db = SqliteLocalDb.getInstance();
        this.otherArgs = otherArgs;
    }

    @Override
    public void perform() throws Exception {
        if (otherArgs.size() != 2) {
            throw new HdRuntimeException("Commits require an author and a message.");
        }

        BranchAndCommit currentBranch = db.getCurrentBranch();
        CommitChangeHandler changeHandler = new CommitChangeHandler(db, currentBranch);
        LocalChangeCrawler crawler = new LocalChangeCrawler(
                config,
                db,
                currentBranch,
                HdFolderPath.fromFolderListingFormat("/"),
                changeHandler);
        crawler.crawl();

        LocalBranchState oldState = db.getBranchState(currentBranch.getBranchName());
        LocalBranchState newState = LocalBranchState.of(
                oldState.getBaseCommitId(),
                currentBranch.getCommitId() + 1,
                oldState.getLastPushedCommitId());
        db.putBranchState(currentBranch.getBranchName(), newState);

        // TODO: Use SQLite transactions to do this safely?
        CommitEntry newCommit = CommitEntry.of(
                currentBranch.getBranchName(), newState.getHeadCommitId(), otherArgs.get(0), otherArgs.get(1));
        db.putCommit(SqliteLocalDbRowKeyer.getInstance().forCommit(newCommit), newCommit);

        LOG.info(
                "Changes committed to %s-%d",
                currentBranch.getBranchName(),
                newState.getHeadCommitId());
    }
}
