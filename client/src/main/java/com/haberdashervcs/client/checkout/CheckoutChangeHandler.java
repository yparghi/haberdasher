package com.haberdashervcs.client.checkout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import com.haberdashervcs.client.commands.RepoConfig;
import com.haberdashervcs.client.crawl.EntryComparisonThisFolder;
import com.haberdashervcs.client.crawl.LocalChangeHandler;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.objects.HdFolderPath;


public final class CheckoutChangeHandler implements LocalChangeHandler {

    private static final HdLogger LOG = HdLoggers.create(CheckoutChangeHandler.class);


    private final RepoConfig config;
    private final LocalDb db;

    public CheckoutChangeHandler(RepoConfig config, LocalDb db) {
        this.config = config;
        this.db = db;
    }


    @Override
    public void handleComparisons(HdFolderPath path, List<EntryComparisonThisFolder> comparisons) throws IOException {
        for (EntryComparisonThisFolder comparison : comparisons) {
            if (comparison.getPathInLocalRepo() != null) {
                handleLocalPathExists(comparison);
            } else {
                handleNewFromCommit(path, comparison);
            }
        }
    }

    private void handleLocalPathExists(EntryComparisonThisFolder comparison) throws IOException {
        final Path localFile = comparison.getPathInLocalRepo();
        final FolderListing.Entry entryInCommit = comparison.getEntryInCommit();

        if (localFile.toFile().isDirectory() && entryInCommit == null) {
            deleteRecursive(localFile);

        } else if (localFile.toFile().isFile() && entryInCommit == null) {
            Files.delete(localFile);

        } else if (localFile.toFile().isDirectory() && entryInCommit != null) {
            if (entryInCommit.getType() == FolderListing.Entry.Type.FILE) {
                deleteRecursive(localFile);
                FileEntry contentsFromCommit = db.getFile(entryInCommit.getId());
                Files.write(localFile, contentsFromCommit.getContents().getRawBytes());
            }

        } else if (localFile.toFile().isFile() && entryInCommit != null) {
            if (entryInCommit.getType() == FolderListing.Entry.Type.FILE) {
                FileEntry contentsFromCommit = db.getFile(entryInCommit.getId());
                Files.write(localFile, contentsFromCommit.getContents().getRawBytes());

            } else {
                Files.delete(localFile);
                Files.createDirectories(localFile);
            }
        }
    }

    // Thanks to: https://stackoverflow.com/a/27917071
    private void deleteRecursive(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }


    private void handleNewFromCommit(HdFolderPath parentPath, EntryComparisonThisFolder comparison) throws IOException {
        Path localToWrite = parentPath
                .joinWithSubfolder(comparison.getEntryInCommit().getName())
                .toLocalPathFromRoot(config.getRoot());

        if (comparison.getEntryInCommit().getType() == FolderListing.Entry.Type.FILE) {
            FileEntry commitFile = db.getFile(comparison.getEntryInCommit().getId());
            String contents = db.resolveDiffs(commitFile);
            Files.write(localToWrite, contents.getBytes(StandardCharsets.UTF_8));

        } else {
            Files.createDirectories(localToWrite);
        }
    }
}
