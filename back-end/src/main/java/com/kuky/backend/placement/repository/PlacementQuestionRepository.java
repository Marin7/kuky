package com.kuky.backend.placement.repository;

import com.kuky.backend.placement.model.PlacementQuestion;
import com.kuky.backend.placement.model.PlacementQuestionOption;
import com.kuky.backend.placement.model.QuestionKind;
import com.kuky.backend.placement.model.Skill;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Globally authored placement questions + their options/accepted answers. */
@Repository
public class PlacementQuestionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PlacementQuestionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<PlacementQuestion> QUESTION_MAPPER = (rs, n) -> {
        PlacementQuestion q = new PlacementQuestion();
        q.setId(rs.getObject("id", UUID.class));
        q.setSkill(Skill.valueOf(rs.getString("skill")));
        q.setPosition(rs.getInt("position"));
        q.setKind(QuestionKind.valueOf(rs.getString("kind")));
        q.setPrompt(rs.getString("prompt"));
        q.setAudioUrl(rs.getString("audio_url"));
        q.setAudioFileId(rs.getObject("audio_file_id", UUID.class));
        q.setActive(rs.getBoolean("active"));
        return q;
    };

    private static final RowMapper<PlacementQuestionOption> OPTION_MAPPER = (rs, n) -> {
        PlacementQuestionOption o = new PlacementQuestionOption();
        o.setId(rs.getObject("id", UUID.class));
        o.setQuestionId(rs.getObject("question_id", UUID.class));
        o.setPosition(rs.getInt("position"));
        o.setLabel(rs.getString("label"));
        o.setCorrect(rs.getBoolean("is_correct"));
        return o;
    };

    /** Active questions for a skill (with options), ordered by position. Used to build a test/attempt. */
    public List<PlacementQuestion> findActiveBySkill(Skill skill) {
        return withOptions(jdbc.query(
                "SELECT * FROM placement_questions WHERE skill = :skill AND active = true ORDER BY position",
                Map.of("skill", skill.name()), QUESTION_MAPPER));
    }

    /** All questions for a skill (admin view, including inactive), ordered by position. */
    public List<PlacementQuestion> findAllBySkill(Skill skill) {
        return withOptions(jdbc.query(
                "SELECT * FROM placement_questions WHERE skill = :skill ORDER BY position",
                Map.of("skill", skill.name()), QUESTION_MAPPER));
    }

    public Optional<PlacementQuestion> findById(UUID id) {
        List<PlacementQuestion> found = withOptions(jdbc.query(
                "SELECT * FROM placement_questions WHERE id = :id",
                Map.of("id", id), QUESTION_MAPPER));
        return found.stream().findFirst();
    }

    /** Find several questions by id at once (used when reconstructing answers for grading/review). */
    public List<PlacementQuestion> findByIds(List<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        return withOptions(jdbc.query(
                "SELECT * FROM placement_questions WHERE id IN (:ids)",
                Map.of("ids", ids), QUESTION_MAPPER));
    }

    private List<PlacementQuestion> withOptions(List<PlacementQuestion> questions) {
        if (questions.isEmpty()) return questions;
        Map<UUID, PlacementQuestion> byId = new LinkedHashMap<>();
        for (PlacementQuestion q : questions) byId.put(q.getId(), q);

        List<PlacementQuestionOption> options = jdbc.query(
                "SELECT * FROM placement_question_options WHERE question_id IN (:ids) ORDER BY position",
                Map.of("ids", byId.keySet()), OPTION_MAPPER);
        for (PlacementQuestionOption o : options) {
            PlacementQuestion q = byId.get(o.getQuestionId());
            if (q != null) q.getOptions().add(o);
        }
        return questions;
    }

    @Transactional
    public PlacementQuestion create(PlacementQuestion question) {
        UUID id = UUID.randomUUID();
        question.setId(id);
        int nextPosition = jdbc.queryForObject(
                "SELECT COALESCE(MAX(position), -1) + 1 FROM placement_questions WHERE skill = :skill",
                Map.of("skill", question.getSkill().name()), Integer.class);
        question.setPosition(nextPosition);
        insert(question);
        return question;
    }

    @Transactional
    public PlacementQuestion update(PlacementQuestion question) {
        jdbc.update("""
                UPDATE placement_questions SET
                    kind = :kind, prompt = :prompt,
                    audio_url = :audioUrl, audio_file_id = :audioFileId, active = :active
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", question.getId())
                .addValue("kind", question.getKind().name())
                .addValue("prompt", question.getPrompt())
                .addValue("audioUrl", question.getAudioUrl())
                .addValue("audioFileId", question.getAudioFileId())
                .addValue("active", question.isActive()));
        jdbc.update("DELETE FROM placement_question_options WHERE question_id = :qid",
                Map.of("qid", question.getId()));
        insertOptions(question.getId(), question.getOptions());
        return question;
    }

    private void insert(PlacementQuestion question) {
        jdbc.update("""
                INSERT INTO placement_questions (id, skill, position, kind, prompt, audio_url, audio_file_id, active)
                VALUES (:id, :skill, :position, :kind, :prompt, :audioUrl, :audioFileId, :active)
                """, new MapSqlParameterSource()
                .addValue("id", question.getId())
                .addValue("skill", question.getSkill().name())
                .addValue("position", question.getPosition())
                .addValue("kind", question.getKind().name())
                .addValue("prompt", question.getPrompt())
                .addValue("audioUrl", question.getAudioUrl())
                .addValue("audioFileId", question.getAudioFileId())
                .addValue("active", question.isActive()));
        insertOptions(question.getId(), question.getOptions());
    }

    private void insertOptions(UUID questionId, List<PlacementQuestionOption> options) {
        int pos = 0;
        for (PlacementQuestionOption o : options) {
            jdbc.update("""
                    INSERT INTO placement_question_options (id, question_id, position, label, is_correct)
                    VALUES (:id, :qid, :position, :label, :correct)
                    """, new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID())
                    .addValue("qid", questionId)
                    .addValue("position", pos++)
                    .addValue("label", o.getLabel())
                    .addValue("correct", o.isCorrect()));
        }
    }

    public void delete(UUID id) {
        jdbc.update("DELETE FROM placement_questions WHERE id = :id", Map.of("id", id));
    }

    @Transactional
    public void reorder(Skill skill, List<UUID> orderedIds) {
        int pos = 0;
        for (UUID id : orderedIds) {
            jdbc.update("UPDATE placement_questions SET position = :position WHERE id = :id AND skill = :skill",
                    new MapSqlParameterSource()
                            .addValue("position", pos++)
                            .addValue("id", id)
                            .addValue("skill", skill.name()));
        }
    }
}
