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
        s.setReviewModel(rs.getString("review_model"));
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
                    updated_at = EXCLUDED.updated_at,
                    teacher_seen_at = NULL
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
                    updated_at = EXCLUDED.updated_at,
                    teacher_seen_at = NULL
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

    public ActivitySubmission saveAnnotatedReview(UUID submissionId, String feedbackJson,
                                                   String responseTextOrKeep, boolean firstReview) {
        Instant now = Instant.now();
        String sql = firstReview ? """
                UPDATE activity_submissions SET
                    feedback = :feedback,
                    response_text = CASE WHEN :hasResponse THEN :responseText ELSE response_text END,
                    status = 'REVIEWED',
                    review_model = 'ANNOTATED',
                    reviewed_at = :now,
                    updated_at = :now
                WHERE id = :id
                RETURNING *
                """ : """
                UPDATE activity_submissions SET
                    feedback = :feedback,
                    response_text = CASE WHEN :hasResponse THEN :responseText ELSE response_text END,
                    updated_at = :now
                WHERE id = :id AND review_model = 'ANNOTATED'
                RETURNING *
                """;
        return jdbc.query(sql, new MapSqlParameterSource()
                .addValue("id", submissionId)
                .addValue("feedback", feedbackJson)
                .addValue("hasResponse", responseTextOrKeep != null)
                .addValue("responseText", responseTextOrKeep)
                .addValue("now", Timestamp.from(now)), MAPPER).stream().findFirst().orElseThrow();
    }

    public ActivitySubmission saveScoredAnnotatedReview(UUID submissionId, String feedbackJson,
                                                    int scorePercent, boolean firstReview) {
        Instant now = Instant.now();
        String sql = firstReview ? """
                UPDATE activity_submissions SET
                    feedback = :feedback,
                    score_percent = :scorePercent,
                    status = 'GRADED',
                    review_model = 'ANNOTATED',
                    reviewed_at = :now,
                    updated_at = :now
                WHERE id = :id
                RETURNING *
                """ : """
                UPDATE activity_submissions SET
                    feedback = :feedback,
                    score_percent = :scorePercent,
                    updated_at = :now
                WHERE id = :id AND status = 'GRADED' AND review_model = 'ANNOTATED'
                RETURNING *
                """;
        return jdbc.query(sql, new MapSqlParameterSource()
                .addValue("id", submissionId)
                .addValue("feedback", feedbackJson)
                .addValue("scorePercent", scorePercent)
                .addValue("now", Timestamp.from(now)), MAPPER).stream().findFirst().orElseThrow();
    }

    /**
     * Progress save while awaiting teacher: feedback/annotations, stay {@code SUBMITTED},
     * clear final {@code score_percent}, set {@code review_model = ANNOTATED}.
     */
    public ActivitySubmission saveAnnotatedProgress(UUID submissionId, String feedbackJson) {
        Instant now = Instant.now();
        return jdbc.query("""
                UPDATE activity_submissions SET
                    feedback = :feedback,
                    score_percent = NULL,
                    status = 'SUBMITTED',
                    review_model = 'ANNOTATED',
                    updated_at = :now
                WHERE id = :id AND status = 'SUBMITTED'
                RETURNING *
                """, new MapSqlParameterSource()
                .addValue("id", submissionId)
                .addValue("feedback", feedbackJson)
                .addValue("now", Timestamp.from(now)), MAPPER).stream().findFirst().orElseThrow();
    }

    /** @deprecated use {@link #saveScoredAnnotatedReview} */
    public ActivitySubmission saveMixedGradedReview(UUID submissionId, String feedbackJson,
                                                    int scorePercent, boolean firstReview) {
        return saveScoredAnnotatedReview(submissionId, feedbackJson, scorePercent, firstReview);
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
            UUID activityId, String activityTitle, int page, UUID unitId,
            Instant submittedAt, boolean unseen) {}

    /** Submissions awaiting a teacher grade, optionally limited to one unit, oldest first. */
    public List<ReviewQueueRow> findSubmittedManualQueue(UUID unitIdOrNull) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String unitFilter = "";
        if (unitIdOrNull != null) {
            unitFilter = " AND p.unit_id = :unitId";
            params.addValue("unitId", unitIdOrNull);
        }
        return jdbc.query("""
                SELECT s.id AS submission_id, u.id AS student_id, u.email AS student_email,
                       u.first_name AS student_first_name, u.last_name AS student_last_name,
                       u.username AS student_username, a.id AS activity_id, a.title AS activity_title,
                       a.page, p.unit_id, s.submitted_at, (s.teacher_seen_at IS NULL) AS unseen
                FROM activity_submissions s
                JOIN users u ON u.id = s.user_id
                JOIN activities a ON a.id = s.activity_id
                JOIN presentations p ON p.id = a.presentation_id
                WHERE s.status = 'SUBMITTED' AND a.format IN ('MANUAL', 'MIXED')
                """ + unitFilter + """
                 ORDER BY s.submitted_at ASC
                """, params, (rs, n) -> {
            var submittedAt = rs.getTimestamp("submitted_at");
            return new ReviewQueueRow(
                    rs.getObject("submission_id", UUID.class),
                    rs.getObject("student_id", UUID.class),
                    rs.getString("student_email"),
                    rs.getString("student_first_name"),
                    rs.getString("student_last_name"),
                    rs.getString("student_username"),
                    rs.getObject("activity_id", UUID.class),
                    rs.getString("activity_title"),
                    rs.getInt("page"),
                    rs.getObject("unit_id", UUID.class),
                    submittedAt == null ? null : submittedAt.toInstant(),
                    rs.getBoolean("unseen"));
        });
    }

    public record StudentUnitActivityRow(
            UUID unitId, String unitLevel, String unitSubject,
            UUID activityId, int page, String title, String format,
            String status, Integer scorePercent, UUID submissionId, boolean unseen) {}

    /** Every activity of the units assigned to the student, with that student's submission (if any). */
    public List<StudentUnitActivityRow> findUnitActivitiesForStudent(UUID userId) {
        return jdbc.query("""
                SELECT u.id AS unit_id, u.level AS unit_level, u.subject AS unit_subject,
                       a.id AS activity_id, a.page, a.title, a.format,
                       COALESCE(s.status, 'PENDING') AS status, s.score_percent,
                       s.id AS submission_id,
                       (s.status IN ('SUBMITTED', 'REVIEWED', 'GRADED') AND s.teacher_seen_at IS NULL) AS unseen
                FROM unit_assignments ua
                JOIN units u ON u.id = ua.unit_id
                JOIN presentations p ON p.unit_id = u.id
                JOIN activities a ON a.presentation_id = p.id
                LEFT JOIN activity_submissions s ON s.activity_id = a.id AND s.user_id = ua.user_id
                WHERE ua.user_id = :uid
                ORDER BY u.level, u.position, a.page
                """, Map.of("uid", userId), (rs, n) -> new StudentUnitActivityRow(
                rs.getObject("unit_id", UUID.class),
                rs.getString("unit_level"),
                rs.getString("unit_subject"),
                rs.getObject("activity_id", UUID.class),
                rs.getInt("page"),
                rs.getString("title"),
                rs.getString("format"),
                rs.getString("status"),
                rs.getObject("score_percent", Integer.class),
                rs.getObject("submission_id", UUID.class),
                rs.getBoolean("unseen")));
    }
}
