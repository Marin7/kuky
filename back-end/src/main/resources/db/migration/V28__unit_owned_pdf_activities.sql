-- Units own their PDF; activities become pages of that PDF (specs/055-unit-pdf-activities).
-- Existing activities and unit<->presentation links are discarded (no student holds a unit yet).
-- Presentations previously linked into a unit stay as standalone decks.

-- 1) Drop all existing activities (cascades questions, options, submissions, answers).
DELETE FROM activities;
DROP TABLE activity_instructions_files;

-- 2) presentations.unit_id now means "owned by this unit": detach legacy links, cascade on delete,
--    at most one owned presentation per unit.
UPDATE presentations SET unit_id = NULL, unit_position = 0 WHERE unit_id IS NOT NULL;

ALTER TABLE presentations DROP CONSTRAINT IF EXISTS presentations_unit_id_fkey;
ALTER TABLE presentations
    ADD CONSTRAINT presentations_unit_id_fkey
        FOREIGN KEY (unit_id) REFERENCES units(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX presentations_unit_owned_unique
    ON presentations (unit_id)
    WHERE unit_id IS NOT NULL;

-- 3) Page count of a PDF file (reported by pdf.js on upload; required for unit-owned files).
ALTER TABLE presentation_files
    ADD COLUMN page_count INT
        CONSTRAINT presentation_files_page_count_check
            CHECK (page_count IS NULL OR page_count BETWEEN 1 AND 500);

-- 4) Activities are identified by (presentation_id, page).
DROP INDEX IF EXISTS activities_presentation_position_idx;
DROP INDEX IF EXISTS activities_trigger_file_idx;
DROP INDEX IF EXISTS activities_image_idx;

ALTER TABLE activities DROP CONSTRAINT IF EXISTS activities_trigger_pair_check;
ALTER TABLE activities DROP CONSTRAINT IF EXISTS activities_trigger_page_check;

ALTER TABLE activities
    DROP COLUMN trigger_file_id,
    DROP COLUMN trigger_page,
    DROP COLUMN position,
    DROP COLUMN level,
    DROP COLUMN homework_type,
    DROP COLUMN instructions_text,
    DROP COLUMN youtube_url,
    DROP COLUMN image_id;

ALTER TABLE activities
    ADD COLUMN page INT NOT NULL
        CONSTRAINT activities_page_check CHECK (page >= 1);

ALTER TABLE activities ALTER COLUMN title DROP NOT NULL;

ALTER TABLE activities
    ADD CONSTRAINT activities_presentation_page_unique UNIQUE (presentation_id, page);

-- 5) Teacher-unseen flag for activity submissions (mirrors homework_submissions.teacher_seen_at).
ALTER TABLE activity_submissions
    ADD COLUMN teacher_seen_at TIMESTAMPTZ;

CREATE INDEX activity_submissions_teacher_unseen_idx
    ON activity_submissions (activity_id)
    WHERE status IN ('SUBMITTED', 'REVIEWED', 'GRADED')
      AND teacher_seen_at IS NULL;
