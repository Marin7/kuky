package com.kuky.backend.learning.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One question's answer within a graded submission, with its computed score. */
public class HomeworkAnswer {

    private UUID id;
    private UUID submissionId;
    private UUID questionId;          // nullable after answer-key edits (ON DELETE SET NULL)
    private String answerJson;        // structured kinds; null for choice / FREE_TEXT
    private String answerText;        // FREE_TEXT plain answer; null for EXERCISE
    private String promptSnapshot;    // FREE_TEXT prompt at submit time; null for EXERCISE
    private BigDecimal score;         // per-question score in [0,1] (0 for FREE_TEXT until validated)
    private String teacherValidation; // VALIDATED | INVALIDATED | null (FREE_TEXT on MIXED)
    private List<UUID> selectedOptionIds = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSubmissionId() { return submissionId; }
    public void setSubmissionId(UUID submissionId) { this.submissionId = submissionId; }
    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }
    public String getAnswerJson() { return answerJson; }
    public void setAnswerJson(String answerJson) { this.answerJson = answerJson; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public String getPromptSnapshot() { return promptSnapshot; }
    public void setPromptSnapshot(String promptSnapshot) { this.promptSnapshot = promptSnapshot; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getTeacherValidation() { return teacherValidation; }
    public void setTeacherValidation(String teacherValidation) { this.teacherValidation = teacherValidation; }
    public List<UUID> getSelectedOptionIds() { return selectedOptionIds; }
    public void setSelectedOptionIds(List<UUID> selectedOptionIds) { this.selectedOptionIds = selectedOptionIds; }
}
