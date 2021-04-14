package com.haberdashervcs.server.operations.checkout;

import com.haberdashervcs.common.io.HdBytes;


public interface CheckoutStream extends Iterable<CheckoutStream.CheckoutFile> {

    final class CheckoutFile {

        public static CheckoutFile of(String path, HdBytes contents) {
            return new CheckoutFile(path, contents);
        }


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
}
