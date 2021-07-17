package com.haberdashervcs.server.browser;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.diff.SequenceComparator;


public final class HistogramDiffer {

    public EditList diff(String original, String modified) {
        RawText originalRT = new RawText(original.getBytes(StandardCharsets.UTF_8));
        RawText modifiedRT = new RawText(modified.getBytes(StandardCharsets.UTF_8));
        SequenceComparator<RawText> comp = RawTextComparator.DEFAULT;
        HistogramDiff differ = new HistogramDiff();

        EditList result = differ.diff(comp, originalRT, modifiedRT);
        return result;
    }

    public FileDiff toFileDiff(String original, String modified, EditList edits) {
        List<String> originalLines = original.lines().collect(Collectors.toUnmodifiableList());
        List<String> modifiedLines = modified.lines().collect(Collectors.toUnmodifiableList());

    }
}
