package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;
import java.util.Optional;

import com.haberdashervcs.common.objects.BranchEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.server.datastore.RepoBrowser;


final class HBaseRepoBrowser implements RepoBrowser {

    static HBaseRepoBrowser forRepo(String org, String repo, HBaseRawHelper helper) {
        return new HBaseRepoBrowser(org, repo, helper);
    }


    private final HBaseRowKeyMaker rowKeyer;
    private final HBaseRawHelper helper;

    private HBaseRepoBrowser(String org, String repo, HBaseRawHelper helper) {
        this.rowKeyer = HBaseRowKeyMaker.of(org, repo);
        this.helper = helper;
    }

    @Override
    public Optional<BranchEntry> getBranch(String branchName) throws IOException {
        return helper.getBranch(rowKeyer.forBranch(branchName));
    }

    @Override
    public FolderListing getFolderAt(String branchName, String path, long commitId) throws IOException {
        return helper.getFolder(rowKeyer.forFolderAt(branchName, path, commitId));
    }
}
