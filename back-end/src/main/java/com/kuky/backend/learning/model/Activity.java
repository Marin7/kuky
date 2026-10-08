package com.kuky.backend.learning.model;

import java.time.Instant;
import java.util.UUID;

/** Student work tied to one page of a unit's PDF (the page is the instructions). */
public class Activity {

    private UUID id;
    private UUID presentationId;
    private int page;
    private String title;
    private HomeworkFormat format = HomeworkFormat.MANUAL;
    private Instant createdAt;
    private Instant updatedAt;

    /** Title, or the "Actividad – página N" fallback when the teacher left it blank. */
    public String displayTitle() {
        return title == null || title.isBlank() ? "Actividad – página " + page : title;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPresentationId() { return presentationId; }
    public void setPresentationId(UUID presentationId) { this.presentationId = presentationId; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public HomeworkFormat getFormat() { return format; }
    public void setFormat(HomeworkFormat format) { this.format = format; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
