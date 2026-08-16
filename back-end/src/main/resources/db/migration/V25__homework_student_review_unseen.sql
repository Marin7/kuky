-- Student unseen flags for teacher homework corrections and feedback.
-- Existing rows are backfilled to NOW() so historical reviews are not unseen.
-- Submit paths also write NOW(); teacher-visible review sets the matching column NULL.

ALTER TABLE homework_submissions
    ADD COLUMN student_grade_seen_at TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE homework_submissions
    ADD COLUMN student_feedback_seen_at TIMESTAMPTZ DEFAULT NOW();

UPDATE homework_submissions
SET student_grade_seen_at = NOW()
WHERE student_grade_seen_at IS NULL;

UPDATE homework_submissions
SET student_feedback_seen_at = NOW()
WHERE student_feedback_seen_at IS NULL;

CREATE INDEX homework_submissions_student_review_unseen_idx
    ON homework_submissions (user_id)
    WHERE student_grade_seen_at IS NULL
       OR student_feedback_seen_at IS NULL;
