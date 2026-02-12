-- ============================================================
-- Wakify: Online status (lastSeen for activity/heartbeat)
-- ============================================================

CALL add_column_if_not_exists('users', 'last_seen', 'DATETIME(6) NULL');
