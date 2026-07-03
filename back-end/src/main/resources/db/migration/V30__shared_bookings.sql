-- A booking may optionally have one additional student attached by the teacher, sharing the
-- same class alongside the primary student (bookings.user_id). Capped at exactly one extra
-- participant, so a single nullable FK column mirrors user_id rather than a join table.
ALTER TABLE bookings
    ADD COLUMN second_student_id UUID REFERENCES users(id) ON DELETE SET NULL;

-- Independent attendance tracking for the second student, mirroring the existing no_show column
-- (which now implicitly means "the primary student's no-show"). Only meaningful while
-- second_student_id is set.
ALTER TABLE bookings
    ADD COLUMN second_student_no_show BOOLEAN;

-- The teacher can never attach the primary student as their own second student.
ALTER TABLE bookings
    ADD CONSTRAINT bookings_second_student_distinct
    CHECK (second_student_id IS NULL OR second_student_id <> user_id);

CREATE INDEX bookings_second_student_id_idx ON bookings (second_student_id)
    WHERE second_student_id IS NOT NULL;
