-- Per-student homework assignment. A student sees a homework item iff a target row
-- links them. This retires the previous "shared to all students" visibility: the V5
-- seeded assignments have no targets, so they become unshared drafts until assigned.

CREATE TABLE homework_targets (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    assignment_id UUID NOT NULL REFERENCES homework_assignments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT homework_targets_unique UNIQUE (assignment_id, user_id)
);

CREATE INDEX homework_targets_user_idx ON homework_targets (user_id);
CREATE INDEX homework_targets_assignment_idx ON homework_targets (assignment_id);
