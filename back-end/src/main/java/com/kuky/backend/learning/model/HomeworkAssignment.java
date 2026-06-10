package com.kuky.backend.learning.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class HomeworkAssignment {

    private UUID id;
    private String title;
    private String instructions;
    private LocalDate dueOn; // nullable — optional due date
    private boolean published;
    private int sortOrder;
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public LocalDate getDueOn() { return dueOn; }
    public void setDueOn(LocalDate dueOn) { this.dueOn = dueOn; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
