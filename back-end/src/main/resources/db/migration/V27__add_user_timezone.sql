ALTER TABLE users
    ADD COLUMN timezone VARCHAR(64),
    ADD COLUMN timezone_is_manual BOOLEAN NOT NULL DEFAULT false;
