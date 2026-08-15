package com.kuky.backend.quiz.model;

import com.kuky.backend.learning.model.MediaSourceKind;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.QuestionOption;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuizQuestion {

    private UUID id;
    private UUID quizId;
    private int position;
    private QuizSkill skill;
    private QuestionKind kind;
    private String prompt;
    private String structureJson = "{}";
    private MediaSourceKind mediaSourceKind;
    private String audioUrl;
    private UUID audioFileId;
    private boolean retired;
    private List<QuestionOption> options = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getQuizId() { return quizId; }
    public void setQuizId(UUID quizId) { this.quizId = quizId; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public QuizSkill getSkill() { return skill; }
    public void setSkill(QuizSkill skill) { this.skill = skill; }
    public QuestionKind getKind() { return kind; }
    public void setKind(QuestionKind kind) { this.kind = kind; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getStructureJson() { return structureJson; }
    public void setStructureJson(String structureJson) {
        this.structureJson = structureJson == null || structureJson.isBlank() ? "{}" : structureJson;
    }
    public MediaSourceKind getMediaSourceKind() { return mediaSourceKind; }
    public void setMediaSourceKind(MediaSourceKind mediaSourceKind) { this.mediaSourceKind = mediaSourceKind; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public UUID getAudioFileId() { return audioFileId; }
    public void setAudioFileId(UUID audioFileId) { this.audioFileId = audioFileId; }
    public boolean isRetired() { return retired; }
    public void setRetired(boolean retired) { this.retired = retired; }
    public List<QuestionOption> getOptions() { return options; }
    public void setOptions(List<QuestionOption> options) { this.options = options; }
}
