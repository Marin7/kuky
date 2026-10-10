-- Every page of a unit's PDF has a title, and so does every activity.
-- Existing units get placeholder names the teacher can rename from the unit view.

CREATE TABLE unit_page_titles (
    presentation_id UUID NOT NULL REFERENCES presentations(id) ON DELETE CASCADE,
    page INT NOT NULL CONSTRAINT unit_page_titles_page_check CHECK (page >= 1),
    title VARCHAR(200) NOT NULL CONSTRAINT unit_page_titles_title_check CHECK (btrim(title) <> ''),
    PRIMARY KEY (presentation_id, page)
);

INSERT INTO unit_page_titles (presentation_id, page, title)
SELECT p.id, gs.page, 'Página ' || gs.page
FROM presentations p
JOIN presentation_files f ON f.presentation_id = p.id
CROSS JOIN LATERAL generate_series(1, f.page_count) AS gs(page)
WHERE p.unit_id IS NOT NULL AND f.page_count IS NOT NULL;

UPDATE activities
SET title = 'Actividad – página ' || page
WHERE title IS NULL OR btrim(title) = '';

ALTER TABLE activities ALTER COLUMN title SET NOT NULL;
