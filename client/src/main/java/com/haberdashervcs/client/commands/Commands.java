package com.haberdashervcs.client.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.haberdashervcs.client.branch.BranchCommand;
import com.haberdashervcs.client.checkout.CheckoutCommand;
import com.haberdashervcs.client.checkout.SyncCommand;
import com.haberdashervcs.client.commit.CommitCommand;
import com.haberdashervcs.client.commit.DiffCommand;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.objects.LocalRepoState;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDb;
import com.haberdashervcs.client.git.ImportGitCommand;
import com.haberdashervcs.client.push.PushCommand;
import com.haberdashervcs.client.rebase.RebaseCommand;


public final class Commands {

    private Commands() {}


    public static Command parseFromArgs(String[] args) throws IOException {

        final String commandWord = args[0];
        ArrayList<String> otherArgs = new ArrayList<>(Arrays.asList(args));
        otherArgs.remove(0);

        Optional<RepoConfig> config = RepoConfig.find();

        if (commandWord.equals("init")) {
            if (config.isPresent()) {
                throw new IllegalStateException("Can't init: This is already an hd repo.");
            } else {
                return new InitCommand(otherArgs);
            }
        } else if (!config.isPresent()) {
            throw new IllegalStateException("This is not an hd repo.");
        }


        LocalDb db = SqliteLocalDb.getInstance();

        List<String> checkedOutPaths = db.getCheckedOutPaths();
        if (checkedOutPaths.size() == 0 && !commandWord.equals("checkout")) {
            throw new IllegalStateException("No paths are checked out. Use the 'checkout' command.");
        }

        LocalRepoState repoState = db.getRepoState();
        if (repoState.getState() == LocalRepoState.State.REBASE_IN_PROGRESS) {
            // TODO: 'rebase abort' does what?
            if (!commandWord.equals("rebase") && !commandWord.equals("status")) {
                throw new IllegalArgumentException("A rebase is currently in progress. Please run 'rebase commit' when you're done.");
            }
        }


        if (commandWord.equals("push")) {
            return new PushCommand(config.get(), otherArgs);

        } else if (commandWord.equals("commit")) {
            // TODO: This should really be broken up with an add/scan command first, for staging changes.
            return new CommitCommand(config.get(), otherArgs);

        } else if (commandWord.equals("import_git")) {
            return new ImportGitCommand(otherArgs);

        } else if (commandWord.equals("checkout")) {
            // TODO: This should really be broken up with an add/scan command first, for staging changes.
            return new CheckoutCommand(config.get(), otherArgs);

        } else if (commandWord.equals("sync")) {
            // TODO: What should this really be?
            return new SyncCommand(otherArgs);

        } else if (commandWord.equals("rebase")) {
            return new RebaseCommand(config.get(), otherArgs);

        } else if (commandWord.equals("diff")) {
            return new DiffCommand(config.get(), otherArgs);

        } else if (commandWord.equals("branch")) {
            return new BranchCommand(config.get(), db, otherArgs);

        } else if (commandWord.equals("status")) {
            return new StatusCommand(config.get(), db, otherArgs);
        }

        throw new IllegalArgumentException("Unknown command: " + commandWord);
    }
}
