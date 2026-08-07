package com.kuky.backend.learning.model;

import java.time.Instant;
import java.util.UUID;

public class ActivityInstructionsFile {

    private UUID id;
    private UUID activityId;
    private String originalName;
    private String contentType;
    private long byteSize;
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getActivityId() { return activityId; }
    public void setActivityId(UUID activityId) { this.activityId = activityId; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getByteSize() { return byteSize; }
    public void setByteSize(long byteSize) { this.byteSize = byteSize; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
