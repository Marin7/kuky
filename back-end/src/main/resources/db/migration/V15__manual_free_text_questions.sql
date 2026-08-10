-- Multi-question MANUAL free-text (non-WRITE homework + MANUAL activities).
-- Adds FREE_TEXT question kind; per-answer plain text + prompt snapshot; migrates legacy.

-- 1) Widen kind CHECKs
ALTER TABLE homework_questions DROP CONSTRAINT homework_questions_kind_check;
ALTER TABLE homework_questions
    ADD CONSTRAINT homework_questions_kind_check
        CHECK (kind IN (
            'SINGLE_CHOICE',
            'MULTI_CHOICE',
            'MULTI_BLANK',
            'DRAG_DROP',
            'TABLE_FILL',
            'MATCHING',
            'TRUE_FALSE',
            'FREE_TEXT'
        ));

ALTER TABLE activity_questions DROP CONSTRAINT activity_questions_kind_check;
ALTER TABLE activity_questions
    ADD CONSTRAINT activity_questions_kind_check
        CHECK (kind IN (
            'SINGLE_CHOICE',
            'MULTI_CHOICE',
            'MULTI_BLANK',
            'DRAG_DROP',
            'TABLE_FILL',
            'MATCHING',
            'TRUE_FALSE',
            'FREE_TEXT'
        ));

-- 2) Answer columns for FREE_TEXT
ALTER TABLE homework_answers
    ADD COLUMN answer_text TEXT,
    ADD COLUMN prompt_snapshot TEXT;

ALTER TABLE activity_answers
    ADD COLUMN answer_text TEXT,
    ADD COLUMN prompt_snapshot TEXT;

-- 3) Seed one FREE_TEXT question on legacy non-WRITE MANUAL homeworks without questions
INSERT INTO homework_questions (id, assignment_id, position, kind, prompt, structure_json)
SELECT gen_random_uuid(), a.id, 0, 'FREE_TEXT', 'Tu respuesta', '{}'::jsonb
FROM homework_assignments a
WHERE a.format = 'MANUAL'
  AND (a.homework_type IS DISTINCT FROM 'WRITE')
  AND NOT EXISTS (
      SELECT 1 FROM homework_questions q WHERE q.assignment_id = a.id
  );

-- 4) Move legacy response_text → answer rows for those homeworks
INSERT INTO homework_answers (id, submission_id, question_id, score, answer_json, answer_text, prompt_snapshot)
SELECT gen_random_uuid(),
       s.id,
       q.id,
       0,
       NULL,
       CASE
           WHEN s.response_text IS NULL OR btrim(s.response_text) = '' THEN NULL
           WHEN left(btrim(s.response_text), 1) = '[' THEN (
               SELECT string_agg(COALESCE(elem->>'text', ''), '' ORDER BY ord)
               FROM jsonb_array_elements(s.response_text::jsonb) WITH ORDINALITY AS t(elem, ord)
           )
           ELSE s.response_text
       END,
       'Tu respuesta'
FROM homework_submissions s
JOIN homework_assignments a ON a.id = s.assignment_id
JOIN homework_questions q ON q.assignment_id = a.id AND q.kind = 'FREE_TEXT' AND q.position = 0
WHERE a.format = 'MANUAL'
  AND (a.homework_type IS DISTINCT FROM 'WRITE')
  AND s.response_text IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM homework_answers ha WHERE ha.submission_id = s.id
  );

UPDATE homework_submissions s
SET response_text = NULL
FROM homework_assignments a
WHERE a.id = s.assignment_id
  AND a.format = 'MANUAL'
  AND (a.homework_type IS DISTINCT FROM 'WRITE')
  AND s.response_text IS NOT NULL
  AND EXISTS (
      SELECT 1 FROM homework_answers ha WHERE ha.submission_id = s.id AND ha.prompt_snapshot IS NOT NULL
  );

-- 5) Same for MANUAL activities
INSERT INTO activity_questions (id, activity_id, position, kind, prompt, structure_json)
SELECT gen_random_uuid(), a.id, 0, 'FREE_TEXT', 'Tu respuesta', '{}'::jsonb
FROM activities a
WHERE a.format = 'MANUAL'
  AND NOT EXISTS (
      SELECT 1 FROM activity_questions q WHERE q.activity_id = a.id
  );

INSERT INTO activity_answers (id, submission_id, question_id, score, answer_json, answer_text, prompt_snapshot)
SELECT gen_random_uuid(),
       s.id,
       q.id,
       0,
       NULL,
       CASE
           WHEN s.response_text IS NULL OR btrim(s.response_text) = '' THEN NULL
           WHEN left(btrim(s.response_text), 1) = '[' THEN (
               SELECT string_agg(COALESCE(elem->>'text', ''), '' ORDER BY ord)
               FROM jsonb_array_elements(s.response_text::jsonb) WITH ORDINALITY AS t(elem, ord)
           )
           ELSE s.response_text
       END,
       'Tu respuesta'
FROM activity_submissions s
JOIN activities a ON a.id = s.activity_id
JOIN activity_questions q ON q.activity_id = a.id AND q.kind = 'FREE_TEXT' AND q.position = 0
WHERE a.format = 'MANUAL'
  AND s.response_text IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM activity_answers aa WHERE aa.submission_id = s.id
  );

UPDATE activity_submissions s
SET response_text = NULL
FROM activities a
WHERE a.id = s.activity_id
  AND a.format = 'MANUAL'
  AND s.response_text IS NOT NULL
  AND EXISTS (
      SELECT 1 FROM activity_answers aa WHERE aa.submission_id = s.id AND aa.prompt_snapshot IS NOT NULL
  );
