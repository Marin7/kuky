package com.kuky.backend.learning.model;

import java.time.Instant;
import java.util.UUID;

/** Student work tied to one page of a unit's PDF (the page is the instructions); it has no title of its own. */
public class Activity {

    /** How an activity is named wherever it needs a name. */
    public static String displayName(int page) {
        return "Actividad – página " + page;
    }

    private UUID id;
    private UUID presentationId;
    private int page;
    private HomeworkFormat format = HomeworkFormat.MANUAL;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPresentationId() { return presentationId; }
    public void setPresentationId(UUID presentationId) { this.presentationId = presentationId; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public HomeworkFormat getFormat() { return format; }
    public void setFormat(HomeworkFormat format) { this.format = format; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
