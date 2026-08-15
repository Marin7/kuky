package com.kuky.backend.quiz.repository;

import com.kuky.backend.quiz.model.QuizAnswer;
import com.kuky.backend.quiz.model.QuizAttempt;
import com.kuky.backend.quiz.model.QuizAttemptStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class QuizAttemptRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public QuizAttemptRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<QuizAttempt> ATTEMPT_MAPPER = (rs, n) -> {
        QuizAttempt a = new QuizAttempt();
        a.setId(rs.getObject("id", UUID.class));
        a.setQuizId(rs.getObject("quiz_id", UUID.class));
        a.setUserId(rs.getObject("user_id", UUID.class));
        a.setStatus(QuizAttemptStatus.valueOf(rs.getString("status")));
        Timestamp started = rs.getTimestamp("started_at");
        a.setStartedAt(started == null ? null : started.toInstant());
        Timestamp submitted = rs.getTimestamp("submitted_at");
        a.setSubmittedAt(submitted == null ? null : submitted.toInstant());
        int score = rs.getInt("score_percent");
        a.setScorePercent(rs.wasNull() ? null : score);
        int fc = rs.getInt("fully_correct_count");
        a.setFullyCorrectCount(rs.wasNull() ? null : fc);
        int qc = rs.getInt("question_unit_count");
        a.setQuestionUnitCount(rs.wasNull() ? null : qc);
        a.setQuizSnapshot(rs.getString("quiz_snapshot"));
        a.setFeedback(rs.getString("feedback"));
        Timestamp seen = rs.getTimestamp("teacher_seen_at");
        a.setTeacherSeenAt(seen == null ? null : seen.toInstant());
        return a;
    };

    private static final RowMapper<QuizAnswer> ANSWER_MAPPER = (rs, n) -> {
        QuizAnswer a = new QuizAnswer();
        a.setId(rs.getObject("id", UUID.class));
        a.setAttemptId(rs.getObject("attempt_id", UUID.class));
        a.setQuestionId(rs.getObject("question_id", UUID.class));
        a.setAnswerJson(rs.getString("answer_json"));
        a.setAnswerText(rs.getString("answer_text"));
        a.setScore(rs.getBigDecimal("score"));
        int tp = rs.getInt("teacher_percent");
        a.setTeacherPercent(rs.wasNull() ? null : tp);
        Array arr = rs.getArray("selected_option_ids");
        if (arr != null) {
            Object raw = arr.getArray();
            if (raw instanceof UUID[] uuids) {
                a.setSelectedOptionIds(List.of(uuids));
            } else if (raw instanceof Object[] objs) {
                List<UUID> ids = new ArrayList<>();
                for (Object o : objs) {
                    if (o != null) ids.add(UUID.fromString(o.toString()));
                }
                a.setSelectedOptionIds(ids);
            }
        }
        return a;
    };

    public Optional<QuizAttempt> findByQuizAndUser(UUID quizId, UUID userId) {
        List<QuizAttempt> rows = jdbc.query(
                "SELECT * FROM quiz_attempts WHERE quiz_id = :qid AND user_id = :uid",
                Map.of("qid", quizId, "uid", userId), ATTEMPT_MAPPER);
        return rows.stream().findFirst();
    }

    public Optional<QuizAttempt> findById(UUID id) {
        List<QuizAttempt> rows = jdbc.query(
                "SELECT * FROM quiz_attempts WHERE id = :id", Map.of("id", id), ATTEMPT_MAPPER);
        return rows.stream().findFirst();
    }

    public List<QuizAttempt> findByQuiz(UUID quizId) {
        return jdbc.query(
                "SELECT * FROM quiz_attempts WHERE quiz_id = :qid ORDER BY submitted_at DESC NULLS LAST, started_at DESC",
                Map.of("qid", quizId), ATTEMPT_MAPPER);
    }

    public List<QuizAttempt> findByUser(UUID userId) {
        return jdbc.query(
                "SELECT * FROM quiz_attempts WHERE user_id = :uid ORDER BY started_at DESC",
                Map.of("uid", userId), ATTEMPT_MAPPER);
    }

    public record ReviewQueueRow(
            UUID attemptId,
            UUID quizId,
            String quizTitle,
            UUID studentId,
            String studentEmail,
            String studentFirstName,
            String studentLastName,
            String studentUsername,
            Instant submittedAt,
            boolean unseen
    ) {}

    /** SUBMITTED attempts (free-text still awaiting teacher), oldest first. */
    public List<ReviewQueueRow> findSubmittedQueue() {
        String sql = """
                SELECT a.id AS attempt_id, a.quiz_id, q.title AS quiz_title,
                       u.id AS student_id, u.email AS student_email,
                       u.first_name AS student_first_name, u.last_name AS student_last_name,
                       u.username AS student_username, a.submitted_at,
                       (a.teacher_seen_at IS NULL) AS unseen
                FROM quiz_attempts a
                JOIN quizzes q ON q.id = a.quiz_id
                JOIN users u ON u.id = a.user_id
                WHERE a.status = 'SUBMITTED'
                ORDER BY a.submitted_at ASC NULLS LAST
                """;
        return jdbc.query(sql, Map.of(), (rs, n) -> {
            Timestamp submittedAt = rs.getTimestamp("submitted_at");
            return new ReviewQueueRow(
                    rs.getObject("attempt_id", UUID.class),
                    rs.getObject("quiz_id", UUID.class),
                    rs.getString("quiz_title"),
                    rs.getObject("student_id", UUID.class),
                    rs.getString("student_email"),
                    rs.getString("student_first_name"),
                    rs.getString("student_last_name"),
                    rs.getString("student_username"),
                    submittedAt == null ? null : submittedAt.toInstant(),
                    rs.getBoolean("unseen"));
        });
    }

    public QuizAttempt insert(QuizAttempt attempt) {
        UUID id = attempt.getId() != null ? attempt.getId() : UUID.randomUUID();
        jdbc.update("""
                INSERT INTO quiz_attempts
                    (id, quiz_id, user_id, status, started_at, quiz_snapshot)
                VALUES (:id, :qid, :uid, :status, :started, CAST(:snapshot AS jsonb))
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("qid", attempt.getQuizId())
                .addValue("uid", attempt.getUserId())
                .addValue("status", attempt.getStatus().name())
                .addValue("started", Timestamp.from(attempt.getStartedAt() == null ? Instant.now() : attempt.getStartedAt()))
                .addValue("snapshot", attempt.getQuizSnapshot()));
        attempt.setId(id);
        return findById(id).orElseThrow();
    }

    public void updateAfterSubmit(QuizAttempt attempt) {
        jdbc.update("""
                UPDATE quiz_attempts
                SET status = :status, submitted_at = :submitted, score_percent = :score,
                    fully_correct_count = :fc, question_unit_count = :qc, feedback = :feedback,
                    teacher_seen_at = NULL
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", attempt.getId())
                .addValue("status", attempt.getStatus().name())
                .addValue("submitted", attempt.getSubmittedAt() == null ? null : Timestamp.from(attempt.getSubmittedAt()))
                .addValue("score", attempt.getScorePercent())
                .addValue("fc", attempt.getFullyCorrectCount())
                .addValue("qc", attempt.getQuestionUnitCount())
                .addValue("feedback", attempt.getFeedback()));
    }

    public int deleteInProgress(UUID quizId, UUID userId) {
        return jdbc.update("""
                DELETE FROM quiz_attempts
                WHERE quiz_id = :qid AND user_id = :uid AND status = 'IN_PROGRESS'
                """, Map.of("qid", quizId, "uid", userId));
    }

    public List<QuizAnswer> findAnswers(UUID attemptId) {
        return jdbc.query(
                "SELECT * FROM quiz_answers WHERE attempt_id = :aid",
                Map.of("aid", attemptId), ANSWER_MAPPER);
    }

    @Transactional
    public void replaceAnswers(UUID attemptId, List<QuizAnswer> answers) {
        jdbc.update("DELETE FROM quiz_answers WHERE attempt_id = :aid", Map.of("aid", attemptId));
        if (answers == null) return;
        for (QuizAnswer a : answers) {
            UUID id = a.getId() != null ? a.getId() : UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO quiz_answers
                        (id, attempt_id, question_id, answer_json, answer_text, score, teacher_percent, selected_option_ids)
                    VALUES (:id, :aid, :qid, CAST(:answerJson AS jsonb), :answerText, :score, :tp,
                            CAST(string_to_array(:opts, ',') AS uuid[]))
                    """, new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("aid", attemptId)
                    .addValue("qid", a.getQuestionId())
                    .addValue("answerJson", a.getAnswerJson())
                    .addValue("answerText", a.getAnswerText())
                    .addValue("score", a.getScore())
                    .addValue("tp", a.getTeacherPercent())
                    .addValue("opts", toUuidArray(a.getSelectedOptionIds())));
            a.setId(id);
        }
    }

    private static Object toUuidArray(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().map(UUID::toString).collect(java.util.stream.Collectors.joining(","));
    }

    public void updateAnswerTeacherPercent(UUID attemptId, UUID questionId, int percent) {
        jdbc.update("""
                UPDATE quiz_answers
                SET teacher_percent = :tp, score = :score
                WHERE attempt_id = :aid AND question_id = :qid
                """, new MapSqlParameterSource()
                .addValue("tp", percent)
                .addValue("score", BigDecimal.valueOf(percent).movePointLeft(2))
                .addValue("aid", attemptId)
                .addValue("qid", questionId));
    }
}
