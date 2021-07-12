package com.haberdashervcs.client.crawl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.haberdashervcs.client.commands.RepoConfig;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.LocalDbRowKeyer;
import com.haberdashervcs.client.db.objects.LocalBranchState;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDbRowKeyer;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.objects.HdFolderPath;


public final class LocalChangeCrawler {

    private static final HdLogger LOG = HdLoggers.create(LocalChangeCrawler.class);


    private final RepoConfig config;
    private final LocalDb db;
    private final LocalDbRowKeyer rowKeyer;
    private final BranchAndCommit baseCommit;
    private final HdFolderPath rootPath;
    private final LocalChangeHandler changeHandler;

    public LocalChangeCrawler(
            RepoConfig config,
            LocalDb db,
            BranchAndCommit baseCommit,
            HdFolderPath rootPath,
            LocalChangeHandler changeHandler) {
        this.config = config;
        this.db = db;
        // TODO: Make this internal to the DB, and change the getFile/getFolder API?
        this.rowKeyer = SqliteLocalDbRowKeyer.getInstance();
        this.baseCommit = baseCommit;
        this.rootPath = rootPath;
        this.changeHandler = changeHandler;
    }


    private static class CrawlEntry {
        private final HdFolderPath path;
        private final @Nullable FolderListing folderInCommit;

        private CrawlEntry(HdFolderPath path, @Nullable FolderListing folderInCommit) {
            this.path = path;
            this.folderInCommit = folderInCommit;
        }
    }


    public void crawl() throws IOException {
        // TODO! BranchAndCommit.getCommitId() causes problems because the head is 0 (incremental)
        //     on a branch. I need to figure this out -- maybe by tossing BranchAndCommit and just
        //     using LocalBranchState everywhere...
        final LocalBranchState branchState = db.getBranchState(baseCommit.getBranchName());

        Optional<FolderListing> rootListing = findFolderFallBackToMain(rootPath, branchState);
        LOG.debug("Got rootListing: %s", rootListing);

        LinkedList<CrawlEntry> crawlEntries = new LinkedList<>();
        crawlEntries.add(new CrawlEntry(rootPath, rootListing.orElse(null)));

        while (!crawlEntries.isEmpty()) {
            final CrawlEntry thisEntry = crawlEntries.pop();
            // TEMP BUG! thisEntry.folderInCommit shouldn't be null...
            LOG.debug("Looking at crawl entry: %s | %s", thisEntry.folderInCommit, thisEntry.path);
            List<EntryComparisonThisFolder> comparisons = comparisonsForThisFolder(thisEntry);

            for (EntryComparisonThisFolder comparison : comparisons) {
                if (comparison.pathInLocalRepo != null) {
                    addCrawlEntriesForLocalSubfolder(thisEntry.path, comparison, crawlEntries, branchState);

                } else if (comparison.entryInCommit.getType() == FolderListing.Entry.Type.FOLDER) {
                    // This path entry is present in the commit, but not locally.
                    HdFolderPath subfolderPath = thisEntry.path.joinWithSubfolder(comparison.entryInCommit.getName());
                    Optional<FolderListing> subfolderListing = findFolderFallBackToMain(
                            subfolderPath, branchState);
                    crawlEntries.add(new CrawlEntry(
                            subfolderPath,
                            subfolderListing.get()));
                }
            }

            changeHandler.handleComparisons(thisEntry.path, comparisons);
        }
    }

    private Optional<FolderListing> findFolderFallBackToMain(HdFolderPath rootPath, LocalBranchState branchState) {
        Optional<FolderListing> rootListing = db.findFolderAt(
                baseCommit.getBranchName(), rootPath.forFolderListing(), baseCommit.getCommitId());
        if (rootListing.isPresent()) {
            return rootListing;
        } else if (!baseCommit.getBranchName().equals("main")) {
            return db.findFolderAt(
                    "main", rootPath.forFolderListing(), branchState.getBaseCommitId() + baseCommit.getCommitId());
        } else {
            return Optional.empty();
        }
    }

    private void addCrawlEntriesForLocalSubfolder(
            HdFolderPath parentPath,
            EntryComparisonThisFolder comparison,
            LinkedList<CrawlEntry> entries,
            LocalBranchState branchState)
            throws IOException {
        Preconditions.checkArgument(comparison.pathInLocalRepo != null);
        HdFolderPath subfolderPath = parentPath.joinWithSubfolder(comparison.name);

        Optional<FolderListing> folderFromCommit;
        if (comparison.entryInCommit != null
                && comparison.entryInCommit.getType() == FolderListing.Entry.Type.FOLDER) {
            folderFromCommit = findFolderFallBackToMain(
                    subfolderPath, branchState);
        } else {
            folderFromCommit = Optional.empty();
        }

        if (comparison.pathInLocalRepo.toFile().isDirectory()) {
            entries.add(new CrawlEntry(subfolderPath, folderFromCommit.orElse(null)));

        } else if (folderFromCommit.isPresent()) {
            entries.add(new CrawlEntry(subfolderPath, folderFromCommit.get()));
        }
    }


    private List<EntryComparisonThisFolder> comparisonsForThisFolder(CrawlEntry thisEntry) throws IOException {
        Path localDir = thisEntry.path.toLocalPathFromRoot(config.getRoot());
        final List<Path> localFilesThisFolder;
        if (!localDir.toFile().exists()) {
            localFilesThisFolder = Collections.emptyList();
        } else {
            if (!localDir.toFile().isDirectory()) {
                throw new AssertionError(
                        "Unexpected: The local path for this crawl entry is a file.");
            }
            localFilesThisFolder = Files.list(localDir).collect(Collectors.toList());
        }


        Map<String, EntryComparisonThisFolder> nameToComparison = new TreeMap<>();
        for (Path localFile : localFilesThisFolder) {
            String localFileName = localFile.getFileName().toString();
            // TODO make this better
            if (localFileName.equals("hdlocal") || localFileName.equals("hdlocal.db")) {
                continue;
            }

            EntryComparisonThisFolder newComparison = new EntryComparisonThisFolder(localFileName);
            newComparison.pathInLocalRepo = localFile;
            nameToComparison.put(localFileName, newComparison);
        }

        if (thisEntry.folderInCommit != null) {
            for (FolderListing.Entry entry : thisEntry.folderInCommit.getEntries()) {
                EntryComparisonThisFolder comparison;
                if (!nameToComparison.containsKey(entry.getName())) {
                    comparison = new EntryComparisonThisFolder(entry.getName());
                } else {
                    comparison = nameToComparison.get(entry.getName());
                }
                comparison.entryInCommit = entry;
                nameToComparison.put(entry.getName(), comparison);
            }
        }

        return nameToComparison.values().stream().collect(Collectors.toList());
    }
}
