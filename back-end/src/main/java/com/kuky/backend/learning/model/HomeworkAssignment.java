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
    private HomeworkType homeworkType; // nullable
    private HomeworkLevel level;       // nullable
    private HomeworkFormat format = HomeworkFormat.MANUAL;
    private String audioUrl;           // nullable — listening homework external source
    private UUID audioFileId;          // nullable — listening homework uploaded file

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
    public HomeworkType getHomeworkType() { return homeworkType; }
    public void setHomeworkType(HomeworkType homeworkType) { this.homeworkType = homeworkType; }
    public HomeworkLevel getLevel() { return level; }
    public void setLevel(HomeworkLevel level) { this.level = level; }
    public HomeworkFormat getFormat() { return format; }
    public void setFormat(HomeworkFormat format) { this.format = format; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public UUID getAudioFileId() { return audioFileId; }
    public void setAudioFileId(UUID audioFileId) { this.audioFileId = audioFileId; }
}
