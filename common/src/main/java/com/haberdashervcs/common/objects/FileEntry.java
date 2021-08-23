package com.haberdashervcs.common.objects;

import java.util.Optional;
import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.haberdashervcs.common.io.HdBytes;


public class FileEntry {

    public static FileEntry forNewContents(String id, byte[] fullContents) {
        return new FileEntry(id, ContentsType.FULL, fullContents, null);
    }

    public static FileEntry forDiffDmp(String id, byte[] diffContents, String baseEntryId) {
        return new FileEntry(id, ContentsType.DIFF_DMP, diffContents, baseEntryId);
    }

    public static FileEntry forDiffBs(String id, byte[] diffContents, String baseEntryId) {
        return new FileEntry(id, ContentsType.DIFF_BS, diffContents, baseEntryId);
    }

    public static FileEntry forNewContents(byte[] bytes) {
        throw new UnsupportedOperationException("TEMPBUILD");
    }

    public enum ContentsType {
        FULL,
        DIFF_DMP,
        DIFF_BS
    }


    private final String id;
    private final ContentsType contentsType;
    private final HdBytes contents;
    private final @Nullable String baseEntryId;

    private FileEntry(String id, ContentsType contentsType, byte[] contents, String baseEntryId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(id));
        this.id = id;
        this.contentsType = Preconditions.checkNotNull(contentsType);
        this.contents = HdBytes.of(contents);
        this.baseEntryId = baseEntryId;
    }

    public String getId() {
        return id;
    }

    public HdBytes getContents() {
        return contents;
    }

    public ContentsType getContentsType() {
        return contentsType;
    }

    public Optional<String> getBaseEntryId() {
        return Optional.ofNullable(baseEntryId);
    }

    public String getDebugString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("contentsType", contentsType)
                .add("baseEntryId", baseEntryId)
                .toString();
    }
}
