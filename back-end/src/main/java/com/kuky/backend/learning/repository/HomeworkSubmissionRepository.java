package com.kuky.backend.learning.repository;

import com.kuky.backend.learning.model.HomeworkSubmission;
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

/**
 * Per-student homework state. A row keyed by (user_id, assignment_id); absence
 * of a row means the assignment is PENDING for that student.
 */
@Repository
public class HomeworkSubmissionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public HomeworkSubmissionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<HomeworkSubmission> SUBMISSION_MAPPER = (rs, rowNum) -> {
        HomeworkSubmission s = new HomeworkSubmission();
        s.setId(rs.getObject("id", UUID.class));
        s.setUserId(rs.getObject("user_id", UUID.class));
        s.setAssignmentId(rs.getObject("assignment_id", UUID.class));
        s.setStatus(rs.getString("status"));
        s.setResponseText(rs.getString("response_text"));
        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        if (submittedAt != null) {
            s.setSubmittedAt(submittedAt.toInstant());
        }
        s.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return s;
    };

    public List<HomeworkSubmission> findByUserId(UUID userId) {
        String sql = "SELECT * FROM homework_submissions WHERE user_id = :userId";
        return jdbc.query(sql, Map.of("userId", userId), SUBMISSION_MAPPER);
    }

    public Optional<HomeworkSubmission> findByUserAndAssignment(UUID userId, UUID assignmentId) {
        String sql = "SELECT * FROM homework_submissions WHERE user_id = :userId AND assignment_id = :assignmentId";
        return jdbc.query(sql,
                Map.of("userId", userId, "assignmentId", assignmentId),
                SUBMISSION_MAPPER).stream().findFirst();
    }

    /**
     * Insert or update the student's submission for an assignment. Keyed by the
     * UNIQUE (user_id, assignment_id) constraint.
     */
    public HomeworkSubmission upsert(UUID userId, UUID assignmentId, String status,
                                     String responseText, Instant submittedAt) {
        Instant now = Instant.now();
        String sql = """
                INSERT INTO homework_submissions
                    (id, user_id, assignment_id, status, response_text, submitted_at, updated_at)
                VALUES
                    (gen_random_uuid(), :userId, :assignmentId, :status, :responseText, :submittedAt, :updatedAt)
                ON CONFLICT (user_id, assignment_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    response_text = EXCLUDED.response_text,
                    submitted_at = EXCLUDED.submitted_at,
                    updated_at = EXCLUDED.updated_at
                RETURNING *
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("assignmentId", assignmentId)
                .addValue("status", status)
                .addValue("responseText", responseText)
                .addValue("submittedAt", submittedAt == null ? null : Timestamp.from(submittedAt))
                .addValue("updatedAt", Timestamp.from(now));
        return jdbc.query(sql, params, SUBMISSION_MAPPER).stream().findFirst().orElseThrow();
    }
}
