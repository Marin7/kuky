package com.kuky.backend.learning.repository;

import com.kuky.backend.learning.model.ActivityQuestion;
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
 * Exercise questions for an activity. Updates replace the full set (delete + reinsert),
 * same pattern as {@link HomeworkQuestionRepository}.
 */
@Repository
public class ActivityQuestionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ActivityQuestionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<ActivityQuestion> QUESTION_MAPPER = (rs, n) -> {
        ActivityQuestion q = new ActivityQuestion();
        q.setId(rs.getObject("id", UUID.class));
        q.setActivityId(rs.getObject("activity_id", UUID.class));
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

    public List<ActivityQuestion> findByActivityId(UUID activityId) {
        List<ActivityQuestion> questions = jdbc.query(
                "SELECT * FROM activity_questions WHERE activity_id = :aid ORDER BY position",
                Map.of("aid", activityId), QUESTION_MAPPER);
        if (questions.isEmpty()) return questions;

        Map<UUID, ActivityQuestion> byId = new LinkedHashMap<>();
        for (ActivityQuestion q : questions) byId.put(q.getId(), q);

        List<QuestionOption> options = jdbc.query("""
                SELECT o.* FROM activity_question_options o
                JOIN activity_questions q ON q.id = o.question_id
                WHERE q.activity_id = :aid
                ORDER BY o.position
                """, Map.of("aid", activityId), OPTION_MAPPER);
        for (QuestionOption o : options) {
            ActivityQuestion q = byId.get(o.getQuestionId());
            if (q != null) q.getOptions().add(o);
        }
        return questions;
    }

    @Transactional
    public void replaceQuestions(UUID activityId, List<ActivityQuestion> questions) {
        jdbc.update("DELETE FROM activity_questions WHERE activity_id = :aid",
                Map.of("aid", activityId));
        int qPos = 0;
        for (ActivityQuestion q : questions) {
            UUID questionId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO activity_questions (id, activity_id, position, kind, prompt, structure_json)
                    VALUES (:id, :aid, :position, :kind, :prompt, CAST(:structureJson AS jsonb))
                    """, new MapSqlParameterSource()
                    .addValue("id", questionId)
                    .addValue("aid", activityId)
                    .addValue("position", qPos++)
                    .addValue("kind", q.getKind().name())
                    .addValue("prompt", q.getPrompt())
                    .addValue("structureJson",
                            q.getStructureJson() == null || q.getStructureJson().isBlank()
                                    ? "{}" : q.getStructureJson()));
            int oPos = 0;
            for (QuestionOption o : q.getOptions()) {
                jdbc.update("""
                        INSERT INTO activity_question_options (id, question_id, position, label, is_correct)
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
