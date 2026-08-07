-- Optional activity photo (reuses shared images table). YouTube becomes optional when a photo is set.

ALTER TABLE activities
    ADD COLUMN image_id UUID REFERENCES images(id) ON DELETE SET NULL;

CREATE INDEX activities_image_idx ON activities (image_id);
