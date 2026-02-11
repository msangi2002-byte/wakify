-- ============================================================
-- Wakify: Pin posts in groups (admin pins post; order: pinned first, then recency)
-- Uses add_column_if_not_exists from V2.
-- ============================================================

CALL add_column_if_not_exists('posts', 'is_pinned', 'TINYINT(1) NOT NULL DEFAULT 0');
CALL add_column_if_not_exists('posts', 'pinned_at', 'DATETIME(6) NULL');
