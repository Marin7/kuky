package com.kuky.backend.quiz.repository;

import com.kuky.backend.quiz.model.Quiz;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class QuizRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public QuizRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Quiz> MAPPER = (rs, n) -> {
        Quiz q = new Quiz();
        q.setId(rs.getObject("id", UUID.class));
        q.setTitle(rs.getString("title"));
        q.setDescription(rs.getString("description"));
        q.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        q.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return q;
    };

    public List<Quiz> findAll() {
        return jdbc.query("SELECT * FROM quizzes ORDER BY updated_at DESC", MAPPER);
    }

    public List<Quiz> findAssignedToUser(UUID userId) {
        return jdbc.query("""
                SELECT q.* FROM quizzes q
                JOIN quiz_assignees a ON a.quiz_id = q.id
                WHERE a.user_id = :uid
                ORDER BY q.title
                """, Map.of("uid", userId), MAPPER);
    }

    public Optional<Quiz> findById(UUID id) {
        List<Quiz> rows = jdbc.query("SELECT * FROM quizzes WHERE id = :id", Map.of("id", id), MAPPER);
        return rows.stream().findFirst();
    }

    public Quiz insert(Quiz quiz) {
        UUID id = quiz.getId() != null ? quiz.getId() : UUID.randomUUID();
        jdbc.update("""
                INSERT INTO quizzes (id, title, description)
                VALUES (:id, :title, :description)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("title", quiz.getTitle())
                .addValue("description", quiz.getDescription()));
        return findById(id).orElseThrow();
    }

    public void update(Quiz quiz) {
        jdbc.update("""
                UPDATE quizzes
                SET title = :title, description = :description, updated_at = NOW()
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", quiz.getId())
                .addValue("title", quiz.getTitle())
                .addValue("description", quiz.getDescription()));
    }

    public void touchUpdatedAt(UUID id) {
        jdbc.update("UPDATE quizzes SET updated_at = NOW() WHERE id = :id", Map.of("id", id));
    }

    public int delete(UUID id) {
        return jdbc.update("DELETE FROM quizzes WHERE id = :id", Map.of("id", id));
    }

    public record QuizListRow(UUID id, String title, int questionCount, int assigneeCount, int attemptCount,
                              boolean hasUnseenAttempts) {}

    public List<QuizListRow> listWithCounts() {
        return jdbc.query("""
                SELECT q.id, q.title,
                       (SELECT COUNT(*) FROM quiz_questions qq WHERE qq.quiz_id = q.id AND qq.retired = false) AS question_count,
                       (SELECT COUNT(*) FROM quiz_assignees a WHERE a.quiz_id = q.id) AS assignee_count,
                       (SELECT COUNT(*) FROM quiz_attempts t WHERE t.quiz_id = q.id AND t.status <> 'IN_PROGRESS') AS attempt_count,
                       EXISTS (
                           SELECT 1 FROM quiz_attempts t
                           WHERE t.quiz_id = q.id
                             AND t.status IN ('SUBMITTED', 'GRADED')
                             AND t.teacher_seen_at IS NULL
                       ) AS has_unseen
                FROM quizzes q
                ORDER BY q.updated_at DESC
                """, (rs, n) -> new QuizListRow(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getInt("question_count"),
                rs.getInt("assignee_count"),
                rs.getInt("attempt_count"),
                rs.getBoolean("has_unseen")));
    }
}
