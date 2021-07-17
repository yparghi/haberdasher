package com.haberdashervcs.server.browser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.eclipse.jgit.diff.Edit;
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


    public List<LineDiff> toLineDiffs(String original, String modified, EditList edits) {
        List<String> originalLines = original.lines().collect(Collectors.toUnmodifiableList());
        List<String> modifiedLines = modified.lines().collect(Collectors.toUnmodifiableList());
        List<LineDiff> out = new ArrayList<>();

        int editIdx = 0;
        Edit nextEdit = null;

        int originalLineIdx = 0;
        while (originalLineIdx < originalLines.size()) {
            if (nextEdit == null && editIdx < edits.size()) {
                nextEdit = edits.get(editIdx);
                ++editIdx;
            }
            if (nextEdit == null || nextEdit.getBeginA() > originalLineIdx) {
                out.add(LineDiff.forSame(originalLines.get(originalLineIdx)));
                ++originalLineIdx;
                continue;
            }

            if (nextEdit.getType() == Edit.Type.INSERT) {
                for (int modifiedIdx = nextEdit.getBeginB(); modifiedIdx < nextEdit.getEndB(); ++modifiedIdx) {
                    out.add(LineDiff.forAdded(modifiedLines.get(modifiedIdx)));
                }

            } else if (nextEdit.getType() == Edit.Type.DELETE) {
                for (int originalIdx = nextEdit.getBeginA(); originalIdx < nextEdit.getEndA(); ++originalIdx) {
                    out.add(LineDiff.forDeleted(originalLines.get(originalIdx)));
                }

            } else if (nextEdit.getType() == Edit.Type.REPLACE) {
                int originalIdx = nextEdit.getBeginA();
                int modifiedIdx = nextEdit.getBeginB();
                while (originalIdx < nextEdit.getEndA() || modifiedIdx < nextEdit.getEndB()) {

                    if (originalIdx < nextEdit.getEndA() && modifiedIdx < nextEdit.getEndB()) {
                        out.add(LineDiff.forDiff(
                                Optional.of(originalLines.get(originalIdx)),
                                Optional.of(modifiedLines.get(modifiedIdx))));
                        ++originalIdx;
                        ++modifiedIdx;

                    } else if (originalIdx < nextEdit.getEndA()) {
                        out.add(LineDiff.forDiff(
                                Optional.of(originalLines.get(originalIdx)),
                                Optional.empty()));
                        ++originalIdx;

                    } else if (modifiedIdx < nextEdit.getEndB()) {
                        out.add(LineDiff.forDiff(
                                Optional.empty(),
                                Optional.of(modifiedLines.get(modifiedIdx))));
                        ++modifiedIdx;
                    }
                }
            }

            originalLineIdx = nextEdit.getEndA();
            nextEdit = null;
        }

        return ImmutableList.copyOf(out);
    }

}
