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
    public record AssigneeView(UUID userId, String email, String firstName, String lastName, String username,
                               String status, String responseText, Instant submittedAt, Integer scorePercent) {}

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
                SELECT u.id AS user_id, u.email, u.first_name, u.last_name, u.username,
                       COALESCE(s.status, 'PENDING') AS status,
                       s.response_text,
                       s.submitted_at,
                       s.score_percent
                FROM homework_targets t
                JOIN users u ON u.id = t.user_id
                LEFT JOIN homework_submissions s
                       ON s.assignment_id = t.assignment_id AND s.user_id = t.user_id
                WHERE t.assignment_id = :aid
                ORDER BY u.email
                """;
        return jdbc.query(sql, Map.of("aid", assignmentId), (rs, n) -> {
            var submittedAt = rs.getTimestamp("submitted_at");
            Integer scorePercent = rs.getObject("score_percent", Integer.class);
            return new AssigneeView(
                    rs.getObject("user_id", UUID.class),
                    rs.getString("email"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("username"),
                    rs.getString("status"),
                    rs.getString("response_text"),
                    submittedAt == null ? null : submittedAt.toInstant(),
                    scorePercent);
        });
    }

    /** Whether an assignment is assigned to a given student. */
    public boolean isAssignedTo(UUID assignmentId, UUID userId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM homework_targets
                WHERE assignment_id = :aid AND user_id = :uid
                """, Map.of("aid", assignmentId, "uid", userId), Integer.class);
        return count != null && count > 0;
    }

    public record StudentAssignmentView(UUID assignmentId, String title, String status, Instant submittedAt,
                                        String format, UUID submissionId) {}

    public List<StudentAssignmentView> findAssignmentsForStudent(UUID userId) {
        String sql = """
                SELECT ha.id AS assignment_id, ha.title,
                       COALESCE(s.status, 'PENDING') AS status,
                       s.submitted_at, ha.format, s.id AS submission_id
                FROM homework_targets t
                JOIN homework_assignments ha ON ha.id = t.assignment_id
                LEFT JOIN homework_submissions s
                       ON s.assignment_id = t.assignment_id AND s.user_id = t.user_id
                WHERE t.user_id = :uid
                ORDER BY ha.created_at DESC
                """;
        return jdbc.query(sql, Map.of("uid", userId), (rs, n) -> {
            var submittedAt = rs.getTimestamp("submitted_at");
            return new StudentAssignmentView(
                    rs.getObject("assignment_id", UUID.class),
                    rs.getString("title"),
                    rs.getString("status"),
                    submittedAt == null ? null : submittedAt.toInstant(),
                    rs.getString("format"),
                    rs.getObject("submission_id", UUID.class));
        });
    }
}
