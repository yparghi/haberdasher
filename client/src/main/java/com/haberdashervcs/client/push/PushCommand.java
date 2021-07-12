package com.haberdashervcs.client.push;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.haberdashervcs.client.commands.Command;
import com.haberdashervcs.client.commands.RepoConfig;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.objects.LocalBranchState;
import com.haberdashervcs.client.db.objects.LocalFileState;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDb;
import com.haberdashervcs.client.talker.JettyServerTalker;
import com.haberdashervcs.client.talker.ServerTalker;
import com.haberdashervcs.common.change.AddChange;
import com.haberdashervcs.common.change.Changeset;
import com.haberdashervcs.common.change.DeleteChange;
import com.haberdashervcs.common.change.ModifyChange;
import com.haberdashervcs.common.io.HdObjectOutputStream;
import com.haberdashervcs.common.io.ProtobufObjectOutputStream;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.objects.HdFolderPath;


public class PushCommand implements Command {

    private static final HdLogger LOG = HdLoggers.create(PushCommand.class);


    private final RepoConfig config;
    private final List<String> otherArgs;
    private final LocalDb db;

    public PushCommand(RepoConfig config, List<String> otherArgs) {
        this.config = config;
        this.otherArgs = otherArgs;
        this.db = SqliteLocalDb.getInstance();
    }


    @Override
    public void perform() throws Exception {
        // Rethink notes, 5/30/2021:
        // - You push to the branch. Don't worry about main. (That's what merging is for.)
        // - What do you push?:
        //     - A stream of: folders and files. (TODO: Commit objects, with messages & authors?)
        //     - Folder listings contain the commit id in them anyway, so the server can row-key them from that.
        //     - File ids are just a content hash, i.e. they have no commit or folder context.
        // - Do we use the Changeset object in pushing?:
        //     - There's a stream of protos sent to the server...
        //     - Maybe we don't need Changeset at all, if it's the *client* deciding what's an add, what's a diff, what's a rename...
        //     - All of that is in the FileEntry object/proto...
        //     - So let the client build all that and just send objects to the server.
        //     - As for RebaseSpec and other "meta" settings on the push, there can be a "push definition" object or something
        //           that's sent first, before file & folder objects.
        //
        // If the commit command is responsible for detecting changed folders and files, then how does push decide *what to push*?:
        // - Say we know the "last pushed commit" -- LPC...
        // - New folders are straightforward because they have commits set on them.
        // - New files?:
        //     - Get a list of ALL file ids by crawling folder listings newer than the LPC.
        //     - How do we know what's not been pushed?: Just store it as a column in the Files table of the DB?
        //         - Then when would we mark it pushed? On successful completion of the whole push op?
        //             - This would be ok w.r.t. idempotency if files are content-hashed, or if the server just ignores
        //               files/ids that are already in the server DB.

        BranchAndCommit currentBC = db.getCurrentBranch();
        LocalBranchState branchState = db.getBranchState(currentBC.getBranchName());
        LOG.debug("TEMP: branch state: %s", branchState.getDebugString());

        // Folder crawl:
        // - Db call that gets all folder listings up to (path, LPC)
        // - For each returned f.l., write it to the stream *and* add it to the crawl queue.
        // - For each file in the returned f.l., if it's *not* been pushed (according to its entry state), then write it out.
        //     - Also, CACHE SEEN FILE IDs, since we're looking at the same folders repeatedly (at different commits).

        ServerTalker serverTalker = JettyServerTalker.forHost(config.getHost());
        ServerTalker.PushContext serverPushContext = serverTalker.push(
                currentBC.getBranchName(), branchState.getBaseCommitId(), currentBC.getCommitId());
        HdObjectOutputStream outStream = ProtobufObjectOutputStream.forOutputStream(
                serverPushContext.getOutputStream());

        HashSet<String> seenFiles = new HashSet<>();
        HashSet<String> filesToSaveAsPushed = new HashSet<>();
        HashSet<HdFolderPath> seenPaths = new HashSet<>();
        LinkedList<HdFolderPath> pathsToCrawl = new LinkedList<>();
        for (String checkedOutPath : db.getCheckedOutPaths()) {
            pathsToCrawl.add(HdFolderPath.fromFolderListingFormat(checkedOutPath));
        }

        while (!pathsToCrawl.isEmpty()) {
            HdFolderPath hdPath  = pathsToCrawl.pop();
            if (seenPaths.contains(hdPath)) {
                throw new AssertionError("Path crawled twice: " + hdPath);
            }
            seenPaths.add(hdPath);


            // Because subfolders may have pushable changes while a parent folder doesn't, we have
            // to crawl even FolderListings that haven't changed since the last pushed commit.
            Optional<FolderListing> baseFolderAtCommit = db.findFolderAt(
                    currentBC.getBranchName(),
                    hdPath.forFolderListing(),
                    branchState.getLastPushedCommitId());
            if (baseFolderAtCommit.isPresent()) {
                for (FolderListing.Entry entry : baseFolderAtCommit.get().getEntries()) {
                    if (entry.getType() == FolderListing.Entry.Type.FOLDER) {
                        HdFolderPath subpath = hdPath.joinWithSubfolder(entry.getName());
                        if (!seenPaths.contains(subpath)) {
                            pathsToCrawl.add(0, subpath);
                        }
                    }
                }
            }


            List<FolderListing> listingsThisPath = db.getListingsSinceCommit(
                    currentBC.getBranchName(),
                    hdPath.forFolderListing(),
                    branchState.getLastPushedCommitId());
            LOG.debug("TEMP: Found %d listings since commit", listingsThisPath.size());

            for (FolderListing listing : listingsThisPath) {
                LOG.debug("Writing folder: %s", listing.getDebugString());
                outStream.writeFolder("TODO do folders still have ids?", listing);

                for (FolderListing.Entry entry : listing.getEntries()) {
                    if (entry.getType() == FolderListing.Entry.Type.FOLDER) {
                        HdFolderPath entryPath = hdPath.joinWithSubfolder(entry.getName());
                        if (!seenPaths.contains(entryPath)) {
                            // TODO! BFS or DFS here? why?
                            pathsToCrawl.add(0, entryPath);
                        }

                    } else if (entry.getType() == FolderListing.Entry.Type.FILE) {
                        if (seenFiles.contains(entry.getId())) {
                            continue;
                        }

                        LocalFileState fileState = db.getFileState(entry.getId());
                        if (fileState.isPushedToServer()) {
                            continue;
                        }

                        FileEntry file = db.getFile(entry.getId());
                        LOG.debug("Writing file: %s", file.getDebugString());
                        outStream.writeFile(file.getId(), file);
                        filesToSaveAsPushed.add(file.getId());
                    }
                }
            }
        }

        serverPushContext.finish();

        for (String fileId : filesToSaveAsPushed) {
            LocalFileState newState = LocalFileState.of(true);
            db.putFileState(fileId, newState);
        }

        LocalBranchState newBranchState = LocalBranchState.of(
                branchState.getBaseCommitId(),
                branchState.getHeadCommitId(),
                branchState.getHeadCommitId());  // New lastPushedCommitId
        db.putBranchState(currentBC.getBranchName(), newBranchState);

        LOG.info("Done.");
    }
}
