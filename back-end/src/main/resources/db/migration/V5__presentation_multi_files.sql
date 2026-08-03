-- Multi-file presentations: reshape presentation_files from 1:1 (PK on presentation_id)
-- to 1:N with surrogate id and persisted display_name.
-- Idempotent so it is safe if a prior incomplete attempt left partial state.

ALTER TABLE presentation_files ADD COLUMN IF NOT EXISTS id UUID;
ALTER TABLE presentation_files ADD COLUMN IF NOT EXISTS display_name VARCHAR(255);

UPDATE presentation_files
SET id = COALESCE(id, gen_random_uuid()),
    display_name = COALESCE(display_name, original_name)
WHERE id IS NULL OR display_name IS NULL;

ALTER TABLE presentation_files ALTER COLUMN id SET NOT NULL;
ALTER TABLE presentation_files ALTER COLUMN display_name SET NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'presentation_files_pkey'
          AND conrelid = 'presentation_files'::regclass
          AND pg_get_constraintdef(oid) LIKE '%presentation_id%'
    ) THEN
        ALTER TABLE presentation_files DROP CONSTRAINT presentation_files_pkey;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'presentation_files_pkey'
          AND conrelid = 'presentation_files'::regclass
    ) THEN
        ALTER TABLE presentation_files ADD PRIMARY KEY (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'presentation_files_presentation_display_unique'
    ) THEN
        ALTER TABLE presentation_files
            ADD CONSTRAINT presentation_files_presentation_display_unique
            UNIQUE (presentation_id, display_name);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS presentation_files_presentation_created_idx
    ON presentation_files (presentation_id, created_at);
