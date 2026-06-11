package com.kuky.backend.presentations.model;

import java.util.UUID;

public class Slide {
    private UUID id;
    private UUID presentationId;
    private String heading;
    private String body;
    private UUID imageId;
    private int sortOrder;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPresentationId() { return presentationId; }
    public void setPresentationId(UUID presentationId) { this.presentationId = presentationId; }
    public String getHeading() { return heading; }
    public void setHeading(String heading) { this.heading = heading; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public UUID getImageId() { return imageId; }
    public void setImageId(UUID imageId) { this.imageId = imageId; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
