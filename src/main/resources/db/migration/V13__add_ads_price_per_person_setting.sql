-- Add ads price per person setting
-- Default value: 2 TZS (one person for 2 TZS)
INSERT INTO system_settings (id, `key`, `value`, updated_at)
SELECT UNHEX(REPLACE(UUID(), '-', '')), 'ads_price_per_person', '2', NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM system_settings WHERE `key` = 'ads_price_per_person'
);
