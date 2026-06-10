package com.kuky.backend.learning.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class PastClass {

    private UUID id;
    private String title;
    private LocalDate heldOn;
    private String teacherNote;
    private boolean published;
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getHeldOn() { return heldOn; }
    public void setHeldOn(LocalDate heldOn) { this.heldOn = heldOn; }
    public String getTeacherNote() { return teacherNote; }
    public void setTeacherNote(String teacherNote) { this.teacherNote = teacherNote; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
