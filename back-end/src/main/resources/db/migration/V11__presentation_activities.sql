-- Presentation-linked activities (homework-parity work nested under presentations).

CREATE TABLE activities (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    presentation_id UUID NOT NULL REFERENCES presentations(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    format VARCHAR(10) NOT NULL DEFAULT 'MANUAL'
        CONSTRAINT activities_format_check CHECK (format IN ('MANUAL', 'EXERCISE')),
    level VARCHAR(5),
    homework_type VARCHAR(20),
    position INT NOT NULL DEFAULT 0,
    trigger_file_id UUID REFERENCES presentation_files(id) ON DELETE SET NULL,
    trigger_page INT
        CONSTRAINT activities_trigger_page_check CHECK (trigger_page IS NULL OR trigger_page >= 1),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT activities_trigger_pair_check CHECK (
        (trigger_file_id IS NULL AND trigger_page IS NULL)
        OR (trigger_file_id IS NOT NULL AND trigger_page IS NOT NULL)
    )
);

CREATE INDEX activities_presentation_position_idx ON activities (presentation_id, position);
CREATE INDEX activities_trigger_file_idx ON activities (trigger_file_id);

CREATE TABLE activity_instructions_files (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    activity_id UUID NOT NULL UNIQUE REFERENCES activities(id) ON DELETE CASCADE,
    original_name TEXT NOT NULL,
    content_type TEXT NOT NULL,
    byte_size BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE activity_questions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    activity_id UUID NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    position INT NOT NULL,
    kind VARCHAR(20) NOT NULL
        CONSTRAINT activity_questions_kind_check
            CHECK (kind IN (
                'SINGLE_CHOICE',
                'MULTI_CHOICE',
                'MULTI_BLANK',
                'DRAG_DROP',
                'TABLE_FILL',
                'MATCHING',
                'TRUE_FALSE'
            )),
    prompt TEXT NOT NULL,
    structure_json JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX activity_questions_activity_idx ON activity_questions (activity_id, position);

CREATE TABLE activity_question_options (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES activity_questions(id) ON DELETE CASCADE,
    position INT NOT NULL,
    label TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX activity_question_options_question_idx ON activity_question_options (question_id, position);

CREATE TABLE activity_submissions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_id UUID NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING'
        CONSTRAINT activity_submissions_status_check
            CHECK (status IN ('PENDING', 'SUBMITTED', 'REVIEWED', 'GRADED')),
    response_text TEXT,
    submitted_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    score_percent INT,
    feedback TEXT,
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT activity_submissions_unique_user_activity UNIQUE (user_id, activity_id)
);

CREATE INDEX activity_submissions_user_id_idx ON activity_submissions (user_id);
CREATE INDEX activity_submissions_activity_id_idx ON activity_submissions (activity_id);

CREATE TABLE activity_answers (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES activity_submissions(id) ON DELETE CASCADE,
    question_id UUID REFERENCES activity_questions(id) ON DELETE SET NULL,
    score NUMERIC(4,3) NOT NULL,
    answer_json JSONB NULL,
    CONSTRAINT activity_answers_unique_submission_question UNIQUE (submission_id, question_id)
);

CREATE INDEX activity_answers_submission_idx ON activity_answers (submission_id);

CREATE TABLE activity_answer_options (
    answer_id UUID NOT NULL REFERENCES activity_answers(id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES activity_question_options(id) ON DELETE CASCADE,
    PRIMARY KEY (answer_id, option_id)
);
