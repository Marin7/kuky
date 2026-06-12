-- Homework Exercises & Self-Correction (feature 007).
-- Adds a MANUAL|EXERCISE format discriminator to homework, a GRADED terminal
-- status + score to submissions, and the structured-question/answer-key tables.
-- Manual + pre-existing homework default to MANUAL and are untouched.

-- 1. Format discriminator on the existing assignment table.
ALTER TABLE homework_assignments
    ADD COLUMN format VARCHAR(10) NOT NULL DEFAULT 'MANUAL'
        CONSTRAINT homework_assignments_format_check CHECK (format IN ('MANUAL', 'EXERCISE'));

-- 2. Auto-graded score + new terminal status on submissions.
ALTER TABLE homework_submissions
    ADD COLUMN score_percent INT;

ALTER TABLE homework_submissions
    DROP CONSTRAINT homework_submissions_status_check;
ALTER TABLE homework_submissions
    ADD CONSTRAINT homework_submissions_status_check
        CHECK (status IN ('PENDING', 'SUBMITTED', 'REVIEWED', 'GRADED'));

-- 3. Exercise questions (ordered within an assignment).
CREATE TABLE homework_questions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    assignment_id UUID NOT NULL REFERENCES homework_assignments(id) ON DELETE CASCADE,
    position INT NOT NULL,
    kind VARCHAR(20) NOT NULL
        CONSTRAINT homework_questions_kind_check
            CHECK (kind IN ('SINGLE_CHOICE', 'MULTI_CHOICE', 'FILL_BLANK')),
    prompt TEXT NOT NULL
);

CREATE INDEX homework_questions_assignment_idx
    ON homework_questions (assignment_id, position);

-- 4. Question options — serve both choice options and fill-blank accepted answers.
--    Fill-blank: each row is an accepted answer (label = accepted text, is_correct = true).
CREATE TABLE homework_question_options (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES homework_questions(id) ON DELETE CASCADE,
    position INT NOT NULL,
    label TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX homework_question_options_question_idx
    ON homework_question_options (question_id, position);

-- 5. Per-question student answers within a graded submission.
--    question_id is SET NULL on delete so later answer-key edits don't break history.
CREATE TABLE homework_answers (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES homework_submissions(id) ON DELETE CASCADE,
    question_id UUID REFERENCES homework_questions(id) ON DELETE SET NULL,
    answer_text TEXT,
    score NUMERIC(4,3) NOT NULL,
    CONSTRAINT homework_answers_unique_submission_question UNIQUE (submission_id, question_id)
);

CREATE INDEX homework_answers_submission_idx ON homework_answers (submission_id);

-- 6. Which options a student selected for a choice question.
CREATE TABLE homework_answer_options (
    answer_id UUID NOT NULL REFERENCES homework_answers(id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES homework_question_options(id) ON DELETE CASCADE,
    PRIMARY KEY (answer_id, option_id)
);
