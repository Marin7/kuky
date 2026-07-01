-- Give the Writing section the same server-authoritative timer the auto-graded
-- sections have: a start/deadline lifecycle, teacher-configurable time limit,
-- and (client-side) auto-submit on expiry.

ALTER TABLE placement_config ADD COLUMN writing_time_seconds INT NOT NULL DEFAULT 1200;

CREATE TABLE placement_writing_attempts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deadline_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ
);

-- At most one active (not-yet-submitted) writing attempt per user at a time;
-- mirrors placement_attempt_sections' race-safe start-or-resume pattern.
CREATE UNIQUE INDEX placement_writing_attempts_active_uniq
    ON placement_writing_attempts (user_id) WHERE submitted_at IS NULL;

CREATE INDEX placement_writing_attempts_user_idx ON placement_writing_attempts (user_id, started_at DESC);

ALTER TABLE placement_writing_submissions
    ADD COLUMN writing_attempt_id UUID REFERENCES placement_writing_attempts(id) ON DELETE SET NULL;
