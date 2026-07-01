-- Dev-only seed data: a demo student account plus teaching materials, homeworks,
-- past/upcoming appointments, and a resource purchase — for local browser QA.
-- NOT a Flyway migration — run manually against the local kuky_dev database, e.g.:
--   psql -U kuky -d kuky_dev -f back-end/src/main/resources/db/dev/full_seed.sql
-- Safe to re-run: it deletes its own previously-seeded rows (matched by title/email) first.
--
-- Login after seeding: estudiante.demo@kuky.es / Demo1234!

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- Cleanup (order matters: children before the FK-nullifying parents)
-- ---------------------------------------------------------------------------
DELETE FROM homework_assignments WHERE title IN ('Redacción: preséntate', 'Ejercicio: presente de indicativo');
DELETE FROM presentations WHERE title IN ('Saludos y presentaciones', 'La rutina diaria');
DELETE FROM units WHERE (level, subject) IN (('A1', 'Primeros pasos'), ('A2', 'Vida cotidiana'));
DELETE FROM users WHERE email = 'estudiante.demo@kuky.es' OR username = 'ana.demo';

DO $$
DECLARE
    student_id  UUID;
    unit_a1     UUID;
    unit_a2     UUID;
    pres1       UUID;
    pres2       UUID;
    hw_manual   UUID;
    hw_exercise UUID;
    q1          UUID;
    q2          UUID;
    sub_id      UUID;
    ans_id      UUID;
    resource_id UUID;
    purchase_id UUID;
