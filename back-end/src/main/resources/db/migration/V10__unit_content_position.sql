-- Shared mixed sequence rank for presentations and homeworks within a unit (FR-013).
ALTER TABLE presentations
    ADD COLUMN unit_position INT NOT NULL DEFAULT 0;

ALTER TABLE homework_assignments
    ADD COLUMN unit_position INT NOT NULL DEFAULT 0;

-- Seed: presentations (current list order = updated_at DESC), then homeworks (created_at DESC).
WITH ranked_presentations AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY unit_id ORDER BY updated_at DESC) - 1 AS pos
    FROM presentations
    WHERE unit_id IS NOT NULL
)
UPDATE presentations p
SET unit_position = r.pos
FROM ranked_presentations r
WHERE p.id = r.id;

WITH presentation_counts AS (
    SELECT unit_id, COUNT(*)::INT AS cnt
    FROM presentations
    WHERE unit_id IS NOT NULL
    GROUP BY unit_id
),
ranked_homeworks AS (
    SELECT ha.id,
           COALESCE(pc.cnt, 0)
               + (ROW_NUMBER() OVER (PARTITION BY ha.unit_id ORDER BY ha.created_at DESC) - 1) AS pos
    FROM homework_assignments ha
    LEFT JOIN presentation_counts pc ON pc.unit_id = ha.unit_id
    WHERE ha.unit_id IS NOT NULL
)
UPDATE homework_assignments ha
SET unit_position = r.pos
FROM ranked_homeworks r
WHERE ha.id = r.id;
