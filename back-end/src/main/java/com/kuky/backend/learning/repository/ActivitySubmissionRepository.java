package com.kuky.backend.learning.repository;

import com.kuky.backend.learning.model.ActivitySubmission;
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

@Repository
public class ActivitySubmissionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ActivitySubmissionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<ActivitySubmission> MAPPER = (rs, n) -> {
        ActivitySubmission s = new ActivitySubmission();
        s.setId(rs.getObject("id", UUID.class));
        s.setUserId(rs.getObject("user_id", UUID.class));
        s.setActivityId(rs.getObject("activity_id", UUID.class));
        s.setStatus(rs.getString("status"));
        s.setResponseText(rs.getString("response_text"));
        s.setScorePercent(rs.getObject("score_percent", Integer.class));
        s.setFeedback(rs.getString("feedback"));
        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        if (submittedAt != null) s.setSubmittedAt(submittedAt.toInstant());
        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        if (reviewedAt != null) s.setReviewedAt(reviewedAt.toInstant());
        s.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return s;
    };

    public Optional<ActivitySubmission> findById(UUID id) {
        return jdbc.query("SELECT * FROM activity_submissions WHERE id = :id",
                Map.of("id", id), MAPPER).stream().findFirst();
    }

    public Optional<ActivitySubmission> findByUserAndActivity(UUID userId, UUID activityId) {
        return jdbc.query("""
                SELECT * FROM activity_submissions
                WHERE user_id = :uid AND activity_id = :aid
                """, Map.of("uid", userId, "aid", activityId), MAPPER).stream().findFirst();
    }

    public List<ActivitySubmission> findByUserId(UUID userId) {
        return jdbc.query(
                "SELECT * FROM activity_submissions WHERE user_id = :uid",
                Map.of("uid", userId), MAPPER);
    }

    public List<ActivitySubmission> findByActivityId(UUID activityId) {
        return jdbc.query(
                "SELECT * FROM activity_submissions WHERE activity_id = :aid",
                Map.of("aid", activityId), MAPPER);
    }

    public ActivitySubmission upsertManual(UUID userId, UUID activityId, String status,
                                           String responseText, Instant submittedAt) {
        Instant now = Instant.now();
        return jdbc.query("""
                INSERT INTO activity_submissions
                    (id, user_id, activity_id, status, response_text, submitted_at, updated_at)
                VALUES
                    (gen_random_uuid(), :uid, :aid, :status, :responseText, :submittedAt, :updatedAt)
                ON CONFLICT (user_id, activity_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    response_text = EXCLUDED.response_text,
                    submitted_at = EXCLUDED.submitted_at,
                    updated_at = EXCLUDED.updated_at
                RETURNING *
                """, new MapSqlParameterSource()
                .addValue("uid", userId)
                .addValue("aid", activityId)
                .addValue("status", status)
                .addValue("responseText", responseText)
                .addValue("submittedAt", submittedAt == null ? null : Timestamp.from(submittedAt))
                .addValue("updatedAt", Timestamp.from(now)), MAPPER).stream().findFirst().orElseThrow();
    }

    public ActivitySubmission upsertGraded(UUID userId, UUID activityId, int scorePercent, Instant submittedAt) {
        Instant now = Instant.now();
        return jdbc.query("""
                INSERT INTO activity_submissions
                    (id, user_id, activity_id, status, response_text, score_percent, submitted_at, updated_at)
                VALUES
                    (gen_random_uuid(), :uid, :aid, 'GRADED', NULL, :scorePercent, :submittedAt, :updatedAt)
                ON CONFLICT (user_id, activity_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    response_text = EXCLUDED.response_text,
                    score_percent = EXCLUDED.score_percent,
                    submitted_at = EXCLUDED.submitted_at,
                    updated_at = EXCLUDED.updated_at
                RETURNING *
                """, new MapSqlParameterSource()
                .addValue("uid", userId)
                .addValue("aid", activityId)
                .addValue("scorePercent", scorePercent)
                .addValue("submittedAt", submittedAt == null ? null : Timestamp.from(submittedAt))
                .addValue("updatedAt", Timestamp.from(now)), MAPPER).stream().findFirst().orElseThrow();
    }

    public ActivitySubmission saveFeedback(UUID submissionId, String feedbackJson) {
        Instant now = Instant.now();
        return jdbc.query("""
                UPDATE activity_submissions SET
                    feedback = :feedback,
                    status = 'REVIEWED',
                    reviewed_at = :reviewedAt,
                    updated_at = :updatedAt
                WHERE id = :id
                RETURNING *
                """, new MapSqlParameterSource()
                .addValue("id", submissionId)
                .addValue("feedback", feedbackJson)
                .addValue("reviewedAt", Timestamp.from(now))
                .addValue("updatedAt", Timestamp.from(now)), MAPPER).stream().findFirst().orElseThrow();
    }

    public ActivitySubmission saveExerciseFeedback(UUID submissionId, String feedback) {
        Instant now = Instant.now();
        return jdbc.query("""
                UPDATE activity_submissions SET
                    feedback = :feedback,
                    reviewed_at = :reviewedAt,
                    updated_at = :updatedAt
                WHERE id = :id
                RETURNING *
                """, new MapSqlParameterSource()
                .addValue("id", submissionId)
                .addValue("feedback", feedback)
                .addValue("reviewedAt", Timestamp.from(now))
                .addValue("updatedAt", Timestamp.from(now)), MAPPER).stream().findFirst().orElseThrow();
    }

    public record ReviewQueueRow(
            UUID submissionId, UUID studentId, String studentEmail,
            String studentFirstName, String studentLastName, String studentUsername,
            String activityTitle, Instant submittedAt) {}

    public List<ReviewQueueRow> findSubmittedManualQueue() {
        return jdbc.query("""
                SELECT s.id AS submission_id, u.id AS student_id, u.email AS student_email,
                       u.first_name AS student_first_name, u.last_name AS student_last_name,
                       u.username AS student_username, a.title AS activity_title, s.submitted_at
                FROM activity_submissions s
                JOIN users u ON u.id = s.user_id
                JOIN activities a ON a.id = s.activity_id
                WHERE s.status = 'SUBMITTED' AND a.format = 'MANUAL'
                ORDER BY s.submitted_at ASC
                """, Map.of(), (rs, n) -> {
            var submittedAt = rs.getTimestamp("submitted_at");
            return new ReviewQueueRow(
                    rs.getObject("submission_id", UUID.class),
                    rs.getObject("student_id", UUID.class),
                    rs.getString("student_email"),
                    rs.getString("student_first_name"),
                    rs.getString("student_last_name"),
                    rs.getString("student_username"),
                    rs.getString("activity_title"),
                    submittedAt == null ? null : submittedAt.toInstant());
        });
    }

    public record Breakdown(int pending, int submitted, int completed) {}

    /** Counts for activities the student can access (share or unit assignment). */
    public Breakdown countBreakdownForStudent(UUID userId) {
        Integer pending = jdbc.queryForObject("""
                SELECT COUNT(1) FROM activities a
                WHERE (
                    EXISTS (SELECT 1 FROM presentation_shares s WHERE s.presentation_id = a.presentation_id AND s.user_id = :uid)
                    OR EXISTS (
                        SELECT 1 FROM presentations p
                        JOIN unit_assignments ua ON ua.unit_id = p.unit_id
                        WHERE p.id = a.presentation_id AND ua.user_id = :uid
                    )
                )
                AND NOT EXISTS (
                    SELECT 1 FROM activity_submissions s
                    WHERE s.activity_id = a.id AND s.user_id = :uid
                      AND s.status IN ('SUBMITTED', 'REVIEWED', 'GRADED')
                )
                """, Map.of("uid", userId), Integer.class);
        Integer submitted = jdbc.queryForObject("""
                SELECT COUNT(1) FROM activity_submissions s
                JOIN activities a ON a.id = s.activity_id
                WHERE s.user_id = :uid AND s.status = 'SUBMITTED'
                  AND (
                    EXISTS (SELECT 1 FROM presentation_shares ps WHERE ps.presentation_id = a.presentation_id AND ps.user_id = :uid)
                    OR EXISTS (
                        SELECT 1 FROM presentations p
                        JOIN unit_assignments ua ON ua.unit_id = p.unit_id
                        WHERE p.id = a.presentation_id AND ua.user_id = :uid
                    )
                  )
                """, Map.of("uid", userId), Integer.class);
        Integer completed = jdbc.queryForObject("""
                SELECT COUNT(1) FROM activity_submissions s
                JOIN activities a ON a.id = s.activity_id
                WHERE s.user_id = :uid AND s.status IN ('REVIEWED', 'GRADED')
                  AND (
                    EXISTS (SELECT 1 FROM presentation_shares ps WHERE ps.presentation_id = a.presentation_id AND ps.user_id = :uid)
                    OR EXISTS (
                        SELECT 1 FROM presentations p
                        JOIN unit_assignments ua ON ua.unit_id = p.unit_id
                        WHERE p.id = a.presentation_id AND ua.user_id = :uid
                    )
                  )
                """, Map.of("uid", userId), Integer.class);
        return new Breakdown(
                pending == null ? 0 : pending,
                submitted == null ? 0 : submitted,
                completed == null ? 0 : completed);
    }
}
