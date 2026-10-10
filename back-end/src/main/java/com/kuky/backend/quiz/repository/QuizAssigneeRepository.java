package com.kuky.backend.quiz.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class QuizAssigneeRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public QuizAssigneeRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record AssigneeView(UUID userId, String email, String firstName, String lastName, String username) {}

    public boolean isAssigned(UUID quizId, UUID userId) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM quiz_assignees WHERE quiz_id = :qid AND user_id = :uid
                """, Map.of("qid", quizId, "uid", userId), Integer.class);
        return n != null && n > 0;
    }

    public List<UUID> findQuizIdsForUser(UUID userId) {
        return jdbc.query(
                "SELECT quiz_id FROM quiz_assignees WHERE user_id = :uid",
                Map.of("uid", userId),
                (rs, n) -> rs.getObject("quiz_id", UUID.class));
    }

    public List<AssigneeView> findAssignees(UUID quizId) {
        return jdbc.query("""
                SELECT u.id AS user_id, u.email, u.first_name, u.last_name, u.username
                FROM quiz_assignees a
                JOIN users u ON u.id = a.user_id
                WHERE a.quiz_id = :qid
                ORDER BY u.email
                """, Map.of("qid", quizId), (rs, n) -> new AssigneeView(
                rs.getObject("user_id", UUID.class),
                rs.getString("email"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("username")));
    }

    /**
     * Assigns the quiz to students through their unit. Rows are created already seen: the unit
     * assignment carries the student notification (as unit homework used to).
     */
    public void addUnitAssignees(UUID quizId, List<UUID> userIds) {
        for (UUID userId : userIds) {
            jdbc.update("""
                    INSERT INTO quiz_assignees (quiz_id, user_id, student_seen_at)
                    VALUES (:qid, :uid, NOW())
                    ON CONFLICT (quiz_id, user_id) DO NOTHING
                    """, new MapSqlParameterSource()
                    .addValue("qid", quizId)
                    .addValue("uid", userId));
        }
    }

    /** Unassigns the quiz from students who have not started it; anyone with an attempt keeps it. */
    public void removeUnstartedAssignees(UUID quizId, List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        jdbc.update("""
                DELETE FROM quiz_assignees qa
                WHERE qa.quiz_id = :qid AND qa.user_id IN (:uids)
                  AND NOT EXISTS (SELECT 1 FROM quiz_attempts t
                                  WHERE t.quiz_id = qa.quiz_id AND t.user_id = qa.user_id)
                """, Map.of("qid", quizId, "uids", userIds));
    }

    @Transactional
    public void replaceAssignees(UUID quizId, List<UUID> userIds) {
        List<UUID> ids = userIds == null ? List.of() : userIds;
        if (ids.isEmpty()) {
            jdbc.update("DELETE FROM quiz_assignees WHERE quiz_id = :qid", Map.of("qid", quizId));
            return;
        }
        jdbc.update("""
                DELETE FROM quiz_assignees
                WHERE quiz_id = :qid AND user_id NOT IN (:uids)
                """, Map.of("qid", quizId, "uids", ids));
        for (UUID userId : ids) {
            jdbc.update("""
                    INSERT INTO quiz_assignees (quiz_id, user_id)
                    VALUES (:qid, :uid)
                    ON CONFLICT (quiz_id, user_id) DO NOTHING
                    """, new MapSqlParameterSource()
                    .addValue("qid", quizId)
                    .addValue("uid", userId));
        }
    }
}
