-- A quiz can be linked to (at most) one unit and is shown at the end of it, after the PDF,
-- in the teacher's order. Assigning the unit assigns its quizzes (quiz_assignees stays the
-- access source of truth). Deleting the unit just unlinks its quizzes.

ALTER TABLE quizzes
    ADD COLUMN unit_id UUID REFERENCES units(id) ON DELETE SET NULL,
    ADD COLUMN unit_position INT NOT NULL DEFAULT 0;

CREATE INDEX quizzes_unit_idx ON quizzes (unit_id, unit_position);
