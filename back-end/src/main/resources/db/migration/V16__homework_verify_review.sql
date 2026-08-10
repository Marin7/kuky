-- Annotated MANUAL review + plain short feedback discriminator.
-- LEGACY_RICH = pre-existing rich whole-submission feedback (frozen).
-- ANNOTATED = in-place answer marks + optional plain ≤500-char note (re-editable).

ALTER TABLE homework_submissions
    ADD COLUMN review_model TEXT
        CONSTRAINT homework_submissions_review_model_check
            CHECK (review_model IS NULL OR review_model IN ('LEGACY_RICH', 'ANNOTATED'));

ALTER TABLE activity_submissions
    ADD COLUMN review_model TEXT
        CONSTRAINT activity_submissions_review_model_check
            CHECK (review_model IS NULL OR review_model IN ('LEGACY_RICH', 'ANNOTATED'));

-- Already-reviewed MANUAL homework with stored feedback → legacy frozen
UPDATE homework_submissions s
SET review_model = 'LEGACY_RICH'
FROM homework_assignments a
WHERE s.assignment_id = a.id
  AND a.format = 'MANUAL'
  AND s.status = 'REVIEWED'
  AND s.feedback IS NOT NULL;

-- Already-reviewed MANUAL activities with stored feedback → legacy frozen
UPDATE activity_submissions s
SET review_model = 'LEGACY_RICH'
FROM activities a
WHERE s.activity_id = a.id
  AND a.format = 'MANUAL'
  AND s.status = 'REVIEWED'
  AND s.feedback IS NOT NULL;
