package com.kuky.backend.testimonials.model;

import java.time.Instant;
import java.util.UUID;

public class Testimonial {
    private UUID id;
    private UUID userId;
    private String studentName;
    private String text;
    private TestimonialStatus status;
    private int displayOrder;
    private Instant submittedAt;
    private Instant reviewedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public TestimonialStatus getStatus() { return status; }
    public void setStatus(TestimonialStatus status) { this.status = status; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
}
