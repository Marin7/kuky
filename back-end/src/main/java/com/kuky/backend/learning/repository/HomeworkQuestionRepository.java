package com.kuky.backend.learning.repository;

import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.QuestionOption;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Exercise questions + their options/accepted answers for an assignment.
 * Updates upsert by id when the client round-trips existing question/option ids,
 * so {@code homework_answers.question_id} and {@code homework_answer_options}
 * stay intact for submissions already made. Questions/options omitted from the
 * incoming set are deleted (cascading answer-option rows for removed options).
 */
@Repository
public class HomeworkQuestionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public HomeworkQuestionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<HomeworkQuestion> QUESTION_MAPPER = (rs, n) -> {
        HomeworkQuestion q = new HomeworkQuestion();
        q.setId(rs.getObject("id", UUID.class));
        q.setAssignmentId(rs.getObject("assignment_id", UUID.class));
        q.setPosition(rs.getInt("position"));
        q.setKind(QuestionKind.valueOf(rs.getString("kind")));
        q.setPrompt(rs.getString("prompt"));
        String structure = rs.getString("structure_json");
        q.setStructureJson(structure == null ? "{}" : structure);
        return q;
    };

    private static final RowMapper<QuestionOption> OPTION_MAPPER = (rs, n) -> {
        QuestionOption o = new QuestionOption();
        o.setId(rs.getObject("id", UUID.class));
        o.setQuestionId(rs.getObject("question_id", UUID.class));
        o.setPosition(rs.getInt("position"));
        o.setLabel(rs.getString("label"));
        o.setCorrect(rs.getBoolean("is_correct"));
        return o;
    };

    /** Questions (with options) for an assignment, ordered by position. */
    public List<HomeworkQuestion> findByAssignment(UUID assignmentId) {
        List<HomeworkQuestion> questions = jdbc.query(
                "SELECT * FROM homework_questions WHERE assignment_id = :aid ORDER BY position",
                Map.of("aid", assignmentId), QUESTION_MAPPER);
        if (questions.isEmpty()) return questions;

        Map<UUID, HomeworkQuestion> byId = new LinkedHashMap<>();
        for (HomeworkQuestion q : questions) byId.put(q.getId(), q);

        List<QuestionOption> options = jdbc.query("""
                SELECT o.* FROM homework_question_options o
                JOIN homework_questions q ON q.id = o.question_id
                WHERE q.assignment_id = :aid
                ORDER BY o.position
                """, Map.of("aid", assignmentId), OPTION_MAPPER);
        for (QuestionOption o : options) {
            HomeworkQuestion q = byId.get(o.getQuestionId());
            if (q != null) q.getOptions().add(o);
        }
        return questions;
    }

    /**
     * Sync an assignment's questions to the given list: update rows whose id
     * already belongs to this assignment, insert the rest, delete any left over.
     */
    @Transactional
    public void replaceQuestions(UUID assignmentId, List<HomeworkQuestion> questions) {
        Set<UUID> existingIds = new HashSet<>(jdbc.query(
                "SELECT id FROM homework_questions WHERE assignment_id = :aid",
                Map.of("aid", assignmentId),
                (rs, n) -> rs.getObject("id", UUID.class)));

        Set<UUID> keptIds = new HashSet<>();
        int qPos = 0;
        for (HomeworkQuestion q : questions) {
            String structureJson = q.getStructureJson() == null || q.getStructureJson().isBlank()
                    ? "{}" : q.getStructureJson();
            UUID questionId;
            if (q.getId() != null && existingIds.contains(q.getId())) {
                questionId = q.getId();
                jdbc.update("""
                        UPDATE homework_questions
                        SET position = :position, kind = :kind, prompt = :prompt,
                            structure_json = CAST(:structureJson AS jsonb)
                        WHERE id = :id AND assignment_id = :aid
                        """, new MapSqlParameterSource()
                        .addValue("id", questionId)
                        .addValue("aid", assignmentId)
                        .addValue("position", qPos++)
                        .addValue("kind", q.getKind().name())
                        .addValue("prompt", q.getPrompt())
                        .addValue("structureJson", structureJson));
                replaceOptions(questionId, q.getOptions());
            } else {
                questionId = UUID.randomUUID();
                jdbc.update("""
                        INSERT INTO homework_questions (id, assignment_id, position, kind, prompt, structure_json)
                        VALUES (:id, :aid, :position, :kind, :prompt, CAST(:structureJson AS jsonb))
                        """, new MapSqlParameterSource()
                        .addValue("id", questionId)
                        .addValue("aid", assignmentId)
                        .addValue("position", qPos++)
                        .addValue("kind", q.getKind().name())
                        .addValue("prompt", q.getPrompt())
                        .addValue("structureJson", structureJson));
                insertOptions(questionId, q.getOptions());
            }
            keptIds.add(questionId);
        }

        if (keptIds.isEmpty()) {
            jdbc.update("DELETE FROM homework_questions WHERE assignment_id = :aid",
                    Map.of("aid", assignmentId));
        } else {
            jdbc.update("""
                    DELETE FROM homework_questions
                    WHERE assignment_id = :aid AND id NOT IN (:kept)
                    """, Map.of("aid", assignmentId, "kept", keptIds));
        }
    }

    private void replaceOptions(UUID questionId, List<QuestionOption> options) {
        List<QuestionOption> opts = options == null ? List.of() : options;
        Set<UUID> existingIds = new HashSet<>(jdbc.query(
                "SELECT id FROM homework_question_options WHERE question_id = :qid",
                Map.of("qid", questionId),
                (rs, n) -> rs.getObject("id", UUID.class)));

        Set<UUID> keptIds = new HashSet<>();
        int oPos = 0;
        for (QuestionOption o : opts) {
            if (o.getId() != null && existingIds.contains(o.getId())) {
                jdbc.update("""
                        UPDATE homework_question_options
                        SET position = :position, label = :label, is_correct = :correct
                        WHERE id = :id AND question_id = :qid
                        """, new MapSqlParameterSource()
                        .addValue("id", o.getId())
                        .addValue("qid", questionId)
                        .addValue("position", oPos++)
                        .addValue("label", o.getLabel())
                        .addValue("correct", o.isCorrect()));
                keptIds.add(o.getId());
            } else {
                UUID optionId = UUID.randomUUID();
                jdbc.update("""
                        INSERT INTO homework_question_options (id, question_id, position, label, is_correct)
                        VALUES (:id, :qid, :position, :label, :correct)
                        """, new MapSqlParameterSource()
                        .addValue("id", optionId)
                        .addValue("qid", questionId)
                        .addValue("position", oPos++)
                        .addValue("label", o.getLabel())
                        .addValue("correct", o.isCorrect()));
                keptIds.add(optionId);
            }
        }

        if (keptIds.isEmpty()) {
            jdbc.update("DELETE FROM homework_question_options WHERE question_id = :qid",
                    Map.of("qid", questionId));
        } else {
            jdbc.update("""
                    DELETE FROM homework_question_options
                    WHERE question_id = :qid AND id NOT IN (:kept)
                    """, Map.of("qid", questionId, "kept", keptIds));
        }
    }

    private void insertOptions(UUID questionId, List<QuestionOption> options) {
        if (options == null || options.isEmpty()) return;
        int oPos = 0;
        for (QuestionOption o : options) {
            jdbc.update("""
                    INSERT INTO homework_question_options (id, question_id, position, label, is_correct)
                    VALUES (:id, :qid, :position, :label, :correct)
                    """, new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID())
                    .addValue("qid", questionId)
                    .addValue("position", oPos++)
                    .addValue("label", o.getLabel())
                    .addValue("correct", o.isCorrect()));
        }
    }

}
