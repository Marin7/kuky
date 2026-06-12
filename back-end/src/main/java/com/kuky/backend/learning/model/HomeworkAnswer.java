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
    private String answerText;        // fill-blank response (raw); null for choice
    private BigDecimal score;         // per-question score in [0,1]
    private List<UUID> selectedOptionIds = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSubmissionId() { return submissionId; }
    public void setSubmissionId(UUID submissionId) { this.submissionId = submissionId; }
    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public List<UUID> getSelectedOptionIds() { return selectedOptionIds; }
    public void setSelectedOptionIds(List<UUID> selectedOptionIds) { this.selectedOptionIds = selectedOptionIds; }
}
