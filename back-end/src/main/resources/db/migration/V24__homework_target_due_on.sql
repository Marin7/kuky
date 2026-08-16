ALTER TABLE homework_targets
    ADD COLUMN due_on DATE;

COMMENT ON COLUMN homework_targets.due_on IS
    'Optional per-student due date for this homework; NULL means no deadline.';

ALTER TABLE homework_assignments
    DROP COLUMN due_on;
