package com.haberdashervcs.common.diff;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.protobuf.FilesProto;
import name.fraser.neil.plaintext.diff_match_patch;


// TODO: If I switch away from DmpMerger to diff3 (or something), should I also port this class?
public class DmpDiffer {

    private static final HdLogger LOG = HdLoggers.create(DmpDiffer.class);


    // TODO Tests
    public static String applyPatches(List<byte[]> patches, FileEntry base) throws IOException {
        Preconditions.checkArgument(base.getContentsType() == FileEntry.ContentsType.FULL);

        diff_match_patch patcher = new diff_match_patch();
        String result = new String(base.getContents().getRawBytes(), StandardCharsets.UTF_8);

        // TODO what about non-utf8?
        for (byte[] patch : patches) {
            FilesProto.DmpDiffContents proto = FilesProto.DmpDiffContents.parseFrom(patch);
            List<String> patchesThisEntry = proto.getPatchesList()
                    .stream()
                    .map((bs) -> new String(bs.toByteArray(), StandardCharsets.UTF_8))
                    .collect(Collectors.toList());
            for (String patchStr : patchesThisEntry) {
                LinkedList<diff_match_patch.Patch> patchesFromStr = (LinkedList)patcher.patch_fromText(patchStr);
                result = (String)(patcher.patch_apply(patchesFromStr, result)[0]);
            }
        }
        return result;
    }


    private final String left;
    private final String right;

    public DmpDiffer(String left, String right) {
        this.left = left;
        this.right = right;
    }

    public DmpDiffResult compare() {
        diff_match_patch matcher = new diff_match_patch();
        LinkedList<diff_match_patch.Patch> patches = matcher.patch_make(left, right);

        if (patches.isEmpty()) {
            return new DmpDiffResult(DmpDiffResult.Type.SAME, patches);
        } else {
            return new DmpDiffResult(DmpDiffResult.Type.DIFFERENT, patches);
        }
    }

}
