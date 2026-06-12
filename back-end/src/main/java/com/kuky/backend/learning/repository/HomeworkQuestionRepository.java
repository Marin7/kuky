package com.kuky.backend.learning.repository;

import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.QuestionOption;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Exercise questions + their options/accepted answers for an assignment. An
 * assignment's questions are stored as a full set; updates replace them wholesale
 * (delete + reinsert), which is safe because {@code homework_answers.question_id}
 * is ON DELETE SET NULL and the recorded grade lives on the submission.
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

    /** Replace all of an assignment's questions (and options) in one transaction. */
    @Transactional
    public void replaceQuestions(UUID assignmentId, List<HomeworkQuestion> questions) {
        jdbc.update("DELETE FROM homework_questions WHERE assignment_id = :aid",
                Map.of("aid", assignmentId));
        int qPos = 0;
        for (HomeworkQuestion q : questions) {
            UUID questionId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO homework_questions (id, assignment_id, position, kind, prompt)
                    VALUES (:id, :aid, :position, :kind, :prompt)
                    """, new MapSqlParameterSource()
                    .addValue("id", questionId)
                    .addValue("aid", assignmentId)
                    .addValue("position", qPos++)
                    .addValue("kind", q.getKind().name())
                    .addValue("prompt", q.getPrompt()));
            int oPos = 0;
            for (QuestionOption o : q.getOptions()) {
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

}
