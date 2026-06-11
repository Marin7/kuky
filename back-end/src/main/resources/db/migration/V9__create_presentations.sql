-- Teacher-authored slide presentations shared per student, plus uploaded images.

-- Uploaded images stored as bytes (served via GET /api/v1/images/{id}).
CREATE TABLE images (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    content_type VARCHAR(50) NOT NULL,
    byte_size INT NOT NULL,
    data BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT images_content_type_check CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    CONSTRAINT images_size_check CHECK (byte_size > 0 AND byte_size <= 2097152)
);

CREATE TABLE presentations (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE presentation_slides (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    presentation_id UUID NOT NULL REFERENCES presentations(id) ON DELETE CASCADE,
    heading VARCHAR(200) NOT NULL,
    body TEXT NOT NULL DEFAULT '',
    image_id UUID REFERENCES images(id) ON DELETE SET NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX presentation_slides_presentation_idx ON presentation_slides (presentation_id, sort_order);

CREATE TABLE presentation_shares (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    presentation_id UUID NOT NULL REFERENCES presentations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT presentation_shares_unique UNIQUE (presentation_id, user_id)
);

CREATE INDEX presentation_shares_user_idx ON presentation_shares (user_id);
