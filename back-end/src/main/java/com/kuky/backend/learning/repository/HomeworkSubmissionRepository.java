package com.kuky.backend.learning.repository;

import com.kuky.backend.learning.model.HomeworkSubmission;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.Types;
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
        s.setScorePercent(rs.getObject("score_percent", Integer.class));
        s.setTeacherScorePercent(rs.getObject("teacher_score_percent", Integer.class));
        s.setFeedback(rs.getString("feedback"));
        s.setReviewModel(rs.getString("review_model"));
        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        if (submittedAt != null) {
            s.setSubmittedAt(submittedAt.toInstant());
        }
        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        if (reviewedAt != null) {
            s.setReviewedAt(reviewedAt.toInstant());
        }
        s.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        s.setAssignmentSnapshot(rs.getString("assignment_snapshot"));
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

    public Optional<HomeworkSubmission> findById(UUID id) {
        String sql = "SELECT * FROM homework_submissions WHERE id = :id";
        return jdbc.query(sql, Map.of("id", id), SUBMISSION_MAPPER).stream().findFirst();
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

    /**
     * Insert or update the student's submission as an auto-graded exercise result
     * (status GRADED, score recorded, no free-text response). Keyed by the UNIQUE
     * (user_id, assignment_id) constraint.
     */
    public HomeworkSubmission upsertGraded(UUID userId, UUID assignmentId, int scorePercent, Instant submittedAt) {
        Instant now = Instant.now();
        String sql = """
                INSERT INTO homework_submissions
                    (id, user_id, assignment_id, status, response_text, score_percent, submitted_at, updated_at)
                VALUES
                    (gen_random_uuid(), :userId, :assignmentId, 'GRADED', NULL, :scorePercent, :submittedAt, :updatedAt)
                ON CONFLICT (user_id, assignment_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    response_text = EXCLUDED.response_text,
                    score_percent = EXCLUDED.score_percent,
                    submitted_at = EXCLUDED.submitted_at,
                    updated_at = EXCLUDED.updated_at
                RETURNING *
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("assignmentId", assignmentId)
                .addValue("scorePercent", scorePercent)
                .addValue("submittedAt", submittedAt == null ? null : Timestamp.from(submittedAt))
                .addValue("updatedAt", Timestamp.from(now));
        return jdbc.query(sql, params, SUBMISSION_MAPPER).stream().findFirst().orElseThrow();
    }

    /** Writes the frozen homework copy once; later teacher edits must not overwrite it. */
    public int setAssignmentSnapshotIfAbsent(UUID submissionId, String snapshotJson) {
        return jdbc.update("""
                UPDATE homework_submissions
                SET assignment_snapshot = CAST(:snapshot AS jsonb)
                WHERE id = :id AND assignment_snapshot IS NULL
                """, new MapSqlParameterSource()
                .addValue("id", submissionId)
                .addValue("snapshot", snapshotJson));
    }

    // --- Teacher review of MANUAL submissions --------------------------------

    public record ReviewQueueRow(UUID submissionId, UUID studentId, String studentEmail, String studentFirstName,
                                 String studentLastName, String studentUsername, String assignmentTitle,
                                 Instant submittedAt) {}

    /** Every MANUAL/MIXED submission currently awaiting teacher feedback, oldest first. */
    public List<ReviewQueueRow> findSubmittedManualQueue() {
        String sql = """
                SELECT s.id AS submission_id, u.id AS student_id, u.email AS student_email,
                       u.first_name AS student_first_name, u.last_name AS student_last_name,
                       u.username AS student_username, ha.title AS assignment_title, s.submitted_at
                FROM homework_submissions s
                JOIN users u ON u.id = s.user_id
                JOIN homework_assignments ha ON ha.id = s.assignment_id
                WHERE s.status = 'SUBMITTED' AND ha.format IN ('MANUAL', 'MIXED')
                ORDER BY s.submitted_at ASC
                """;
        return jdbc.query(sql, Map.of(), (rs, n) -> {
            var submittedAt = rs.getTimestamp("submitted_at");
            return new ReviewQueueRow(
                    rs.getObject("submission_id", UUID.class),
                    rs.getObject("student_id", UUID.class),
                    rs.getString("student_email"),
                    rs.getString("student_first_name"),
                    rs.getString("student_last_name"),
                    rs.getString("student_username"),
                    rs.getString("assignment_title"),
                    submittedAt == null ? null : submittedAt.toInstant());
        });
    }

    public record SubmissionDetailRow(UUID submissionId, UUID studentId, String studentEmail,
                                      String studentFirstName, String studentLastName, String studentUsername,
                                      String assignmentTitle, String status, String responseText, String feedback,
                                      String reviewModel, Integer scorePercent, Integer teacherScorePercent,
                                      String format, String homeworkType,
                                      Instant submittedAt, Instant reviewedAt) {}

    /** Full detail of a single submission, joined with its student and assignment, for the review screen. */
    public Optional<SubmissionDetailRow> findDetailById(UUID submissionId) {
        String sql = """
                SELECT s.id AS submission_id, u.id AS student_id, u.email AS student_email,
                       u.first_name AS student_first_name, u.last_name AS student_last_name,
                       u.username AS student_username, ha.title AS assignment_title,
                       s.status, s.response_text, s.feedback, s.review_model, s.score_percent,
                       s.teacher_score_percent,
                       ha.format, ha.homework_type,
                       s.submitted_at, s.reviewed_at
                FROM homework_submissions s
                JOIN users u ON u.id = s.user_id
                JOIN homework_assignments ha ON ha.id = s.assignment_id
                WHERE s.id = :id
                """;
        return jdbc.query(sql, Map.of("id", submissionId), (rs, n) -> {
            var submittedAt = rs.getTimestamp("submitted_at");
            var reviewedAt = rs.getTimestamp("reviewed_at");
            return new SubmissionDetailRow(
                    rs.getObject("submission_id", UUID.class),
                    rs.getObject("student_id", UUID.class),
                    rs.getString("student_email"),
                    rs.getString("student_first_name"),
                    rs.getString("student_last_name"),
                    rs.getString("student_username"),
                    rs.getString("assignment_title"),
                    rs.getString("status"),
                    rs.getString("response_text"),
                    rs.getString("feedback"),
                    rs.getString("review_model"),
                    rs.getObject("score_percent", Integer.class),
                    rs.getObject("teacher_score_percent", Integer.class),
                    rs.getString("format"),
                    rs.getString("homework_type"),
                    submittedAt == null ? null : submittedAt.toInstant(),
                    reviewedAt == null ? null : reviewedAt.toInstant());
        }).stream().findFirst();
    }

    /** Saves the teacher's feedback and transitions the submission to REVIEWED. */
    public int updateFeedback(UUID submissionId, String feedbackJson, Instant reviewedAt) {
        String sql = """
                UPDATE homework_submissions
                SET feedback = :feedback, status = 'REVIEWED', reviewed_at = :reviewedAt, updated_at = :updatedAt
                WHERE id = :id
                """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("feedback", feedbackJson)
                .addValue("reviewedAt", Timestamp.from(reviewedAt))
                .addValue("updatedAt", Timestamp.from(Instant.now()))
                .addValue("id", submissionId));
    }

    /**
     * Saves an annotated manual review. First review records its timestamp; subsequent
     * annotated edits preserve it and cannot overwrite another review model.
     */
    public int saveAnnotatedReview(UUID submissionId, String feedbackJson, String responseTextOrKeep,
                                   boolean firstReview) {
        String sql = firstReview ? """
                UPDATE homework_submissions
                SET feedback = :feedback,
                    response_text = CASE WHEN :hasResponse THEN :responseText ELSE response_text END,
                    status = 'REVIEWED',
                    review_model = 'ANNOTATED',
                    reviewed_at = :now,
                    updated_at = :now
                WHERE id = :id
                """ : """
                UPDATE homework_submissions
                SET feedback = :feedback,
                    response_text = CASE WHEN :hasResponse THEN :responseText ELSE response_text END,
                    updated_at = :now
                WHERE id = :id AND review_model = 'ANNOTATED'
                """;
        Instant now = Instant.now();
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", submissionId)
                .addValue("feedback", feedbackJson)
                .addValue("hasResponse", responseTextOrKeep != null)
                .addValue("responseText", responseTextOrKeep)
                .addValue("now", Timestamp.from(now)));
    }

    /**
     * Finalizes (or re-edits) a scored annotated review: combined score, {@code GRADED}.
     * Used for MIXED, ALL_MANUAL (with percents), and WRITE.
     * {@code teacherScorePercent} is set for WRITE; pass {@code null} for question-based reviews.
     */
    public int saveScoredAnnotatedReview(UUID submissionId, String feedbackJson, String responseTextOrKeep,
                                         int scorePercent, Integer teacherScorePercent, boolean firstReview) {
        String sql = firstReview ? """
                UPDATE homework_submissions
                SET feedback = :feedback,
                    response_text = CASE WHEN :hasResponse THEN :responseText ELSE response_text END,
                    score_percent = :scorePercent,
                    teacher_score_percent = CASE
                        WHEN :hasTeacherPercent THEN :teacherScorePercent
                        ELSE teacher_score_percent END,
                    status = 'GRADED',
                    review_model = 'ANNOTATED',
                    reviewed_at = :now,
                    updated_at = :now
                WHERE id = :id
                """ : """
                UPDATE homework_submissions
                SET feedback = :feedback,
                    response_text = CASE WHEN :hasResponse THEN :responseText ELSE response_text END,
                    score_percent = :scorePercent,
                    teacher_score_percent = CASE
                        WHEN :hasTeacherPercent THEN :teacherScorePercent
                        ELSE teacher_score_percent END,
                    updated_at = :now
                WHERE id = :id AND status = 'GRADED' AND review_model = 'ANNOTATED'
                """;
        Instant now = Instant.now();
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", submissionId)
                .addValue("feedback", feedbackJson)
                .addValue("hasResponse", responseTextOrKeep != null)
                .addValue("responseText", responseTextOrKeep)
                .addValue("scorePercent", scorePercent)
                .addValue("hasTeacherPercent", teacherScorePercent != null)
                .addValue("teacherScorePercent", teacherScorePercent, Types.INTEGER)
                .addValue("now", Timestamp.from(now)));
    }

    /** Convenience overload without WRITE teacher percent. */
    public int saveScoredAnnotatedReview(UUID submissionId, String feedbackJson, String responseTextOrKeep,
                                         int scorePercent, boolean firstReview) {
        return saveScoredAnnotatedReview(submissionId, feedbackJson, responseTextOrKeep, scorePercent, null, firstReview);
    }

    /**
     * Progress save while awaiting teacher: annotations / draft percent, stay {@code SUBMITTED},
     * clear final {@code score_percent}, set {@code review_model = ANNOTATED}. Does not set {@code reviewed_at}.
     */
    public int saveAnnotatedProgress(UUID submissionId, String feedbackJson, String responseTextOrKeep,
                                     Integer teacherScorePercent) {
        String sql = """
                UPDATE homework_submissions
                SET feedback = :feedback,
                    response_text = CASE WHEN :hasResponse THEN :responseText ELSE response_text END,
                    teacher_score_percent = CASE
                        WHEN :hasTeacherPercent THEN :teacherScorePercent
                        ELSE teacher_score_percent END,
                    score_percent = NULL,
                    status = 'SUBMITTED',
                    review_model = 'ANNOTATED',
                    updated_at = :now
                WHERE id = :id AND status = 'SUBMITTED'
                """;
        Instant now = Instant.now();
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", submissionId)
                .addValue("feedback", feedbackJson)
                .addValue("hasResponse", responseTextOrKeep != null)
                .addValue("responseText", responseTextOrKeep)
                .addValue("hasTeacherPercent", teacherScorePercent != null)
                .addValue("teacherScorePercent", teacherScorePercent, Types.INTEGER)
                .addValue("now", Timestamp.from(now)));
    }

    /** @deprecated use {@link #saveScoredAnnotatedReview} */
    public int saveMixedGradedReview(UUID submissionId, String feedbackJson, int scorePercent,
                                     boolean firstReview) {
        return saveScoredAnnotatedReview(submissionId, feedbackJson, null, scorePercent, null, firstReview);
    }

    /**
     * Saves or clears plain exercise teacher feedback without changing status or reviewed_at.
     * {@code feedbackJsonOrNull} is a single-segment FormattedText JSON, or {@code null} to clear.
     */
    public int updateExerciseFeedback(UUID submissionId, String feedbackJsonOrNull) {
        String sql = """
                UPDATE homework_submissions
                SET feedback = :feedback, updated_at = :updatedAt
                WHERE id = :id
                """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("feedback", feedbackJsonOrNull)
                .addValue("updatedAt", Timestamp.from(Instant.now()))
                .addValue("id", submissionId));
    }
}
