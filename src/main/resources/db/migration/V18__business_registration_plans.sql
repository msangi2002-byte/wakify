CREATE TABLE IF NOT EXISTS business_registration_plans (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    price DECIMAL(12,2) NOT NULL,
    sort_order INT DEFAULT 0,
    is_active BIT(1) DEFAULT 1,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
