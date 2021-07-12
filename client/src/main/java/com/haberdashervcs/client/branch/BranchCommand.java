package com.haberdashervcs.client.branch;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.haberdashervcs.client.checkout.CheckoutChangeHandler;
import com.haberdashervcs.client.commands.Command;
import com.haberdashervcs.client.commands.RepoConfig;
import com.haberdashervcs.client.crawl.LocalChangeCrawler;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.objects.LocalBranchState;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;
import com.haberdashervcs.common.objects.HdFolderPath;


public class BranchCommand implements Command {

    private static final HdLogger LOG = HdLoggers.create(BranchCommand.class);


    private final RepoConfig config;
    private final LocalDb db;
    private final List<String> otherArgs;

    public BranchCommand(RepoConfig config, LocalDb db, List<String> otherArgs) {
        this.config = config;
        this.db = db;
        this.otherArgs = ImmutableList.copyOf(otherArgs);
    }


    @Override
    public void perform() throws Exception {
        // TEMP! Notes from 6/18/2021, re. testing rebase:
        // - I start on main, on the head from the server, let's say commit #2.
        // - I'll *CREATE* a branch, say 'some_branch'. It has a base commit of 2 (on main).
        // - On 'some_branch' I'll echo 'branch text' > new.txt. Then I commit this as #branch-1.
        //     (since branch commits are increments, starting at 0)
        // - I'll *SWITCH* to main, and echo 'main text' > new.txt. Then I commit this as #main-3.
        // - Then I switch back to some_branch, and run rebase (onto 3, either implicitly or as a
        //     given argument)
        // - I'd expect rebase to give me a conflict on new.txt.
        //
        // That gives these subcommands:
        // - branch create (which is just local)
        // - branch switch
        // ? branch push? (No.)
        //     - I like (but I'm not certain about) an explicit command to create the branch on the
        //         server, independent of pushing changes. But I'm not sure it's worth the hassle.
        //         So for now, let's just use 'push' instead of 'branch push', and the server can
        //         create a BranchEntry as needed.

        if (otherArgs.size() == 0) {
            BranchAndCommit currentBC = db.getCurrentBranch();
            LocalBranchState state = db.getBranchState(currentBC.getBranchName());
            LOG.info(
                    "On branch '%s' with base commit %d and head commit %d.",
                    currentBC.getBranchName(),
                    state.getBaseCommitId(),
                    currentBC.getCommitId());
            return;
        }


        final String subcommand = otherArgs.get(0);
        final String branchName = otherArgs.get(1);

        if (subcommand.equals("create")) {
            handleCreate(branchName);

        } else if (subcommand.equals("switch")) {
            handleSwitch(branchName);

        } else {
            throw new IllegalArgumentException("Unknown subcommand: " + subcommand);
        }
    }


    private void handleCreate(String branchName) {
        db.createNewBranch(branchName);
        LocalBranchState newBranchState = db.getBranchState(branchName);
        LOG.info(
                "New branch '%s' created at base commit %d.",
                branchName,
                newBranchState.getBaseCommitId());
    }


    // TODO: Check for local changes so they don't get clobbered?
    private void handleSwitch(String branchName) throws IOException {
        BranchAndCommit bc = db.switchToBranch(branchName);

        LocalChangeCrawler crawler = new LocalChangeCrawler(
                config,
                db,
                bc,
                HdFolderPath.fromFolderListingFormat("/"),
                new CheckoutChangeHandler(config, db));
        crawler.crawl();

        LOG.info(
                "Switched to branch '%s' at head commit %d.",
                branchName,
                bc.getCommitId());
    }
}
