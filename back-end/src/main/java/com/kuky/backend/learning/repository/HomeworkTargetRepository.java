package com.kuky.backend.learning.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Which students a homework assignment is assigned to, plus each assignee's submission state. */
@Repository
public class HomeworkTargetRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public HomeworkTargetRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** An assignee of an assignment, with their submission status (PENDING if no row). */
    public record AssigneeView(UUID userId, String email, String status,
                               String responseText, Instant submittedAt) {}

    @Transactional
    public void replaceTargets(UUID assignmentId, List<UUID> userIds) {
        jdbc.update("DELETE FROM homework_targets WHERE assignment_id = :aid",
                Map.of("aid", assignmentId));
        for (UUID userId : userIds) {
            jdbc.update("""
                    INSERT INTO homework_targets (id, assignment_id, user_id)
                    VALUES (:id, :aid, :uid)
                    ON CONFLICT (assignment_id, user_id) DO NOTHING
                    """, new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID())
                    .addValue("aid", assignmentId)
                    .addValue("uid", userId));
        }
    }

    public List<AssigneeView> findAssigneesWithSubmissions(UUID assignmentId) {
        String sql = """
                SELECT u.id AS user_id, u.email AS email,
                       COALESCE(s.status, 'PENDING') AS status,
                       s.response_text AS response_text,
                       s.submitted_at AS submitted_at
                FROM homework_targets t
                JOIN users u ON u.id = t.user_id
                LEFT JOIN homework_submissions s
                       ON s.assignment_id = t.assignment_id AND s.user_id = t.user_id
                WHERE t.assignment_id = :aid
                ORDER BY u.email
                """;
        return jdbc.query(sql, Map.of("aid", assignmentId), (rs, n) -> {
            var submittedAt = rs.getTimestamp("submitted_at");
            return new AssigneeView(
                    rs.getObject("user_id", UUID.class),
                    rs.getString("email"),
                    rs.getString("status"),
                    rs.getString("response_text"),
                    submittedAt == null ? null : submittedAt.toInstant());
        });
    }
}
