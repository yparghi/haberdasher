package com.haberdashervcs.server.operations;

import com.haberdashervcs.server.core.HdBytes;


public interface CheckoutStream {

    final class CheckoutFile {
        private final String path;
        private final HdBytes contents;

        private CheckoutFile(String path, HdBytes contents) {
            this.path = path;
            this.contents = contents;
        }

        public String getPath() {
            return path;
        }

        public HdBytes getContents() {
            return contents;
        }
    }


    boolean hasNextFile();

    CheckoutFile nextFile();
}
