-- ============================================================
-- Wakify: notifications.type must fit enum values e.g. COMMUNITY_INVITE (16 chars)
-- ============================================================

ALTER TABLE notifications MODIFY COLUMN type VARCHAR(50) NOT NULL;
