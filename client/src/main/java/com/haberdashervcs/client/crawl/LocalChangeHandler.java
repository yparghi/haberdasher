package com.haberdashervcs.client.crawl;

import java.io.IOException;
import java.util.List;

import com.haberdashervcs.common.objects.HdFolderPath;


public interface LocalChangeHandler {

    void handleComparisons(HdFolderPath path, List<EntryComparisonThisFolder> comparisons) throws IOException;
}
