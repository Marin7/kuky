-- Sentence-starter question kind (feature 057-sentence-starter-answer).
-- Manual grading; ordered starters (+ optional model answers) in structure_json.
-- Not available in quizzes, so quiz_questions_kind_check is unchanged.

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
            'SPOT_WRONG_WORDS',
            'SENTENCE_STARTER'
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
            'SPOT_WRONG_WORDS',
            'SENTENCE_STARTER'
        ));
