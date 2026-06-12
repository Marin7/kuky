package com.kuky.backend.learning.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** An exercise question with its ordered options/accepted answers. */
public class HomeworkQuestion {

    private UUID id;
    private UUID assignmentId;
    private int position;
    private QuestionKind kind;
    private String prompt;
    private List<QuestionOption> options = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAssignmentId() { return assignmentId; }
    public void setAssignmentId(UUID assignmentId) { this.assignmentId = assignmentId; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public QuestionKind getKind() { return kind; }
    public void setKind(QuestionKind kind) { this.kind = kind; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public List<QuestionOption> getOptions() { return options; }
    public void setOptions(List<QuestionOption> options) { this.options = options; }
}
