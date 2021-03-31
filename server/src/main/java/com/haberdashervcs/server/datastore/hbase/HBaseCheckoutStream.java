package com.haberdashervcs.server.datastore.hbase;

import java.util.HashMap;
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


    private final String rootPath;
    private final FolderListing rootListing;
    private final ImmutableMap<String, HdBytes> foundFiles;

    private HBaseCheckoutStream(String startingPath, FolderListing checkoutRoot, Map<String, HdBytes> foundFiles) {
        this.rootPath = startingPath;
        this.rootListing = checkoutRoot;
        this.foundFiles = ImmutableMap.copyOf(foundFiles);
    }

    @Override
    // TODO: Separate Iterator instance?
    public boolean hasNextFile() {
        return false;
    }

    @Override
    public CheckoutFile nextFile() {
        return null;
    }
}
