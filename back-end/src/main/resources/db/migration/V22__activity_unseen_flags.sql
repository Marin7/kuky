-- In-app unseen flags. No notifications table.
-- Existing submitted/assigned rows are backfilled to NOW() so historical work is not unseen.

ALTER TABLE homework_submissions
    ADD COLUMN teacher_seen_at TIMESTAMPTZ;

ALTER TABLE quiz_attempts
    ADD COLUMN teacher_seen_at TIMESTAMPTZ;

ALTER TABLE unit_assignments
    ADD COLUMN student_seen_at TIMESTAMPTZ;

ALTER TABLE quiz_assignees
    ADD COLUMN student_seen_at TIMESTAMPTZ;

ALTER TABLE homework_targets
    ADD COLUMN student_seen_at TIMESTAMPTZ;

UPDATE homework_submissions
SET teacher_seen_at = NOW()
WHERE status IN ('SUBMITTED', 'REVIEWED', 'GRADED')
  AND teacher_seen_at IS NULL;

UPDATE quiz_attempts
SET teacher_seen_at = NOW()
WHERE status IN ('SUBMITTED', 'GRADED')
  AND teacher_seen_at IS NULL;

UPDATE unit_assignments
SET student_seen_at = NOW()
WHERE student_seen_at IS NULL;

UPDATE quiz_assignees
SET student_seen_at = NOW()
WHERE student_seen_at IS NULL;

UPDATE homework_targets
SET student_seen_at = NOW()
WHERE student_seen_at IS NULL;

CREATE INDEX homework_submissions_teacher_unseen_idx
    ON homework_submissions (assignment_id)
    WHERE status IN ('SUBMITTED', 'REVIEWED', 'GRADED')
      AND teacher_seen_at IS NULL;

CREATE INDEX quiz_attempts_teacher_unseen_idx
    ON quiz_attempts (quiz_id)
    WHERE status IN ('SUBMITTED', 'GRADED')
      AND teacher_seen_at IS NULL;

CREATE INDEX unit_assignments_student_unseen_idx
    ON unit_assignments (user_id)
    WHERE student_seen_at IS NULL;

CREATE INDEX quiz_assignees_student_unseen_idx
    ON quiz_assignees (user_id)
    WHERE student_seen_at IS NULL;

CREATE INDEX homework_targets_student_unseen_idx
    ON homework_targets (user_id)
    WHERE student_seen_at IS NULL;
