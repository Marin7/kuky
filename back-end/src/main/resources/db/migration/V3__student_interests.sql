-- Student interests: optional free-text note + multi-select catalogue join table
ALTER TABLE users
    ADD COLUMN interests_note VARCHAR(280);

CREATE TABLE user_interests (
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    interest_code VARCHAR(40) NOT NULL,
    PRIMARY KEY (user_id, interest_code),
    CONSTRAINT user_interests_code_check CHECK (interest_code IN (
        'TRAVEL', 'MUSIC', 'SPORTS', 'FOOD', 'CINEMA', 'READING',
        'TECHNOLOGY', 'NATURE', 'ART', 'WORK', 'FAMILY', 'CULTURE'
    ))
);

CREATE INDEX user_interests_user_id_idx ON user_interests (user_id);
