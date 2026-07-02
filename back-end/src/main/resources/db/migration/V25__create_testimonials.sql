-- Student-written testimonials, curated by the teacher, shown on the public homepage.

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
