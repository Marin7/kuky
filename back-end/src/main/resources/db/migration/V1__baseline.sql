-- Schema baseline + required reference seeds only; no localhost dump data.

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE images (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    content_type VARCHAR(50) NOT NULL,
    byte_size INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT images_content_type_check CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    CONSTRAINT images_size_check CHECK (byte_size > 0 AND byte_size <= 2097152)
);

CREATE TABLE audio_files (
    id            UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    byte_size     INT NOT NULL,
    data          BYTEA NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT audio_files_size_check CHECK (byte_size > 0 AND byte_size <= 26214400)
);

CREATE TABLE users (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    gdpr_consent BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    username VARCHAR(50),
    avatar_image_id UUID REFERENCES images(id) ON DELETE SET NULL,
    timezone VARCHAR(64),
    timezone_is_manual BOOLEAN NOT NULL DEFAULT false,
    extended_class_eligible BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_role_check CHECK (role IN ('USER', 'STUDENT', 'ADMIN'))
);

CREATE INDEX users_email_idx ON users (email);
CREATE UNIQUE INDEX users_username_lower_idx ON users (LOWER(username)) WHERE username IS NOT NULL;

CREATE TABLE password_reset_tokens (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(36) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT prt_token_unique UNIQUE (token)
);
CREATE INDEX prt_token_idx ON password_reset_tokens (token);
CREATE INDEX prt_user_id_idx ON password_reset_tokens (user_id);

