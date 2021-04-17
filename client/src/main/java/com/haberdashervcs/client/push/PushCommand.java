package com.haberdashervcs.client.push;

import java.util.LinkedList;
import java.util.List;

import com.haberdashervcs.client.commands.Command;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDb;
import com.haberdashervcs.common.change.Changeset;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FolderListing;


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
        // TODO! For now just log/toString the changeset here so I can inspect that it's working
    }

    private Changeset buildChangeset(String baseRemoteCommit) {
        // Given commit and commit + 1...
        // Crawl the checkout adding each new tree and file as you go...
        // For any file in a changed tree, determine if it's an add/delete/modify(/rename?).

        final CommitEntry baseCommit = db.getCommit(baseRemoteCommit);
        final CommitEntry localHeadCommit = db.getCommit(db.getCurrentCommit());
        LinkedList<CrawlDiffEntry> changedTrees = new LinkedList<>();

        FolderListing rootOld = db.getFolder(baseCommit.getRootFolderId());
        FolderListing rootNew = db.getFolder(localHeadCommit.getRootFolderId());
        changedTrees.add(new CrawlDiffEntry(rootOld, rootNew));

        while (!changedTrees.isEmpty()) {
            CrawlDiffEntry thisDiff = changedTrees.pop();

            if (thisDiff.getOld() != null && thisDiff.getNew() == null) {
                // delete...
            } else if (thisDiff.getOld() == null && thisDiff.getNew() != null) {
                // add...
            } else {
                for (FolderListing.FolderEntry entry : thisDiff.getNew().getEntries()) {
                    // TODO files -> check getEntryByName() in old...
                    // folders -> check getEntry and maybe add a new CrawlDiffEntry?
                }
            }
        }

        return null;
    }
}
