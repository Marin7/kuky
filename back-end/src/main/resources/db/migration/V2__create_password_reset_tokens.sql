CREATE TABLE password_reset_tokens (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(36) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT prt_token_unique UNIQUE (token)
);

CREATE INDEX prt_token_idx ON password_reset_tokens (token);
CREATE INDEX prt_user_id_idx ON password_reset_tokens (user_id);
