-- Spot-the-wrong-words question kind (feature 054-spot-wrong-words).
-- Passage lives in prompt; wrong words by word index in structure_json.

ALTER TABLE homework_questions DROP CONSTRAINT homework_questions_kind_check;
ALTER TABLE homework_questions
    ADD CONSTRAINT homework_questions_kind_check
        CHECK (kind IN (
            'SINGLE_CHOICE',
            'MULTI_CHOICE',
            'MULTI_BLANK',
            'DRAG_DROP',
            'TABLE_FILL',
            'MATCHING',
            'TRUE_FALSE',
            'FREE_TEXT',
            'SPOT_WRONG_WORDS'
        ));

ALTER TABLE activity_questions DROP CONSTRAINT activity_questions_kind_check;
ALTER TABLE activity_questions
    ADD CONSTRAINT activity_questions_kind_check
        CHECK (kind IN (
            'SINGLE_CHOICE',
            'MULTI_CHOICE',
            'MULTI_BLANK',
            'DRAG_DROP',
            'TABLE_FILL',
            'MATCHING',
            'TRUE_FALSE',
            'FREE_TEXT',
            'SPOT_WRONG_WORDS'
        ));

ALTER TABLE quiz_questions DROP CONSTRAINT quiz_questions_kind_check;
ALTER TABLE quiz_questions
    ADD CONSTRAINT quiz_questions_kind_check
        CHECK (kind IN (
            'SINGLE_CHOICE',
            'MULTI_CHOICE',
            'MULTI_BLANK',
            'DRAG_DROP',
            'TABLE_FILL',
            'MATCHING',
            'TRUE_FALSE',
            'FREE_TEXT',
            'SPOT_WRONG_WORDS'
        ));
