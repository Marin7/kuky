-- Homework is no longer linked to units (a unit is its PDF + page activities only).
-- Students keep any homework already granted to them (homework_targets is untouched);
-- it simply stops being grouped under a unit. With no homework to order against,
-- the unit PDF's position in a unit sequence is meaningless too.

DROP INDEX IF EXISTS homework_assignments_unit_idx;

ALTER TABLE homework_assignments
    DROP COLUMN unit_id,
    DROP COLUMN unit_position;

ALTER TABLE presentations
    DROP COLUMN unit_position;
