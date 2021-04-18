package com.haberdashervcs.server.datastore.hbase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.protobuf.ByteString;
import com.haberdashervcs.common.io.HdObjectByteConverter;
import com.haberdashervcs.common.io.ProtobufObjectByteConverter;
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
public final class HBaseRawHelper {

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


    void putFileAdd(final byte[] rowKey, FileEntry fileEntry) throws IOException {
        final Table filesTable = conn.getTable(TableName.valueOf("Files"));
        final String columnFamilyName = "cfMain";
        final String columnName = "contents";

        // TODO compress the commit id and other string fields to bytes? Is that necessary?
        FilesProto.FileEntry fileProto = FilesProto.FileEntry.newBuilder()
                .setContents(ByteString.copyFrom(fileEntry.getContents().getRawBytes()))
                .setChangeType(FilesProto.ChangeType.ADD)
                .build();

        Put put = new Put(rowKey);
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                fileProto.toByteArray());
        filesTable.put(put);
    }

    FoldersProto.FolderListing convertFolderToProto(FolderListing folderListing) {
        FoldersProto.FolderListing.Builder out = FoldersProto.FolderListing.newBuilder();

        for (FolderListing.FolderEntry entry : folderListing.getEntries()) {
            FoldersProto.FolderListingEntry protoEntry = FoldersProto.FolderListingEntry.newBuilder()
                    .setFileId(entry.getId())
                    .setName(entry.getName())
                    .setType(entry.getType() == FolderListing.FolderEntry.Type.FILE
                            ? FoldersProto.FolderListingEntry.Type.FILE
                            : FoldersProto.FolderListingEntry.Type.FOLDER)
                    .build();
            out.addEntries(protoEntry);
        }

        return out.build();
    }

    void putFolder(final byte[] rowKey, FolderListing folderListing) throws IOException {
        final Table filesTable = conn.getTable(TableName.valueOf("Folders"));
        final String columnFamilyName = "cfMain";
        final String columnName = "listing";

        // TODO compress the commit id and other string fields to bytes? Is that necessary?
        FoldersProto.FolderListing folderProto = convertFolderToProto(folderListing);

        Put put = new Put(rowKey);
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                folderProto.toByteArray());
        filesTable.put(put);
    }

    void putCommit(final byte[] rowKey, String rootFolderId) throws IOException {
        final Table commitsTable = conn.getTable(TableName.valueOf("Commits"));
        final String columnFamilyName = "cfMain";
        final String columnName = "entry";

        CommitsProto.CommitEntry commitProto = CommitsProto.CommitEntry.newBuilder()
                .setRootFolderId(rootFolderId)
                .build();

        Put put = new Put(rowKey);
        put.addColumn(
                Bytes.toBytes(columnFamilyName),
                Bytes.toBytes(columnName),
                commitProto.toByteArray());
        commitsTable.put(put);
    }

    public FilesProto.FileEntry fileEntryForText(String text, FilesProto.ChangeType changeType) {
        return FilesProto.FileEntry.newBuilder()
                .setChangeType(changeType)
                .setContents(ByteString.copyFrom(text, StandardCharsets.UTF_8))
                .build();
    }

}
