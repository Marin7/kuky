-- Listening homework: explicit media source kind (Enlace / file / video page / YouTube).
ALTER TABLE homework_assignments
    ADD COLUMN media_source_kind VARCHAR(20);

ALTER TABLE homework_assignments
    ADD CONSTRAINT homework_assignments_media_source_kind_check
    CHECK (
        media_source_kind IS NULL
        OR media_source_kind IN ('AUDIO_URL', 'UPLOADED_FILE', 'VIDEO_PAGE', 'YOUTUBE')
    );

-- Prefer uploaded file when both were somehow set.
UPDATE homework_assignments
SET audio_url = NULL
WHERE audio_file_id IS NOT NULL
  AND audio_url IS NOT NULL;

UPDATE homework_assignments
SET media_source_kind = 'UPLOADED_FILE'
WHERE audio_file_id IS NOT NULL;

UPDATE homework_assignments
SET media_source_kind = 'AUDIO_URL'
WHERE audio_file_id IS NULL
  AND audio_url IS NOT NULL
  AND media_source_kind IS NULL;
