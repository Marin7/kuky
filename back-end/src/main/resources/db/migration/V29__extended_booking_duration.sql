-- Per-student permission to book the 1.5-hour class option, granted/revoked by the teacher.
ALTER TABLE users
    ADD COLUMN extended_class_eligible BOOLEAN NOT NULL DEFAULT false;

-- The existing unique index only prevented two bookings sharing the exact same slot_start,
-- which was sufficient while every booking was a fixed 60 minutes on one shared grid. With a
-- 90-minute booking able to start at a time that only partially overlaps a 60-minute booking,
-- slot_start equality is no longer sufficient — replace it with a true overlap-exclusion
-- constraint over each booking's actual [slot_start, slot_start + duration) window.
--
-- A GiST exclusion constraint's index expression must be IMMUTABLE, but timestamptz + interval
-- is only STABLE in PostgreSQL (interval arithmetic can be timezone-dependent). So slot_end must
-- be a real stored column, computed once here (and by the application on every future insert),
-- rather than computed inline inside the constraint expression.
ALTER TABLE bookings
    ADD COLUMN slot_end TIMESTAMPTZ;

UPDATE bookings
    SET slot_end = slot_start + (duration_minutes * INTERVAL '1 minute');

ALTER TABLE bookings
    ALTER COLUMN slot_end SET NOT NULL;

DROP INDEX bookings_active_slot_uniq;

ALTER TABLE bookings
    ADD CONSTRAINT bookings_no_overlap
    EXCLUDE USING gist (
        tstzrange(slot_start, slot_end, '[)') WITH &&
    ) WHERE (status = 'CONFIRMED');
