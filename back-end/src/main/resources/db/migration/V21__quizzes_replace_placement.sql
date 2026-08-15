-- Replace placement test with quizzes (prod has no placement rows).

DROP TABLE IF EXISTS
    placement_answer_options,
    placement_answers,
    placement_attempt_sections,
    placement_attempts,
    placement_question_options,
    placement_questions,
    placement_writing_submissions,
    placement_writing_attempts,
    placement_level_thresholds,
    placement_config
CASCADE;

CREATE TABLE quizzes (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE quiz_questions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    position INT NOT NULL,
    skill VARCHAR(10) NOT NULL
        CONSTRAINT quiz_questions_skill_check
            CHECK (skill IN ('READING', 'WRITING', 'GRAMMAR', 'LISTENING')),
    kind VARCHAR(20) NOT NULL
        CONSTRAINT quiz_questions_kind_check
            CHECK (kind IN (
                'SINGLE_CHOICE', 'MULTI_CHOICE', 'MULTI_BLANK', 'DRAG_DROP',
                'TABLE_FILL', 'MATCHING', 'TRUE_FALSE', 'FREE_TEXT'
            )),
    prompt TEXT NOT NULL,
    structure_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    media_source_kind VARCHAR(20)
        CONSTRAINT quiz_questions_media_source_kind_check
            CHECK (
                media_source_kind IS NULL
                OR media_source_kind IN ('AUDIO_URL', 'UPLOADED_FILE', 'VIDEO_PAGE', 'YOUTUBE')
            ),
    audio_url TEXT,
    audio_file_id UUID REFERENCES audio_files(id) ON DELETE SET NULL,
    retired BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX quiz_questions_quiz_idx ON quiz_questions (quiz_id, position);

CREATE TABLE quiz_question_options (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    position INT NOT NULL,
    label TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false,
    retired BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX quiz_question_options_question_idx ON quiz_question_options (question_id, position);

CREATE TABLE quiz_assignees (
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (quiz_id, user_id)
);
CREATE INDEX quiz_assignees_user_idx ON quiz_assignees (user_id);

CREATE TABLE quiz_attempts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(12) NOT NULL DEFAULT 'IN_PROGRESS'
        CONSTRAINT quiz_attempts_status_check
            CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'GRADED')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    submitted_at TIMESTAMPTZ,
    score_percent INT,
    fully_correct_count INT,
    question_unit_count INT,
    quiz_snapshot JSONB NOT NULL,
    feedback TEXT,
    CONSTRAINT quiz_attempts_unique_user_quiz UNIQUE (quiz_id, user_id)
);
CREATE INDEX quiz_attempts_quiz_idx ON quiz_attempts (quiz_id);
CREATE INDEX quiz_attempts_user_idx ON quiz_attempts (user_id);

CREATE TABLE quiz_answers (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL,
    answer_json JSONB,
    answer_text TEXT,
    score NUMERIC(4,3),
    teacher_percent INT
        CONSTRAINT quiz_answers_teacher_percent_check
            CHECK (teacher_percent IS NULL OR teacher_percent BETWEEN 0 AND 100),
    selected_option_ids UUID[],
    CONSTRAINT quiz_answers_unique_attempt_question UNIQUE (attempt_id, question_id)
);
CREATE INDEX quiz_answers_attempt_idx ON quiz_answers (attempt_id);
