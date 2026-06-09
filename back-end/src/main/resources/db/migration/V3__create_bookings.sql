CREATE TABLE bookings (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    slot_start TIMESTAMPTZ NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 60,
    status VARCHAR(20) NOT NULL,
    zoom_meeting_id VARCHAR(64),
    zoom_join_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT bookings_status_check CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

CREATE UNIQUE INDEX bookings_active_slot_uniq ON bookings (slot_start) WHERE status = 'CONFIRMED';

CREATE INDEX bookings_user_id_idx ON bookings (user_id);
