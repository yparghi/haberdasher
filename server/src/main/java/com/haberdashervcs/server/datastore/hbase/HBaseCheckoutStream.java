package com.haberdashervcs.server.datastore.hbase;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.haberdashervcs.server.core.HdBytes;
import com.haberdashervcs.server.operations.CheckoutStream;
import com.haberdashervcs.server.operations.FolderListing;


class HBaseCheckoutStream implements CheckoutStream {

    static class Builder {

        private final String rootPath;
        private final FolderListing rootListing;
        private final HashMap<String, HdBytes> foundFiles;

        static Builder atRoot(String startingPath, FolderListing checkoutRoot) {
            return new Builder(startingPath, checkoutRoot);
        }

        private Builder(String startingPath, FolderListing checkoutRoot) {
            this.rootListing = checkoutRoot;
            this.rootPath = startingPath;
            this.foundFiles = new HashMap<>();
        }

        HBaseCheckoutStream build() {
            return new HBaseCheckoutStream(rootPath, rootListing, foundFiles);
        }

        Builder addFile(String path, HdBytes bytes) {
            Preconditions.checkArgument(!foundFiles.containsKey(path));
            foundFiles.put(path, bytes);
            return this;
        }
    }

    private static class CheckoutFileIterator implements Iterator<CheckoutFile> {

        private final ImmutableMap<String, HdBytes> foundFiles;
        private Iterator<Map.Entry<String, HdBytes>> foundFilesIterator;

        private CheckoutFileIterator(ImmutableMap<String, HdBytes> foundFiles) {
            this.foundFiles = foundFiles;
            this.foundFilesIterator = foundFiles.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return (foundFilesIterator.hasNext());
        }

        @Override
        public CheckoutFile next() {
            Map.Entry<String, HdBytes> nextEntry = foundFilesIterator.next();
            return CheckoutFile.of(nextEntry.getKey(), nextEntry.getValue());
        }
    }


    private final String rootPath;
    private final FolderListing rootListing;
    private final ImmutableMap<String, HdBytes> foundFiles;

    private HBaseCheckoutStream(String startingPath, FolderListing checkoutRoot, Map<String, HdBytes> foundFiles) {
        this.rootPath = startingPath;
        this.rootListing = checkoutRoot;
        this.foundFiles = ImmutableMap.copyOf(foundFiles);
    }

    @Override
    public Iterator<CheckoutFile> iterator() {
        return new CheckoutFileIterator(foundFiles);
    }
}
