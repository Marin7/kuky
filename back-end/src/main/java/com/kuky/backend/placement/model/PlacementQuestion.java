package com.kuky.backend.placement.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A globally authored placement item with its ordered options/accepted answers. */
public class PlacementQuestion {

    private UUID id;
    private Skill skill;
    private int position;
    private QuestionKind kind;
    private String prompt;
    private String audioUrl; // LISTENING only, external source
    private UUID audioFileId; // LISTENING only, uploaded source
    private boolean active = true;
    private List<PlacementQuestionOption> options = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public QuestionKind getKind() { return kind; }
    public void setKind(QuestionKind kind) { this.kind = kind; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public UUID getAudioFileId() { return audioFileId; }
    public void setAudioFileId(UUID audioFileId) { this.audioFileId = audioFileId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<PlacementQuestionOption> getOptions() { return options; }
    public void setOptions(List<PlacementQuestionOption> options) { this.options = options; }
}
