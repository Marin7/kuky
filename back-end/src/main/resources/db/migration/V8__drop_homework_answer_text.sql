-- Homework FILL_BLANK was removed in V7; MULTI_BLANK/TABLE_FILL use answer_json.
-- Choice kinds use homework_answer_options. Drop the unused answer_text column.
-- Placement-test placement_answers.answer_text is unchanged.

ALTER TABLE homework_answers DROP COLUMN answer_text;
