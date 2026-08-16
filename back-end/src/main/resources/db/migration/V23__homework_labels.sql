ALTER TABLE homework_assignments
    ADD COLUMN labels TEXT[] NOT NULL DEFAULT '{}';

COMMENT ON COLUMN homework_assignments.labels IS
    'Teacher-only organization labels; empty array means unlabeled.';
