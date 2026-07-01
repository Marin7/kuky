-- Placement test for potential students (feature 011).
-- Free auto-graded Reading/Listening/Grammar (per-skill CEFR, server-timed
-- sections) + a full evaluation: offline bank transfer (static text only, no
-- payment tracking), a Writing submission, and a live appointment booked
-- through the existing `bookings` flow (reused, unchanged).

-- 1. Singleton config: time limits, pass threshold, writing prompt, payment text.
CREATE TABLE placement_config (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    reading_time_seconds INT NOT NULL DEFAULT 600,
    listening_time_seconds INT NOT NULL DEFAULT 480,
    grammar_time_seconds INT NOT NULL DEFAULT 420,
    pass_threshold_percent INT NOT NULL DEFAULT 60,
    writing_prompt TEXT NOT NULL DEFAULT '',
    payment_instructions TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO placement_config (reading_time_seconds, listening_time_seconds, grammar_time_seconds)
VALUES (600, 480, 420);

-- 2. Authored questions (global, not assigned per-student).
CREATE TABLE placement_questions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    skill VARCHAR(10) NOT NULL
        CONSTRAINT placement_questions_skill_check CHECK (skill IN ('READING', 'LISTENING', 'GRAMMAR')),
    position INT NOT NULL,
    kind VARCHAR(20) NOT NULL
        CONSTRAINT placement_questions_kind_check CHECK (kind IN ('SINGLE_CHOICE', 'MULTI_CHOICE', 'FILL_BLANK')),
    prompt TEXT NOT NULL,
    cefr_level VARCHAR(2) NOT NULL
        CONSTRAINT placement_questions_cefr_level_check CHECK (cefr_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
    audio_url TEXT,
    audio_file_id UUID REFERENCES audio_files(id) ON DELETE SET NULL,
    active BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX placement_questions_skill_position_idx ON placement_questions (skill, position);

-- 3. Choice options / fill-blank accepted answers (mirrors homework_question_options).
CREATE TABLE placement_question_options (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES placement_questions(id) ON DELETE CASCADE,
    position INT NOT NULL,
    label TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX placement_question_options_question_idx ON placement_question_options (question_id, position);

-- 4. One run-through by a logged-in user. Multiple attempts per user allowed.
CREATE TABLE placement_attempts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(12) NOT NULL DEFAULT 'IN_PROGRESS'
        CONSTRAINT placement_attempts_status_check CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    -- Computed result: 'A0' (below A1) or 'A1'..'C2'. No CHECK here (unlike
    -- placement_questions.cefr_level) because 'A0' is a valid computed value.
    overall_cefr VARCHAR(2)
);

CREATE INDEX placement_attempts_user_started_idx ON placement_attempts (user_id, started_at DESC);

-- 5. Per-section timing + result. Row created when the user starts a section;
--    deadline_at is server-computed and is the sole authority for expiry.
CREATE TABLE placement_attempt_sections (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES placement_attempts(id) ON DELETE CASCADE,
    skill VARCHAR(10) NOT NULL
        CONSTRAINT placement_attempt_sections_skill_check CHECK (skill IN ('READING', 'LISTENING', 'GRAMMAR')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deadline_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ,
    score_percent INT,
    -- Computed result: 'A0' or 'A1'..'C2'; no CHECK, same reasoning as overall_cefr.
    cefr_level VARCHAR(2),
    CONSTRAINT placement_attempt_sections_unique_skill UNIQUE (attempt_id, skill)
);

CREATE INDEX placement_attempt_sections_attempt_idx ON placement_attempt_sections (attempt_id);

-- 6. Per-question graded answer within a section (mirrors homework_answers).
CREATE TABLE placement_answers (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    attempt_section_id UUID NOT NULL REFERENCES placement_attempt_sections(id) ON DELETE CASCADE,
    question_id UUID REFERENCES placement_questions(id) ON DELETE SET NULL,
    answer_text TEXT,
    score NUMERIC(4,3) NOT NULL,
    CONSTRAINT placement_answers_unique_section_question UNIQUE (attempt_section_id, question_id)
);

CREATE INDEX placement_answers_section_idx ON placement_answers (attempt_section_id);

-- 7. Selected options for a choice answer (mirrors homework_answer_options).
CREATE TABLE placement_answer_options (
    answer_id UUID NOT NULL REFERENCES placement_answers(id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES placement_question_options(id) ON DELETE CASCADE,
    PRIMARY KEY (answer_id, option_id)
);

-- 8. Free-text Writing submissions. Trust-based: no payment gate, no review-status
--    machinery — the teacher reads these directly ahead of the live appointment.
CREATE TABLE placement_writing_submissions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    prompt_snapshot TEXT NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX placement_writing_submissions_user_idx ON placement_writing_submissions (user_id, submitted_at DESC);
