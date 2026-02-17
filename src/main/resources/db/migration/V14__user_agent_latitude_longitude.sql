-- Store location coordinates for map display (captured at registration/profile)
ALTER TABLE users ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE users ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

ALTER TABLE agents ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE agents ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
