package com.haberdashervcs.client.commit;

import java.util.List;

import com.haberdashervcs.client.commands.Command;
import com.haberdashervcs.client.commands.RepoConfig;
import com.haberdashervcs.client.crawl.LocalChangeCrawler;
import com.haberdashervcs.client.crawl.LocalChangeHandler;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDb;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;
import com.haberdashervcs.common.objects.HdFolderPath;


public class DiffCommand implements Command {

    private static final HdLogger LOG = HdLoggers.create(DiffCommand.class);


    private final RepoConfig config;
    private final String startPath;

    public DiffCommand(RepoConfig config, List<String> otherArgs) {
        this.config = config;
        this.startPath = otherArgs.get(0);
    }


    @Override
    public void perform() throws Exception {
        LocalDb db = SqliteLocalDb.getInstance();
        BranchAndCommit currentHead = db.getCurrentBranch();
        LocalChangeHandler handler = new PrintChangeHandler(db);
        HdFolderPath startHdPath = HdFolderPath.fromStringArg(this.startPath, config.getRoot());

        LocalChangeCrawler crawler = new LocalChangeCrawler(
                config,
                db,
                currentHead,
                startHdPath,
                handler);
        crawler.crawl();
    }
}
