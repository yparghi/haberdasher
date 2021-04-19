package com.haberdashervcs.client.commit;

import java.nio.file.Path;

import com.haberdashervcs.common.objects.FolderListing;


final class LocalCrawlEntry {

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
    Optional<FolderListing> compare() {

    }
}
