-- Replace per-question CEFR tagging with teacher-configurable score thresholds
-- per level (feature 011 follow-up). The level a student is awarded is now
-- computed purely from their section score (0-100%) against these thresholds,
-- not from authored item difficulty — questions no longer carry a CEFR tag.

ALTER TABLE placement_questions DROP COLUMN cefr_level;

ALTER TABLE placement_config DROP COLUMN pass_threshold_percent;

CREATE TABLE placement_level_thresholds (
    level VARCHAR(2) PRIMARY KEY
        CONSTRAINT placement_level_thresholds_level_check CHECK (level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
    min_score_percent INT NOT NULL
        CONSTRAINT placement_level_thresholds_min_score_check CHECK (min_score_percent BETWEEN 0 AND 100)
);

-- Seed defaults; teacher-editable via the admin panel. A section's score below
-- A1's threshold yields the computed result "A0" (below A1).
INSERT INTO placement_level_thresholds (level, min_score_percent) VALUES
    ('A1', 0),
    ('A2', 20),
    ('B1', 40),
    ('B2', 60),
    ('C1', 75),
    ('C2', 90);
