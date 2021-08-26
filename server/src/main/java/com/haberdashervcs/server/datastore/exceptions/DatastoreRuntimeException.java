package com.haberdashervcs.server.datastore.exceptions;

import com.haberdashervcs.common.exceptions.HdRuntimeException;


public class DatastoreRuntimeException extends HdRuntimeException {

    public DatastoreRuntimeException(String message) {
        super(message);
    }
}
