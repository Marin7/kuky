package com.kuky.backend.scheduling.exception;

public class BookingNotAllowedException extends RuntimeException {

    public enum Reason {
        RANGE, LEAD, STATE, CUTOFF, NOT_ELIGIBLE_FOR_NO_SHOW
    }

    private final Reason reason;

    public BookingNotAllowedException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
