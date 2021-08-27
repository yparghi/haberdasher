package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.haberdashervcs.common.diff.DmpDiffer;
import com.haberdashervcs.common.io.HdObjectByteConverter;
import com.haberdashervcs.common.io.ProtobufObjectByteConverter;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchEntry;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.objects.MergeLock;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.CheckAndMutate;
import org.apache.hadoop.hbase.client.CheckAndMutateResult;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;


/**
 * Low-level logic for HBase gets and puts.
 */
public final class HBaseRawHelper {

    private static final HdLogger LOG = HdLoggers.create(HBaseRawHelper.class);

    public static HBaseRawHelper forConnection(Connection conn) {
        return new HBaseRawHelper(conn);
    }


    private final Connection conn;
    // TODO pass/configure this
    private final HdObjectByteConverter byteConv = ProtobufObjectByteConverter.getInstance();

    private HBaseRawHelper(Connection conn) {
        this.conn = conn;
    }

    CommitEntry getCommit(byte[] rowKey) throws IOException {
        final Table commitsTable = conn.getTable(TableName.valueOf("Commits"));
        final String columnFamilyName = "cfMain";

        Get get = new Get(rowKey);
        Result result = commitsTable.get(get);
        byte[] commitEntryBytes = result.getValue(
                Bytes.toBytes(columnFamilyName), Bytes.toBytes("entry"));

        return byteConv.commitFromBytes(commitEntryBytes);
    }

    FileEntry getFile(final byte[] rowKey) throws IOException {
        final Table filesTable = conn.getTable(TableName.valueOf("Files"));
        final String columnFamilyName = "cfMain";

        Get get = new Get(rowKey);
        Result result = filesTable.get(get);
        byte[] fileValue = result.getValue(
                Bytes.toBytes(columnFamilyName), Bytes.toBytes("contents"));

        return byteConv.fileFromBytes(fileValue);
    }


    FolderListing getFolder(byte[] folderRowKey) throws IOException {
        final Table foldersTable = conn.getTable(TableName.valueOf("Folders"));
        final String columnFamilyName = "cfMain";

        Get get = new Get(folderRowKey);
        Result result = foldersTable.get(get);
        byte[] folderValue = result.getValue(
                Bytes.toBytes(columnFamilyName), Bytes.toBytes("listing"));

        return byteConv.folderFromBytes(folderValue);
    }


