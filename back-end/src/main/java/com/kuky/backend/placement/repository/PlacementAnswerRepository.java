package com.kuky.backend.placement.repository;

import com.kuky.backend.placement.model.PlacementAnswer;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Per-question graded answers (and selected options) for a section (mirrors learning.HomeworkAnswerRepository). */
@Repository
public class PlacementAnswerRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PlacementAnswerRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<PlacementAnswer> ANSWER_MAPPER = (rs, n) -> {
        PlacementAnswer a = new PlacementAnswer();
        a.setId(rs.getObject("id", UUID.class));
        a.setAttemptSectionId(rs.getObject("attempt_section_id", UUID.class));
        a.setQuestionId(rs.getObject("question_id", UUID.class));
        a.setAnswerText(rs.getString("answer_text"));
        a.setScore(rs.getBigDecimal("score"));
        return a;
    };

    @Transactional
    public void saveAll(UUID attemptSectionId, List<PlacementAnswer> answers) {
        jdbc.update("DELETE FROM placement_answers WHERE attempt_section_id = :sid",
                Map.of("sid", attemptSectionId));
        for (PlacementAnswer a : answers) {
            UUID answerId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO placement_answers (id, attempt_section_id, question_id, answer_text, score)
                    VALUES (:id, :sid, :qid, :answerText, :score)
                    """, new MapSqlParameterSource()
                    .addValue("id", answerId)
                    .addValue("sid", attemptSectionId)
                    .addValue("qid", a.getQuestionId())
                    .addValue("answerText", a.getAnswerText())
                    .addValue("score", a.getScore()));
            for (UUID optionId : a.getSelectedOptionIds()) {
                jdbc.update("""
                        INSERT INTO placement_answer_options (answer_id, option_id)
                        VALUES (:aid, :oid)
                        """, new MapSqlParameterSource().addValue("aid", answerId).addValue("oid", optionId));
            }
        }
    }

    public List<PlacementAnswer> findBySection(UUID attemptSectionId) {
        List<PlacementAnswer> answers = jdbc.query(
                "SELECT * FROM placement_answers WHERE attempt_section_id = :sid",
                Map.of("sid", attemptSectionId), ANSWER_MAPPER);
        if (answers.isEmpty()) return answers;

        Map<UUID, PlacementAnswer> byId = new LinkedHashMap<>();
        for (PlacementAnswer a : answers) byId.put(a.getId(), a);

        jdbc.query("""
                SELECT ao.answer_id, ao.option_id FROM placement_answer_options ao
                JOIN placement_answers a ON a.id = ao.answer_id
                WHERE a.attempt_section_id = :sid
                """, Map.of("sid", attemptSectionId), (rs, n) -> {
            UUID answerId = rs.getObject("answer_id", UUID.class);
            UUID optionId = rs.getObject("option_id", UUID.class);
            PlacementAnswer a = byId.get(answerId);
            if (a != null) a.getSelectedOptionIds().add(optionId);
            return null;
        });
        return answers;
    }
}
