-- Drop homework FILL_BLANK; MULTI_BLANK covers 1–20 typed gaps.
-- Placement-test FILL_BLANK is unchanged (separate table / enum).
-- No data migration — no existing homework FILL_BLANK rows.

ALTER TABLE homework_questions
    DROP CONSTRAINT homework_questions_kind_check;

ALTER TABLE homework_questions
    ADD CONSTRAINT homework_questions_kind_check
        CHECK (kind IN (
            'SINGLE_CHOICE',
            'MULTI_CHOICE',
            'MULTI_BLANK',
            'DRAG_DROP',
            'TABLE_FILL',
            'MATCHING'
        ));
