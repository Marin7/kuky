-- Percentage grades for manual FREE_TEXT / WRITE answers (replaces teacher_validation).

-- 1. Add nullable teacher_score_percent (0–100) on answers + WRITE submissions
ALTER TABLE homework_answers
    ADD COLUMN teacher_score_percent INT
        CONSTRAINT homework_answers_teacher_score_percent_check
            CHECK (teacher_score_percent IS NULL
                OR (teacher_score_percent >= 0 AND teacher_score_percent <= 100));

ALTER TABLE activity_answers
    ADD COLUMN teacher_score_percent INT
        CONSTRAINT activity_answers_teacher_score_percent_check
            CHECK (teacher_score_percent IS NULL
                OR (teacher_score_percent >= 0 AND teacher_score_percent <= 100));

ALTER TABLE homework_submissions
    ADD COLUMN teacher_score_percent INT
        CONSTRAINT homework_submissions_teacher_score_percent_check
            CHECK (teacher_score_percent IS NULL
                OR (teacher_score_percent >= 0 AND teacher_score_percent <= 100));

-- 2. Backfill answers from legacy binary validation
UPDATE homework_answers
SET teacher_score_percent = CASE teacher_validation
        WHEN 'VALIDATED' THEN 100
        WHEN 'INVALIDATED' THEN 0
        ELSE NULL
    END,
    score = CASE teacher_validation
        WHEN 'VALIDATED' THEN 1.000
        WHEN 'INVALIDATED' THEN 0.000
        ELSE score
    END
WHERE teacher_validation IS NOT NULL;

UPDATE activity_answers
SET teacher_score_percent = CASE teacher_validation
        WHEN 'VALIDATED' THEN 100
        WHEN 'INVALIDATED' THEN 0
        ELSE NULL
    END,
    score = CASE teacher_validation
        WHEN 'VALIDATED' THEN 1.000
        WHEN 'INVALIDATED' THEN 0.000
        ELSE score
    END
WHERE teacher_validation IS NOT NULL;

-- 3. Backfill WRITE graded/reviewed submissions (no answer rows; score was overall %)
UPDATE homework_submissions s
SET teacher_score_percent = s.score_percent
FROM homework_assignments a
WHERE s.assignment_id = a.id
  AND s.score_percent IS NOT NULL
  AND s.status IN ('GRADED', 'REVIEWED')
  AND (
      a.homework_type = 'WRITE'
      OR (a.format = 'MANUAL' AND a.homework_type IS DISTINCT FROM 'WRITE'
          AND NOT EXISTS (
              SELECT 1 FROM homework_answers ha WHERE ha.submission_id = s.id
          ))
  );

-- 4. Drop legacy teacher_validation
ALTER TABLE homework_answers DROP CONSTRAINT IF EXISTS homework_answers_teacher_validation_check;
ALTER TABLE homework_answers DROP COLUMN IF EXISTS teacher_validation;

ALTER TABLE activity_answers DROP CONSTRAINT IF EXISTS activity_answers_teacher_validation_check;
ALTER TABLE activity_answers DROP COLUMN IF EXISTS teacher_validation;
