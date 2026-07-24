-- Move image/presentation binary payloads out of Postgres (served from disk/classpath).
-- Safe on fresh installs where V1 never created these columns.

ALTER TABLE images DROP COLUMN IF EXISTS data;
ALTER TABLE presentation_files DROP COLUMN IF EXISTS data;
