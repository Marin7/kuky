-- New auto-gradable homework exercise kinds + JSON payloads.
-- Legacy SINGLE_CHOICE / MULTI_CHOICE / FILL_BLANK keep using options / answer_text.

ALTER TABLE homework_questions
    DROP CONSTRAINT homework_questions_kind_check;

ALTER TABLE homework_questions
    ADD CONSTRAINT homework_questions_kind_check
        CHECK (kind IN (
            'SINGLE_CHOICE',
            'MULTI_CHOICE',
            'FILL_BLANK',
            'MULTI_BLANK',
            'DRAG_DROP',
            'TABLE_FILL',
            'MATCHING'
        ));

ALTER TABLE homework_questions
    ADD COLUMN structure_json JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE homework_answers
    ADD COLUMN answer_json JSONB NULL;
