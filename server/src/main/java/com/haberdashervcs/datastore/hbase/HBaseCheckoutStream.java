package com.haberdashervcs.datastore.hbase;

import com.haberdashervcs.operations.CheckoutStream;


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
