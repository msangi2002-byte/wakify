-- ============================================================
-- Wakify: SQL updates for new features
-- Database: MySQL
-- ============================================================
-- Safe to run multiple times: adds columns only if missing.
-- Foreign keys are omitted to avoid "incompatible" with Hibernate-
-- created id columns (type/collation). JPA still enforces relationships.
-- ============================================================

-- Helper: add column only if it does not exist (avoids "Duplicate column" error)
DELIMITER //
DROP PROCEDURE IF EXISTS add_column_if_not_exists //
CREATE PROCEDURE add_column_if_not_exists(
  IN p_table VARCHAR(64),
  IN p_column VARCHAR(64),
  IN p_definition VARCHAR(500)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column
  ) THEN
    SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END //
DELIMITER ;

-- ----- 1. POSTS: location, feeling_activity, tagged users -----
CALL add_column_if_not_exists('posts', 'location', 'VARCHAR(500) NULL');
CALL add_column_if_not_exists('posts', 'feeling_activity', 'VARCHAR(200) NULL');

-- Junction table for post tagged users. No FK to avoid "incompatible" (id type may be BINARY(16) etc).
-- JPA still enforces the relationship. Add FKs later with exact type from SHOW CREATE TABLE posts/users if needed.
CREATE TABLE IF NOT EXISTS post_tagged_users (
  post_id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  PRIMARY KEY (post_id, user_id)
) ENGINE=InnoDB;

-- ----- 2. USERS: profile privacy -----
CALL add_column_if_not_exists('users', 'profile_visibility', "VARCHAR(20) NULL DEFAULT 'PUBLIC'");
CALL add_column_if_not_exists('users', 'following_list_visibility', "VARCHAR(20) NULL DEFAULT 'PUBLIC'");

-- ----- 3. User restrictions (restricted list) -----
CREATE TABLE IF NOT EXISTS user_restrictions (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  restricter_id VARCHAR(36) NOT NULL,
  restricted_id VARCHAR(36) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_restricter_restricted (restricter_id, restricted_id)
) ENGINE=InnoDB;

-- ----- 4. Archived conversations -----
CREATE TABLE IF NOT EXISTS user_archived_conversations (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  other_user_id VARCHAR(36) NOT NULL,
  archived_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_user_other (user_id, other_user_id)
) ENGINE=InnoDB;

-- ----- 5. MESSAGES: type, is_deleted -----
CALL add_column_if_not_exists('messages', 'type', "VARCHAR(20) NULL DEFAULT 'TEXT'");
CALL add_column_if_not_exists('messages', 'is_deleted', 'BIT(1) NULL DEFAULT 0');

-- ----- 6. Community invites -----
CREATE TABLE IF NOT EXISTS community_invites (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  community_id VARCHAR(36) NOT NULL,
  inviter_id VARCHAR(36) NOT NULL,
  invitee_id VARCHAR(36) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_community_invitee (community_id, invitee_id)
) ENGINE=InnoDB;

-- ----- 7. Community polls -----
CREATE TABLE IF NOT EXISTS community_polls (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  community_id VARCHAR(36) NOT NULL,
  creator_id VARCHAR(36) NOT NULL,
  question VARCHAR(500) NOT NULL,
  ends_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS community_poll_options (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  poll_id VARCHAR(36) NOT NULL,
  text VARCHAR(300) NOT NULL,
  display_order INT NOT NULL DEFAULT 0,
  votes_count INT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS community_poll_votes (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  poll_id VARCHAR(36) NOT NULL,
  option_id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_poll_user (poll_id, user_id)
) ENGINE=InnoDB;

-- ----- 8. Community events -----
CREATE TABLE IF NOT EXISTS community_events (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  community_id VARCHAR(36) NOT NULL,
  creator_id VARCHAR(36) NOT NULL,
  title VARCHAR(300) NOT NULL,
  description TEXT NULL,
  location VARCHAR(500) NULL,
  start_time DATETIME(6) NOT NULL,
  end_time DATETIME(6) NULL,
  cover_image VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NULL
) ENGINE=InnoDB;

-- ----- 9. Business follows -----
CREATE TABLE IF NOT EXISTS business_follows (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  business_id VARCHAR(36) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_user_business (user_id, business_id)
) ENGINE=InnoDB;

-- ----- 10. Notification settings -----
CREATE TABLE IF NOT EXISTS user_notification_settings (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  type VARCHAR(30) NOT NULL,
  enabled BIT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NULL,
  UNIQUE KEY uk_user_type (user_id, type)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_muted_notifications (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  muted_user_id VARCHAR(36) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_user_muted (user_id, muted_user_id)
) ENGINE=InnoDB;

-- ----- 11. Auth events (login/register background data) -----
CREATE TABLE IF NOT EXISTS auth_events (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  event_type VARCHAR(30) NOT NULL,
  user_id VARCHAR(36) NULL,
  identifier VARCHAR(255) NULL,
  ip_address VARCHAR(45) NULL,
  user_agent TEXT NULL,
  device_type VARCHAR(50) NULL,
  browser VARCHAR(100) NULL,
  os VARCHAR(100) NULL,
  accept_language VARCHAR(200) NULL,
  country_from_ip VARCHAR(10) NULL,
  timezone VARCHAR(100) NULL,
  device_id VARCHAR(255) NULL,
  success BIT(1) NULL,
  created_at DATETIME(6) NOT NULL,
  KEY idx_auth_events_user_id (user_id),
  KEY idx_auth_events_created_at (created_at),
  KEY idx_auth_events_ip (ip_address)
) ENGINE=InnoDB;
