package com.haberdashervcs.server.datastore.hbase;

import com.haberdashervcs.server.operations.CheckoutStream;


class HBaseCheckoutStream implements CheckoutStream {

    @Override
    public boolean hasNextFile() {
        return false;
    }

    @Override
    public CheckoutFile nextFile() {
        return null;
    }
}
