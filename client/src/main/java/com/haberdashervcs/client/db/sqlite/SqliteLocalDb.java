package com.haberdashervcs.client.db.sqlite;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.objects.LocalBranchState;
import com.haberdashervcs.client.db.objects.LocalDbObjectByteConverter;
import com.haberdashervcs.client.db.objects.LocalFileState;
import com.haberdashervcs.client.db.objects.LocalRepoState;
import com.haberdashervcs.client.db.objects.ProtobufLocalDbObjectByteConverter;
import com.haberdashervcs.common.diff.BsDiffer;
import com.haberdashervcs.common.diff.DmpDiffer;
import com.haberdashervcs.common.io.HdObjectByteConverter;
import com.haberdashervcs.common.io.ProtobufObjectByteConverter;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;


public final class SqliteLocalDb implements LocalDb {

    private static final HdLogger LOG = HdLoggers.create(SqliteLocalDb.class);

    private static final SqliteLocalDb INSTANCE = new SqliteLocalDb();
    private static final String DB_FILENAME = "hdlocal.db";

    // TODO: This should use the RepoConfig, so we can use getRoot() to find the DB.
    public static SqliteLocalDb getInstance() {
        return INSTANCE;
    }


    private Supplier<Connection> conn = Suppliers.memoize(new Supplier<Connection>() {
        @Override
        public Connection get() {
            try {
                return DriverManager.getConnection("jdbc:sqlite:" + DB_FILENAME);
            } catch (SQLException sqlEx) {
                throw new RuntimeException(sqlEx);
            }
        }
    });

    private final HdObjectByteConverter byteConv;
    private final LocalDbObjectByteConverter localByteConv;

    private SqliteLocalDb() {
        this.byteConv = ProtobufObjectByteConverter.getInstance();
        this.localByteConv = ProtobufLocalDbObjectByteConverter.getInstance();
    }

    @Override
    public void init(BranchAndCommit mainHead) {
        Preconditions.checkArgument(mainHead.getBranchName().equals("main"));

        final File dbFile = new File(DB_FILENAME);
        if (dbFile.exists()) {
            throw new IllegalStateException(
                    String.format("The file `%s` already exists!", DB_FILENAME));
        }

        createTable(SqliteLocalDbSchemas.META_SCHEMA);
        createTable(SqliteLocalDbSchemas.BRANCHES_SCHEMA);
        createTable(SqliteLocalDbSchemas.CHECKOUTS_SCHEMA);
        createTable(SqliteLocalDbSchemas.COMMITS_SCHEMA);
        createTable(SqliteLocalDbSchemas.FOLDERS_SCHEMA);
        createTable(SqliteLocalDbSchemas.FILES_SCHEMA);

        insertMetaValue(
                "REPO_STATE",
                localByteConv.repoStateToBytes(
                        LocalRepoState.forState(LocalRepoState.State.NORMAL)));

        addBranch("main", LocalBranchState.of(
                mainHead.getCommitId(), mainHead.getCommitId(), mainHead.getCommitId()));
        insertMetaValue("CURRENT_BRANCH", "main");
    }

