package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;

import com.google.protobuf.ByteString;
import com.haberdashervcs.common.io.HdBytes;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.protobuf.CommitsProto;
import com.haberdashervcs.common.protobuf.FilesProto;
import com.haberdashervcs.common.protobuf.FoldersProto;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;


/**
 * Easy methods for HBase gets and puts.
 */
final class HBaseRawHelper {

    static HBaseRawHelper forConnection(Connection conn) {
        return new HBaseRawHelper(conn);
    }


    private final Connection conn;

    private HBaseRawHelper(Connection conn) {
        this.conn = conn;
    }

    CommitEntry getCommit(String rowKey) throws IOException {
        final Table commitsTable = conn.getTable(TableName.valueOf("Commits"));
        final String columnFamilyName = "cfMain";

        Get get = new Get(Bytes.toBytes(rowKey));
        Result result = commitsTable.get(get);
        byte[] commitEntryBytes = result.getValue(
                Bytes.toBytes(columnFamilyName), Bytes.toBytes("entry"));

        return CommitEntry.fromBytes(commitEntryBytes);
    }

    FileEntry getFile(String fileId) throws IOException {
        final Table filesTable = conn.getTable(TableName.valueOf("Files"));
        final String columnFamilyName = "cfMain";

        Get get = new Get(Bytes.toBytes(fileId));
        Result result = filesTable.get(get);
        byte[] fileValue = result.getValue(
                Bytes.toBytes(columnFamilyName), Bytes.toBytes("contents"));

        return FileEntry.fromBytes(fileId, HdBytes.of(fileValue));
    }


    FolderListing getFolder(String folderId) throws IOException {
        final Table foldersTable = conn.getTable(TableName.valueOf("Folders"));
        final String columnFamilyName = "cfMain";

        final String rowKey = folderId; // TODO commits/refs in the row key?

        Get get = new Get(Bytes.toBytes(rowKey));
        Result result = foldersTable.get(get);
        byte[] folderValue = result.getValue(
                Bytes.toBytes(columnFamilyName), Bytes.toBytes("listing"));

        return FolderListing.fromBytes(folderValue);
    }


    String putFileAdd(final String fileId, FileEntry fileEntry) throws IOException {
        final Table filesTable = conn.getTable(TableName.valueOf("Files"));
        final String columnFamilyName = "cfMain";
        final String columnName = "contents";

        final String rowKey = fileId;

        // TODO compress the commit id and other string fields to bytes? Is that necessary?
        FilesProto.FileEntry fileProto = FilesProto.FileEntry.newBuilder()
                .setContents(ByteString.copyFrom(fileEntry.getContents().getRawBytes()))
                .setChangeType(FilesProto.ChangeType.ADD)
                .build();

        Put put = new Put(Bytes.toBytes(rowKey));
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                fileProto.toByteArray());
        filesTable.put(put);

        return fileId;
    }

    FoldersProto.FolderListing convertFolderToProto(FolderListing folderListing) {
        FoldersProto.FolderListing.Builder out = FoldersProto.FolderListing.newBuilder();

        for (FolderListing.FolderEntry entry : folderListing.getEntries()) {
            FoldersProto.FolderListingEntry protoEntry = FoldersProto.FolderListingEntry.newBuilder()
                    .setFileId(entry.getFileId())
                    .setName(entry.getName())
                    .setType(entry.getType() == FolderListing.FolderEntry.Type.FILE
                            ? FoldersProto.FolderListingEntry.Type.FILE
                            : FoldersProto.FolderListingEntry.Type.FOLDER)
                    .build();
            out.addEntries(protoEntry);
        }

        return out.build();
    }

    String putFolder(final String folderId, FolderListing folderListing) throws IOException {
        final Table filesTable = conn.getTable(TableName.valueOf("Folders"));
        final String columnFamilyName = "cfMain";
        final String columnName = "listing";

        final String rowKey = folderId;

        // TODO compress the commit id and other string fields to bytes? Is that necessary?
        FoldersProto.FolderListing folderProto = convertFolderToProto(folderListing);

        Put put = new Put(Bytes.toBytes(rowKey));
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                folderProto.toByteArray());
        filesTable.put(put);

        return folderId;
    }

    void putCommit(String commitId, String rootFolderId) throws IOException {
        final Table commitsTable = conn.getTable(TableName.valueOf("Commits"));
        final String columnFamilyName = "cfMain";
        final String columnName = "entry";

        final String rowKey = commitId;

        CommitsProto.CommitEntry commitProto = CommitsProto.CommitEntry.newBuilder()
                .setRootFolderId(rootFolderId)
                .build();

        Put put = new Put(Bytes.toBytes(rowKey));
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                commitProto.toByteArray());
        commitsTable.put(put);
    }
}
