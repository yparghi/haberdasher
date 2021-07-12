package com.haberdashervcs.client.commit;

import java.io.IOException;
import java.util.List;

import com.haberdashervcs.client.crawl.EntryComparisonThisFolder;
import com.haberdashervcs.client.crawl.LocalChangeHandler;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.common.diff.DmpDiffResult;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.HdFolderPath;


public class PrintChangeHandler implements LocalChangeHandler {

    private static final HdLogger LOG = HdLoggers.create(PrintChangeHandler.class);


    private final LocalDb db;

    PrintChangeHandler(LocalDb db) {
        this.db = db;
    }


    @Override
    public void handleComparisons(HdFolderPath path, List<EntryComparisonThisFolder> comparisons) throws IOException {
        for (EntryComparisonThisFolder comparison : comparisons) {
            LOG.info("Got comparison in %s: %s", path, comparison);

            if (comparison.isDiffableText()) {
                DmpDiffResult diffResult = comparison.generateDiffs(db);
                if (diffResult.getType() == DmpDiffResult.Type.DIFFERENT) {
                    LOG.info("Text diff: %s", diffResult.getPatches());
                }
            }
        }
    }
}