CREATE TABLE email_activation_tokens (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(36) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX email_activation_tokens_user_idx ON email_activation_tokens (user_id);

CREATE TABLE bookings (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    slot_start TIMESTAMPTZ NOT NULL,
    slot_end TIMESTAMPTZ NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 60,
    status VARCHAR(20) NOT NULL,
    zoom_meeting_id VARCHAR(64),
    zoom_join_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cancelled_at TIMESTAMPTZ,
    reminder_sent_at TIMESTAMPTZ,
    no_show BOOLEAN NOT NULL DEFAULT FALSE,
    second_student_id UUID REFERENCES users(id) ON DELETE SET NULL,
    second_student_no_show BOOLEAN,
    CONSTRAINT bookings_status_check CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    CONSTRAINT bookings_second_student_distinct CHECK (second_student_id IS NULL OR second_student_id <> user_id),
    CONSTRAINT bookings_no_overlap EXCLUDE USING gist (
        tstzrange(slot_start, slot_end, '[)') WITH &&
    ) WHERE (status = 'CONFIRMED')
);
CREATE INDEX bookings_user_id_idx ON bookings (user_id);
CREATE INDEX bookings_second_student_id_idx ON bookings (second_student_id) WHERE second_student_id IS NOT NULL;

CREATE TABLE learning_presentation (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    heading VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT true,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE past_classes (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    held_on DATE NOT NULL,
    teacher_note TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE availability_rules (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    day_of_week SMALLINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT availability_rules_day_check CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT availability_rules_time_check CHECK (end_time > start_time)
);
CREATE INDEX availability_rules_day_idx ON availability_rules (day_of_week);

CREATE TABLE availability_exceptions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    exception_date DATE NOT NULL,
    kind VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT availability_exceptions_kind_check CHECK (kind IN ('BLOCK', 'OPEN')),
    CONSTRAINT availability_exceptions_time_check CHECK (end_time > start_time)
);
CREATE INDEX availability_exceptions_date_idx ON availability_exceptions (exception_date);

CREATE TABLE week_availability (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT week_availability_time_check CHECK (end_time > start_time)
);
CREATE INDEX week_availability_date_idx ON week_availability (slot_date);

CREATE TABLE materialized_weeks (
    week_start DATE PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE units (
    id         UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    level      VARCHAR(5)   NOT NULL,
    subject    VARCHAR(200) NOT NULL,
    position   INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT units_level_check CHECK (level IN ('A1','A2','B1','B2','C1','C2'))
);
CREATE INDEX units_level_position_idx ON units (level, position);

CREATE TABLE unit_assignments (
    id         UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    unit_id    UUID         NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT unit_assignments_unique UNIQUE (unit_id, user_id)
);
CREATE INDEX unit_assignments_user_idx ON unit_assignments (user_id);

CREATE TABLE presentations (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    level VARCHAR(5),
    unit_id UUID REFERENCES units(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX presentations_unit_idx ON presentations (unit_id);

CREATE TABLE presentation_slides (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    presentation_id UUID NOT NULL REFERENCES presentations(id) ON DELETE CASCADE,
    heading VARCHAR(200) NOT NULL,
    body TEXT NOT NULL DEFAULT '',
    image_id UUID REFERENCES images(id) ON DELETE SET NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX presentation_slides_presentation_idx ON presentation_slides (presentation_id, sort_order);

CREATE TABLE presentation_shares (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    presentation_id UUID NOT NULL REFERENCES presentations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT presentation_shares_unique UNIQUE (presentation_id, user_id)
);
CREATE INDEX presentation_shares_user_idx ON presentation_shares (user_id);

CREATE TABLE presentation_files (
    presentation_id UUID NOT NULL PRIMARY KEY REFERENCES presentations(id) ON DELETE CASCADE,
    original_name   VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100) NOT NULL DEFAULT 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    byte_size       INT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE homework_assignments (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    instructions TEXT NOT NULL,
    due_on DATE,
    published BOOLEAN NOT NULL DEFAULT true,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    homework_type VARCHAR(20),
    level VARCHAR(5),
    format VARCHAR(10) NOT NULL DEFAULT 'MANUAL',
    audio_url TEXT,
    audio_file_id UUID REFERENCES audio_files(id) ON DELETE SET NULL,
    unit_id UUID REFERENCES units(id) ON DELETE SET NULL,
    CONSTRAINT homework_assignments_format_check CHECK (format IN ('MANUAL', 'EXERCISE'))
);
CREATE INDEX homework_assignments_unit_idx ON homework_assignments (unit_id);

CREATE TABLE homework_targets (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    assignment_id UUID NOT NULL REFERENCES homework_assignments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT homework_targets_unique UNIQUE (assignment_id, user_id)
);
CREATE INDEX homework_targets_user_idx ON homework_targets (user_id);
CREATE INDEX homework_targets_assignment_idx ON homework_targets (assignment_id);

CREATE TABLE homework_submissions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assignment_id UUID NOT NULL REFERENCES homework_assignments(id) ON DELETE CASCADE,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    response_text TEXT,
    submitted_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    score_percent INT,
    feedback TEXT,
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT homework_submissions_status_check CHECK (status IN ('PENDING', 'SUBMITTED', 'REVIEWED', 'GRADED')),
    CONSTRAINT homework_submissions_unique_user_assignment UNIQUE (user_id, assignment_id)
);
CREATE INDEX homework_submissions_user_id_idx ON homework_submissions (user_id);

CREATE TABLE homework_questions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    assignment_id UUID NOT NULL REFERENCES homework_assignments(id) ON DELETE CASCADE,
    position INT NOT NULL,
    kind VARCHAR(20) NOT NULL
        CONSTRAINT homework_questions_kind_check
            CHECK (kind IN ('SINGLE_CHOICE', 'MULTI_CHOICE', 'FILL_BLANK')),
    prompt TEXT NOT NULL
);
CREATE INDEX homework_questions_assignment_idx ON homework_questions (assignment_id, position);

CREATE TABLE homework_question_options (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES homework_questions(id) ON DELETE CASCADE,
    position INT NOT NULL,
    label TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX homework_question_options_question_idx ON homework_question_options (question_id, position);

CREATE TABLE homework_answers (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES homework_submissions(id) ON DELETE CASCADE,
    question_id UUID REFERENCES homework_questions(id) ON DELETE SET NULL,
    answer_text TEXT,
    score NUMERIC(4,3) NOT NULL,
    CONSTRAINT homework_answers_unique_submission_question UNIQUE (submission_id, question_id)
);
CREATE INDEX homework_answers_submission_idx ON homework_answers (submission_id);

CREATE TABLE homework_answer_options (
    answer_id UUID NOT NULL REFERENCES homework_answers(id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES homework_question_options(id) ON DELETE CASCADE,
    PRIMARY KEY (answer_id, option_id)
);

CREATE TABLE placement_config (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    reading_time_seconds INT NOT NULL DEFAULT 600,
    listening_time_seconds INT NOT NULL DEFAULT 480,
    grammar_time_seconds INT NOT NULL DEFAULT 420,
    writing_prompt TEXT NOT NULL DEFAULT '',
    writing_time_seconds INT NOT NULL DEFAULT 1200,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE placement_level_thresholds (
    level VARCHAR(2) PRIMARY KEY
        CONSTRAINT placement_level_thresholds_level_check CHECK (level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
    min_score_percent INT NOT NULL
        CONSTRAINT placement_level_thresholds_min_score_check CHECK (min_score_percent BETWEEN 0 AND 100)
);

CREATE TABLE placement_questions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    skill VARCHAR(10) NOT NULL
        CONSTRAINT placement_questions_skill_check CHECK (skill IN ('READING', 'LISTENING', 'GRAMMAR')),
    position INT NOT NULL,
    kind VARCHAR(20) NOT NULL
        CONSTRAINT placement_questions_kind_check CHECK (kind IN ('SINGLE_CHOICE', 'MULTI_CHOICE', 'FILL_BLANK')),
    prompt TEXT NOT NULL,
    audio_url TEXT,
    audio_file_id UUID REFERENCES audio_files(id) ON DELETE SET NULL,
    active BOOLEAN NOT NULL DEFAULT true
);
CREATE INDEX placement_questions_skill_position_idx ON placement_questions (skill, position);

CREATE TABLE placement_question_options (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES placement_questions(id) ON DELETE CASCADE,
    position INT NOT NULL,
    label TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX placement_question_options_question_idx ON placement_question_options (question_id, position);

CREATE TABLE placement_attempts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(12) NOT NULL DEFAULT 'IN_PROGRESS'
        CONSTRAINT placement_attempts_status_check CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    overall_cefr VARCHAR(2)
);
CREATE INDEX placement_attempts_user_started_idx ON placement_attempts (user_id, started_at DESC);

CREATE TABLE placement_attempt_sections (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES placement_attempts(id) ON DELETE CASCADE,
    skill VARCHAR(10) NOT NULL
        CONSTRAINT placement_attempt_sections_skill_check CHECK (skill IN ('READING', 'LISTENING', 'GRAMMAR')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deadline_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ,
    score_percent INT,
    cefr_level VARCHAR(2),
    CONSTRAINT placement_attempt_sections_unique_skill UNIQUE (attempt_id, skill)
);
CREATE INDEX placement_attempt_sections_attempt_idx ON placement_attempt_sections (attempt_id);

CREATE TABLE placement_answers (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    attempt_section_id UUID NOT NULL REFERENCES placement_attempt_sections(id) ON DELETE CASCADE,
    question_id UUID REFERENCES placement_questions(id) ON DELETE SET NULL,
    answer_text TEXT,
    score NUMERIC(4,3) NOT NULL,
    CONSTRAINT placement_answers_unique_section_question UNIQUE (attempt_section_id, question_id)
);
CREATE INDEX placement_answers_section_idx ON placement_answers (attempt_section_id);

CREATE TABLE placement_answer_options (
    answer_id UUID NOT NULL REFERENCES placement_answers(id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES placement_question_options(id) ON DELETE CASCADE,
    PRIMARY KEY (answer_id, option_id)
);

CREATE TABLE placement_writing_attempts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deadline_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX placement_writing_attempts_active_uniq
    ON placement_writing_attempts (user_id) WHERE submitted_at IS NULL;
CREATE INDEX placement_writing_attempts_user_idx ON placement_writing_attempts (user_id, started_at DESC);

CREATE TABLE placement_writing_submissions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    prompt_snapshot TEXT NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    writing_attempt_id UUID REFERENCES placement_writing_attempts(id) ON DELETE SET NULL
);
CREATE INDEX placement_writing_submissions_user_idx ON placement_writing_submissions (user_id, submitted_at DESC);

CREATE TABLE testimonials (
    id            UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES users(id),
    student_name  VARCHAR(200) NOT NULL,
    text          TEXT NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    display_order INT NOT NULL DEFAULT 0,
    submitted_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at   TIMESTAMPTZ,
    CONSTRAINT testimonials_user_unique UNIQUE (user_id),
    CONSTRAINT testimonials_status_check CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'UNPUBLISHED'))
);
CREATE INDEX testimonials_status_order_idx ON testimonials (status, display_order);

-- Required reference data (not localhost dumps)
INSERT INTO placement_config (reading_time_seconds, listening_time_seconds, grammar_time_seconds, writing_prompt, writing_time_seconds)
VALUES (600, 480, 420, '', 1200);

INSERT INTO placement_level_thresholds (level, min_score_percent) VALUES
    ('A1', 0),
    ('A2', 20),
    ('B1', 40),
    ('B2', 60),
    ('C1', 75),
    ('C2', 90);

-- Product copy for learning intro (generic, not personal data)
INSERT INTO learning_presentation (heading, body, published, sort_order) VALUES
    ('Cómo son mis clases', 'Clases individuales por videollamada, adaptadas a tu nivel y a tus objetivos. Hablamos desde el primer día: aprenderás español usándolo de verdad.', true, 1),
    ('Qué aprenderás', 'Trabajamos gramática, vocabulario y conversación con materiales reales. Cada clase tiene un objetivo claro y terminamos con una pequeña tarea para afianzar lo aprendido.', true, 2),
    ('Cómo funcionan las tareas', 'Después de cada clase te asigno una tarea breve. Puedes escribir tu respuesta aquí mismo y marcarla como entregada; la revisaré antes de nuestra siguiente sesión.', true, 3);
