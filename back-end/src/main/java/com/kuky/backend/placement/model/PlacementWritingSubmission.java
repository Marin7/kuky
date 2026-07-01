package com.kuky.backend.placement.model;

import java.time.Instant;
import java.util.UUID;

public class PlacementWritingSubmission {

    private UUID id;
    private UUID userId;
    private UUID writingAttemptId;
    private String body;
    private String promptSnapshot;
    private Instant submittedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getWritingAttemptId() { return writingAttemptId; }
    public void setWritingAttemptId(UUID writingAttemptId) { this.writingAttemptId = writingAttemptId; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getPromptSnapshot() { return promptSnapshot; }
    public void setPromptSnapshot(String promptSnapshot) { this.promptSnapshot = promptSnapshot; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}
