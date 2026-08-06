-- Add TRUE_FALSE auto-gradable homework exercise kind.
-- Placement-test QuestionKind is unchanged.

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
            'MATCHING',
            'TRUE_FALSE'
        ));
