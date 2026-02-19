-- ============================================================
-- Fix: notifications.type must fit longest enum e.g. BUSINESS_REQUEST_RECEIVED (25 chars)
-- Data truncated for column 'type' at row 1 – ensure column is at least 64 chars
-- ============================================================

ALTER TABLE notifications MODIFY COLUMN type VARCHAR(64) NOT NULL;
