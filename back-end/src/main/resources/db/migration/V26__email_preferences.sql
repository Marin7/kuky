-- Per-account email preferences (feature 049-email-preferences).
--
-- Opt-in only: NOT NULL DEFAULT false means every existing and future account
-- starts with the option switched off. There is deliberately no backfill and no
-- data-dependent default — "nobody is emailed unless they asked to be" is a
-- property of this DDL rather than a step someone has to remember to leave out.

ALTER TABLE users
    ADD COLUMN email_on_homework_assigned BOOLEAN NOT NULL DEFAULT false;
