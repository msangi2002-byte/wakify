-- ============================================================
-- Wakify: Contact hashes for "People You May Know" (Facebook-style)
-- Store hashed phone/email from contact upload; match against users.
-- No FK to avoid id type issues; JPA can still link in code.
-- ============================================================

CREATE TABLE IF NOT EXISTS user_contact_hashes (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  contact_type VARCHAR(10) NOT NULL,
  hash VARCHAR(64) NOT NULL,
  created_at DATETIME(6) NULL,
  UNIQUE KEY uq_user_contact (user_id, contact_type, hash),
  KEY idx_user_id (user_id),
  KEY idx_hash (hash)
) ENGINE=InnoDB;
