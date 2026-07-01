-- Dev-only seed data for the placement test (feature 011, US1 manual validation).
-- NOT a Flyway migration — run manually against the local kuky_dev database, e.g.:
--   psql -U kuky -d kuky_dev -f back-end/src/main/resources/db/dev/placement_seed.sql
-- Safe to re-run: it clears prior placement_questions before inserting.
-- Intended to be retired once the admin authoring UI (US3) is in regular use.

DELETE FROM placement_questions;

-- Reading
INSERT INTO placement_questions (id, skill, position, kind, prompt) VALUES
    (gen_random_uuid(), 'READING', 0, 'SINGLE_CHOICE', '¿Cómo se dice "hello" en español?'),
    (gen_random_uuid(), 'READING', 1, 'SINGLE_CHOICE', 'Elige la opción correcta: "Yo ___ estudiante."'),
    (gen_random_uuid(), 'READING', 2, 'FILL_BLANK', 'Completa: "Ella ___ (vivir) en Madrid."'),
    (gen_random_uuid(), 'READING', 3, 'MULTI_CHOICE', 'Selecciona todos los sinónimos de "feliz".');

-- Listening — no audio attached in the seed; admin can add a clip later.
INSERT INTO placement_questions (id, skill, position, kind, prompt) VALUES
    (gen_random_uuid(), 'LISTENING', 0, 'SINGLE_CHOICE', '¿Qué número escuchaste?'),
    (gen_random_uuid(), 'LISTENING', 1, 'FILL_BLANK', 'Escribe la palabra que escuchaste.');

-- Grammar
INSERT INTO placement_questions (id, skill, position, kind, prompt) VALUES
    (gen_random_uuid(), 'GRAMMAR', 0, 'SINGLE_CHOICE', '"Yo ___ a la escuela." (ir, presente)'),
    (gen_random_uuid(), 'GRAMMAR', 1, 'SINGLE_CHOICE', '"Si ___ tiempo, iría." (tener, imperfecto de subjuntivo)'),
    (gen_random_uuid(), 'GRAMMAR', 2, 'FILL_BLANK', 'Completa con el pretérito: "Ayer yo ___ (comer) paella."');

-- Options for the single/multi-choice questions above, in insertion order.
DO $$
DECLARE q UUID;
BEGIN
    SELECT id INTO q FROM placement_questions WHERE skill = 'READING' AND position = 0;
    INSERT INTO placement_question_options (id, question_id, position, label, is_correct) VALUES
        (gen_random_uuid(), q, 0, 'hola', true),
        (gen_random_uuid(), q, 1, 'adiós', false),
        (gen_random_uuid(), q, 2, 'gracias', false);

    SELECT id INTO q FROM placement_questions WHERE skill = 'READING' AND position = 1;
    INSERT INTO placement_question_options (id, question_id, position, label, is_correct) VALUES
        (gen_random_uuid(), q, 0, 'soy', true),
        (gen_random_uuid(), q, 1, 'eres', false),
        (gen_random_uuid(), q, 2, 'es', false);

    SELECT id INTO q FROM placement_questions WHERE skill = 'READING' AND position = 2;
    INSERT INTO placement_question_options (id, question_id, position, label, is_correct) VALUES
        (gen_random_uuid(), q, 0, 'vive', true);

    SELECT id INTO q FROM placement_questions WHERE skill = 'READING' AND position = 3;
    INSERT INTO placement_question_options (id, question_id, position, label, is_correct) VALUES
        (gen_random_uuid(), q, 0, 'contento', true),
        (gen_random_uuid(), q, 1, 'alegre', true),
        (gen_random_uuid(), q, 2, 'triste', false),
        (gen_random_uuid(), q, 3, 'enfadado', false);

    SELECT id INTO q FROM placement_questions WHERE skill = 'LISTENING' AND position = 0;
    INSERT INTO placement_question_options (id, question_id, position, label, is_correct) VALUES
        (gen_random_uuid(), q, 0, 'tres', true),
        (gen_random_uuid(), q, 1, 'siete', false),
        (gen_random_uuid(), q, 2, 'diez', false);

    SELECT id INTO q FROM placement_questions WHERE skill = 'LISTENING' AND position = 1;
    INSERT INTO placement_question_options (id, question_id, position, label, is_correct) VALUES
        (gen_random_uuid(), q, 0, 'biblioteca', true);

    SELECT id INTO q FROM placement_questions WHERE skill = 'GRAMMAR' AND position = 0;
    INSERT INTO placement_question_options (id, question_id, position, label, is_correct) VALUES
        (gen_random_uuid(), q, 0, 'voy', true),
        (gen_random_uuid(), q, 1, 'va', false),
        (gen_random_uuid(), q, 2, 'vas', false);

    SELECT id INTO q FROM placement_questions WHERE skill = 'GRAMMAR' AND position = 1;
    INSERT INTO placement_question_options (id, question_id, position, label, is_correct) VALUES
        (gen_random_uuid(), q, 0, 'tuviera', true),
        (gen_random_uuid(), q, 1, 'tengo', false),
        (gen_random_uuid(), q, 2, 'tendría', false);

    SELECT id INTO q FROM placement_questions WHERE skill = 'GRAMMAR' AND position = 2;
    INSERT INTO placement_question_options (id, question_id, position, label, is_correct) VALUES
        (gen_random_uuid(), q, 0, 'comí', true);
END $$;
