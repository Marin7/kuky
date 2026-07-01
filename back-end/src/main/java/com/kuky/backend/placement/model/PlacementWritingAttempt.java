package com.kuky.backend.placement.model;

import java.time.Instant;
import java.util.UUID;

/** Timing for one Writing attempt: start/deadline lifecycle, mirrors PlacementAttemptSection. */
public class PlacementWritingAttempt {

    private UUID id;
    private UUID userId;
    private Instant startedAt;
    private Instant deadlineAt;
    private Instant submittedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(Instant deadlineAt) { this.deadlineAt = deadlineAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public boolean isSubmitted() { return submittedAt != null; }
}
