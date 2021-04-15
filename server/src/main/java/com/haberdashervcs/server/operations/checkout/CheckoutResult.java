package com.haberdashervcs.server.operations.checkout;

import com.google.common.base.Preconditions;

import java.util.Optional;


public final class CheckoutResult {

    public enum Status {
        OK,
        FAILED
    }

    public static CheckoutResult failed(String errorMessage) {
        return new CheckoutResult(Status.FAILED, errorMessage);
    }

    public static CheckoutResult ok() {
        return new CheckoutResult(Status.OK, null);
    }


    private final Status status;
    private final Optional<String> errorMessage;

    private CheckoutResult(Status status, String errorMessage) {
        this.status = Preconditions.checkNotNull(status);
        this.errorMessage = Optional.ofNullable(errorMessage);
    }

    public Status getStatus() {
        return status;
    }

    public String getErrorMessage() {
        Preconditions.checkState(status == Status.FAILED);
        return errorMessage.get();
    }
}
