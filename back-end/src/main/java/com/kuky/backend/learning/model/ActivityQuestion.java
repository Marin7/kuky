package com.kuky.backend.learning.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Exercise question for an activity (mirrors HomeworkQuestion). */
public class ActivityQuestion {

    private UUID id;
    private UUID activityId;
    private int position;
    private QuestionKind kind;
    private String prompt;
    private String structureJson = "{}";
    private List<QuestionOption> options = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getActivityId() { return activityId; }
    public void setActivityId(UUID activityId) { this.activityId = activityId; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public QuestionKind getKind() { return kind; }
    public void setKind(QuestionKind kind) { this.kind = kind; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getStructureJson() { return structureJson; }
    public void setStructureJson(String structureJson) {
        this.structureJson = structureJson == null || structureJson.isBlank() ? "{}" : structureJson;
    }
    public List<QuestionOption> getOptions() { return options; }
    public void setOptions(List<QuestionOption> options) { this.options = options; }

    /** Convert to HomeworkQuestion shape for shared grading helpers. */
    public HomeworkQuestion toHomeworkQuestion() {
        HomeworkQuestion q = new HomeworkQuestion();
        q.setId(id);
        q.setAssignmentId(activityId);
        q.setPosition(position);
        q.setKind(kind);
        q.setPrompt(prompt);
        q.setStructureJson(structureJson);
        q.setOptions(options);
        return q;
    }

    public static ActivityQuestion fromHomeworkQuestion(HomeworkQuestion hq, UUID activityId) {
        ActivityQuestion q = new ActivityQuestion();
        q.setId(hq.getId());
        q.setActivityId(activityId);
        q.setPosition(hq.getPosition());
        q.setKind(hq.getKind());
        q.setPrompt(hq.getPrompt());
        q.setStructureJson(hq.getStructureJson());
        q.setOptions(hq.getOptions());
        return q;
    }
}
