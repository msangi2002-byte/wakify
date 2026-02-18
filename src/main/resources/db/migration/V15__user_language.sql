-- Preferred language (e.g. sw, en) for UI and Audience Analytics "By Language"
ALTER TABLE users ADD COLUMN IF NOT EXISTS language VARCHAR(10);
