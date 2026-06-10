package com.kuky.backend.learning.model;

import java.time.Instant;
import java.util.UUID;

public class PresentationBlock {

    private UUID id;
    private String heading;
    private String body;
    private boolean published;
    private int sortOrder;
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getHeading() { return heading; }
    public void setHeading(String heading) { this.heading = heading; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
