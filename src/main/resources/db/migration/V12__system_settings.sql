-- System-wide config (e.g. agent register amount, to-be-business amount)
CREATE TABLE IF NOT EXISTS system_settings (
  id BINARY(16) NOT NULL,
  `key` VARCHAR(128) NOT NULL,
  `value` VARCHAR(512) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_system_settings_key (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Default amounts (TZS): agent registration 20000, business activation 10000
INSERT INTO system_settings (id, `key`, `value`, updated_at)
VALUES
  (UNHEX(REPLACE(UUID(), '-', '')), 'agent_register_amount', '20000', NOW(6)),
  (UNHEX(REPLACE(UUID(), '-', '')), 'to_be_business_amount', '10000', NOW(6));
