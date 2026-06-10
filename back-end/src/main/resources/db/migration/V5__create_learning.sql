-- "Mi aprendizaje" — student learning section.
-- Three tables hold shared, seeded definitions shown to every authenticated
-- student; one table holds per-student homework state. The future teacher
-- backoffice will write to the same tables.

-- Teacher presentation: ordered intro blocks (read-only to students)
CREATE TABLE learning_presentation (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    heading VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT true,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Past (completed) classes shown to students as seeded sample data
CREATE TABLE past_classes (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    held_on DATE NOT NULL,
    teacher_note TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Homework task definitions (shared, seeded). Per-student progress lives in homework_submissions.
CREATE TABLE homework_assignments (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    instructions TEXT NOT NULL,
    due_on DATE,
    published BOOLEAN NOT NULL DEFAULT true,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Per-student homework state. Absence of a row ⇒ the assignment is PENDING for that student.
CREATE TABLE homework_submissions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assignment_id UUID NOT NULL REFERENCES homework_assignments(id) ON DELETE CASCADE,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    response_text TEXT,
    submitted_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT homework_submissions_status_check CHECK (status IN ('PENDING', 'SUBMITTED', 'REVIEWED')),
    CONSTRAINT homework_submissions_unique_user_assignment UNIQUE (user_id, assignment_id)
);

CREATE INDEX homework_submissions_user_id_idx ON homework_submissions (user_id);

-- -----------------------------------------------------------------------
-- Seed data: placeholder content (clearly-labelled, removable later)
-- -----------------------------------------------------------------------

-- Presentation blocks
INSERT INTO learning_presentation (heading, body, sort_order) VALUES
    ('Cómo son mis clases',
     'Clases individuales por videollamada, adaptadas a tu nivel y a tus objetivos. Hablamos desde el primer día: aprenderás español usándolo de verdad.',
     1),
    ('Qué aprenderás',
     'Trabajamos gramática, vocabulario y conversación con materiales reales. Cada clase tiene un objetivo claro y terminamos con una pequeña tarea para afianzar lo aprendido.',
     2),
    ('Cómo funcionan las tareas',
     'Después de cada clase te asigno una tarea breve. Puedes escribir tu respuesta aquí mismo y marcarla como entregada; la revisaré antes de nuestra siguiente sesión.',
     3);

-- Past classes (most recent first when displayed)
INSERT INTO past_classes (title, held_on, teacher_note) VALUES
    ('El pretérito indefinido',
     DATE '2026-06-03',
     'Repasamos las formas regulares e irregulares más frecuentes. Muy buen progreso con los verbos irregulares.'),
    ('Vocabulario de viajes',
     DATE '2026-05-27',
     'Aprendimos vocabulario para moverte por la ciudad y pedir indicaciones. Practica los diálogos en casa.'),
    ('Conversación: pedir en un restaurante',
     DATE '2026-05-20',
     'Simulamos pedir en un restaurante. Recuerda usar "me gustaría" y "para mí" para sonar más natural.'),
    ('Los artículos y el género',
     DATE '2026-05-13',
     'Trabajamos el género de los sustantivos y las excepciones más comunes. Buen trabajo.');

-- Homework assignments: one overdue (past due), one upcoming, one with no due date
INSERT INTO homework_assignments (title, instructions, due_on, sort_order) VALUES
    ('Escribe sobre tus últimas vacaciones',
     'Redacta entre 8 y 10 frases sobre tus últimas vacaciones usando el pretérito indefinido. Incluye dónde fuiste, qué hiciste y qué te gustó.',
     DATE '2026-06-05',
     1),
    ('Vocabulario de viajes: crea tu diálogo',
     'Escribe un diálogo corto (6–8 líneas) pidiendo indicaciones para llegar a una estación de tren. Usa al menos cinco palabras nuevas de la clase.',
     DATE '2026-06-20',
     2),
    ('Lectura libre',
     'Lee un artículo corto en español sobre un tema que te interese y anota tres palabras nuevas. No hay fecha límite: hazlo a tu ritmo.',
     NULL,
     3);
