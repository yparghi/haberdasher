package com.haberdashervcs.client.rebase;

import com.google.common.base.Joiner;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class DmpMergerTest {

    private static final Joiner LINE_JOIN = Joiner.on('\n');


    @Test
    public void additiveMerge() throws Exception {
        String original = LINE_JOIN.join(
                "some",
                "text"
        );
        String modified = LINE_JOIN.join(
                "some",
                "text",
                "with new line"
        );
        String toApplyOnto = LINE_JOIN.join(
                "additions",
                "to top of file",
                "some",
                "text"
        );

        DmpMerger merger = new DmpMerger();
        DmpMerger.DmpMergeResult result = merger.mergeText(original, modified, toApplyOnto);
        System.out.println("TEMP: Got patches: " + result.getPatches());

        String expected = LINE_JOIN.join(
                "additions",
                "to top of file",
                "some",
                "text",
                "with new line"
        );
        assertEquals(expected, result.getNewText());
        assertArrayEquals(new boolean[]{true}, result.getEachPatchResult());
        assertEquals(expected, result.getMergedText());
    }


    @Test
    public void conflictingMerge() throws Exception {
        String original = LINE_JOIN.join(
                "some",
                "text",
                "original"
        );
        String modified = LINE_JOIN.join(
                "some",
                "text",
                "modified"
        );
        String toApplyOnto = LINE_JOIN.join(
                "some",
                "text",
                "toApplyOnto"
        );

        DmpMerger merger = new DmpMerger();
        DmpMerger.DmpMergeResult result = merger.mergeText(original, modified, toApplyOnto);
        System.out.println("TEMP: Got patches: " + result.getPatches());

        assertEquals(toApplyOnto, result.getNewText());
        assertArrayEquals(new boolean[]{false}, result.getEachPatchResult());

        String conflictText = LINE_JOIN.join(
                "====",
                "Failed patches:",
                "",
                "@@ -7,12 +7,12 @@",
                " ext%0A",
                "-original",
                "+modified",
                "",
                "====",
                "",
                "some",
                "text",
                "toApplyOnto"
        );
        assertEquals(conflictText, result.getMergedText());
    }
}
