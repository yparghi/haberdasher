package com.haberdashervcs.server.browser;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import junit.framework.TestCase;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.junit.Test;


// These tests aren't quite necessary because they just verify the output of JGit's HistogramDiff.
// But I like having them to spell out the interpretation of range numbers.
public class HistogramDifferTest extends TestCase {

    private static final Joiner LINE_JOINER = Joiner.on('\n');


    @Test
    public void testAddLine() throws Exception {
        HistogramDiffer differ = new HistogramDiffer();
        String original = LINE_JOINER.join(
                "apple",
                "banana");
        String modified = LINE_JOINER.join(
                "apple",
                "banana",
                "cantaloupe");

        EditList editsResult = differ.diff(original, modified);
        assertEquals(1, editsResult.size());
        // Edit line ranges start at 0. In original, line 1 is replaced with a new line 1 and line
        //     2. So the old range [1,2) is replaced by a new range [1,3).
        Edit expected = new Edit(1, 2, 1, 3);
        assertEquals(ImmutableList.of(expected), editsResult);

        List<LineDiff> expectedDiff = ImmutableList.of(
                LineDiff.forSame("apple"),
                // TODO! How do we handle whitespace differences? Do we care, for display purposes?
                LineDiff.forDiff(Optional.of("banana"), Optional.of("banana")),
                LineDiff.forDiff(Optional.empty(), Optional.of("cantaloupe")));
        assertEquals(expectedDiff, differ.toLineDiffs(original, modified, editsResult));
    }


    @Test
    public void testAddLinesInDifferentPlaces() throws Exception {
        HistogramDiffer differ = new HistogramDiffer();
        String original = LINE_JOINER.join(
                "apple",
                "banana",
                "cantaloupe",
                "denver",
                "elephant");
        String modified = LINE_JOINER.join(
                "apple",
                "banana",
                "xxx",
                "cantaloupe",
                "denver",
                "yyy",
                "elephant");

        EditList editsResult = differ.diff(original, modified);
        List<Edit> expected = ImmutableList.of(
                new Edit(2, 2, 2, 3),  // Insert at line 2 (so the range in original is empty)
                new Edit(4, 4, 5, 6)); // Insert at line 4
        assertEquals(expected, editsResult);

        List<LineDiff> expectedDiff = ImmutableList.of(
                LineDiff.forSame("apple"),
                LineDiff.forSame("banana"),
                LineDiff.forAdded("xxx"),
                LineDiff.forSame("cantaloupe"),
                LineDiff.forSame("denver"),
                LineDiff.forAdded("yyy"),
                LineDiff.forSame("elephant")
        );
        assertEquals(expectedDiff, differ.toLineDiffs(original, modified, editsResult));
    }

}