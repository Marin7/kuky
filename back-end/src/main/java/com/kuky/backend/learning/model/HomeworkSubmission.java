package com.kuky.backend.learning.model;

import java.time.Instant;
import java.util.UUID;

public class HomeworkSubmission {

    private UUID id;
    private UUID userId;
    private UUID assignmentId;
    private String status; // one of HomeworkStatus
    private String responseText; // nullable — JSON-encoded List<FormattedTextSegment>
    private Integer scorePercent; // nullable — set only when status = GRADED
    private String feedback; // nullable — JSON-encoded List<FormattedTextSegment>, set once REVIEWED
    private Instant submittedAt; // nullable
    private Instant reviewedAt; // nullable — set when feedback is saved
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getAssignmentId() { return assignmentId; }
    public void setAssignmentId(UUID assignmentId) { this.assignmentId = assignmentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResponseText() { return responseText; }
    public void setResponseText(String responseText) { this.responseText = responseText; }
    public Integer getScorePercent() { return scorePercent; }
    public void setScorePercent(Integer scorePercent) { this.scorePercent = scorePercent; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
