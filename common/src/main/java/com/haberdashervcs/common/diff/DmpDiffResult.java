package com.haberdashervcs.common.diff;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.haberdashervcs.common.protobuf.FilesProto;
import name.fraser.neil.plaintext.diff_match_patch;


public class DmpDiffResult {
    public enum Type {
        SAME,
        DIFFERENT
    }

    private final Type type;
    private final List<diff_match_patch.Patch> patches;

    DmpDiffResult(Type type, List<diff_match_patch.Patch> patches) {
        this.type = type;

        if (type == Type.SAME) {
            Preconditions.checkArgument(patches.size() == 0);
        } else {
            Preconditions.checkArgument(patches.size() > 0);
        }
        this.patches = patches;
    }

    public Type getType() {
        return type;
    }

    public byte[] getPatchesAsBytes() {
        Preconditions.checkState(type == Type.DIFFERENT);
        return patchesToBytes(patches);
    }

    public List<diff_match_patch.Patch> getPatches() {
        Preconditions.checkState(type == Type.DIFFERENT);
        return patches;
    }

    // NOTE: This assumes protobuf encoding -- maybe we might want to apply the ByteConverter abstraction here as well,
    //     but for now I'm fine with just treating diff conversions as a black box returning bytes.
    private byte[] patchesToBytes(List<diff_match_patch.Patch> patches) {
        FilesProto.DmpDiffContents.Builder out = FilesProto.DmpDiffContents.newBuilder();
        for (diff_match_patch.Patch patch : patches) {
            out.addPatches(ByteString.copyFrom(patch.toString().getBytes(StandardCharsets.UTF_8)));
        }
        return out.build().toByteArray();
    }
}
