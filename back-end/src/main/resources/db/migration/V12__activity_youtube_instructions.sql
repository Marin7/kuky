-- Activity media: text instructions + optional YouTube URL.
-- trigger_page means "insert after this PDF page" (between N and N+1), not replace page N.

ALTER TABLE activities
    ADD COLUMN instructions_text TEXT NOT NULL DEFAULT '',
    ADD COLUMN youtube_url TEXT;
