package com.haberdashervcs.client.git;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.haberdashervcs.client.commands.Command;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDb;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;


public class ImportGitCommand implements Command {

    private static final HdLogger LOG = HdLoggers.create(ImportGitCommand.class);


    private final List<String> otherArgs;

    public ImportGitCommand(List<String> otherArgs) {
        this.otherArgs = ImmutableList.copyOf(otherArgs);
    }

    @Override
    public void perform() throws Exception {
        final Path repoPath = Paths.get(otherArgs.get(0));
        final String branch = otherArgs.get(1);

        Git git = Git.open(repoPath.toFile());
        RevWalk walk = new RevWalk(git.getRepository());
        walk.sort(RevSort.REVERSE, true);
        ObjectId headId = git.getRepository().resolve(branch);
        RevCommit headCommit = walk.parseCommit(headId);
        walk.markStart(headCommit);

        int testCounter = 0; // TEMP!
        for (RevCommit commit : walk) {
            processCommit(commit, git.getRepository());
            if ((++testCounter) >= 2) {
                break;
            }
        }

        walk.close();
        git.getRepository().close();
        git.close();
    }

    private void processCommit(RevCommit commit, Repository repo) throws Exception {
        LOG.debug("Got commit: %s", commit.getId());
        GitChangeCrawler crawler = new GitChangeCrawler(SqliteLocalDb.getInstance(), commit, repo);
        crawler.compare();
    }
}
