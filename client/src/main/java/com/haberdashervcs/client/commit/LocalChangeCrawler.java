package com.haberdashervcs.client.commit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.FolderListing;


final class LocalChangeCrawler {

    private static final HdLogger LOG = HdLoggers.create(LocalChangeCrawler.class);


    private final LocalDb db;
    private final FolderListing commitStartingDir;
    private final Path localStartingDir;

    LocalChangeCrawler(LocalDb db, FolderListing fromBaseCommit, Path localDir) {
        this.db = db;
        this.commitStartingDir = fromBaseCommit;
        this.localStartingDir = localDir;
    }

    // TODO... Returns empty if the folders are the same? Hmm, no...
    // - We're building a folder listing...
    // - If there's *any* difference from the corresponding commit folder, we should add a new folder obj to the db.
    // - But if they're the same... ???
    // - How can this be correct RECURSIVELY????....????????....
    //   **?? Would it be weird to have separate folder LISTINGS vs. folder ENTRIES? does that even make sense?
    //     - TODO sketch this out...
    void compare() {
        try {
            compareInternal();
        } catch (IOException ioEx) {
            throw new RuntimeException(ioEx);
        }
    }

    private static class CrawlEntry {
        final FolderListing currentInCommit;
        final Path currentLocal;

        public CrawlEntry(FolderListing currentInCommit, Path currentLocal) {
            this.currentInCommit = currentInCommit;
            this.currentLocal = Preconditions.checkNotNull(currentLocal);
        }
    }

    void compareInternal() throws IOException {
        LinkedList<CrawlEntry> entries = new LinkedList<>();
        entries.add(new CrawlEntry(commitStartingDir, localStartingDir));

        while (!entries.isEmpty()) {
            CrawlEntry current = entries.pop();

            if (current.currentInCommit == null) {
                LOG.info("Local-only folder: " + current.currentLocal);
            }
            List<Path> localEntriesThisFolder = Files.list(current.currentLocal).collect(Collectors.toList());

            // TODO diffing
            for (Path subpath : localEntriesThisFolder) {
                String name = subpath.getFileName().toString();

                Optional<FolderListing.FolderEntry> commitEntry;

                if (current.currentInCommit == null) {
                    commitEntry = Optional.empty();
                } else {
                    commitEntry = current.currentInCommit.getEntryForName(name);
                }

                if (!commitEntry.isPresent()) {
                    LOG.info("New path: " + subpath.toAbsolutePath());
                }

                // TODO what if it's a folder in the commit, but now a file locally? Etc.
                if (subpath.toFile().isDirectory()) {
                    LOG.info("Local crawl: dir to crawl: " + subpath.toAbsolutePath());
                    if (commitEntry.isPresent()) {
                        FolderListing commitFolder = db.getFolder(commitEntry.get().getId());
                        entries.add(new CrawlEntry(commitFolder, subpath));
                    } else {
                        entries.add(new CrawlEntry(null, subpath));
                    }
                }
            }
        }
    }
}
