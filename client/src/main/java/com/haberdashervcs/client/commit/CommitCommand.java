package com.haberdashervcs.client.commit;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import com.haberdashervcs.client.commands.Command;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDb;
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
        final Path localRoot = Paths.get("");

        LocalChangeCrawler crawler = new LocalChangeCrawler(db, commitRoot, localRoot);
        crawler.compare();
    }
}