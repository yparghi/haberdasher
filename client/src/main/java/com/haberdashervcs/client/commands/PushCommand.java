package com.haberdashervcs.client.commands;

import java.util.LinkedList;
import java.util.List;

import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDb;
import com.haberdashervcs.common.change.Changeset;
import com.haberdashervcs.common.objects.CommitEntry;


public class PushCommand implements Command {

    private final List<String> otherArgs;
    private final LocalDb db;

    PushCommand(List<String> otherArgs) {
        this.otherArgs = otherArgs;
        this.db = SqliteLocalDb.getInstance();
    }

    @Override
    // TODO: A flag to push all commits as one, i.e. automatically squash?
    public void perform() throws Exception {
        final String baseRemoteCommit = db.getBaseRemoteCommit();

        // The idea:
        // - Starting from the baseRemoteCommit + 1, push commits one at a time.
        // - For each one, serialize a Changeset and send it over the wire.
        // - And on the wire? Like an object stream, send the meta info for the changeset, then each object in it, and
        //   the server will reconstitute the changeset.
        
        Changeset changeset = buildChangeset(baseRemoteCommit);
    }

    private Changeset buildChangeset(String baseRemoteCommit) {
        // Given commit and commit + 1...
        // Crawl the checkout adding each new tree and file as you go...
        // For any file in a changed tree, determine if it's an add/delete/modify(/rename?).

        final CommitEntry baseCommit = db.getCommit(baseRemoteCommit);
        final CommitEntry localHeadCommit = db.getCommit(db.getCurrentCommit());
        LinkedList<String> changedTrees = new LinkedList<>();


        return null;
    }
}
