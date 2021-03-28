package com.haberdashervcs.data.change;

public final class ApplyChangesetResult {

    public enum Status {
        OK,
        FAILED
    }

    public static ApplyChangesetResult forStatus(Status status) {
        return new ApplyChangesetResult(status);
    }

    private final Status status;

    private ApplyChangesetResult(Status status) {
        this.status = status;
    }
}