BEGIN
    -- Student account (STUDENT role grants booking/purchases/learning access)
    INSERT INTO users (id, email, password_hash, status, role, gdpr_consent, first_name, last_name, username)
    VALUES (
        gen_random_uuid(), 'estudiante.demo@kuky.es',
        crypt('Demo1234!', gen_salt('bf', 12)),
        'ACTIVE', 'STUDENT', true, 'Ana', 'Demo', 'ana.demo'
    )
    RETURNING id INTO student_id;

    -- Units
    INSERT INTO units (id, level, subject, position)
    VALUES (gen_random_uuid(), 'A1', 'Primeros pasos', 1) RETURNING id INTO unit_a1;
    INSERT INTO units (id, level, subject, position)
    VALUES (gen_random_uuid(), 'A2', 'Vida cotidiana', 1) RETURNING id INTO unit_a2;

    INSERT INTO unit_assignments (id, unit_id, user_id) VALUES (gen_random_uuid(), unit_a1, student_id);
    INSERT INTO unit_assignments (id, unit_id, user_id) VALUES (gen_random_uuid(), unit_a2, student_id);

    -- Presentations: one shared directly (presentation_shares), one via unit_assignments only
    INSERT INTO presentations (id, title, level, unit_id)
    VALUES (gen_random_uuid(), 'Saludos y presentaciones', 'A1', unit_a1) RETURNING id INTO pres1;
    INSERT INTO presentations (id, title, level, unit_id)
    VALUES (gen_random_uuid(), 'La rutina diaria', 'A2', unit_a2) RETURNING id INTO pres2;

    INSERT INTO presentation_shares (id, presentation_id, user_id) VALUES (gen_random_uuid(), pres1, student_id);
    -- pres2 stays without a direct share row, so it only shows up via the unit_assignments path.

    -- Homeworks: one MANUAL (submitted, awaiting review), one EXERCISE (graded)
    INSERT INTO homework_assignments (id, title, instructions, due_on, format, unit_id, sort_order)
    VALUES (gen_random_uuid(), 'Redacción: preséntate',
            'Escribe entre 8 y 10 frases presentándote: nombre, de dónde eres, qué te gusta.',
            CURRENT_DATE + 7, 'MANUAL', unit_a1, 1)
    RETURNING id INTO hw_manual;

    INSERT INTO homework_assignments (id, title, instructions, due_on, format, unit_id, sort_order)
    VALUES (gen_random_uuid(), 'Ejercicio: presente de indicativo',
            'Responde a las preguntas sobre el presente de indicativo.',
            CURRENT_DATE + 14, 'EXERCISE', unit_a1, 2)
    RETURNING id INTO hw_exercise;

    INSERT INTO homework_targets (id, assignment_id, user_id) VALUES (gen_random_uuid(), hw_manual, student_id);
    INSERT INTO homework_targets (id, assignment_id, user_id) VALUES (gen_random_uuid(), hw_exercise, student_id);

    -- Exercise questions + answer key
    INSERT INTO homework_questions (id, assignment_id, position, kind, prompt)
    VALUES (gen_random_uuid(), hw_exercise, 0, 'SINGLE_CHOICE', '"Yo ___ estudiante." (ser)')
    RETURNING id INTO q1;
    INSERT INTO homework_question_options (id, question_id, position, label, is_correct) VALUES
        (gen_random_uuid(), q1, 0, 'soy', true),
        (gen_random_uuid(), q1, 1, 'eres', false),
        (gen_random_uuid(), q1, 2, 'es', false);

    INSERT INTO homework_questions (id, assignment_id, position, kind, prompt)
    VALUES (gen_random_uuid(), hw_exercise, 1, 'FILL_BLANK', 'Completa: "Ella ___ (vivir) en Madrid."')
    RETURNING id INTO q2;
    INSERT INTO homework_question_options (id, question_id, position, label, is_correct)
    VALUES (gen_random_uuid(), q2, 0, 'vive', true);

    -- Manual homework: SUBMITTED, awaiting teacher review
    INSERT INTO homework_submissions (id, user_id, assignment_id, status, response_text, submitted_at)
    VALUES (gen_random_uuid(), student_id, hw_manual, 'SUBMITTED',
            'Hola, me llamo Ana. Soy de Bucarest y me gusta leer y viajar.', NOW() - INTERVAL '2 days');

    -- Exercise homework: GRADED, one right / one wrong answer (50%)
    INSERT INTO homework_submissions (id, user_id, assignment_id, status, submitted_at, score_percent)
    VALUES (gen_random_uuid(), student_id, hw_exercise, 'GRADED', NOW() - INTERVAL '1 day', 50)
    RETURNING id INTO sub_id;

    INSERT INTO homework_answers (id, submission_id, question_id, answer_text, score)
    VALUES (gen_random_uuid(), sub_id, q1, 'eres', 0) RETURNING id INTO ans_id;
    INSERT INTO homework_answer_options (answer_id, option_id)
    SELECT ans_id, id FROM homework_question_options WHERE question_id = q1 AND label = 'eres';

    INSERT INTO homework_answers (id, submission_id, question_id, answer_text, score)
    VALUES (gen_random_uuid(), sub_id, q2, 'vive', 1);

    -- Bookings: one completed, one cancelled, one upcoming
    INSERT INTO bookings (id, user_id, slot_start, duration_minutes, status, created_at)
    VALUES (gen_random_uuid(), student_id, NOW() - INTERVAL '10 days', 60, 'CONFIRMED', NOW() - INTERVAL '17 days');

    INSERT INTO bookings (id, user_id, slot_start, duration_minutes, status, created_at, cancelled_at)
    VALUES (gen_random_uuid(), student_id, NOW() - INTERVAL '5 days', 60, 'CANCELLED',
            NOW() - INTERVAL '12 days', NOW() - INTERVAL '6 days');

    INSERT INTO bookings (id, user_id, slot_start, duration_minutes, status, zoom_join_url, created_at)
    VALUES (gen_random_uuid(), student_id, NOW() + INTERVAL '3 days', 60, 'CONFIRMED',
            'https://zoom.us/j/000000000-demo', NOW() - INTERVAL '1 day');

    -- Purchase + entitlement for an already-seeded paid resource
    SELECT id INTO resource_id FROM resources WHERE slug = 'gramatica-a1';
    IF resource_id IS NOT NULL THEN
        INSERT INTO purchases (id, user_id, item_type, resource_id, amount_cents,
                                receipt_reference, payment_provider, purchased_at)
        VALUES (gen_random_uuid(), student_id, 'RESOURCE', resource_id, 2500,
                'DEMO-RECEIPT-0001', 'MANUAL', NOW() - INTERVAL '20 days')
        RETURNING id INTO purchase_id;

        INSERT INTO entitlements (id, user_id, resource_id, source_purchase_id, granted_at)
        VALUES (gen_random_uuid(), student_id, resource_id, purchase_id, NOW() - INTERVAL '20 days');
    END IF;

    RAISE NOTICE 'Seeded demo student: estudiante.demo@kuky.es / Demo1234!';
END $$;
