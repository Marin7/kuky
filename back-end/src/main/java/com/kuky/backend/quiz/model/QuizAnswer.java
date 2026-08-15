package com.kuky.backend.quiz.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuizAnswer {

    private UUID id;
    private UUID attemptId;
    private UUID questionId;
    private String answerJson;
    private String answerText;
    private BigDecimal score;
    private Integer teacherPercent;
    private List<UUID> selectedOptionIds = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAttemptId() { return attemptId; }
    public void setAttemptId(UUID attemptId) { this.attemptId = attemptId; }
    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }
    public String getAnswerJson() { return answerJson; }
    public void setAnswerJson(String answerJson) { this.answerJson = answerJson; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public Integer getTeacherPercent() { return teacherPercent; }
    public void setTeacherPercent(Integer teacherPercent) { this.teacherPercent = teacherPercent; }
    public List<UUID> getSelectedOptionIds() { return selectedOptionIds; }
    public void setSelectedOptionIds(List<UUID> selectedOptionIds) { this.selectedOptionIds = selectedOptionIds; }
}
