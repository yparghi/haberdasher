package com.haberdashervcs.client.commit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.FolderListing;


final class LocalCrawlEntry {

    private static final HdLogger LOG = HdLoggers.create(LocalCrawlEntry.class);


    private final FolderListing fromBaseCommit;
    private final Path localDir;

    LocalCrawlEntry(FolderListing fromBaseCommit, Path localDir) {
        this.fromBaseCommit = fromBaseCommit;
        this.localDir = localDir;
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

    void compareInternal() throws IOException {
        List<Path> subpaths = Files.list(localDir).collect(Collectors.toList());
        // TODO diffing
        for (Path subpath : subpaths) {
            String name = subpath.getFileName().toString();
            Optional<FolderListing.FolderEntry> commitEntry = fromBaseCommit.getEntryForName(name);
            if (!commitEntry.isPresent()) {
                LOG.info("Local crawl: new file: " + subpath.toAbsolutePath());
            }
            if (subpath.toFile().isDirectory()) {
                LOG.info("Local crawl: dir to crawl: " + subpath.toAbsolutePath());
            }
        }
    }
}
