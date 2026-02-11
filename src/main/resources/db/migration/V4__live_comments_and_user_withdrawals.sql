-- ============================================================
-- Wakify: Live stream comments + User (host) cash withdrawals
-- No FK to avoid id type issues; JPA enforces in code.
-- ============================================================

-- Live stream comments (comment kwenye live)
CREATE TABLE IF NOT EXISTS live_stream_comments (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  live_stream_id VARCHAR(36) NOT NULL,
  author_id VARCHAR(36) NOT NULL,
  content VARCHAR(500) NOT NULL,
  created_at DATETIME(6) NULL,
  KEY idx_live_stream_id (live_stream_id),
  KEY idx_author_id (author_id)
) ENGINE=InnoDB;

-- User cash withdrawal requests (host convert gift cash to pesa)
CREATE TABLE IF NOT EXISTS user_cash_withdrawals (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  amount DECIMAL(15,2) NOT NULL,
  payment_method VARCHAR(50) NULL,
  payment_phone VARCHAR(20) NULL,
  payment_name VARCHAR(100) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  transaction_id VARCHAR(100) NULL,
  rejection_reason VARCHAR(255) NULL,
  processed_at DATETIME(6) NULL,
  created_at DATETIME(6) NULL,
  KEY idx_user_id (user_id),
  KEY idx_status (status)
) ENGINE=InnoDB;
