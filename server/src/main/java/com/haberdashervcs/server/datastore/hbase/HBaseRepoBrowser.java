package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.haberdashervcs.common.objects.BranchEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.objects.HdFolderPath;
import com.haberdashervcs.server.browser.BranchDiff;
import com.haberdashervcs.server.browser.FileDiff;
import com.haberdashervcs.server.browser.HistogramDiffer;
import com.haberdashervcs.server.browser.LineDiff;
import com.haberdashervcs.server.browser.RepoBrowser;


final class HBaseRepoBrowser implements RepoBrowser {

    static HBaseRepoBrowser forRepo(String org, String repo, HBaseRawHelper helper) {
        return new HBaseRepoBrowser(org, repo, helper);
    }


    private final HBaseRowKeyer rowKeyer;
    private final HBaseRawHelper helper;

    private HBaseRepoBrowser(String org, String repo, HBaseRawHelper helper) {
        this.rowKeyer = HBaseRowKeyer.forRepo(org, repo);
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

    @Override
    public BranchDiff getDiffToMain(String branchName) throws IOException {
        Preconditions.checkArgument(!branchName.equals("main"));
        BranchEntry branch = helper.getBranch(rowKeyer.forBranch(branchName)).get();

        // TODO: Can this code be reused w/ Merger?
        final List<HBaseRawHelper.FolderListingWithOriginalBytes> branchHeadHistories =
                helper.getAllHeadBranchHistories(rowKeyer.getOrg(), rowKeyer.getRepo(), branchName);
        final List<Optional<HBaseRawHelper.FolderListingWithOriginalBytes>> mainHeadHistories = new ArrayList<>();

        for (HBaseRawHelper.FolderListingWithOriginalBytes headOnBranch : branchHeadHistories) {

            Optional<HBaseRawHelper.FolderListingWithOriginalBytes> fromMain = helper.getMostRecentListingForPath(
                    rowKeyer.getOrg(), rowKeyer.getRepo(), "main", branch.getHeadCommitId(), headOnBranch.listing.getPath());
            mainHeadHistories.add(fromMain);
        }

        if (branchHeadHistories.size() != mainHeadHistories.size()) {
            throw new AssertionError("Mismatched history entries!");
        }

        Map<String, String> pathToFileIdBranch = new HashMap<>();
        Map<String, String> pathToFileIdMain = new HashMap<>();

        for (HBaseRawHelper.FolderListingWithOriginalBytes folder : branchHeadHistories) {
            for (FolderListing.Entry entry : folder.listing.getEntries()) {
                if (entry.getType() == FolderListing.Entry.Type.FILE) {
                    String path = HdFolderPath.fromFolderListingFormat(folder.listing.getPath())
                            .filePathForName(entry.getName());
                    pathToFileIdBranch.put(path, entry.getId());
                }
            }
        }

        for (Optional<HBaseRawHelper.FolderListingWithOriginalBytes> oFolder : mainHeadHistories) {
            if (!oFolder.isPresent()) {
                continue;
            }
            for (FolderListing.Entry entry : oFolder.get().listing.getEntries()) {
                if (entry.getType() == FolderListing.Entry.Type.FILE) {
                    String path = HdFolderPath.fromFolderListingFormat(oFolder.get().listing.getPath())
                            .filePathForName(entry.getName());
                    pathToFileIdMain.put(path, entry.getId());
                }
            }
        }

        List<FileDiff> out = new ArrayList<>();
        for (Map.Entry<String, String> entry : pathToFileIdBranch.entrySet()) {
            String path = entry.getKey();
            String branchFileId = entry.getValue();
            if (!pathToFileIdMain.containsKey(path)) {
                // TODO! Addition diff -- or just send along the added file contents?
                out.add(FileDiff.of(path, FileDiff.Type.ADDED, ImmutableList.of()));
                continue;
            }

            String mainFileId = pathToFileIdMain.get(path);
            if (mainFileId.equals(branchFileId)) {
                continue;
            } else {
                FileEntry mainFile = helper.getFile(rowKeyer.forFile(mainFileId));
                FileEntry branchFile = helper.getFile(rowKeyer.forFile(branchFileId));
                String mainFileContents = helper.resolveDiffs(mainFile, rowKeyer);
                String branchFileContents = helper.resolveDiffs(branchFile, rowKeyer);
                List<LineDiff> diffs = new HistogramDiffer().toLineDiffs(mainFileContents, branchFileContents);
                out.add(FileDiff.of(path, FileDiff.Type.DIFF, diffs));
            }
        }

        // Check for deletions
        for (Map.Entry<String, String> entry : pathToFileIdMain.entrySet()) {
            if (!pathToFileIdBranch.containsKey(entry.getKey())) {
                // TODO! Deletion diff -- or just send along the deleted file contents?
                out.add(FileDiff.of(entry.getKey(), FileDiff.Type.DELETED, ImmutableList.of()));
            }
        }

        return BranchDiff.of(out);
    }
}
