-- Tables with assigned chair (chairperson). Links and list via API/UI.
CREATE TABLE IF NOT EXISTS app_tables (
  id BINARY(16) NOT NULL,
  name VARCHAR(255) NOT NULL,
  description TEXT DEFAULT NULL,
  chair_id BINARY(16) DEFAULT NULL,
  created_at DATETIME(6) DEFAULT NULL,
  updated_at DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY FK_app_tables_chair (chair_id),
  CONSTRAINT FK_app_tables_chair FOREIGN KEY (chair_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
