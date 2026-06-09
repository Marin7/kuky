package com.kuky.backend.scheduling.model;

import java.time.Instant;

public class Slot {

    public enum Status {
        OPEN, BOOKED, UNAVAILABLE
    }

    private final Instant start;
    private final Instant end;
    private final Status status;

    public Slot(Instant start, Instant end, Status status) {
        this.start = start;
        this.end = end;
        this.status = status;
    }

    public Instant getStart() { return start; }
    public Instant getEnd() { return end; }
    public Status getStatus() { return status; }
}
