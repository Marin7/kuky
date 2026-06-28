-- Class units (packages): group presentations and homeworks by level + subject,
-- with explicit ordering within each level.

CREATE TABLE units (
    id         UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    level      VARCHAR(5)   NOT NULL,
    subject    VARCHAR(200) NOT NULL,
    position   INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT units_level_check CHECK (level IN ('A1','A2','B1','B2','C1','C2'))
);

CREATE INDEX units_level_position_idx ON units (level, position);

-- Unit → student grant table (grants presentation access, NOT homework access).
CREATE TABLE unit_assignments (
    id         UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    unit_id    UUID         NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT unit_assignments_unique UNIQUE (unit_id, user_id)
);

CREATE INDEX unit_assignments_user_idx ON unit_assignments (user_id);

-- A presentation belongs to at most one unit (nullable FK; SET NULL on unit delete).
ALTER TABLE presentations
    ADD COLUMN unit_id UUID REFERENCES units(id) ON DELETE SET NULL;

CREATE INDEX presentations_unit_idx ON presentations (unit_id);

-- A homework_assignment belongs to at most one unit for organisational grouping only.
-- unit_id is NEVER consulted for student access (only homework_targets governs access).
ALTER TABLE homework_assignments
    ADD COLUMN unit_id UUID REFERENCES units(id) ON DELETE SET NULL;

CREATE INDEX homework_assignments_unit_idx ON homework_assignments (unit_id);
