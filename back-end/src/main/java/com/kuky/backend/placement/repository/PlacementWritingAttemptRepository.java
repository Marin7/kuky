package com.kuky.backend.placement.repository;

import com.kuky.backend.placement.model.PlacementWritingAttempt;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Timing rows for Writing attempts — mirrors PlacementAttemptRepository's section methods. */
@Repository
public class PlacementWritingAttemptRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PlacementWritingAttemptRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<PlacementWritingAttempt> MAPPER = (rs, n) -> {
        PlacementWritingAttempt a = new PlacementWritingAttempt();
        a.setId(rs.getObject("id", UUID.class));
        a.setUserId(rs.getObject("user_id", UUID.class));
        a.setStartedAt(rs.getTimestamp("started_at").toInstant());
        a.setDeadlineAt(rs.getTimestamp("deadline_at").toInstant());
        a.setSubmittedAt(rs.getTimestamp("submitted_at") == null ? null : rs.getTimestamp("submitted_at").toInstant());
        return a;
    };

    public Optional<PlacementWritingAttempt> findInProgress(UUID userId) {
        return jdbc.query("""
                SELECT * FROM placement_writing_attempts
                WHERE user_id = :uid AND submitted_at IS NULL
                ORDER BY started_at DESC LIMIT 1
                """, Map.of("uid", userId), MAPPER).stream().findFirst();
    }

    public PlacementWritingAttempt create(UUID userId, Instant now, Instant deadline) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO placement_writing_attempts (id, user_id, started_at, deadline_at)
                VALUES (:id, :uid, :now, :deadline)
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("uid", userId)
                .addValue("now", Timestamp.from(now)).addValue("deadline", Timestamp.from(deadline)));
        return findInProgress(userId).orElseThrow();
    }

    public void markSubmitted(UUID id, Instant now) {
        jdbc.update("UPDATE placement_writing_attempts SET submitted_at = :now WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("now", Timestamp.from(now)));
    }
}
