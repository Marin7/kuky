-- A past confirmed class the student didn't attend. Defaults to attended (FALSE);
-- the teacher flags a specific class as a no-show.
ALTER TABLE bookings ADD COLUMN no_show BOOLEAN NOT NULL DEFAULT FALSE;
