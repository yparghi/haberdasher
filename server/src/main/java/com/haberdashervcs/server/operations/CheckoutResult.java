package com.haberdashervcs.server.operations;

import com.google.common.base.Preconditions;

import java.util.Optional;


public final class CheckoutResult {

    public enum Status {
        OK,
        FAILED
    }

    public static CheckoutResult failed(String errorMessage) {
        return new CheckoutResult(Status.FAILED, null, errorMessage);
    }

    public static CheckoutResult forStream(CheckoutStream stream) {
        return new CheckoutResult(Status.OK, stream, null);
    }


    private final Status status;
    private final Optional<CheckoutStream> stream;
    private final Optional<String> errorMessage;

    private CheckoutResult(Status status, CheckoutStream stream, String errorMessage) {
        this.status = Preconditions.checkNotNull(status);
        this.stream = Optional.ofNullable(stream);
        this.errorMessage = Optional.ofNullable(errorMessage);
    }

    public CheckoutStream getStream() {
        Preconditions.checkState(status == Status.OK && stream.isPresent());
        return stream.get();
    }
}
