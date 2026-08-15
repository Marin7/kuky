package com.kuky.backend.learning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.model.HomeworkType;
import com.kuky.backend.learning.model.MediaSourceKind;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.QuestionOption;
import com.kuky.backend.learning.exception.HomeworkUpdatedException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serializes live homework content into {@code homework_submissions.assignment_snapshot}
 * and reads it back for result/review.
 */
@Component
public class AssignmentSnapshot {

    private final ObjectMapper mapper;

    public AssignmentSnapshot(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public boolean present(HomeworkSubmission submission) {
        return submission != null && submission.getAssignmentSnapshot() != null
                && !submission.getAssignmentSnapshot().isBlank();
    }

    public void requireCurrentRevision(HomeworkAssignment assignment, Instant echoed) {
        Instant live = assignment == null ? null : assignment.getContentRevisedAt();
        if (echoed == null || live == null || !truncate(live).equals(truncate(echoed))) {
            throw new HomeworkUpdatedException(
                    "La tarea se ha actualizado. Debes empezar de nuevo con la versión actual.");
        }
    }

    private static Instant truncate(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS);
    }

    public String serialize(HomeworkAssignment assignment, List<HomeworkQuestion> questions) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("title", assignment.getTitle());
            root.put("instructions", assignment.getInstructions());
            if (assignment.getHomeworkType() != null) {
                root.put("homeworkType", assignment.getHomeworkType().name());
            } else {
                root.putNull("homeworkType");
            }
            if (assignment.getLevel() != null) {
                root.put("level", assignment.getLevel().name());
            } else {
                root.putNull("level");
            }
            root.put("format", assignment.getFormat() == null ? "MANUAL" : assignment.getFormat().name());
            root.put("audioUrl", assignment.getAudioUrl());
            if (assignment.getAudioFileId() != null) {
                root.put("audioFileId", assignment.getAudioFileId().toString());
            } else {
                root.putNull("audioFileId");
            }
            if (assignment.getMediaSourceKind() != null) {
                root.put("mediaSourceKind", assignment.getMediaSourceKind().name());
            } else {
                root.putNull("mediaSourceKind");
            }
            ArrayNode qs = root.putArray("questions");
            for (HomeworkQuestion q : questions == null ? List.<HomeworkQuestion>of() : questions) {
                ObjectNode qn = qs.addObject();
                qn.put("id", q.getId().toString());
                qn.put("position", q.getPosition());
                qn.put("kind", q.getKind().name());
                qn.put("prompt", q.getPrompt());
                qn.set("structure", mapper.readTree(
                        q.getStructureJson() == null || q.getStructureJson().isBlank()
                                ? "{}" : q.getStructureJson()));
                ArrayNode opts = qn.putArray("options");
                if (q.getOptions() != null) {
                    int pos = 0;
                    for (QuestionOption o : q.getOptions()) {
                        ObjectNode on = opts.addObject();
                        if (o.getId() != null) on.put("id", o.getId().toString());
                        on.put("position", o.getPosition() > 0 ? o.getPosition() : pos);
                        on.put("label", o.getLabel());
                        on.put("correct", o.isCorrect());
                        pos++;
                    }
                }
            }
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo guardar la copia de la tarea.", e);
        }
    }

    public List<HomeworkQuestion> questionsOf(HomeworkSubmission submission) {
        if (!present(submission)) return List.of();
        try {
            JsonNode root = mapper.readTree(submission.getAssignmentSnapshot());
            JsonNode arr = root.path("questions");
            if (!arr.isArray()) return List.of();
            List<HomeworkQuestion> out = new ArrayList<>();
            int i = 0;
            for (JsonNode qn : arr) {
                HomeworkQuestion q = new HomeworkQuestion();
                String id = text(qn, "id");
                if (id != null) q.setId(UUID.fromString(id));
                q.setPosition(qn.path("position").asInt(i));
                q.setKind(QuestionKind.valueOf(qn.path("kind").asText("FREE_TEXT")));
                q.setPrompt(qn.path("prompt").asText(""));
                JsonNode structure = qn.get("structure");
                q.setStructureJson(structure == null || structure.isNull() ? "{}" : mapper.writeValueAsString(structure));
                JsonNode opts = qn.path("options");
                if (opts.isArray()) {
                    int op = 0;
                    for (JsonNode on : opts) {
                        QuestionOption o = new QuestionOption();
                        String oid = text(on, "id");
                        if (oid != null) o.setId(UUID.fromString(oid));
                        o.setQuestionId(q.getId());
                        o.setPosition(on.path("position").asInt(op++));
                        o.setLabel(on.path("label").asText(""));
                        o.setCorrect(on.path("correct").asBoolean(false));
                        q.getOptions().add(o);
                    }
                }
                out.add(q);
                i++;
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer la copia de la tarea.", e);
        }
    }

    /** Overlay snapshot title/instructions/media onto a live assignment copy for result views. */
    public void applyContent(HomeworkAssignment assignment, HomeworkSubmission submission) {
        if (!present(submission) || assignment == null) return;
        try {
            JsonNode root = mapper.readTree(submission.getAssignmentSnapshot());
            if (root.hasNonNull("title")) assignment.setTitle(root.get("title").asText());
            if (root.hasNonNull("instructions")) assignment.setInstructions(root.get("instructions").asText());
            if (root.hasNonNull("homeworkType")) {
                assignment.setHomeworkType(HomeworkType.valueOf(root.get("homeworkType").asText()));
            }
            String audioUrl = text(root, "audioUrl");
            assignment.setAudioUrl(audioUrl);
            String fileId = text(root, "audioFileId");
            assignment.setAudioFileId(fileId == null ? null : UUID.fromString(fileId));
            String media = text(root, "mediaSourceKind");
            assignment.setMediaSourceKind(media == null ? null : MediaSourceKind.valueOf(media));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer la copia de la tarea.", e);
        }
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;
        String s = v.asText();
        return s == null || s.isBlank() || "null".equals(s) ? null : s;
    }
}
