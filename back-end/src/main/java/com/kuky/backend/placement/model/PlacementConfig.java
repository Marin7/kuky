package com.kuky.backend.placement.model;

import java.time.Instant;
import java.util.UUID;

/** Singleton teacher-editable config: per-section time limits + writing prompt. */
public class PlacementConfig {

    private UUID id;
    private int readingTimeSeconds;
    private int listeningTimeSeconds;
    private int grammarTimeSeconds;
    private int writingTimeSeconds;
    private String writingPrompt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public int getReadingTimeSeconds() { return readingTimeSeconds; }
    public void setReadingTimeSeconds(int readingTimeSeconds) { this.readingTimeSeconds = readingTimeSeconds; }
    public int getListeningTimeSeconds() { return listeningTimeSeconds; }
    public void setListeningTimeSeconds(int listeningTimeSeconds) { this.listeningTimeSeconds = listeningTimeSeconds; }
    public int getGrammarTimeSeconds() { return grammarTimeSeconds; }
    public void setGrammarTimeSeconds(int grammarTimeSeconds) { this.grammarTimeSeconds = grammarTimeSeconds; }
    public int getWritingTimeSeconds() { return writingTimeSeconds; }
    public void setWritingTimeSeconds(int writingTimeSeconds) { this.writingTimeSeconds = writingTimeSeconds; }
    public String getWritingPrompt() { return writingPrompt; }
    public void setWritingPrompt(String writingPrompt) { this.writingPrompt = writingPrompt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    /** Time limit for a given skill, per this config. */
    public int timeLimitSecondsFor(Skill skill) {
        return switch (skill) {
            case READING -> readingTimeSeconds;
            case LISTENING -> listeningTimeSeconds;
            case GRAMMAR -> grammarTimeSeconds;
        };
    }
}
