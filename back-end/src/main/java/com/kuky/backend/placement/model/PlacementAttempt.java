package com.kuky.backend.placement.model;

import java.time.Instant;
import java.util.UUID;

public class PlacementAttempt {

    private UUID id;
    private UUID userId;
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;
    private Instant startedAt;
    private Instant completedAt;
    private String overallCefr; // "A0".."C2", nullable until completed

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public AttemptStatus getStatus() { return status; }
    public void setStatus(AttemptStatus status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getOverallCefr() { return overallCefr; }
    public void setOverallCefr(String overallCefr) { this.overallCefr = overallCefr; }
}
