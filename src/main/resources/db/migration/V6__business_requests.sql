-- ============================================================
-- Wakify: business_requests table (user requests to become a business; agent selected at form submit)
-- ============================================================

CREATE TABLE IF NOT EXISTS business_requests (
  id BINARY(16) NOT NULL,
  business_name VARCHAR(255) NOT NULL,
  owner_phone VARCHAR(255) NOT NULL,
  category VARCHAR(255) DEFAULT NULL,
  region VARCHAR(255) DEFAULT NULL,
  district VARCHAR(255) DEFAULT NULL,
  ward VARCHAR(255) DEFAULT NULL,
  street VARCHAR(255) DEFAULT NULL,
  description TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME(6) DEFAULT NULL,
  updated_at DATETIME(6) DEFAULT NULL,
  user_id BINARY(16) NOT NULL,
  agent_id BINARY(16) NOT NULL,
  PRIMARY KEY (id),
  KEY FK_business_requests_user (user_id),
  KEY FK_business_requests_agent (agent_id),
  KEY idx_business_requests_agent_created (agent_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
