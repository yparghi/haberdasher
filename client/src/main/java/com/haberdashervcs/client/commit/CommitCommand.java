package com.haberdashervcs.client.commit;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.haberdashervcs.client.commands.Command;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDb;
import com.haberdashervcs.client.push.CrawlDiffEntry;
import com.haberdashervcs.common.change.AddChange;
import com.haberdashervcs.common.change.DeleteChange;
import com.haberdashervcs.common.change.ModifyChange;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FolderListing;


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

        final CommitEntry localHeadCommit = db.getCommit(db.getCurrentCommit());
        final FolderListing commitRoot = db.getFolder(localHeadCommit.getRootFolderId());

        LinkedList<LocalCrawlEntry> changedFolders = new LinkedList<>();
        Path currentDir = Paths.get("");
        changedFolders.add(new LocalCrawlEntry(commitRoot, currentDir));

        while (!changedFolders.isEmpty()) {

        }



        FolderListing rootNew = db.getFolder(localHeadCommit.getRootFolderId());
        changedTrees.add(new CrawlDiffEntry(rootOld, rootNew));

        // TODO: Do I need a class/abstraction for this walking code? It feels partly redundant with similar server
        // code.
        while (!changedTrees.isEmpty()) {
            CrawlDiffEntry thisDiff = changedTrees.pop();

            if (thisDiff.getOld() != null && thisDiff.getNew() == null) {
                LOG.debug("TEMP push: old is nonnull, new is null");
                for (FolderListing.FolderEntry entry : thisDiff.getOld().getEntries()) {
                    if (entry.getType() == FolderListing.FolderEntry.Type.FILE) {
                        out.withDeleteChange(DeleteChange.forFile(entry.getId()));
                    } else {
                        FolderListing deletedSubFolder = db.getFolder(entry.getId());
                        changedTrees.add(new CrawlDiffEntry(deletedSubFolder, null));
                    }
                }

            } else if (thisDiff.getOld() == null && thisDiff.getNew() != null) {
                LOG.debug("TEMP push: old is null, new is nonnull");
                for (FolderListing.FolderEntry entry : thisDiff.getNew().getEntries()) {
                    if (entry.getType() == FolderListing.FolderEntry.Type.FILE) {
                        out.withAddChange(AddChange.forContents(entry.getId(), db.getFile(entry.getId())));
                    } else {
                        FolderListing addedSubFolder = db.getFolder(entry.getId());
                        changedTrees.add(new CrawlDiffEntry(null, addedSubFolder));
                    }
                }


            } else {
                LOG.debug("TEMP push: both are nonnull");
                for (FolderListing.FolderEntry entry : thisDiff.getNew().getEntries()) {
                    Optional<FolderListing.FolderEntry> oldEntry = thisDiff.getOld().getEntryForName(entry.getName());
                    if (!oldEntry.isPresent()) {
                        out.withAddChange(AddChange.forContents(entry.getId(), db.getFile(entry.getId())));
                    } else {
                        out.withModifyChange(ModifyChange.forContents(entry.getId(), db.getFile(entry.getId())));
                    }

                    // TODO files -> check getEntryByName() in old...
                    // folders -> check getEntry and maybe add a new CrawlDiffEntry?

                }

                // TODO also check the other side, entries in old that don't correspond to new...
            }
        }
    }
}