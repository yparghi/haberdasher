package com.haberdashervcs.server.browser;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import junit.framework.TestCase;
import org.junit.Test;


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

        List<LineDiff> expectedDiff = ImmutableList.of(
                LineDiff.forSame("apple"),
                // TODO! How do we handle whitespace differences? Do we care, for display purposes?
                LineDiff.forDiff(Optional.of("banana"), Optional.of("banana")),
                LineDiff.forDiff(Optional.empty(), Optional.of("cantaloupe")));
        assertEquals(expectedDiff, differ.toLineDiffs(original, modified));
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

        List<LineDiff> expectedDiff = ImmutableList.of(
                LineDiff.forSame("apple"),
                LineDiff.forSame("banana"),
                LineDiff.forAdded("xxx"),
                LineDiff.forSame("cantaloupe"),
                LineDiff.forSame("denver"),
                LineDiff.forAdded("yyy"),
                LineDiff.forSame("elephant")
        );
        assertEquals(expectedDiff, differ.toLineDiffs(original, modified));
    }

}