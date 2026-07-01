package com.kuky.backend.placement.model;

import java.time.Instant;
import java.util.UUID;

/** Per-section timing + result row. Created when the section is started. */
public class PlacementAttemptSection {

    private UUID id;
    private UUID attemptId;
    private Skill skill;
    private Instant startedAt;
    private Instant deadlineAt;
    private Instant submittedAt;
    private Integer scorePercent;
    private String cefrLevel; // "A0".."C2", nullable until submitted

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAttemptId() { return attemptId; }
    public void setAttemptId(UUID attemptId) { this.attemptId = attemptId; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(Instant deadlineAt) { this.deadlineAt = deadlineAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Integer getScorePercent() { return scorePercent; }
    public void setScorePercent(Integer scorePercent) { this.scorePercent = scorePercent; }
    public String getCefrLevel() { return cefrLevel; }
    public void setCefrLevel(String cefrLevel) { this.cefrLevel = cefrLevel; }

    public boolean isSubmitted() { return submittedAt != null; }
}
