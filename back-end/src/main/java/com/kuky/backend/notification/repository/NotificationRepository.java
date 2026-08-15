package com.kuky.backend.notification.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Repository
public class NotificationRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public NotificationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean hasUnseenHomeworkSubmissions() {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM homework_submissions
                WHERE status IN ('SUBMITTED', 'REVIEWED', 'GRADED')
                  AND teacher_seen_at IS NULL
                """, Map.of(), Integer.class);
        return n != null && n > 0;
    }

    public boolean hasUnseenQuizAttempts() {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM quiz_attempts
                WHERE status IN ('SUBMITTED', 'GRADED')
                  AND teacher_seen_at IS NULL
                """, Map.of(), Integer.class);
        return n != null && n > 0;
    }

    public boolean hasUnseenLearning(UUID userId) {
        Integer n = jdbc.queryForObject("""
                SELECT (
                    (SELECT COUNT(*) FROM unit_assignments
                     WHERE user_id = :uid AND student_seen_at IS NULL)
                  + (SELECT COUNT(*) FROM quiz_assignees
                     WHERE user_id = :uid AND student_seen_at IS NULL)
                  + (SELECT COUNT(*) FROM homework_targets
                     WHERE user_id = :uid AND student_seen_at IS NULL)
                )
                """, Map.of("uid", userId), Integer.class);
        return n != null && n > 0;
    }

    public void markHomeworkTeacherSeen(UUID submissionId) {
        jdbc.update("""
                UPDATE homework_submissions
                SET teacher_seen_at = NOW()
                WHERE id = :id AND teacher_seen_at IS NULL
                """, Map.of("id", submissionId));
    }

    public void markQuizAttemptTeacherSeen(UUID attemptId) {
        jdbc.update("""
                UPDATE quiz_attempts
                SET teacher_seen_at = NOW()
                WHERE id = :id AND teacher_seen_at IS NULL
                """, Map.of("id", attemptId));
    }

    public boolean isUnitAssigned(UUID unitId, UUID userId) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM unit_assignments
                WHERE unit_id = :unitId AND user_id = :uid
                """, Map.of("unitId", unitId, "uid", userId), Integer.class);
        return n != null && n > 0;
    }

    public void markUnitStudentSeen(UUID unitId, UUID userId) {
        jdbc.update("""
                UPDATE unit_assignments
                SET student_seen_at = NOW()
                WHERE unit_id = :unitId AND user_id = :uid AND student_seen_at IS NULL
                """, Map.of("unitId", unitId, "uid", userId));
    }

    public void markQuizAssigneeStudentSeen(UUID quizId, UUID userId) {
        jdbc.update("""
                UPDATE quiz_assignees
                SET student_seen_at = NOW()
                WHERE quiz_id = :qid AND user_id = :uid AND student_seen_at IS NULL
                """, Map.of("qid", quizId, "uid", userId));
    }

    public boolean isHomeworkAssigned(UUID assignmentId, UUID userId) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM homework_targets
                WHERE assignment_id = :aid AND user_id = :uid
                """, Map.of("aid", assignmentId, "uid", userId), Integer.class);
        return n != null && n > 0;
    }

    public void markHomeworkTargetStudentSeen(UUID assignmentId, UUID userId) {
        jdbc.update("""
                UPDATE homework_targets
                SET student_seen_at = NOW()
                WHERE assignment_id = :aid AND user_id = :uid AND student_seen_at IS NULL
                """, Map.of("aid", assignmentId, "uid", userId));
    }

    public Set<UUID> findUnseenUnitIds(UUID userId) {
        List<UUID> ids = jdbc.query("""
                SELECT unit_id FROM unit_assignments
                WHERE user_id = :uid AND student_seen_at IS NULL
                """, Map.of("uid", userId), (rs, n) -> rs.getObject("unit_id", UUID.class));
        return new HashSet<>(ids);
    }

    public Set<UUID> findUnseenQuizIds(UUID userId) {
        List<UUID> ids = jdbc.query("""
                SELECT quiz_id FROM quiz_assignees
                WHERE user_id = :uid AND student_seen_at IS NULL
                """, Map.of("uid", userId), (rs, n) -> rs.getObject("quiz_id", UUID.class));
        return new HashSet<>(ids);
    }

    public Set<UUID> findUnseenHomeworkIds(UUID userId) {
        List<UUID> ids = jdbc.query("""
                SELECT assignment_id FROM homework_targets
                WHERE user_id = :uid AND student_seen_at IS NULL
                """, Map.of("uid", userId), (rs, n) -> rs.getObject("assignment_id", UUID.class));
        return new HashSet<>(ids);
    }
}
