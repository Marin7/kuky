-- Listening homework audio (feature: reading/listening self-correcting exercises).
-- A listening homework can carry either an external audio URL (YouTube, a direct
-- audio link, …) OR an uploaded audio file stored as bytes (served via
-- GET /api/v1/audio/{id}, mirroring images). At most one source is set.

CREATE TABLE audio_files (
    id            UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    byte_size     INT NOT NULL,
    data          BYTEA NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT audio_files_size_check CHECK (byte_size > 0 AND byte_size <= 26214400) -- 25 MB
);

ALTER TABLE homework_assignments
    ADD COLUMN audio_url TEXT;

ALTER TABLE homework_assignments
    ADD COLUMN audio_file_id UUID REFERENCES audio_files(id) ON DELETE SET NULL;
