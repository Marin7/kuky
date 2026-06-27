-- General Availability Template (feature 009).
-- `availability_rules` (V7) is re-roled as the general weekly TEMPLATE (the default week).
-- Each concrete week is snapshotted from the template the first time it enters the bookable
-- horizon; from then on the snapshot below is the source of truth for bookings.

-- Absolute available windows for a specific date (no BLOCK/OPEN deltas — windows are absolute).
-- Multiple rows per date are allowed and merged on read. Zero rows for a date in a
-- materialized week means that date is fully unavailable (distinct from "not materialized").
CREATE TABLE week_availability (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT week_availability_time_check CHECK (end_time > start_time)
);

CREATE INDEX week_availability_date_idx ON week_availability (slot_date);

-- Marker recording that a week (its Monday, ISO) has been snapshotted. Needed so a
-- materialized-but-empty week is distinguishable from one not yet seeded from the template.
CREATE TABLE materialized_weeks (
    week_start DATE PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
