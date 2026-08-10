-- Per-question grading: MIXED format + teacher validation on manual answers.

ALTER TABLE homework_answers
    ADD COLUMN teacher_validation VARCHAR(16)
        CONSTRAINT homework_answers_teacher_validation_check
            CHECK (teacher_validation IS NULL OR teacher_validation IN ('VALIDATED', 'INVALIDATED'));

ALTER TABLE activity_answers
    ADD COLUMN teacher_validation VARCHAR(16)
        CONSTRAINT activity_answers_teacher_validation_check
            CHECK (teacher_validation IS NULL OR teacher_validation IN ('VALIDATED', 'INVALIDATED'));

-- Expand format to include MIXED (derived from question kinds on save).
ALTER TABLE homework_assignments DROP CONSTRAINT IF EXISTS homework_assignments_format_check;
ALTER TABLE homework_assignments
    ADD CONSTRAINT homework_assignments_format_check
        CHECK (format IN ('MANUAL', 'EXERCISE', 'MIXED'));

ALTER TABLE activities DROP CONSTRAINT IF EXISTS activities_format_check;
ALTER TABLE activities
    ADD CONSTRAINT activities_format_check
        CHECK (format IN ('MANUAL', 'EXERCISE', 'MIXED'));

-- Backfill MIXED where both FREE_TEXT and structured questions already coexist (expect 0).
UPDATE homework_assignments a
SET format = 'MIXED'
WHERE EXISTS (
    SELECT 1 FROM homework_questions q
    WHERE q.assignment_id = a.id AND q.kind = 'FREE_TEXT'
)
AND EXISTS (
    SELECT 1 FROM homework_questions q
    WHERE q.assignment_id = a.id AND q.kind <> 'FREE_TEXT'
);

UPDATE activities a
SET format = 'MIXED'
WHERE EXISTS (
    SELECT 1 FROM activity_questions q
    WHERE q.activity_id = a.id AND q.kind = 'FREE_TEXT'
)
AND EXISTS (
    SELECT 1 FROM activity_questions q
    WHERE q.activity_id = a.id AND q.kind <> 'FREE_TEXT'
);
