package com.haberdashervcs.client.rebase;

import name.fraser.neil.plaintext.diff_match_patch;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


// NOTE: We should probably use another merge implementation! From my reading, DMP appears to
//     overwrite lines when applying a patch -- in other words, it's not sensitive to conflicts.
//     Maybe we should write our own diff3 impl.
//     Reference reading:
//     - https://stackoverflow.com/questions/1203725/three-way-merge-algorithms-for-text
//     - https://opensource.apple.com/source/gnudiff/gnudiff-10/diffutils/diff3.c.auto.html
//
// DMP merge reference: https://groups.google.com/g/diff-match-patch/c/6HK8QKgc5Pc
final class DmpMerger {

    static final class DmpMergeResult {
        private final String newText;
        private final boolean[] eachPatchResult;
        private final List<diff_match_patch.Patch> patches;

        DmpMergeResult(String newText, boolean[] eachPatchResult, List<diff_match_patch.Patch> patches) {
            this.newText = newText;
            this.eachPatchResult = eachPatchResult;
            this.patches = patches;
        }

        String getNewText() {
            return newText;
        }

        boolean[] getEachPatchResult() {
            return eachPatchResult;
        }

        boolean isCleanMerge() {
            for (boolean result : eachPatchResult) {
                if (!result) {
                    return false;
                }
            }
            return true;
        }

        List<diff_match_patch.Patch> getPatches() {
            return patches;
        }

        // TODO: This needs to be replaced with an actual merge-conflict format. Though maybe we'll
        //     need to throw out all of this -- DMP has some weird properties like using diff hunk
        //     ranges based on character numbers, not line numbers.
        String getMergedText() {
            ArrayList<diff_match_patch.Patch> failed = new ArrayList<>();
            for (int i = 0; i < patches.size(); ++i) {
                if (!eachPatchResult[i]) {
                    failed.add(patches.get(i));
                }
            }

            if (failed.size() == 0) {
                return newText;
            }

            StringBuilder out = new StringBuilder("====\nFailed patches:\n\n");
            for (diff_match_patch.Patch failedPatch : failed) {
                out.append(failedPatch.toString());
            }
            out.append("\n====\n\n");
            out.append(newText);
            return out.toString();
        }
    }


    DmpMergeResult mergeText(String original, String modified, String toApplyOnto) {
        diff_match_patch dmp = new diff_match_patch();
        // Very strict matching, because we want to be sensitive to merge conflicts
        dmp.Match_Threshold = 0.1f;

        LinkedList<diff_match_patch.Patch> diffs = dmp.patch_make(original, modified);
        Object[] result = dmp.patch_apply(diffs, toApplyOnto);
        String newText = (String) result[0];
        boolean[] eachPatchResult = (boolean[]) result[1];

        return new DmpMergeResult(newText, eachPatchResult, diffs);
    }
}
