-- Teacher-managed availability replaces the previously hard-coded working hours.
-- The public schedule is derived from these tables (no materialized slots).

-- Recurring weekly pattern: one row per (weekday, window). A weekday with no rows
-- is fully unavailable (this is how weekends are modelled — simply no rows).
CREATE TABLE availability_rules (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    day_of_week SMALLINT NOT NULL,          -- ISO: 1=Monday … 7=Sunday
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT availability_rules_day_check CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT availability_rules_time_check CHECK (end_time > start_time)
);

CREATE INDEX availability_rules_day_idx ON availability_rules (day_of_week);

-- Date-specific overrides: BLOCK removes time on a date; OPEN adds time on a date.
CREATE TABLE availability_exceptions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    exception_date DATE NOT NULL,
    kind VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT availability_exceptions_kind_check CHECK (kind IN ('BLOCK', 'OPEN')),
    CONSTRAINT availability_exceptions_time_check CHECK (end_time > start_time)
);

CREATE INDEX availability_exceptions_date_idx ON availability_exceptions (exception_date);

-- Behaviour-preserving seed: reproduce the previous Mon–Fri 09:00–18:00 schedule
-- with the 12:00–14:00 lunch gap (weekends intentionally have no rows).
INSERT INTO availability_rules (day_of_week, start_time, end_time) VALUES
    (1, TIME '09:00', TIME '12:00'), (1, TIME '14:00', TIME '18:00'),
    (2, TIME '09:00', TIME '12:00'), (2, TIME '14:00', TIME '18:00'),
    (3, TIME '09:00', TIME '12:00'), (3, TIME '14:00', TIME '18:00'),
    (4, TIME '09:00', TIME '12:00'), (4, TIME '14:00', TIME '18:00'),
    (5, TIME '09:00', TIME '12:00'), (5, TIME '14:00', TIME '18:00');
