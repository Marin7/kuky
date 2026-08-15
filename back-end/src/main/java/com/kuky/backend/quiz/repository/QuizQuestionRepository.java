package com.kuky.backend.quiz.repository;

import com.kuky.backend.learning.model.MediaSourceKind;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.QuestionOption;
import com.kuky.backend.quiz.model.QuizQuestion;
import com.kuky.backend.quiz.model.QuizSkill;
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

@Repository
public class QuizQuestionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public QuizQuestionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<QuizQuestion> QUESTION_MAPPER = (rs, n) -> {
        QuizQuestion q = new QuizQuestion();
        q.setId(rs.getObject("id", UUID.class));
        q.setQuizId(rs.getObject("quiz_id", UUID.class));
        q.setPosition(rs.getInt("position"));
        q.setSkill(QuizSkill.valueOf(rs.getString("skill")));
        q.setKind(QuestionKind.valueOf(rs.getString("kind")));
        q.setPrompt(rs.getString("prompt"));
        String structure = rs.getString("structure_json");
        q.setStructureJson(structure == null ? "{}" : structure);
        String media = rs.getString("media_source_kind");
        q.setMediaSourceKind(media == null ? null : MediaSourceKind.valueOf(media));
        q.setAudioUrl(rs.getString("audio_url"));
        q.setAudioFileId(rs.getObject("audio_file_id", UUID.class));
        q.setRetired(rs.getBoolean("retired"));
        return q;
    };

    private static final RowMapper<QuestionOption> OPTION_MAPPER = (rs, n) -> {
        QuestionOption o = new QuestionOption();
        o.setId(rs.getObject("id", UUID.class));
        o.setQuestionId(rs.getObject("question_id", UUID.class));
        o.setPosition(rs.getInt("position"));
        o.setLabel(rs.getString("label"));
        o.setCorrect(rs.getBoolean("is_correct"));
        o.setRetired(rs.getBoolean("retired"));
        return o;
    };

    public int countLive(UUID quizId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM quiz_questions WHERE quiz_id = :qid AND retired = false",
                Map.of("qid", quizId), Integer.class);
        return n == null ? 0 : n;
    }

    public List<QuizQuestion> findLiveByQuiz(UUID quizId) {
        List<QuizQuestion> questions = jdbc.query(
                "SELECT * FROM quiz_questions WHERE quiz_id = :qid AND retired = false ORDER BY position",
                Map.of("qid", quizId), QUESTION_MAPPER);
        attachOptions(quizId, questions, false);
        return questions;
    }

    private void attachOptions(UUID quizId, List<QuizQuestion> questions, boolean includeRetired) {
        if (questions.isEmpty()) return;
        Map<UUID, QuizQuestion> byId = new LinkedHashMap<>();
        for (QuizQuestion q : questions) byId.put(q.getId(), q);
        String sql = includeRetired
                ? """
                    SELECT o.* FROM quiz_question_options o
                    JOIN quiz_questions q ON q.id = o.question_id
                    WHERE q.quiz_id = :qid
                    ORDER BY o.position
                    """
                : """
                    SELECT o.* FROM quiz_question_options o
                    JOIN quiz_questions q ON q.id = o.question_id
                    WHERE q.quiz_id = :qid AND q.retired = false AND o.retired = false
                    ORDER BY o.position
                    """;
        List<QuestionOption> options = jdbc.query(sql, Map.of("qid", quizId), OPTION_MAPPER);
        for (QuestionOption o : options) {
            QuizQuestion q = byId.get(o.getQuestionId());
            if (q != null) q.getOptions().add(o);
        }
    }

    @Transactional
    public void replaceQuestions(UUID quizId, List<QuizQuestion> questions) {
        Set<UUID> existingIds = new HashSet<>(jdbc.query(
                "SELECT id FROM quiz_questions WHERE quiz_id = :qid",
                Map.of("qid", quizId),
                (rs, n) -> rs.getObject("id", UUID.class)));

        Set<UUID> keptIds = new HashSet<>();
        int qPos = 0;
        for (QuizQuestion q : questions) {
            String structureJson = q.getStructureJson() == null || q.getStructureJson().isBlank()
                    ? "{}" : q.getStructureJson();
            UUID questionId;
            if (q.getId() != null && existingIds.contains(q.getId())) {
                questionId = q.getId();
                jdbc.update("""
                        UPDATE quiz_questions
                        SET position = :position, skill = :skill, kind = :kind, prompt = :prompt,
                            structure_json = CAST(:structureJson AS jsonb),
                            media_source_kind = :mediaKind, audio_url = :audioUrl, audio_file_id = :audioFileId,
                            retired = false
                        WHERE id = :id AND quiz_id = :qid
                        """, params(questionId, quizId, qPos++, q, structureJson));
                replaceOptions(questionId, q.getOptions());
            } else {
                questionId = q.getId() != null ? q.getId() : UUID.randomUUID();
                jdbc.update("""
                        INSERT INTO quiz_questions
                            (id, quiz_id, position, skill, kind, prompt, structure_json,
                             media_source_kind, audio_url, audio_file_id)
                        VALUES (:id, :qid, :position, :skill, :kind, :prompt, CAST(:structureJson AS jsonb),
                                :mediaKind, :audioUrl, :audioFileId)
                        """, params(questionId, quizId, qPos++, q, structureJson));
                insertOptions(questionId, q.getOptions());
            }
            keptIds.add(questionId);
            q.setId(questionId);
        }

        Set<UUID> leftover = new HashSet<>(existingIds);
        leftover.removeAll(keptIds);
        retireOrDeleteQuestions(leftover);
    }

    private MapSqlParameterSource params(UUID id, UUID quizId, int position, QuizQuestion q, String structureJson) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("qid", quizId)
                .addValue("position", position)
                .addValue("skill", q.getSkill().name())
                .addValue("kind", q.getKind().name())
                .addValue("prompt", q.getPrompt())
                .addValue("structureJson", structureJson)
                .addValue("mediaKind", q.getMediaSourceKind() == null ? null : q.getMediaSourceKind().name())
                .addValue("audioUrl", q.getAudioUrl())
                .addValue("audioFileId", q.getAudioFileId());
    }

    private void retireOrDeleteQuestions(Set<UUID> leftover) {
        if (leftover.isEmpty()) return;
        Set<UUID> referenced = new HashSet<>(jdbc.query("""
                SELECT DISTINCT question_id FROM quiz_answers
                WHERE question_id IN (:ids)
                """, Map.of("ids", leftover),
                (rs, n) -> rs.getObject("question_id", UUID.class)));
        Set<UUID> toRetire = new HashSet<>(referenced);
        toRetire.retainAll(leftover);
        Set<UUID> toDelete = new HashSet<>(leftover);
        toDelete.removeAll(toRetire);
        if (!toRetire.isEmpty()) {
            jdbc.update("UPDATE quiz_questions SET retired = true WHERE id IN (:ids)", Map.of("ids", toRetire));
            jdbc.update("UPDATE quiz_question_options SET retired = true WHERE question_id IN (:ids)",
                    Map.of("ids", toRetire));
        }
        if (!toDelete.isEmpty()) {
            jdbc.update("DELETE FROM quiz_questions WHERE id IN (:ids)", Map.of("ids", toDelete));
        }
    }

    private void replaceOptions(UUID questionId, List<QuestionOption> options) {
        List<QuestionOption> opts = options == null ? List.of() : options;
        Set<UUID> existingIds = new HashSet<>(jdbc.query(
                "SELECT id FROM quiz_question_options WHERE question_id = :qid",
                Map.of("qid", questionId),
                (rs, n) -> rs.getObject("id", UUID.class)));
        Set<UUID> keptIds = new HashSet<>();
        int oPos = 0;
        for (QuestionOption o : opts) {
            if (o.getId() != null && existingIds.contains(o.getId())) {
                jdbc.update("""
                        UPDATE quiz_question_options
                        SET position = :position, label = :label, is_correct = :correct, retired = false
                        WHERE id = :id AND question_id = :qid
                        """, new MapSqlParameterSource()
                        .addValue("id", o.getId())
                        .addValue("qid", questionId)
                        .addValue("position", oPos++)
                        .addValue("label", o.getLabel())
                        .addValue("correct", o.isCorrect()));
                keptIds.add(o.getId());
            } else {
                UUID optionId = o.getId() != null ? o.getId() : UUID.randomUUID();
                jdbc.update("""
                        INSERT INTO quiz_question_options (id, question_id, position, label, is_correct)
                        VALUES (:id, :qid, :position, :label, :correct)
                        """, new MapSqlParameterSource()
                        .addValue("id", optionId)
                        .addValue("qid", questionId)
                        .addValue("position", oPos++)
                        .addValue("label", o.getLabel())
                        .addValue("correct", o.isCorrect()));
                keptIds.add(optionId);
                o.setId(optionId);
            }
        }
        Set<UUID> leftover = new HashSet<>(existingIds);
        leftover.removeAll(keptIds);
        if (!leftover.isEmpty()) {
            jdbc.update("DELETE FROM quiz_question_options WHERE id IN (:ids)", Map.of("ids", leftover));
        }
    }

    private void insertOptions(UUID questionId, List<QuestionOption> options) {
        if (options == null || options.isEmpty()) return;
        int oPos = 0;
        for (QuestionOption o : options) {
            UUID optionId = o.getId() != null ? o.getId() : UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO quiz_question_options (id, question_id, position, label, is_correct)
                    VALUES (:id, :qid, :position, :label, :correct)
                    """, new MapSqlParameterSource()
                    .addValue("id", optionId)
                    .addValue("qid", questionId)
                    .addValue("position", oPos++)
                    .addValue("label", o.getLabel())
                    .addValue("correct", o.isCorrect()));
            o.setId(optionId);
        }
    }
}
