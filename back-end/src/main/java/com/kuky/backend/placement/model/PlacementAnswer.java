package com.kuky.backend.placement.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A graded answer to one question within a section (mirrors learning.HomeworkAnswer). */
public class PlacementAnswer {

    private UUID id;
    private UUID attemptSectionId;
    private UUID questionId; // nullable — SET NULL if the question is later deleted
    private String answerText; // fill-blank raw input
    private BigDecimal score; // 0.000..1.000
    private List<UUID> selectedOptionIds = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAttemptSectionId() { return attemptSectionId; }
    public void setAttemptSectionId(UUID attemptSectionId) { this.attemptSectionId = attemptSectionId; }
    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public List<UUID> getSelectedOptionIds() { return selectedOptionIds; }
    public void setSelectedOptionIds(List<UUID> selectedOptionIds) { this.selectedOptionIds = selectedOptionIds; }
}
