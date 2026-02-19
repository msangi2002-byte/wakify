-- User location at request time (for agent map) and document fields (filled by agent)
ALTER TABLE business_requests ADD COLUMN IF NOT EXISTS latitude DOUBLE NULL;
ALTER TABLE business_requests ADD COLUMN IF NOT EXISTS longitude DOUBLE NULL;
ALTER TABLE business_requests ADD COLUMN IF NOT EXISTS nida_number VARCHAR(64) NULL;
ALTER TABLE business_requests ADD COLUMN IF NOT EXISTS tin_number VARCHAR(64) NULL;
ALTER TABLE business_requests ADD COLUMN IF NOT EXISTS company_name VARCHAR(255) NULL;
ALTER TABLE business_requests ADD COLUMN IF NOT EXISTS id_document_url VARCHAR(512) NULL;
ALTER TABLE business_requests ADD COLUMN IF NOT EXISTS id_back_document_url VARCHAR(512) NULL;
