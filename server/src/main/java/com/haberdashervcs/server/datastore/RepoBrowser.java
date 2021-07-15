package com.haberdashervcs.server.datastore;

import java.io.IOException;
import java.util.Optional;

import com.haberdashervcs.common.objects.BranchEntry;
import com.haberdashervcs.common.objects.FolderListing;


/**
 * Provides info on the contents of a repo, e.g. for viewing it in a web browser.
 */
public interface RepoBrowser {

    Optional<BranchEntry> getBranch(String branchName) throws IOException;

    FolderListing getFolderAt(String branchName, String path, long commitId) throws IOException;
}
