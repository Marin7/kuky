-- University student portal: role, level, schedule, exams, news, catalog availability

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('USER', 'STUDENT', 'UNIVERSITY_STUDENT', 'ADMIN'));

ALTER TABLE users ADD COLUMN IF NOT EXISTS university_level VARCHAR(20);

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_university_level_check;
ALTER TABLE users ADD CONSTRAINT users_university_level_check
    CHECK (
        university_level IS NULL
        OR university_level IN ('BEGINNER', 'INTERMEDIATE')
    );

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_university_level_role_check;
ALTER TABLE users ADD CONSTRAINT users_university_level_role_check
    CHECK (
        (role = 'UNIVERSITY_STUDENT' AND university_level IS NOT NULL)
        OR (role <> 'UNIVERSITY_STUDENT' AND university_level IS NULL)
    );

CREATE TABLE university_schedule_sessions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    level       VARCHAR(20) NOT NULL,
    day_of_week SMALLINT NOT NULL,
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    title       VARCHAR(200),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT university_schedule_sessions_level_check
        CHECK (level IN ('BEGINNER', 'INTERMEDIATE')),
    CONSTRAINT university_schedule_sessions_dow_check
        CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT university_schedule_sessions_time_check
        CHECK (end_time > start_time),
    CONSTRAINT university_schedule_sessions_unique
        UNIQUE (level, day_of_week, start_time)
);

CREATE TABLE university_schedule_exceptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    level           VARCHAR(20) NOT NULL,
    exception_date  DATE NOT NULL,
    kind            VARCHAR(20) NOT NULL,
    session_id      UUID REFERENCES university_schedule_sessions(id) ON DELETE SET NULL,
    start_time      TIME,
    end_time        TIME,
    title           VARCHAR(200),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT university_schedule_exceptions_level_check
        CHECK (level IN ('BEGINNER', 'INTERMEDIATE')),
    CONSTRAINT university_schedule_exceptions_kind_check
        CHECK (kind IN ('CANCEL', 'EXTRA')),
    CONSTRAINT university_schedule_exceptions_extra_times_check
        CHECK (
            (kind = 'CANCEL')
            OR (kind = 'EXTRA' AND start_time IS NOT NULL AND end_time IS NOT NULL AND end_time > start_time)
        )
);

CREATE INDEX university_schedule_exceptions_date_idx
    ON university_schedule_exceptions (exception_date, level);

CREATE TABLE university_exam_dates (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(200) NOT NULL,
    exam_at     TIMESTAMPTZ NOT NULL,
    description TEXT,
    published   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE university_news_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title         VARCHAR(200) NOT NULL,
    body          TEXT NOT NULL,
    published     BOOLEAN NOT NULL DEFAULT FALSE,
    published_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE university_homework_availability (
    assignment_id UUID NOT NULL REFERENCES homework_assignments(id) ON DELETE CASCADE,
    level         VARCHAR(20) NOT NULL,
    PRIMARY KEY (assignment_id, level),
    CONSTRAINT university_homework_availability_level_check
        CHECK (level IN ('BEGINNER', 'INTERMEDIATE'))
);

CREATE TABLE university_presentation_availability (
    presentation_id UUID NOT NULL REFERENCES presentations(id) ON DELETE CASCADE,
    level           VARCHAR(20) NOT NULL,
    PRIMARY KEY (presentation_id, level),
    CONSTRAINT university_presentation_availability_level_check
        CHECK (level IN ('BEGINNER', 'INTERMEDIATE'))
);
