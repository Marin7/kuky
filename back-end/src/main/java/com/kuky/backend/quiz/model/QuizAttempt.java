package com.kuky.backend.quiz.model;

import java.time.Instant;
import java.util.UUID;

public class QuizAttempt {

    private UUID id;
    private UUID quizId;
    private UUID userId;
    private QuizAttemptStatus status;
    private Instant startedAt;
    private Instant submittedAt;
    private Integer scorePercent;
    private Integer fullyCorrectCount;
    private Integer questionUnitCount;
    private String quizSnapshot;
    private String feedback;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getQuizId() { return quizId; }
    public void setQuizId(UUID quizId) { this.quizId = quizId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public QuizAttemptStatus getStatus() { return status; }
    public void setStatus(QuizAttemptStatus status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Integer getScorePercent() { return scorePercent; }
    public void setScorePercent(Integer scorePercent) { this.scorePercent = scorePercent; }
    public Integer getFullyCorrectCount() { return fullyCorrectCount; }
    public void setFullyCorrectCount(Integer fullyCorrectCount) { this.fullyCorrectCount = fullyCorrectCount; }
    public Integer getQuestionUnitCount() { return questionUnitCount; }
    public void setQuestionUnitCount(Integer questionUnitCount) { this.questionUnitCount = questionUnitCount; }
    public String getQuizSnapshot() { return quizSnapshot; }
    public void setQuizSnapshot(String quizSnapshot) { this.quizSnapshot = quizSnapshot; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