    void putFile(final byte[] rowKey, FileEntry fileEntry) throws IOException {
        LOG.debug(
                "TEMP: Putting file: %s / %s",
                new String(rowKey, StandardCharsets.UTF_8),
                fileEntry.getDebugString());

        final Table filesTable = conn.getTable(TableName.valueOf("Files"));
        final String columnFamilyName = "cfMain";
        final String columnName = "contents";

        Put put = new Put(rowKey);
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                byteConv.fileToBytes(fileEntry));
        filesTable.put(put);
    }

    void putFolderIfNotExists(final byte[] rowKey, FolderListing folderListing) throws IOException {
        LOG.debug("Writing FolderListing: %s", folderListing.getDebugString());

        final Table foldersTable = conn.getTable(TableName.valueOf("Folders"));
        final String columnFamilyName = "cfMain";
        final String columnName = "listing";

        Put put = new Put(rowKey);
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                byteConv.folderToBytes(folderListing));

        CheckAndMutate cAndM = CheckAndMutate.newBuilder(rowKey)
                .ifNotExists(Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName))
                .build(put);

        CheckAndMutateResult result = foldersTable.checkAndMutate(cAndM);
        if (!result.isSuccess()) {
            throw new IOException("CheckAndMutate failed");
        }
    }

    void putCommit(final byte[] rowKey, CommitEntry commitEntry) throws IOException {
        final Table commitsTable = conn.getTable(TableName.valueOf("Commits"));
        final String columnFamilyName = "cfMain";
        final String columnName = "entry";

        Put put = new Put(rowKey);
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                byteConv.commitToBytes(commitEntry));
        commitsTable.put(put);
    }


    Optional<FolderListing> getHeadAtCommit(
            String org,
            String repo,
            String branchName,
            long commitId,
            String path,
            MergeStates mergeStates,
            final long nowTs)
            throws IOException {
        final Table historyTable = conn.getTable(TableName.valueOf("Folders"));
        final String columnFamilyName = "cfMain";
        final String columnName = "listing";

        String startRow = folderListingRowKey(org, repo, branchName, path, commitId);
        String stopRow = folderListingRowKey(org, repo, branchName, path, 0);
        LOG.debug("getHeadAtCommit scan: start / stop: %s / %s", startRow, stopRow);

        Scan scan = new Scan()
                .setReversed(true)
                .withStartRow(startRow.getBytes(StandardCharsets.UTF_8), true)
                .withStopRow(stopRow.getBytes(StandardCharsets.UTF_8), true);

        ResultScanner scanner = historyTable.getScanner(scan);
        Result result;
        while ((result = scanner.next()) != null) {
            byte[] rowBytes = result.getValue(
                    Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName));
            FolderListing listing = byteConv.folderFromBytes(rowBytes);

            if (!listing.getMergeLockId().isPresent()) {
                scanner.close();
                return Optional.of(listing);

            } else {
                String mergeId = listing.getMergeLockId().get();
                // TODO! BUG??? If a folder listing has a merge lock, but MergeStates doesn't have that lock's id in its
                //     data set, then what happened?:
                // - Either the merge failed, or it succeeded and clean-up failed.
                // - So we need to LOOK UP THE LOCK BY ID to be sure.
                // - And then ask something to remove the merge lock entry from the FolderListing using checkAndMutate.
                //
                // We'll need application code to:
                // 1. Make sure a merge lock *can't be changed* in whatever the time window is, say 1 hour e.g.
                // 2. Clean up (delete) the merge lock entry in the FolderListing.
                // ...And maybe all this can be encapsulated in MergeStates (at least for now?) so that it fulfills a
                //     simple "look up a merge lock by id" API.
                Optional<MergeLock> lock = mergeStates.forMergeLockId(mergeId);
                if (lock.isPresent() && lock.get().getState() == MergeLock.State.COMPLETED) {
                    scanner.close();
                    return Optional.of(listing);

                } else {
                    // TODO! For now we assume an absent merge lock failed, but that is simply not a valid assumption.
                    continue;
                }
            }
        }

        // Nothing on the branch? Then look for the head commit on main.
        if (!branchName.equals("main")) {
            return getHeadAtCommit(org, repo, "main", commitId, path, mergeStates, nowTs);
        } else {
            scanner.close();
            return Optional.empty();
        }
    }


    private String folderListingRowKey(String org, String repo, String branchName, String path, long commitId) {
        // TODO! Better formatting of the long, base 256
        return String.format(
                "%s:%s:%s:%s:%020d",
                org, repo, branchName, path, commitId);
    }


    // TODO: Use rowKeyer for this? Instead of private helper methods?
    // TODO: How do I make sure this will NEVER spill over to another repo/org?
    //     - a row key prefix filter, computed from org + repo?
    Optional<FolderListingWithOriginalBytes> getMostRecentListingForPath(String org, String repo, String branch, long commitId, String path) throws IOException {
        final Table historyTable = conn.getTable(TableName.valueOf("Folders"));
        final String columnFamilyName = "cfMain";
        final String columnName = "listing";

        String startRow = folderListingRowKey(org, repo, branch, path, commitId);
        String stopRow = folderListingRowKey(org, repo, branch, path, 0);

        LOG.debug("History scan: start / stop: %s / %s", startRow, stopRow);
        Scan scan = new Scan()
                .setReversed(true)
                .withStartRow(startRow.getBytes(StandardCharsets.UTF_8), true)
                .withStopRow(stopRow.getBytes(StandardCharsets.UTF_8), true);

        ResultScanner scanner = historyTable.getScanner(scan);
        Result result = scanner.next();
        if (result == null) {
            return Optional.empty();
        } else {
            byte[] rowBytes = result.getValue(
                    Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName));
            scanner.close();
            return Optional.of(
                    new FolderListingWithOriginalBytes(byteConv.folderFromBytes(rowBytes), rowBytes));
        }
    }


    // For use with checkAndMutate() to update FolderHistory entries atomically, respecting locks.
    static class FolderListingWithOriginalBytes {
        final FolderListing listing;
        final byte[] originalBytes;

        FolderListingWithOriginalBytes(FolderListing listing, byte[] bytes) {
            this.listing = listing;
            this.originalBytes = bytes;
        }
    }

    List<FolderListingWithOriginalBytes> getAllHeadBranchHistories(String org, String repo, String branchName) throws IOException {
        final Table historyTable = conn.getTable(TableName.valueOf("Folders"));
        final String columnFamilyName = "cfMain";
        final String columnName = "listing";

        final String rowPrefixStr = String.format(
                "%s:%s:%s:",
                org, repo, branchName);
        LOG.debug("All branch history prefix: %s", rowPrefixStr);

        Scan scan = new Scan()
                .setRowPrefixFilter(rowPrefixStr.getBytes(StandardCharsets.UTF_8));

        Map<String, FolderListingWithOriginalBytes> mostRecentSeenPerPath = new HashMap<>();

        ResultScanner scanner = historyTable.getScanner(scan);
        Result result;
        while ((result = scanner.next()) != null) {
            byte[] rowBytes = result.getValue(
                    Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName));
            FolderListing fromBytes = byteConv.folderFromBytes(rowBytes);

            if (!mostRecentSeenPerPath.containsKey(fromBytes.getPath())) {
                mostRecentSeenPerPath.put(fromBytes.getPath(), new FolderListingWithOriginalBytes(fromBytes, rowBytes));

            } else {
                FolderListingWithOriginalBytes existing = mostRecentSeenPerPath.get(fromBytes.getPath());
                if (existing.listing.getCommitId() < fromBytes.getCommitId()) {
                    mostRecentSeenPerPath.put(fromBytes.getPath(), new FolderListingWithOriginalBytes(fromBytes, rowBytes));
                }
            }
        }

        scanner.close();
        return ImmutableList.copyOf(mostRecentSeenPerPath.values());
    }


    List<MergeLock> getMerges(byte[] earlierRowKey, byte[] laterRowKey) throws IOException {
        ArrayList<MergeLock> out = new ArrayList<>();
        final Table mergesTable = conn.getTable(TableName.valueOf("Merges"));
        final String columnFamilyName = "cfMain";
        final String columnName = "locks";

        // Note HBase scans are *not* snapshots/consistent. But that's fine because I only care that *one* set of merge
        // states, regardless of which ones changed during the scan, is applied consistently to any read operation like
        // checkout.
        Scan scan = new Scan()
                .withStartRow(earlierRowKey)
                .withStopRow(laterRowKey);

        ResultScanner scanner = mergesTable.getScanner(scan);
        Result result;
        while ((result = scanner.next()) != null) {
            byte[] resultBytes = result.getValue(
                    Bytes.toBytes(columnFamilyName),
                    Bytes.toBytes(columnName));
            out.add(byteConv.mergeLockFromBytes(resultBytes));
        }

        scanner.close();
        return out;
    }

    void putMerge(byte[] rowKey, MergeLock lock) throws IOException {
        final Table mergesTable = conn.getTable(TableName.valueOf("Merges"));
        final String columnFamilyName = "cfMain";
        final String columnName = "locks";

        Put put = new Put(rowKey);
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                byteConv.mergeLockToBytes(lock));
        mergesTable.put(put);
    }

    void putBranch(byte[] rowKey, BranchEntry branch) throws IOException {
        final Table branchesTable = conn.getTable(TableName.valueOf("Branches"));
        final String columnFamilyName = "cfMain";
        final String columnName = "branch";

        Put put = new Put(rowKey);
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                byteConv.branchToBytes(branch));
        branchesTable.put(put);
    }

    boolean putBranchIfUnchanged(byte[] rowKey, BranchEntry branch, byte[] originalBytes) throws IOException {
        final Table branchesTable = conn.getTable(TableName.valueOf("Branches"));
        final String columnFamilyName = "cfMain";
        final String columnName = "branch";

        Put put = new Put(rowKey);
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                byteConv.branchToBytes(branch));

        CheckAndMutate cAndM = CheckAndMutate.newBuilder(rowKey)
                .ifEquals(Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName), originalBytes)
                .build(put);

        CheckAndMutateResult result = branchesTable.checkAndMutate(cAndM);
        return (result.isSuccess());
    }

    static class BranchWithOriginalBytes {
        BranchEntry branch;
        byte[] originalBytes;
    }


    Optional<BranchEntry> getBranch(final byte[] rowKey) throws IOException {
        Optional<BranchWithOriginalBytes> bb = getBranchWithOriginalBytes(rowKey);
        if (bb.isPresent()) {
            return Optional.of(bb.get().branch);
        } else {
            return Optional.empty();
        }
    }


    Optional<BranchWithOriginalBytes> getBranchWithOriginalBytes(final byte[] rowKey) throws IOException {
        final Table branchesTable = conn.getTable(TableName.valueOf("Branches"));
        final String columnFamilyName = "cfMain";
        final String columnName = "branch";

        Get get = new Get(rowKey);
        Result result = branchesTable.get(get);
        if (result.isEmpty()) {
            return Optional.empty();
        }

        byte[] rowValue = result.getValue(
                Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName));

        BranchWithOriginalBytes out = new BranchWithOriginalBytes();
        out.branch = byteConv.branchFromBytes(rowValue);
        out.originalBytes = rowValue;
        return Optional.of(out);
    }


    // TODO: Should I switch to unix style patches everywhere, instead of diff-match-patch?
    // TODO: Put this in one common place? Instead of copy-pasting it from SqliteLocalDb.
    private static final int MAX_DIFF_SEARCH = 20;
    // TODO: Internalize rowKeyer?
    String resolveDiffs(final FileEntry file, HBaseRowKeyer rowKeyer) throws IOException {
        if (file.getContentsType() == FileEntry.ContentsType.FULL) {
            return new String(file.getContents().getRawBytes(), StandardCharsets.UTF_8);
        }

        Preconditions.checkState(file.getContentsType() == FileEntry.ContentsType.DIFF_DMP);
        ArrayList<byte[]> diffs = new ArrayList<>();
        diffs.add(file.getContents().getRawBytes());
        FileEntry current = file;
        for (int i = 0; i < MAX_DIFF_SEARCH; ++i) {
            FileEntry parent = getFile(
                    rowKeyer.forFile(file.getBaseEntryId().get()));
            if (parent.getContentsType() == FileEntry.ContentsType.DIFF_DMP) {
                diffs.add(0, parent.getContents().getRawBytes());
                current = parent;
                continue;

            } else if (parent.getContentsType() == FileEntry.ContentsType.FULL) {
                try {
                    return DmpDiffer.applyPatches(diffs, parent);
                } catch (IOException ioEx) {
                    throw new RuntimeException(ioEx);
                }

            } else {
                throw new IllegalStateException("Unknown contents type: " + parent.getContentsType());
            }
        }
        throw new IllegalStateException("Couldn't resolve a diff after " + MAX_DIFF_SEARCH + " entries");
    }

}