    private void addBranch(String branchName, LocalBranchState branchState) {
        try {
            byte[] branchStateBytes = localByteConv.branchStateToBytes(branchState);

            PreparedStatement stmt = conn.get().prepareStatement(
                    "INSERT INTO Branches (branchName, branchState) VALUES (?, ?)");
            stmt.setString(1, branchName);
            stmt.setBytes(2, branchStateBytes);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated != 1) {
                throw new IllegalStateException("Expected 1 row inserted, got " + rowsUpdated);
            }
        } catch (SQLException sqlEx) {
            throw new RuntimeException(sqlEx);
        }
    }


    private void createTable(String sql) {
        try {
            Statement stmt = conn.get().createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException sqlEx) {
            throw new RuntimeException(sqlEx);
        }
    }


    @Override
    // TODO! can I toss b&c in favor of LocalBranchState?
    public BranchAndCommit getCurrentBranch() {
        String currentBranch = getMetaValueAsString("CURRENT_BRANCH");
        LocalBranchState state = getBranchState(currentBranch);
        return BranchAndCommit.of(currentBranch, state.getHeadCommitId());
    }


    @Override
    public BranchAndCommit switchToBranch(String branchName) {
        LocalBranchState state = getBranchState(branchName);
        updateMetaValue("CURRENT_BRANCH", branchName);
        return BranchAndCommit.of(branchName, state.getHeadCommitId());
    }


    @Override
    public void createNewBranch(String branchName) {
        LocalBranchState mainState = getBranchState("main");
        // TODO! Test out the correctness of starting commits #0
        LocalBranchState newBranchState = LocalBranchState.of(mainState.getHeadCommitId(), 0, 0);
        addBranch(branchName, newBranchState);
        updateMetaValue("CURRENT_BRANCH", branchName);
    }


    @Override
    public LocalBranchState getBranchState(String branchName) {
        try {
            PreparedStatement getStmt = conn.get().prepareStatement(
                    "SELECT branchState FROM Branches WHERE branchName = ?");
            getStmt.setString(1, branchName);
            ResultSet rs = getStmt.executeQuery();
            rs.next();
            byte[] branchStateBytes = rs.getBytes("branchState");
            return localByteConv.branchStateFromBytes(branchStateBytes);
        } catch (SQLException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }


    @Override
    public void putBranchState(String branchName, LocalBranchState newState) {
        try {
            PreparedStatement stmt = conn.get().prepareStatement(
                    "UPDATE Branches SET branchState = ? WHERE branchName = ?");
            stmt.setBytes(1, localByteConv.branchStateToBytes(newState));
            stmt.setString(2, branchName);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated != 1) {
                throw new IllegalStateException("Expected 1 row inserted, got " + rowsUpdated);
            }
        } catch (SQLException sqlEx) {
            throw new RuntimeException(sqlEx);
        }
    }


    @Override
    public LocalRepoState getRepoState() {
        try {
            return localByteConv.repoStateFromBytes(getMetaValue("REPO_STATE"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to get repo state", e);
        }
    }


    @Override
    public void updateRepoState(LocalRepoState newState) {
        byte[] bytes = localByteConv.repoStateToBytes(newState);
        updateMetaValue("REPO_STATE", bytes);
    }


    private byte[] getMetaValue(String key) {
        try {
            PreparedStatement getStmt = conn.get().prepareStatement(
                    "SELECT value FROM Meta WHERE key = ?");
            getStmt.setString(1, key);
            ResultSet rs = getStmt.executeQuery();
            rs.next();
            return rs.getBytes("value");
        } catch (SQLException sqlEx) {
            throw new RuntimeException(sqlEx);
        }
    }

    private String getMetaValueAsString(String key) {
        return new String(getMetaValue(key), StandardCharsets.UTF_8);
    }

    private void insertMetaValue(String key, String value) {
        insertMetaValue(key, value.getBytes(StandardCharsets.UTF_8));
    }

    private void insertMetaValue(String key, byte[] value) {
        try {
            PreparedStatement stmt = conn.get().prepareStatement(
                    "INSERT INTO Meta (key, value) VALUES (?, ?)");
            stmt.setString(1, key);
            stmt.setBytes(2, value);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated != 1) {
                throw new IllegalStateException("Expected 1 row inserted, got " + rowsUpdated);
            }
        } catch (SQLException sqlEx) {
            throw new RuntimeException(sqlEx);
        }
    }

    private void updateMetaValue(String key, String value) {
        updateMetaValue(key, value.getBytes(StandardCharsets.UTF_8));
    }

    private void updateMetaValue(String key, byte[] value) {
        try {
            PreparedStatement stmt = conn.get().prepareStatement(
                    "UPDATE Meta SET value = ? WHERE key = ?");
            stmt.setBytes(1, value);
            stmt.setString(2, key);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated != 1) {
                throw new IllegalStateException("Expected 1 row updated, got " + rowsUpdated);
            }
        } catch (SQLException sqlEx) {
            throw new RuntimeException(sqlEx);
        }
    }


    @Override
    public CommitEntry getCommit(String key) {
        try {
            PreparedStatement getStmt = conn.get().prepareStatement(
                    "SELECT contents FROM Commits WHERE id = ?");
            getStmt.setString(1, key);
            ResultSet rs = getStmt.executeQuery();
            rs.next();
            byte[] contentsBytes = rs.getBytes("contents");
            return byteConv.commitFromBytes(contentsBytes);
        } catch (SQLException | IOException ex) {
            throw new RuntimeException("Couldn't find commit: " + key, ex);
        }
    }

    @Override
    public FolderListing getFolder(String key) {
        try {
            PreparedStatement getStmt = conn.get().prepareStatement(
                    "SELECT contents FROM Folders WHERE id = ?");
            getStmt.setString(1, key);
            ResultSet rs = getStmt.executeQuery();
            rs.next();
            byte[] contentsBytes = rs.getBytes("contents");
            return byteConv.folderFromBytes(contentsBytes);
        } catch (SQLException | IOException ex) {
            throw new RuntimeException("Couldn't find folder: " + key, ex);
        }
    }


    @Override
    public Optional<FolderListing> findFolderAt(String branchName, String path, long commitId) {
        LOG.debug("TEMP: findFolderAt: %s | %s | %020d", branchName, path, commitId);
        String maxRow = String.format("%s:%s:%020d", branchName, path, commitId);
        String rowLike = String.format("%s:%s:%%", branchName, path);
        try {
            PreparedStatement getStmt = conn.get().prepareStatement(
                    "SELECT contents FROM Folders WHERE id <= ? AND id LIKE ? ORDER BY id DESC LIMIT 1");
            getStmt.setString(1, maxRow);
            getStmt.setString(2, rowLike);
            ResultSet rs = getStmt.executeQuery();
            if (rs.next()) {
                byte[] contentsBytes = rs.getBytes("contents");
                return Optional.of(byteConv.folderFromBytes(contentsBytes));
            } else {
                return Optional.empty();
            }
        } catch (SQLException | IOException ex) {
            throw new RuntimeException("Couldn't find folder at: " + maxRow, ex);
        }
    }


    public FileEntry getFile(String key) {
        try {
            PreparedStatement getStmt = conn.get().prepareStatement(
                    "SELECT contents FROM Files WHERE id = ?");
            getStmt.setString(1, key);
            ResultSet rs = getStmt.executeQuery();
            rs.next();
            byte[] contentsBytes = rs.getBytes("contents");
            return byteConv.fileFromBytes(contentsBytes);
        } catch (SQLException | IOException ex) {
            throw new RuntimeException("Couldn't find file: " + key, ex);
        }
    }

    @Override
    public LocalFileState getFileState(String fileId) {
        try {
            PreparedStatement getStmt = conn.get().prepareStatement(
                    "SELECT entryState FROM Files WHERE id = ?");
            getStmt.setString(1, fileId);
            ResultSet rs = getStmt.executeQuery();
            rs.next();
            byte[] stateBytes = rs.getBytes("entryState");
            return localByteConv.fileStateFromBytes(stateBytes);
        } catch (SQLException | IOException ex) {
            throw new RuntimeException("Error loading file state for id: " + fileId, ex);
        }
    }

    @Override
    public void putFileState(String fileId, LocalFileState newState) {
        try {
            PreparedStatement stmt = conn.get().prepareStatement(
                    "UPDATE Files SET entryState = ? WHERE id = ?");
            byte[] stateBytes = localByteConv.fileStateToBytes(newState);
            stmt.setBytes(1, stateBytes);
            stmt.setString(2, fileId);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated != 1) {
                throw new IllegalStateException("Expected 1 row updated, got " + rowsUpdated);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error putting file state for id: " + fileId, ex);
        }
    }

    @Override
    public void putCommit(String key, CommitEntry commit) {
        try {
            byte[] commitBytes = byteConv.commitToBytes(commit);

            PreparedStatement stmt = conn.get().prepareStatement(
                    "INSERT INTO Commits (id, contents) VALUES (?, ?)");
            stmt.setString(1, key);
            stmt.setBytes(2, commitBytes);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated != 1) {
                throw new IllegalStateException("Expected 1 row inserted, got " + rowsUpdated);
            }
        } catch (SQLException | IOException ex) {
            throw new RuntimeException("Error putting commit: " + key, ex);
        }
    }

    @Override
    public void putFolder(String key, FolderListing folder) {
        LOG.info("Putting folder: %s", key);
        try {
            byte[] folderBytes = byteConv.folderToBytes(folder);

            PreparedStatement stmt = conn.get().prepareStatement(
                    "INSERT INTO Folders (id, contents) VALUES (?, ?)");
            stmt.setString(1, key);
            stmt.setBytes(2, folderBytes);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated != 1) {
                throw new IllegalStateException("Expected 1 row inserted, got " + rowsUpdated);
            }
        } catch (SQLException | IOException ex) {
            throw new RuntimeException("Error putting folder: " + key, ex);
        }
    }

    @Override
    public void putFile(String key, FileEntry file, LocalFileState state) {
        LOG.debug("Putting file: %s", key);
        try {
            byte[] fileBytes = byteConv.fileToBytes(file);

            PreparedStatement stmt = conn.get().prepareStatement(
                    "INSERT INTO Files (id, contents, entryState) VALUES (?, ?, ?)");
            stmt.setString(1, key);
            stmt.setBytes(2, fileBytes);
            stmt.setBytes(3, localByteConv.fileStateToBytes(state));

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated != 1) {
                throw new IllegalStateException("Expected 1 row inserted, got " + rowsUpdated);
            }
        } catch (SQLException | IOException ex) {
            throw new RuntimeException("Error putting file: " + key, ex);
        }
    }


    private static final int MAX_DIFF_SEARCH = 20;

    @Override
    public String resolveDiffsToString(final FileEntry file) {
        return new String(resolveDiffsToBytes(file), StandardCharsets.UTF_8);
    }

    @Override
    // TODO: Force writing a 'FULL' FileEntry when N diff entries are stacked in the history.
    public byte[] resolveDiffsToBytes(final FileEntry file) {
        if (file.getContentsType() == FileEntry.ContentsType.FULL) {
            return file.getContents().getRawBytes();
        }

        // TODO: Move this code somewhere common.
        if (file.getContentsType() == FileEntry.ContentsType.DIFF_DMP) {
            return resolveDmpDiffs(file);
        } else if (file.getContentsType() == FileEntry.ContentsType.DIFF_BS) {
            return resolveBsDiffs(file);
        } else {
            throw new IllegalStateException("Unexpected FileEntry type: " + file.getContentsType());
        }
    }

    // TODO Can we merge any of this with resolveDmpDiffs() ?
    private byte[] resolveBsDiffs(FileEntry file) {
        Preconditions.checkArgument(file.getContentsType() == FileEntry.ContentsType.DIFF_BS);
        ArrayList<byte[]> diffs = new ArrayList<>();
        diffs.add(file.getContents().getRawBytes());
        FileEntry current = file;
        for (int i = 0; i < MAX_DIFF_SEARCH; ++i) {
            FileEntry parent = getFile(current.getBaseEntryId().get());

            if (parent.getContentsType() == FileEntry.ContentsType.DIFF_BS) {
                diffs.add(0, parent.getContents().getRawBytes());
                current = parent;
                continue;

            } else if (parent.getContentsType() == FileEntry.ContentsType.FULL) {
                byte[] baseContents = parent.getContents().getRawBytes();
                for (byte[] diff : diffs) {
                    try {
                         baseContents = BsDiffer.patch(baseContents, diff);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return baseContents;

            } else {
                throw new IllegalStateException("Unexpected contents type: " + parent.getContentsType());
            }
        }
        throw new IllegalStateException("Couldn't resolve a diff after " + MAX_DIFF_SEARCH + " entries");
    }

    private byte[] resolveDmpDiffs(FileEntry file) {
        Preconditions.checkArgument(file.getContentsType() == FileEntry.ContentsType.DIFF_DMP);
        ArrayList<byte[]> diffs = new ArrayList<>();
        diffs.add(file.getContents().getRawBytes());
        FileEntry current = file;
        for (int i = 0; i < MAX_DIFF_SEARCH; ++i) {
            FileEntry parent = getFile(current.getBaseEntryId().get());
            if (parent.getContentsType() == FileEntry.ContentsType.DIFF_DMP) {
                diffs.add(0, parent.getContents().getRawBytes());
                current = parent;
                continue;

            } else if (parent.getContentsType() == FileEntry.ContentsType.FULL) {
                try {
                    // This is wasteful, but maybe we can avoid this wasted conversion when moving off of DMP.
                    return DmpDiffer.applyPatches(diffs, parent).getBytes(StandardCharsets.UTF_8);
                } catch (IOException ioEx) {
                    throw new RuntimeException(ioEx);
                }

            } else {
                throw new IllegalStateException("Unexpected contents type: " + parent.getContentsType());
            }
        }
        throw new IllegalStateException("Couldn't resolve a diff after " + MAX_DIFF_SEARCH + " entries");
    }


    @Override
    public List<FolderListing> getListingsSinceCommit(String branchName, String path, long commitIdExclusive) {
        // TODO: Sort out the relationship b/w the db impl and the row keyer. I think the keyer should be internal to
        //     this db impl.
        String rowMinimum = String.format("%s:%s:%020d", branchName, path, commitIdExclusive);
        String rowLike = String.format("%s:%s:%%", branchName, path);
        LOG.debug("TEMP: filters: %s / %s", rowMinimum, rowLike);
        try {
            PreparedStatement getStmt = conn.get().prepareStatement(
                    "SELECT id, contents FROM Folders WHERE id > ? AND id LIKE ?");
            getStmt.setString(1, rowMinimum);
            getStmt.setString(2, rowLike);
            ResultSet rs = getStmt.executeQuery();

            ArrayList<FolderListing> out = new ArrayList<>();
            while (rs.next()) {
                String id = rs.getString("id");
                byte[] contentsBytes = rs.getBytes("contents");
                FolderListing folder = byteConv.folderFromBytes(contentsBytes);
                if (!(folder.getCommitId() > commitIdExclusive)
                        || !folder.getPath().equals(path)) {
                    throw new AssertionError(
                            "SQL bug: Unexpected folder!: id: " + id +
                                    " / contents: " + folder.getDebugString());
                }
                out.add(folder);
            }
            return out;

        } catch (SQLException | IOException ex) {
            throw new RuntimeException("Error getting folder range for: " + path, ex);
        }
    }


    @Override
    public List<String> getCheckedOutPaths() {
        try {
            PreparedStatement getStmt = conn.get().prepareStatement(
                    "SELECT path FROM Checkouts");
            ResultSet rs = getStmt.executeQuery();

            ImmutableList.Builder<String> out = ImmutableList.builder();
            while (rs.next()) {
                out.add(rs.getString("path"));
            }
            return out.build();

        } catch (SQLException ex) {
            throw new RuntimeException("Error getting checked-out paths", ex);
        }
    }

    @Override
    public void addCheckedOutPath(String path) {
        try {
            PreparedStatement stmt = conn.get().prepareStatement(
                    "INSERT INTO Checkouts (path) VALUES (?)");
            stmt.setString(1, path);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated != 1) {
                throw new IllegalStateException("Expected 1 row inserted, got " + rowsUpdated);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error adding checked out path: " + path, ex);
        }
    }

    @Override
    public List<FolderListing> getAllBranchHeadsSince(String branchName, long baseCommitId) {
        try {
            String likePhrase = branchName + "%";
            PreparedStatement getStmt = conn.get().prepareStatement(
                    "SELECT contents FROM Folders WHERE id LIKE '" + likePhrase + "'");
            ResultSet rs = getStmt.executeQuery();

            HashMap<String, FolderListing> newestPerPath = new HashMap<>();
            while (rs.next()) {
                byte[] bytes = rs.getBytes("contents");
                FolderListing folder = byteConv.folderFromBytes(bytes);

                if (!newestPerPath.containsKey(folder.getPath())) {
                    newestPerPath.put(folder.getPath(), folder);

                } else if (newestPerPath.get(folder.getPath()).getCommitId() < folder.getCommitId()) {
                    newestPerPath.put(folder.getPath(), folder);
                }
            }

            return new ArrayList<>(newestPerPath.values());

        } catch (SQLException | IOException ex) {
            throw new RuntimeException("Error in getAllBranchHeadsSince", ex);
        }
    }

    @Override
    public Optional<FolderListing> getMostRecentListingForPath(
            long maxCommitId, String branchName, String path) {
        try {
            String likePhrase = String.format("%s:%s:%%", branchName, path);
            String maxCommitPhrase = String.format("%s:%s:%020d", branchName, path, maxCommitId);
            PreparedStatement getStmt = conn.get().prepareStatement("" +
                    "SELECT contents FROM Folders " +
                    "WHERE id LIKE '" + likePhrase + "' " +
                    "AND id <= '" + maxCommitPhrase + "' " +
                    "ORDER BY id DESC LIMIT 1");
            ResultSet rs = getStmt.executeQuery();

            if (!rs.next()) {
                return Optional.empty();
            } else {
                byte[] bytes = rs.getBytes("contents");
                FolderListing folder = byteConv.folderFromBytes(bytes);
                return Optional.of(folder);
            }
        } catch (SQLException | IOException ex) {
            throw new RuntimeException("Error in getAllBranchHeadsSince", ex);
        }
    }
}
