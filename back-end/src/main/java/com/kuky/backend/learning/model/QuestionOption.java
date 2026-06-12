package com.kuky.backend.learning.model;

import java.util.UUID;

/**
 * A question option. For choice questions it is a selectable option (with
 * {@code isCorrect} marking the answer key). For fill-blank questions it is an
 * accepted answer ({@code label} = accepted text, {@code isCorrect} always true).
 */
public class QuestionOption {

    private UUID id;
    private UUID questionId;
    private int position;
    private String label;
    private boolean isCorrect;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
}
