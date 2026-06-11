-- Replace per-slide authoring with a single uploaded .pptx file per presentation.
-- The old presentation_slides table is kept (but will be empty going forward).

CREATE TABLE presentation_files (
    presentation_id UUID NOT NULL PRIMARY KEY REFERENCES presentations(id) ON DELETE CASCADE,
    original_name   VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100) NOT NULL DEFAULT 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    byte_size       INT NOT NULL,
    data            BYTEA NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
