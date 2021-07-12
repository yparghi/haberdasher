package com.haberdashervcs.client.checkout;

import java.util.List;
import java.util.Optional;

import com.haberdashervcs.client.commands.Command;
import com.haberdashervcs.client.commands.RepoConfig;
import com.haberdashervcs.client.crawl.LocalChangeCrawler;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.LocalDbRowKeyer;
import com.haberdashervcs.client.db.objects.LocalFileState;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDb;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDbRowKeyer;
import com.haberdashervcs.client.talker.JettyServerTalker;
import com.haberdashervcs.client.talker.ServerTalker;
import com.haberdashervcs.common.io.HdObjectId;
import com.haberdashervcs.common.io.HdObjectInputStream;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.objects.HdFolderPath;


/**
 * Adds a path to the local repo.
 */
public final class CheckoutCommand implements Command {

    private static final HdLogger LOG = HdLoggers.create(CheckoutCommand.class);


    private final RepoConfig config;
    private final List<String> otherArgs;
    private final LocalDb db;

    public CheckoutCommand(RepoConfig config, List<String> otherArgs) {
        this.config = config;
        this.otherArgs = otherArgs;
        this.db = SqliteLocalDb.getInstance();
    }


    @Override
    public void perform() throws Exception {
        BranchAndCommit bc = db.getCurrentBranch();
        String path = otherArgs.get(0);
        ServerTalker server = JettyServerTalker.forHost(config.getHost());

        if (pathIsCheckedOut(path, db)) {
            throw new IllegalStateException("This path is already checked out.");
        }

        ServerTalker.CheckoutContext checkoutContext = server.checkout(
                bc.getBranchName(), path, bc.getCommitId());

        // TODO: Pass/configure
        LocalDbRowKeyer rowKeyer = SqliteLocalDbRowKeyer.getInstance();
        HdObjectInputStream inStream = checkoutContext.getInputStream();
        Optional<HdObjectId> nextObj;
        while ((nextObj = inStream.next()).isPresent()) {
            switch (nextObj.get().getType()) {
                case FILE:
                    FileEntry file = inStream.getFile();
                    String fileRowKey = rowKeyer.forFile(nextObj.get().getId());
                    LocalFileState state = LocalFileState.of(true);
                    LOG.debug("TEMP: Putting file: %s", file.getDebugString());
                    db.putFile(fileRowKey, file, state);
                    break;

                case FOLDER:
                    FolderListing folder = inStream.getFolder();
                    String folderRowKey = rowKeyer.forFolder(bc.getBranchName(), folder.getPath(), bc.getCommitId());
                    db.putFolder(folderRowKey, folder);
                    break;

                default:
                    checkoutContext.finish();
                    throw new IllegalStateException("Unexpected object type: " + nextObj.get().getType());
            }
        }

        checkoutContext.finish();


        LocalChangeCrawler crawler = new LocalChangeCrawler(
                config,
                db,
                bc,
                HdFolderPath.fromFolderListingFormat(path),
                new CheckoutChangeHandler(config, db));
        crawler.crawl();
        db.addCheckedOutPath(path);
    }


    // TODO: How do I handle the case where I'm trying to checkout a *parent* of a currently
    //     checked out path?
    private boolean pathIsCheckedOut(String path, LocalDb db) {
        List<String> checkedOutPaths = db.getCheckedOutPaths();
        for (String checkedOutPath : checkedOutPaths) {
            if (path.startsWith(checkedOutPath)) {
                return true;
            }
        }
        return false;
    }
}
