ALTER TABLE users
  ADD COLUMN first_name      VARCHAR(100),
  ADD COLUMN last_name       VARCHAR(100),
  ADD COLUMN username        VARCHAR(50),
  ADD COLUMN avatar_image_id UUID REFERENCES images(id) ON DELETE SET NULL;

CREATE UNIQUE INDEX users_username_lower_idx
  ON users (LOWER(username))
  WHERE username IS NOT NULL;

-- Seed username from email prefix for existing users; append row index for duplicates
WITH ranked AS (
  SELECT id,
         LEFT(SPLIT_PART(email, '@', 1), 47)                   AS base_un,
         ROW_NUMBER() OVER (
           PARTITION BY LOWER(LEFT(SPLIT_PART(email, '@', 1), 47))
           ORDER BY created_at
         ) - 1                                                  AS dup_idx
  FROM users
)
UPDATE users u
   SET username = CASE
                    WHEN r.dup_idx = 0 THEN r.base_un
                    ELSE r.base_un || r.dup_idx::text
                  END
  FROM ranked r
 WHERE u.id = r.id;
