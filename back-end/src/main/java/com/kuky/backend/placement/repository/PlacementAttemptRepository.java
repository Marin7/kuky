package com.kuky.backend.placement.repository;

import com.kuky.backend.placement.model.AttemptStatus;
import com.kuky.backend.placement.model.PlacementAttempt;
import com.kuky.backend.placement.model.PlacementAttemptSection;
import com.kuky.backend.placement.model.Skill;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Attempts (one run-through) and their per-section timing/result rows. */
@Repository
public class PlacementAttemptRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PlacementAttemptRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<PlacementAttempt> ATTEMPT_MAPPER = (rs, n) -> {
        PlacementAttempt a = new PlacementAttempt();
        a.setId(rs.getObject("id", UUID.class));
        a.setUserId(rs.getObject("user_id", UUID.class));
        a.setStatus(AttemptStatus.valueOf(rs.getString("status")));
        a.setStartedAt(rs.getTimestamp("started_at").toInstant());
        a.setCompletedAt(rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant());
        a.setOverallCefr(rs.getString("overall_cefr"));
        return a;
    };

    private static final RowMapper<PlacementAttemptSection> SECTION_MAPPER = (rs, n) -> {
        PlacementAttemptSection s = new PlacementAttemptSection();
        s.setId(rs.getObject("id", UUID.class));
        s.setAttemptId(rs.getObject("attempt_id", UUID.class));
        s.setSkill(Skill.valueOf(rs.getString("skill")));
        s.setStartedAt(rs.getTimestamp("started_at").toInstant());
        s.setDeadlineAt(rs.getTimestamp("deadline_at").toInstant());
        s.setSubmittedAt(rs.getTimestamp("submitted_at") == null ? null : rs.getTimestamp("submitted_at").toInstant());
        s.setScorePercent(rs.getObject("score_percent") == null ? null : rs.getInt("score_percent"));
        s.setCefrLevel(rs.getString("cefr_level"));
        return s;
    };

    public Optional<PlacementAttempt> findInProgress(UUID userId) {
        List<PlacementAttempt> rows = jdbc.query("""
                SELECT * FROM placement_attempts
                WHERE user_id = :uid AND status = 'IN_PROGRESS'
                ORDER BY started_at DESC LIMIT 1
                """, Map.of("uid", userId), ATTEMPT_MAPPER);
        return rows.stream().findFirst();
    }

    public Optional<PlacementAttempt> findById(UUID id) {
        return jdbc.query("SELECT * FROM placement_attempts WHERE id = :id", Map.of("id", id), ATTEMPT_MAPPER)
                .stream().findFirst();
    }

    /** Most recently completed attempt for a user (for the teacher's student-evaluation view). */
    public Optional<PlacementAttempt> findLatestCompleted(UUID userId) {
        return jdbc.query("""
                SELECT * FROM placement_attempts
                WHERE user_id = :uid AND status = 'COMPLETED'
                ORDER BY completed_at DESC LIMIT 1
                """, Map.of("uid", userId), ATTEMPT_MAPPER).stream().findFirst();
    }

    public PlacementAttempt create(UUID userId, Instant now) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO placement_attempts (id, user_id, status, started_at)
                VALUES (:id, :uid, 'IN_PROGRESS', :now)
                """, new MapSqlParameterSource().addValue("id", id).addValue("uid", userId)
                .addValue("now", Timestamp.from(now)));
        return findById(id).orElseThrow();
    }

    public void complete(UUID attemptId, String overallCefr, Instant now) {
        jdbc.update("""
                UPDATE placement_attempts SET status = 'COMPLETED', overall_cefr = :cefr, completed_at = :now
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", attemptId).addValue("cefr", overallCefr).addValue("now", Timestamp.from(now)));
    }

    public List<PlacementAttemptSection> findSections(UUID attemptId) {
        return jdbc.query("SELECT * FROM placement_attempt_sections WHERE attempt_id = :aid",
                Map.of("aid", attemptId), SECTION_MAPPER);
    }

    public Optional<PlacementAttemptSection> findSection(UUID attemptId, Skill skill) {
        return jdbc.query("SELECT * FROM placement_attempt_sections WHERE attempt_id = :aid AND skill = :skill",
                        Map.of("aid", attemptId, "skill", skill.name()), SECTION_MAPPER)
                .stream().findFirst();
    }

    public PlacementAttemptSection startSection(UUID attemptId, Skill skill, Instant now, Instant deadline) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO placement_attempt_sections (id, attempt_id, skill, started_at, deadline_at)
                VALUES (:id, :aid, :skill, :now, :deadline)
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("aid", attemptId).addValue("skill", skill.name())
                .addValue("now", Timestamp.from(now)).addValue("deadline", Timestamp.from(deadline)));
        return findSection(attemptId, skill).orElseThrow();
    }

    public void submitSection(UUID sectionId, int scorePercent, String cefrLevel, Instant now) {
        jdbc.update("""
                UPDATE placement_attempt_sections
                SET submitted_at = :now, score_percent = :score, cefr_level = :cefr
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", sectionId).addValue("now", Timestamp.from(now))
                .addValue("score", scorePercent).addValue("cefr", cefrLevel));
    }
}
