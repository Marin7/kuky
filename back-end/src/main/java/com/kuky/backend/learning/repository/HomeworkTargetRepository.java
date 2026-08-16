package com.kuky.backend.learning.repository;

import com.kuky.backend.learning.model.FormattedTextSegment;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
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
                               String status, String responseText, Instant submittedAt, Integer scorePercent,
                               UUID submissionId, boolean hasTeacherFeedback, boolean unseen, LocalDate dueOn) {}

    public void replaceTargets(UUID assignmentId, List<UUID> userIds) {
        replaceTargets(assignmentId, userIds, null);
    }

    /**
     * Replaces assignees without resetting {@code student_seen_at} on students who remain.
     * Newly added students get {@code student_seen_at = NULL} (unseen) and {@code dueOn}
     * from this call. Existing rows keep their stored {@code due_on}.
     */
    @Transactional
    public void replaceTargets(UUID assignmentId, List<UUID> userIds, LocalDate dueOnForNew) {
        List<UUID> ids = userIds == null ? List.of() : userIds;
        if (ids.isEmpty()) {
            jdbc.update("DELETE FROM homework_targets WHERE assignment_id = :aid",
                    Map.of("aid", assignmentId));
            return;
        }
        jdbc.update("""
                DELETE FROM homework_targets
                WHERE assignment_id = :aid AND user_id NOT IN (:uids)
                """, Map.of("aid", assignmentId, "uids", ids));
        addTargets(assignmentId, ids, null, dueOnForNew);
    }

    /** Idempotent: adds targets for each user (skips existing). New rows are unseen, no due date. */
    @Transactional
    public void addTargets(UUID assignmentId, List<UUID> userIds) {
        addTargets(assignmentId, userIds, null, null);
    }

    /**
     * Idempotent add. Pass {@code studentSeenAt} non-null to create already-seen rows
     * (unit sync — student is notified via the unit assignment instead).
     */
    @Transactional
    public void addTargets(UUID assignmentId, List<UUID> userIds, Instant studentSeenAt) {
        addTargets(assignmentId, userIds, studentSeenAt, null);
    }

    @Transactional
    public void addTargets(UUID assignmentId, List<UUID> userIds, Instant studentSeenAt, LocalDate dueOn) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (UUID userId : userIds) {
            jdbc.update("""
                    INSERT INTO homework_targets (id, assignment_id, user_id, student_seen_at, due_on)
                    VALUES (:id, :aid, :uid, :seenAt, :dueOn)
                    ON CONFLICT (assignment_id, user_id) DO NOTHING
                    """, new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID())
                    .addValue("aid", assignmentId)
                    .addValue("uid", userId)
                    .addValue("seenAt", studentSeenAt == null ? null : Timestamp.from(studentSeenAt))
                    .addValue("dueOn", dueOn == null ? null : Date.valueOf(dueOn)));
        }
    }

    /** Removes targets for the given users only (leaves other assignees intact). */
    @Transactional
    public void removeTargets(UUID assignmentId, List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        jdbc.update("""
                DELETE FROM homework_targets
                WHERE assignment_id = :aid AND user_id IN (:uids)
                """, Map.of("aid", assignmentId, "uids", userIds));
    }

    public int updateDueOn(UUID assignmentId, UUID userId, LocalDate dueOn) {
        return jdbc.update("""
                UPDATE homework_targets SET due_on = :dueOn
                WHERE assignment_id = :aid AND user_id = :uid
                """, new MapSqlParameterSource()
                .addValue("dueOn", dueOn == null ? null : Date.valueOf(dueOn))
                .addValue("aid", assignmentId)
                .addValue("uid", userId));
    }

    public LocalDate findDueOn(UUID assignmentId, UUID userId) {
        List<LocalDate> rows = jdbc.query("""
                SELECT due_on FROM homework_targets
                WHERE assignment_id = :aid AND user_id = :uid
                """, Map.of("aid", assignmentId, "uid", userId),
                (rs, n) -> rs.getObject("due_on", LocalDate.class));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public Map<UUID, LocalDate> findDueOnsForUser(UUID userId) {
        Map<UUID, LocalDate> out = new HashMap<>();
        jdbc.query("""
                SELECT assignment_id, due_on FROM homework_targets WHERE user_id = :uid
                """, Map.of("uid", userId), rs -> {
            out.put(rs.getObject("assignment_id", UUID.class), rs.getObject("due_on", LocalDate.class));
        });
        return out;
    }

    public List<AssigneeView> findAssigneesWithSubmissions(UUID assignmentId) {
        String sql = """
                SELECT u.id AS user_id, u.email, u.first_name, u.last_name, u.username,
                       COALESCE(s.status, 'PENDING') AS status,
                       s.response_text,
                       s.submitted_at,
                       s.score_percent,
                       s.id AS submission_id,
                       s.feedback,
                       (s.status IN ('SUBMITTED', 'REVIEWED', 'GRADED') AND s.teacher_seen_at IS NULL) AS unseen,
                       t.due_on
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
                    scorePercent,
                    rs.getObject("submission_id", UUID.class),
                    FormattedTextSegment.hasTeacherFeedback(rs.getString("feedback")),
                    rs.getBoolean("unseen"),
                    rs.getObject("due_on", LocalDate.class));
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
                                        String format, UUID submissionId, Integer scorePercent,
                                        boolean hasTeacherFeedback, boolean unseen, LocalDate dueOn) {}

    public List<StudentAssignmentView> findAssignmentsForStudent(UUID userId) {
        String sql = """
                SELECT ha.id AS assignment_id, ha.title,
                       COALESCE(s.status, 'PENDING') AS status,
                       s.submitted_at, ha.format, s.id AS submission_id, s.score_percent, s.feedback,
                       (s.status IN ('SUBMITTED', 'REVIEWED', 'GRADED') AND s.teacher_seen_at IS NULL) AS unseen,
                       t.due_on
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
                    rs.getObject("submission_id", UUID.class),
                    rs.getObject("score_percent", Integer.class),
                    FormattedTextSegment.hasTeacherFeedback(rs.getString("feedback")),
                    rs.getBoolean("unseen"),
                    rs.getObject("due_on", LocalDate.class));
        });
    }
}
