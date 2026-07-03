ALTER TABLE homework_submissions
    ADD COLUMN feedback TEXT,
    ADD COLUMN reviewed_at TIMESTAMPTZ;

-- Existing plain-text answers become the single-segment equivalent of the new
-- JSON-encoded rich-text format, with no formatting applied, so they keep
-- rendering correctly under the new representation.
UPDATE homework_submissions
SET response_text = '[{"text":' || to_json(response_text)::text || '}]'
WHERE response_text IS NOT NULL
  AND response_text NOT LIKE '[%';
