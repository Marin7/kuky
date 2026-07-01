package com.kuky.backend.placement.repository;

import com.kuky.backend.placement.model.PlacementWritingSubmission;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PlacementWritingRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PlacementWritingRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<PlacementWritingSubmission> MAPPER = (rs, n) -> {
        PlacementWritingSubmission s = new PlacementWritingSubmission();
        s.setId(rs.getObject("id", UUID.class));
        s.setUserId(rs.getObject("user_id", UUID.class));
        s.setWritingAttemptId(rs.getObject("writing_attempt_id", UUID.class));
        s.setBody(rs.getString("body"));
        s.setPromptSnapshot(rs.getString("prompt_snapshot"));
        s.setSubmittedAt(rs.getTimestamp("submitted_at").toInstant());
        return s;
    };

    public PlacementWritingSubmission insert(PlacementWritingSubmission submission) {
        UUID id = UUID.randomUUID();
        submission.setId(id);
        jdbc.update("""
                INSERT INTO placement_writing_submissions (id, user_id, writing_attempt_id, body, prompt_snapshot, submitted_at)
                VALUES (:id, :uid, :attemptId, :body, :prompt, :submittedAt)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("uid", submission.getUserId())
                .addValue("attemptId", submission.getWritingAttemptId())
                .addValue("body", submission.getBody())
                .addValue("prompt", submission.getPromptSnapshot())
                .addValue("submittedAt", Timestamp.from(submission.getSubmittedAt())));
        return submission;
    }

    public Optional<PlacementWritingSubmission> findLatestByUser(UUID userId) {
        return jdbc.query("""
                SELECT * FROM placement_writing_submissions
                WHERE user_id = :uid ORDER BY submitted_at DESC LIMIT 1
                """, Map.of("uid", userId), MAPPER).stream().findFirst();
    }

    public List<PlacementWritingSubmission> findByUser(UUID userId) {
        return jdbc.query("""
                SELECT * FROM placement_writing_submissions
                WHERE user_id = :uid ORDER BY submitted_at DESC
                """, Map.of("uid", userId), MAPPER);
    }
}
