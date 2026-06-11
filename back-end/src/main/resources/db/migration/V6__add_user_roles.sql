-- Authorization tier for the teacher backoffice.
-- A single persisted role distinguishes the teacher/admin from students.
-- The admin account is promoted at startup by AdminBootstrap (by configured email),
-- not here, so this migration is environment-agnostic.

ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'STUDENT'
        CONSTRAINT users_role_check CHECK (role IN ('STUDENT', 'ADMIN'));
