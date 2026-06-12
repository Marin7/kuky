package com.kuky.backend.learning.repository;

import com.kuky.backend.learning.model.HomeworkAnswer;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Per-question student answers (and selected options) for a graded submission. */
@Repository
public class HomeworkAnswerRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public HomeworkAnswerRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<HomeworkAnswer> ANSWER_MAPPER = (rs, n) -> {
        HomeworkAnswer a = new HomeworkAnswer();
        a.setId(rs.getObject("id", UUID.class));
        a.setSubmissionId(rs.getObject("submission_id", UUID.class));
        a.setQuestionId(rs.getObject("question_id", UUID.class));
        a.setAnswerText(rs.getString("answer_text"));
        a.setScore(rs.getBigDecimal("score"));
        return a;
    };

    /** Replace and insert all answers for a submission in one transaction. */
    @Transactional
    public void saveAll(UUID submissionId, List<HomeworkAnswer> answers) {
        jdbc.update("DELETE FROM homework_answers WHERE submission_id = :sid",
                Map.of("sid", submissionId));
        for (HomeworkAnswer a : answers) {
            UUID answerId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO homework_answers (id, submission_id, question_id, answer_text, score)
                    VALUES (:id, :sid, :qid, :answerText, :score)
                    """, new MapSqlParameterSource()
                    .addValue("id", answerId)
                    .addValue("sid", submissionId)
                    .addValue("qid", a.getQuestionId())
                    .addValue("answerText", a.getAnswerText())
                    .addValue("score", a.getScore()));
            for (UUID optionId : a.getSelectedOptionIds()) {
                jdbc.update("""
                        INSERT INTO homework_answer_options (answer_id, option_id)
                        VALUES (:aid, :oid)
                        """, new MapSqlParameterSource()
                        .addValue("aid", answerId)
                        .addValue("oid", optionId));
            }
        }
    }

    /** Answers (with selected option ids) for a submission, keyed by question id. */
    public List<HomeworkAnswer> findBySubmission(UUID submissionId) {
        List<HomeworkAnswer> answers = jdbc.query(
                "SELECT * FROM homework_answers WHERE submission_id = :sid",
                Map.of("sid", submissionId), ANSWER_MAPPER);
        if (answers.isEmpty()) return answers;

        Map<UUID, HomeworkAnswer> byId = new LinkedHashMap<>();
        for (HomeworkAnswer a : answers) byId.put(a.getId(), a);

        jdbc.query("""
                SELECT ao.answer_id, ao.option_id FROM homework_answer_options ao
                JOIN homework_answers a ON a.id = ao.answer_id
                WHERE a.submission_id = :sid
                """, Map.of("sid", submissionId), (rs, n) -> {
            UUID answerId = rs.getObject("answer_id", UUID.class);
            UUID optionId = rs.getObject("option_id", UUID.class);
            HomeworkAnswer a = byId.get(answerId);
            if (a != null) a.getSelectedOptionIds().add(optionId);
            return null;
        });
        return answers;
    }
}
