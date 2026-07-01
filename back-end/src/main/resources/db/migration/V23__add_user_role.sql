-- Splits "has an account" from "is a student": role gains a third value, USER,
-- which becomes the default for new registrations. Only the teacher can promote
-- a USER to STUDENT (or demote back) from the admin panel.
-- No data is touched: every existing row is already STUDENT or ADMIN, which is
-- exactly the grandfathering this feature requires.

ALTER TABLE users
    DROP CONSTRAINT users_role_check;

ALTER TABLE users
    ALTER COLUMN role SET DEFAULT 'USER';

ALTER TABLE users
    ADD CONSTRAINT users_role_check CHECK (role IN ('USER', 'STUDENT', 'ADMIN'));
