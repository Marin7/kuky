package com.kuky.backend.learning.model;

import java.time.Instant;
import java.util.UUID;

public class Activity {

    private UUID id;
    private UUID presentationId;
    private String title;
    private HomeworkFormat format = HomeworkFormat.MANUAL;
    private String level;
    private String homeworkType;
    private int position;
    private UUID triggerFileId;
    private Integer triggerPage;
    private String instructionsText = "";
    private String youtubeUrl;
    private UUID imageId;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPresentationId() { return presentationId; }
    public void setPresentationId(UUID presentationId) { this.presentationId = presentationId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public HomeworkFormat getFormat() { return format; }
    public void setFormat(HomeworkFormat format) { this.format = format; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getHomeworkType() { return homeworkType; }
    public void setHomeworkType(String homeworkType) { this.homeworkType = homeworkType; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public UUID getTriggerFileId() { return triggerFileId; }
    public void setTriggerFileId(UUID triggerFileId) { this.triggerFileId = triggerFileId; }
    public Integer getTriggerPage() { return triggerPage; }
    public void setTriggerPage(Integer triggerPage) { this.triggerPage = triggerPage; }
    public String getInstructionsText() { return instructionsText; }
    public void setInstructionsText(String instructionsText) { this.instructionsText = instructionsText; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }
    public UUID getImageId() { return imageId; }
    public void setImageId(UUID imageId) { this.imageId = imageId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
