-- Update script for wakilfly_db MySQL database
-- Adds missing tables from database.sql
-- Run with: mysql -u user -p wakilfly_db < update_mysql.sql

USE `wakilfly_db`;

-- --------------------------------------------------------
-- Add missing: conversations table
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS `conversations` (
  `id` binary(16) NOT NULL,
  `participant_one_id` binary(16) NOT NULL,
  `participant_two_id` binary(16) NOT NULL,
  `last_message_at` datetime(6) DEFAULT NULL,
  `last_message_preview` varchar(255) DEFAULT NULL,
  `is_buyer_seller_chat` bit(1) DEFAULT b'0',
  `product_id` binary(16) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_conversations_participant_one` (`participant_one_id`),
  KEY `FK_conversations_participant_two` (`participant_two_id`),
  CONSTRAINT `FK_conversations_participant_one` FOREIGN KEY (`participant_one_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_conversations_participant_two` FOREIGN KEY (`participant_two_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------
-- Add reply_to_id to messages (reply to specific message)
-- --------------------------------------------------------
ALTER TABLE `messages` ADD COLUMN `reply_to_id` binary(16) DEFAULT NULL;
ALTER TABLE `messages` ADD KEY `FK_message_reply_to` (`reply_to_id`);
ALTER TABLE `messages` ADD CONSTRAINT `FK_message_reply_to` FOREIGN KEY (`reply_to_id`) REFERENCES `messages` (`id`) ON DELETE SET NULL;

-- --------------------------------------------------------
-- Add guest_stream_key to live_stream_join_requests (guest gets own stream ID)
-- Skip if column already exists (duplicate column error).
-- --------------------------------------------------------
ALTER TABLE `live_stream_join_requests` ADD COLUMN `guest_stream_key` varchar(255) DEFAULT NULL;
ALTER TABLE `live_stream_join_requests` ADD UNIQUE KEY `UK_guest_stream_key` (`guest_stream_key`);

COMMIT;
