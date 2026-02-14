-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Host: mysql:3306
-- Generation Time: Feb 11, 2026 at 08:31 PM
-- Server version: 8.0.45
-- PHP Version: 8.3.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `wakilfly_db`
-- Schema reference: when editing the database, add or update table definitions here.
--

DELIMITER $$
--
-- Procedures
--
CREATE DEFINER=`admin`@`%` PROCEDURE `add_column_if_not_exists` (IN `p_table` VARCHAR(64), IN `p_column` VARCHAR(64), IN `p_definition` VARCHAR(500))   BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column
  ) THEN
    SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `ads`
--

CREATE TABLE `ads` (
  `id` binary(16) NOT NULL,
  `amount_spent` decimal(15,2) DEFAULT NULL,
  `clicks` int DEFAULT NULL,
  `cost_per_click` decimal(10,2) DEFAULT NULL,
  `cost_per_view` decimal(10,2) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `daily_budget` decimal(15,2) DEFAULT NULL,
  `description` varchar(2000) DEFAULT NULL,
  `end_date` datetime(6) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `impressions` int DEFAULT NULL,
  `start_date` datetime(6) DEFAULT NULL,
  `status` enum('PENDING','ACTIVE','PAUSED','COMPLETED','REJECTED') NOT NULL,
  `target_categories` varchar(255) DEFAULT NULL,
  `target_regions` varchar(255) DEFAULT NULL,
  `target_url` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `total_budget` decimal(15,2) DEFAULT NULL,
  `type` enum('BANNER','FEED','PRODUCT','STORY','POPUP') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `video_url` varchar(255) DEFAULT NULL,
  `business_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `agents`
--

CREATE TABLE `agents` (
  `id` binary(16) NOT NULL,
  `agent_code` varchar(255) DEFAULT NULL,
  `approved_at` datetime(6) DEFAULT NULL,
  `available_balance` decimal(12,2) DEFAULT NULL,
  `businesses_activated` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `district` varchar(255) DEFAULT NULL,
  `id_document_url` varchar(255) DEFAULT NULL,
  `is_verified` bit(1) DEFAULT NULL,
  `license_number` varchar(255) DEFAULT NULL,
  `national_id` varchar(255) DEFAULT NULL,
  `region` varchar(255) DEFAULT NULL,
  `status` enum('PENDING','ACTIVE','SUSPENDED','INACTIVE') DEFAULT NULL,
  `total_earnings` decimal(12,2) DEFAULT NULL,
  `total_referrals` int DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `ward` varchar(255) DEFAULT NULL,
  `user_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `audit_logs`
--

CREATE TABLE `audit_logs` (
  `id` binary(16) NOT NULL,
  `action` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `details` text,
  `entity_id` binary(16) DEFAULT NULL,
  `entity_type` varchar(255) DEFAULT NULL,
  `ip_address` varchar(255) DEFAULT NULL,
  `new_values` text,
  `old_values` text,
  `user_agent` varchar(255) DEFAULT NULL,
  `user_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `auth_events`
--

CREATE TABLE `auth_events` (
  `id` binary(16) NOT NULL,
  `event_type` enum('REGISTRATION','LOGIN','LOGIN_FAILED','LOGOUT','PASSWORD_RESET_REQUEST','PASSWORD_RESET_SUCCESS') NOT NULL,
  `user_id` binary(16) DEFAULT NULL,
  `identifier` varchar(255) DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `user_agent` text,
  `device_type` varchar(50) DEFAULT NULL,
  `browser` varchar(100) DEFAULT NULL,
  `os` varchar(100) DEFAULT NULL,
  `accept_language` varchar(200) DEFAULT NULL,
  `country_from_ip` varchar(10) DEFAULT NULL,
  `timezone` varchar(100) DEFAULT NULL,
  `device_id` varchar(255) DEFAULT NULL,
  `success` bit(1) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `auth_events`
--

INSERT INTO `auth_events` (`id`, `event_type`, `user_id`, `identifier`, `ip_address`, `user_agent`, `device_type`, `browser`, `os`, `accept_language`, `country_from_ip`, `timezone`, `device_id`, `success`, `created_at`) VALUES
(0x1f21d58dfdb9497697915584a428b8e9, 'LOGIN', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, '102.216.245.155', 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36', 'Desktop', 'Chrome', 'Linux', 'en-GB,en-US;q=0.9,en;q=0.8,sw;q=0.7', NULL, NULL, NULL, b'1', '2026-02-11 22:04:04.962905'),
(0x549433ab492d4ee693c0a0fde0cee118, 'LOGIN', 0x1a531d9d25a54266a87e10f0ae60e187, NULL, '197.186.15.104', 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36', 'Mobile', 'Chrome', 'Android', 'en-GB,en-US;q=0.9,en;q=0.8', NULL, NULL, NULL, b'1', '2026-02-11 22:04:06.399349'),
(0x923cc8639e63495595e2f04c5f259142, 'LOGIN', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, '154.72.20.43', 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36', 'Desktop', 'Chrome', 'Linux', 'en-GB,en-US;q=0.9,en;q=0.8,sw;q=0.7', NULL, NULL, NULL, b'1', '2026-02-11 23:20:07.329929');

-- --------------------------------------------------------

--
-- Table structure for table `businesses`
--

CREATE TABLE `businesses` (
  `id` binary(16) NOT NULL,
  `category` varchar(255) DEFAULT NULL,
  `cover_image` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `district` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `followers_count` int DEFAULT NULL,
  `is_verified` bit(1) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `logo` varchar(255) DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `rating` double DEFAULT NULL,
  `region` varchar(255) DEFAULT NULL,
  `reviews_count` int DEFAULT NULL,
  `status` enum('PENDING','ACTIVE','SUSPENDED','EXPIRED','INACTIVE') DEFAULT NULL,
  `street` varchar(255) DEFAULT NULL,
  `trust_badge` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `ward` varchar(255) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `agent_id` binary(16) DEFAULT NULL,
  `owner_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `business_follows`
--

CREATE TABLE `business_follows` (
  `id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `business_id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `business_requests`
-- User requests to become a business; user selects an agent when filling the form.
-- Agent sees requests where they were selected (agent_id = this agent).
--
CREATE TABLE `business_requests` (
  `id` binary(16) NOT NULL,
  `business_name` varchar(255) NOT NULL,
  `owner_phone` varchar(255) NOT NULL,
  `category` varchar(255) DEFAULT NULL,
  `region` varchar(255) DEFAULT NULL,
  `district` varchar(255) DEFAULT NULL,
  `ward` varchar(255) DEFAULT NULL,
  `street` varchar(255) DEFAULT NULL,
  `description` text,
  `status` enum('PENDING','APPROVED','REJECTED','CONVERTED') NOT NULL DEFAULT 'PENDING',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` binary(16) NOT NULL,
  `agent_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_business_requests_user` (`user_id`),
  KEY `FK_business_requests_agent` (`agent_id`),
  KEY `idx_business_requests_agent_created` (`agent_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `calls`
--

CREATE TABLE `calls` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `duration_seconds` int DEFAULT NULL,
  `ended_at` datetime(6) DEFAULT NULL,
  `room_id` varchar(255) DEFAULT NULL,
  `started_at` datetime(6) DEFAULT NULL,
  `status` enum('INITIATING','RINGING','ONGOING','ENDED','MISSED','REJECTED','BUSY') NOT NULL,
  `type` enum('VOICE','VIDEO') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `caller_id` binary(16) NOT NULL,
  `receiver_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `calls`
--

INSERT INTO `calls` (`id`, `created_at`, `duration_seconds`, `ended_at`, `room_id`, `started_at`, `status`, `type`, `updated_at`, `caller_id`, `receiver_id`) VALUES
(0x08c54a1620b74994bd3c50003cf1c67e, '2026-02-10 03:58:12.616815', NULL, '2026-02-10 05:20:43.816854', 'call_e526b47b', NULL, 'REJECTED', 'VOICE', '2026-02-10 05:20:43.867984', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x140bf017a0894023886af1b0d92d77be, '2026-02-10 05:13:59.417185', NULL, '2026-02-10 05:20:47.472738', 'call_7a11c366', NULL, 'REJECTED', 'VIDEO', '2026-02-10 05:20:47.477322', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x17a1968558fc4c18868e84a7ada55c80, '2026-02-10 03:56:34.820841', NULL, '2026-02-10 05:20:49.612915', 'call_7d13e00c', NULL, 'REJECTED', 'VIDEO', '2026-02-10 05:20:49.615919', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x1a935601113b458fafd60ad680fa0f6e, '2026-02-10 16:56:09.812648', 67, '2026-02-10 16:57:19.972165', 'call_a50d4e5d', '2026-02-10 16:56:12.622007', 'ENDED', 'VOICE', '2026-02-10 16:57:19.978268', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x22c0f4bb3b794de9b9abd0abf934975b, '2026-02-10 17:15:17.483838', 47, '2026-02-10 17:16:07.560081', 'call_b593bf5c', '2026-02-10 17:15:19.713164', 'ENDED', 'VIDEO', '2026-02-10 17:16:07.565626', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x2a0a8ecf7e1d4bbf94e781ff961c5e43, '2026-02-10 17:37:33.716587', 16, '2026-02-10 17:37:55.503664', 'call_2972986b', '2026-02-10 17:37:38.702947', 'ENDED', 'VOICE', '2026-02-10 17:37:55.509256', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x2fec8d5aeb0d48fd98453b5720836b59, '2026-02-10 17:22:34.808928', 14, '2026-02-10 17:22:53.116367', 'call_47c7da74', '2026-02-10 17:22:38.949298', 'ENDED', 'VIDEO', '2026-02-10 17:22:53.120767', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x354a7c597f29417f87b087d0d3c41ffe, '2026-02-10 17:32:57.378207', 26, '2026-02-10 17:33:29.689264', 'call_0807cf2b', '2026-02-10 17:33:03.581422', 'ENDED', 'VOICE', '2026-02-10 17:33:29.693747', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x35e73dba51d54e819cb853528af873be, '2026-02-11 12:38:03.484666', 49, '2026-02-11 12:38:56.612784', 'call_9fac34e0', '2026-02-11 12:38:07.223607', 'ENDED', 'VOICE', '2026-02-11 12:38:56.615114', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x384197abcb894fb4bb94bef6a56428b3, '2026-02-11 08:33:16.908361', NULL, '2026-02-11 08:33:41.901903', 'call_618ca28a', NULL, 'ENDED', 'VIDEO', '2026-02-11 08:33:41.906129', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x3b762ad3d2bf4119ba09a8bf7904d1a3, '2026-02-11 01:51:44.171858', 28, '2026-02-11 01:52:26.112379', 'call_ed775b3d', '2026-02-11 01:51:57.828531', 'ENDED', 'VIDEO', '2026-02-11 01:52:26.116812', 0xf97655b1d77642efbd9514045a4e7270, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x3dd64504464641f2bc471dcc3dd8845f, '2026-02-10 04:16:02.411963', NULL, '2026-02-10 05:20:52.094865', 'call_7cf4046e', NULL, 'REJECTED', 'VIDEO', '2026-02-10 05:20:52.099499', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x3fb62aa853de483b8ba0daff9ba3415b, '2026-02-11 03:07:14.546921', NULL, NULL, 'call_8a5b6582', '2026-02-11 03:07:31.722475', 'ONGOING', 'VIDEO', '2026-02-11 03:07:31.725669', 0x786190a6cc07406480c9318e630b693f, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x4810b10a7d97461fae1f093ca09b18ba, '2026-02-10 17:14:57.172267', 12, '2026-02-10 17:15:12.468638', 'call_7ad76764', '2026-02-10 17:14:59.656919', 'ENDED', 'VOICE', '2026-02-10 17:15:12.475715', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x4f15bf1094b445f98dd266503e4ec567, '2026-02-11 08:42:04.578419', NULL, '2026-02-11 10:49:00.116764', 'call_a27facdd', NULL, 'ENDED', 'VIDEO', '2026-02-11 10:49:00.125858', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x6e97e12bec3346a59d5853cda888ca4d),
(0x52d929c923514852b19d5ef0b04a82e4, '2026-02-10 03:49:54.687951', NULL, '2026-02-10 05:17:40.118679', 'call_98f4e632', NULL, 'REJECTED', 'VOICE', '2026-02-10 05:17:40.138123', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x57d6ad85e02e4c8781a5256378a6907e, '2026-02-10 03:56:13.205118', NULL, '2026-02-10 05:20:54.561640', 'call_548fd986', NULL, 'REJECTED', 'VOICE', '2026-02-10 05:20:54.565986', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x57f9e1bb28374d1ab16fa3ae231402c5, '2026-02-10 16:35:03.461500', 67, '2026-02-10 16:36:14.312874', 'call_c529bcbf', '2026-02-10 16:35:07.101347', 'ENDED', 'VOICE', '2026-02-10 16:36:14.315835', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x5d5869a0360c417baedfa777c1d781ec, '2026-02-10 05:08:21.796196', NULL, '2026-02-10 05:17:43.748948', 'call_85c5b474', NULL, 'REJECTED', 'VIDEO', '2026-02-10 05:17:43.755261', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x5e96de339ea445e29247dc28c5a49903, '2026-02-10 17:17:49.599978', 19, '2026-02-10 17:18:10.633924', 'call_bc39f1c2', '2026-02-10 17:17:51.593211', 'ENDED', 'VIDEO', '2026-02-10 17:18:10.638782', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x6c21b36dbea4425094377621d6dff290, '2026-02-10 18:22:50.234738', 31, '2026-02-10 18:23:24.215506', 'call_b66d7e2e', '2026-02-10 18:22:52.283783', 'ENDED', 'VOICE', '2026-02-10 18:23:24.222449', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x6c8d392a353b46da816e3a52135673e7, '2026-02-10 16:43:51.697035', 33, '2026-02-10 16:44:27.064332', 'call_85ce269d', '2026-02-10 16:43:53.591995', 'ENDED', 'VOICE', '2026-02-10 16:44:27.072547', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x6e33d79c50754a7a91f111d5f3437105, '2026-02-10 17:16:21.190552', 13, '2026-02-10 17:16:38.664080', 'call_f134446b', '2026-02-10 17:16:25.152661', 'ENDED', 'VOICE', '2026-02-10 17:16:38.667210', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x709950f1b1de42d3911b6311dbcb0ac3, '2026-02-10 19:05:44.756382', NULL, '2026-02-10 19:06:11.255430', 'call_8d477901', NULL, 'ENDED', 'VIDEO', '2026-02-10 19:06:11.262998', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x769ca587347143048cd741ee5e0df937, '2026-02-10 04:07:49.254255', NULL, '2026-02-10 05:17:45.782085', 'call_a6027815', NULL, 'REJECTED', 'VIDEO', '2026-02-10 05:17:45.786239', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x777eb7dfed1743768e210dcd1928ea25, '2026-02-10 17:33:34.809662', 5, '2026-02-10 17:33:45.333908', 'call_786cbd25', '2026-02-10 17:33:39.905570', 'ENDED', 'VOICE', '2026-02-10 17:33:45.337675', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x784d22e108aa4e5aa1ed4dfd4c61bbfe, '2026-02-10 04:08:37.228768', NULL, '2026-02-10 05:20:57.112699', 'call_84b45f27', NULL, 'REJECTED', 'VOICE', '2026-02-10 05:20:57.117717', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x7ad6892093a54468a0ab847f960c62f8, '2026-02-10 14:19:05.657344', 29, '2026-02-10 14:19:38.707532', 'call_f9c52fd2', '2026-02-10 14:19:09.551862', 'ENDED', 'VOICE', '2026-02-10 14:19:38.712874', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x7d830732d0934b87b4d965bab4487539, '2026-02-10 05:28:47.148994', NULL, NULL, 'call_fbf13d2e', '2026-02-10 16:21:06.296888', 'ONGOING', 'VOICE', '2026-02-10 16:21:06.322214', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x7f2fb5ce8f084e8db784f98d9a0ee5ba, '2026-02-10 14:27:47.934584', 33, '2026-02-10 14:28:24.394004', 'call_b5acedc3', '2026-02-10 14:27:50.544804', 'ENDED', 'VOICE', '2026-02-10 14:28:24.398733', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x83b384b0e0884b00acde2082e4fe151f, '2026-02-11 02:44:36.655935', 17, '2026-02-11 02:44:58.181274', 'call_218e1a22', '2026-02-11 02:44:40.190679', 'ENDED', 'VIDEO', '2026-02-11 02:44:58.186319', 0x786190a6cc07406480c9318e630b693f, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x90319215fb86433c97507485be5ce2b5, '2026-02-10 17:07:49.520459', NULL, NULL, 'call_3e72a1be', '2026-02-10 17:07:52.683459', 'ONGOING', 'VOICE', '2026-02-10 17:07:52.686259', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x911f514c25e14118948b0423daeb63d0, '2026-02-10 04:07:22.544354', NULL, '2026-02-10 05:21:00.273434', 'call_b5a5d2ee', NULL, 'REJECTED', 'VOICE', '2026-02-10 05:21:00.277403', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x9aa0d7e7d6e3408a8d014c28d3755487, '2026-02-10 19:17:56.639599', NULL, '2026-02-10 19:17:58.627784', 'call_0f807391', NULL, 'ENDED', 'VOICE', '2026-02-10 19:17:58.629730', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x9b3b7f5344444bd98a4b63da5b9fb26f, '2026-02-10 04:15:36.594011', NULL, '2026-02-10 05:21:02.116187', 'call_897b757b', NULL, 'REJECTED', 'VOICE', '2026-02-10 05:21:02.119936', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x9cd20e54890440229c7c7d77682401fc, '2026-02-10 17:06:44.844732', 56, '2026-02-10 17:07:44.715332', 'call_030e25e0', '2026-02-10 17:06:48.017511', 'ENDED', 'VIDEO', '2026-02-10 17:07:44.718230', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x9e988c94298e430a9c3c731ae799753c, '2026-02-10 16:57:58.982141', 506, '2026-02-10 17:06:29.653670', 'call_394aaaf8', '2026-02-10 16:58:03.473720', 'ENDED', 'VIDEO', '2026-02-10 17:06:29.660903', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x9fae1463e41e46728b0515313946f427, '2026-02-10 17:23:02.167226', 12, '2026-02-10 17:23:18.325417', 'call_f2d5f07a', '2026-02-10 17:23:06.060361', 'ENDED', 'VOICE', '2026-02-10 17:23:18.329140', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xa4866aaf832d46e08da56e606fe38c6a, '2026-02-10 18:23:29.353901', 148, '2026-02-10 18:26:02.428075', 'call_c34d0e18', '2026-02-10 18:23:33.629991', 'ENDED', 'VIDEO', '2026-02-10 18:26:02.433584', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xa73591f651d1401dbb7aeb70a942e7af, '2026-02-11 12:35:12.359804', 80, '2026-02-11 12:36:36.297622', 'call_82bb107f', '2026-02-11 12:35:15.999319', 'ENDED', 'VIDEO', '2026-02-11 12:36:36.302936', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xa8557db821e741208c357fbaa5abccc7, '2026-02-10 16:26:03.428261', 174, '2026-02-10 16:29:11.992410', 'call_b7f89e64', '2026-02-10 16:26:17.093447', 'ENDED', 'VOICE', '2026-02-10 16:29:11.998778', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xa9c3aef1e87542ba8878a11d44811038, '2026-02-11 01:52:23.496588', 17, '2026-02-11 01:52:47.963891', 'call_874e90e2', '2026-02-11 01:52:30.448455', 'ENDED', 'VOICE', '2026-02-11 01:52:47.970783', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0xf97655b1d77642efbd9514045a4e7270),
(0xae1c103519de4e8b9386662b99f9d9b6, '2026-02-10 05:37:39.508551', NULL, NULL, 'call_7838cf7a', '2026-02-10 05:37:43.058696', 'ONGOING', 'VOICE', '2026-02-10 05:37:43.062170', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2985ef03dcca48c79358408a4f589d0b),
(0xb39a82f73643408f8955da94d1b03f59, '2026-02-10 03:42:17.424020', NULL, '2026-02-10 05:21:04.518934', 'call_825e0288', NULL, 'REJECTED', 'VOICE', '2026-02-10 05:21:04.522587', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xba752563e7b84baea08f551606e877fb, '2026-02-11 02:52:01.488226', NULL, NULL, 'call_6556f5e2', '2026-02-11 02:52:09.269723', 'ONGOING', 'VIDEO', '2026-02-11 02:52:09.277795', 0x786190a6cc07406480c9318e630b693f, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xbafa19f9f9744a6dae630d022f6b5c8f, '2026-02-10 17:17:30.972943', 10, '2026-02-10 17:17:45.549478', 'call_5bb5c7ab', '2026-02-10 17:17:34.700140', 'ENDED', 'VOICE', '2026-02-10 17:17:45.555336', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xc19981e69ce6449dadf8be831fa4ffd8, '2026-02-10 19:16:13.794391', 66, '2026-02-10 19:17:25.103820', 'call_7db605dc', '2026-02-10 19:16:18.644919', 'ENDED', 'VIDEO', '2026-02-10 19:17:25.110658', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xc349f702156c486180a06d41a191ede2, '2026-02-10 19:18:00.713672', 104, '2026-02-10 19:19:47.874207', 'call_15aa3dac', '2026-02-10 19:18:03.246301', 'ENDED', 'VIDEO', '2026-02-10 19:19:47.879057', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xc6e5c1a0f59a452aa9712f083188cb14, '2026-02-10 03:42:41.717840', NULL, '2026-02-10 05:21:07.010117', 'call_63f488bc', NULL, 'REJECTED', 'VOICE', '2026-02-10 05:21:07.013609', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xc96956f9eaf74de7b54694d605091dd7, '2026-02-10 14:19:42.275044', 31, '2026-02-10 14:20:17.013932', 'call_cf827c2d', '2026-02-10 14:19:45.889067', 'ENDED', 'VOICE', '2026-02-10 14:20:17.019256', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xcb67f5c044e14820ad4b6f24c838bb65, '2026-02-10 03:41:34.336007', NULL, '2026-02-10 05:21:09.016052', 'call_fafa9f4a', NULL, 'REJECTED', 'VIDEO', '2026-02-10 05:21:09.021897', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xcb9b319387264c8c80c2bd13dd8c9fd4, '2026-02-10 16:23:46.500046', 56, '2026-02-10 16:24:47.836506', 'call_d75ef012', '2026-02-10 16:23:51.149885', 'ENDED', 'VIDEO', '2026-02-10 16:24:47.841252', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xcdba222ca39b425f9211241221fd2773, '2026-02-10 05:12:50.113645', NULL, NULL, 'call_558428e7', '2026-02-10 05:21:24.073435', 'ONGOING', 'VOICE', '2026-02-10 05:21:24.079362', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xd2e3c8dde8c342648d93bd2ed8608b43, '2026-02-11 12:38:48.377314', NULL, '2026-02-11 12:38:53.110389', 'call_1127b8a1', NULL, 'ENDED', 'VOICE', '2026-02-11 12:38:53.112896', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xd5ac7e1bddf545899568e2ffe3050d08, '2026-02-10 03:48:33.998960', NULL, NULL, 'call_b8b5e32f', '2026-02-10 05:21:25.537133', 'ONGOING', 'VOICE', '2026-02-10 05:21:25.541774', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xd7a44c3197464ad18ff402c07be14544, '2026-02-10 19:11:02.788673', NULL, '2026-02-10 19:11:05.460757', 'call_f8995779', NULL, 'ENDED', 'VOICE', '2026-02-10 19:11:05.463467', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xd8ef3c47a9384f7c87a71914ee324b6a, '2026-02-10 19:11:07.605978', 22, '2026-02-10 19:11:35.787603', 'call_447a226f', '2026-02-10 19:11:13.738662', 'ENDED', 'VIDEO', '2026-02-10 19:11:35.791741', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xd9d39dd6a9344f0fb9606459e88ddc81, '2026-02-10 17:16:44.716092', 34, '2026-02-10 17:17:22.405952', 'call_e2e21aeb', '2026-02-10 17:16:47.721078', 'ENDED', 'VOICE', '2026-02-10 17:17:22.411210', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xe3f3de4290b74aa998456c58c08c472c, '2026-02-10 03:49:00.975810', NULL, '2026-02-10 05:21:32.355448', 'call_02d815e9', NULL, 'REJECTED', 'VIDEO', '2026-02-10 05:21:32.359209', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xe79dd5d7eff3479c9fd881fc7308ff29, '2026-02-10 16:36:20.286177', 238, '2026-02-10 16:40:22.326305', 'call_c1300203', '2026-02-10 16:36:24.120685', 'ENDED', 'VIDEO', '2026-02-10 16:40:22.331930', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xf07443c2c70c414b84796c702afb93d4, '2026-02-11 02:15:45.489633', 32, '2026-02-11 02:16:26.414484', 'call_5674cedb', '2026-02-11 02:15:54.065651', 'ENDED', 'VIDEO', '2026-02-11 02:16:26.419908', 0x786190a6cc07406480c9318e630b693f, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xf125745010a3431b8bb82dd7c4d9d24a, '2026-02-10 17:18:23.123302', 36, '2026-02-10 17:19:03.327212', 'call_2cb8d4f9', '2026-02-10 17:18:26.919678', 'ENDED', 'VIDEO', '2026-02-10 17:19:03.334338', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xf21199044d144aeb8d0537f517457d68, '2026-02-10 16:22:27.218728', 71, '2026-02-10 16:23:42.382915', 'call_1fd32ece', '2026-02-10 16:22:31.292262', 'ENDED', 'VOICE', '2026-02-10 16:23:42.386359', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xf39dc009563c4abe8870fb5f98bc1dd7, '2026-02-11 12:39:06.120465', NULL, '2026-02-11 12:39:25.510445', 'call_b87ec764', NULL, 'ENDED', 'VOICE', '2026-02-11 12:39:25.516374', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xf57bf87a26b843a791ec6ea7f2a12a10, '2026-02-10 19:05:31.720880', NULL, '2026-02-10 19:05:41.983220', 'call_9a99c5b3', NULL, 'ENDED', 'VOICE', '2026-02-10 19:05:41.985083', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xf58bd5b0caa64a1cb7da0ff965249b1a, '2026-02-10 23:19:48.628979', 292, '2026-02-10 23:24:47.271522', 'call_91075b3c', '2026-02-10 23:19:54.808397', 'ENDED', 'VOICE', '2026-02-10 23:24:47.428501', 0x1a531d9d25a54266a87e10f0ae60e187, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xf81cbab670154934bf4bf8f3433ea586, '2026-02-10 05:08:03.431416', NULL, '2026-02-10 05:21:34.516048', 'call_74c2a936', NULL, 'REJECTED', 'VOICE', '2026-02-10 05:21:34.518926', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xfa5874bdba314e4da90da6262a2bf267, '2026-02-11 08:32:49.952535', NULL, '2026-02-11 08:33:09.796366', 'call_ad813bac', NULL, 'ENDED', 'VOICE', '2026-02-11 08:33:09.800345', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xfc0d182dd93d492da59a71fb10bbfcc1, '2026-02-10 17:22:07.890518', 15, '2026-02-10 17:22:25.060205', 'call_a83b2984', '2026-02-10 17:22:10.029265', 'ENDED', 'VOICE', '2026-02-10 17:22:25.066534', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xfcc03c8ce5454956881e8d3bb0b83856, '2026-02-10 16:44:31.370261', 205, '2026-02-10 16:48:11.084657', 'call_2208ff85', '2026-02-10 16:44:45.988671', 'ENDED', 'VIDEO', '2026-02-10 16:48:11.090778', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585);

-- --------------------------------------------------------

--
-- Table structure for table `carts`
--

CREATE TABLE `carts` (
  `id` binary(16) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `cart_items`
--

CREATE TABLE `cart_items` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `quantity` int NOT NULL,
  `cart_id` binary(16) NOT NULL,
  `product_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `coin_packages`
--

CREATE TABLE `coin_packages` (
  `id` binary(16) NOT NULL,
  `bonus_coins` int DEFAULT NULL,
  `coin_amount` int NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `icon_url` varchar(255) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `is_popular` bit(1) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `sort_order` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `coin_packages`
--

INSERT INTO `coin_packages` (`id`, `bonus_coins`, `coin_amount`, `description`, `icon_url`, `is_active`, `is_popular`, `name`, `price`, `sort_order`) VALUES
(0x4a73b2ebfa9a49c481e08cf6fb74d41c, 100, 500, 'For the big spenders', NULL, b'1', b'0', 'Whale Pack', 15000.00, 3),
(0x8cd6d242fab74a41a9dc8145159baf53, 20, 150, 'Better value for money', NULL, b'1', b'1', 'Value Pack', 5000.00, 2),
(0xe6301840ce964137b65f9ab4a0ef7ca7, 0, 50, 'Get started with 50 coins', NULL, b'1', b'0', 'Starter Pack', 2000.00, 1);

-- --------------------------------------------------------

--
-- Table structure for table `comments`
--

CREATE TABLE `comments` (
  `id` binary(16) NOT NULL,
  `content` text NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  `likes_count` int DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `author_id` binary(16) NOT NULL,
  `parent_id` binary(16) DEFAULT NULL,
  `post_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `comments`
--

INSERT INTO `comments` (`id`, `content`, `created_at`, `is_deleted`, `likes_count`, `updated_at`, `author_id`, `parent_id`, `post_id`) VALUES
(0x059d007bdbe04f0081232afbad0deb2c, '😢😢😢😢', '2026-02-11 08:32:22.285618', b'0', 0, '2026-02-11 08:32:22.285661', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, 0xba39e4f8a5814ba5bd25566d4a46b011),
(0x2694eab738774a068b64eaa26b78f584, 'Haha', '2026-02-09 15:43:04.576633', b'0', 0, '2026-02-09 15:43:04.576676', 0x786190a6cc07406480c9318e630b693f, NULL, 0x5ca6683d51b94dc084fdd22a5f49a4f0),
(0x2d73cd34a6ff4118bfc66323a2cb2df3, 'nan', '2026-02-10 04:22:52.702250', b'0', 0, '2026-02-10 04:22:52.702303', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x8c88efedfd3045dc8758714337942f8c, 0x3fb24b08d68547f3987ce3eec7502153),
(0x5d5a06522ad94deaae715d9349d97c77, 'kwel man', '2026-02-09 06:50:30.999330', b'0', 0, '2026-02-09 06:50:30.999364', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, 0xa55e69f9711e49c4a3c2f738057593dd),
(0x5f4ad3b249744f228500d9c8dc9b34c2, 'Vzr 👍', '2026-02-09 22:37:38.537427', b'0', 0, '2026-02-09 22:37:38.537459', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, 0x771a8b4087094a5f985c471c0e3109f3),
(0x6ede609f48204c349010f98b1acf7af8, '😁😁😁😁', '2026-02-11 08:31:06.533857', b'0', 0, '2026-02-11 08:31:06.533888', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, 0x7662961193ca4c3e8439c9448ed9dbec),
(0x75552f6e8d964369aa786af0d4f0464c, 'Mambo unaendela je', '2026-02-09 20:37:28.402372', b'0', 0, '2026-02-09 20:37:28.402407', 0x1a531d9d25a54266a87e10f0ae60e187, NULL, 0x34b75a1485f0454ab6c196beb1e3f85d),
(0x7efa8a11edcb4c10ad7aaf28eed21437, 'pop', '2026-02-11 01:24:26.978323', b'0', 0, '2026-02-11 01:24:26.978370', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, 0x2d9e09b17e284bfaa8e73c95ad8c96b4),
(0x8c88efedfd3045dc8758714337942f8c, 'po', '2026-02-10 04:22:41.284886', b'0', 0, '2026-02-10 04:22:41.284933', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, 0x3fb24b08d68547f3987ce3eec7502153),
(0x925d373788124f7ab2e1eb4361cc8b7c, 'Popsmoke', '2026-02-10 00:13:57.938267', b'0', 0, '2026-02-10 00:13:57.938286', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, 0x771a8b4087094a5f985c471c0e3109f3),
(0x9af86197a3cb4a1094d5e831a3ac6999, 'haha hatar san', '2026-02-09 22:13:49.585071', b'0', 0, '2026-02-09 22:13:49.585137', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, 0xdcb0332044874f42a5ac575908054a0f),
(0x9de007e0af0b4d8381b098627d586c15, 'mambo vp', '2026-02-09 14:41:39.989158', b'0', 0, '2026-02-09 14:41:39.989188', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, 0x5eb37da3b59c438a87d78aa5096a9778),
(0x9fddcf6ad51b4f0cb815f1bc1aaea885, 'Good', '2026-02-10 00:18:55.027960', b'0', 0, '2026-02-10 00:18:55.028020', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, 0x771a8b4087094a5f985c471c0e3109f3),
(0xa7eb6149ef8345f39478debb64a45ef5, '💩💩', '2026-02-11 08:31:26.071051', b'0', 0, '2026-02-11 08:31:26.071074', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, 0x93bf9d65c25c4ce0a3fe362c11eb6e84),
(0xacbba5fa16734b42b78f4681ef6249dd, 'Pc', '2026-02-10 19:06:44.478631', b'0', 0, '2026-02-10 19:06:44.478681', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, 0x34b75a1485f0454ab6c196beb1e3f85d),
(0xb58cc38973fe4bfab7ad06c0a769caf5, 'mambo', '2026-02-09 20:37:06.083879', b'0', 0, '2026-02-09 20:37:06.083907', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, 0x34b75a1485f0454ab6c196beb1e3f85d),
(0xd5786e12d9c74c228420320c79088f8b, '🥰', '2026-02-10 02:37:07.776034', b'0', 0, '2026-02-10 02:37:07.776147', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, 0x771a8b4087094a5f985c471c0e3109f3),
(0xdbbb96be33354231a80fa2f487454413, 'hahaha unajua sana kaka', '2026-02-09 06:43:02.962606', b'0', 0, '2026-02-09 06:43:02.962650', 0x786190a6cc07406480c9318e630b693f, NULL, 0xa55e69f9711e49c4a3c2f738057593dd),
(0xf6383ee726a44b83911db1ab2d31230a, 'hi', '2026-02-10 03:21:35.571592', b'0', 0, '2026-02-10 03:21:35.571658', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xd5786e12d9c74c228420320c79088f8b, 0x771a8b4087094a5f985c471c0e3109f3),
(0xf78d54889f89474cb1612420f07bb9ae, 'Hi bro', '2026-02-10 00:13:46.085286', b'0', 0, '2026-02-10 00:13:46.085376', 0x1a531d9d25a54266a87e10f0ae60e187, NULL, 0x771a8b4087094a5f985c471c0e3109f3),
(0xfe14f5d219ee4800bdad8e2d92004b10, 'Iuiuu', '2026-02-10 00:13:57.109806', b'0', 0, '2026-02-10 00:13:57.109829', 0x1a531d9d25a54266a87e10f0ae60e187, NULL, 0x771a8b4087094a5f985c471c0e3109f3);

-- --------------------------------------------------------

--
-- Table structure for table `comment_likes`
--

CREATE TABLE `comment_likes` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `comment_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `comment_likes`
--

INSERT INTO `comment_likes` (`id`, `created_at`, `comment_id`, `user_id`) VALUES
(0xd91fa698c470497f93b5c24642df8b2a, '2026-02-10 04:22:45.206185', 0x8c88efedfd3045dc8758714337942f8c, 0xe56e92e29d434c72b898cfd2a65f0b7e);

-- --------------------------------------------------------

--
-- Table structure for table `commissions`
--

CREATE TABLE `commissions` (
  `id` binary(16) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `paid_at` datetime(6) DEFAULT NULL,
  `status` enum('PENDING','APPROVED','PAID','CANCELLED') DEFAULT NULL,
  `type` enum('ACTIVATION','BUSINESS_ACTIVATION','SUBSCRIPTION_RENEWAL','REFERRAL') NOT NULL,
  `agent_id` binary(16) NOT NULL,
  `business_id` binary(16) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `communities`
--

CREATE TABLE `communities` (
  `id` binary(16) NOT NULL,
  `cover_image` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `members_count` int DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `privacy` enum('PUBLIC','FOLLOWERS','PRIVATE') NOT NULL,
  `type` enum('GROUP','CHANNEL') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `creator_id` binary(16) NOT NULL,
  `allow_member_posts` tinyint(1) NOT NULL DEFAULT '1'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `communities`
--

INSERT INTO `communities` (`id`, `cover_image`, `created_at`, `description`, `members_count`, `name`, `privacy`, `type`, `updated_at`, `creator_id`, `allow_member_posts`) VALUES
(0x1a25329af10845009fd6410c97ae188a, 'https://storage.wakilfy.com/communities/83a65e5f-d310-4b8e-87bf-a1d4e8a91462.png', '2026-02-09 20:56:16.720550', 'for devos', 4, 'watubaki', 'PUBLIC', 'GROUP', '2026-02-10 19:07:28.028003', 0xe56e92e29d434c72b898cfd2a65f0b7e, 1);

-- --------------------------------------------------------

--
-- Table structure for table `community_events`
--

CREATE TABLE `community_events` (
  `id` binary(16) NOT NULL,
  `community_id` binary(16) NOT NULL,
  `creator_id` binary(16) NOT NULL,
  `title` varchar(300) NOT NULL,
  `description` text,
  `location` varchar(500) DEFAULT NULL,
  `start_time` datetime(6) NOT NULL,
  `end_time` datetime(6) DEFAULT NULL,
  `cover_image` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `community_invites`
--

CREATE TABLE `community_invites` (
  `id` binary(16) NOT NULL,
  `community_id` binary(16) NOT NULL,
  `inviter_id` binary(16) NOT NULL,
  `invitee_id` binary(16) NOT NULL,
  `status` enum('PENDING','ACCEPTED','DECLINED') NOT NULL,
  `created_at` datetime(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `community_members`
--

CREATE TABLE `community_members` (
  `id` binary(16) NOT NULL,
  `is_banned` bit(1) DEFAULT NULL,
  `joined_at` datetime(6) DEFAULT NULL,
  `role` enum('ADMIN','MODERATOR','MEMBER') NOT NULL,
  `community_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `community_members`
--

INSERT INTO `community_members` (`id`, `is_banned`, `joined_at`, `role`, `community_id`, `user_id`) VALUES
(0x00f8373277244d7a94ba475eadf33021, b'0', '2026-02-09 22:19:14.149488', 'MEMBER', 0x1a25329af10845009fd6410c97ae188a, 0x786190a6cc07406480c9318e630b693f),
(0xb12455fcffa94955bf2ec08d1bd8ebdc, b'0', '2026-02-10 00:13:05.659912', 'MEMBER', 0x1a25329af10845009fd6410c97ae188a, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xbabedc05ebd748ba9681e5a42877035c, b'0', '2026-02-10 00:18:44.997008', 'MEMBER', 0x1a25329af10845009fd6410c97ae188a, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xc3b69cefb82e43ae825865bd8a7a8f66, b'0', '2026-02-09 20:56:16.726305', 'ADMIN', 0x1a25329af10845009fd6410c97ae188a, 0xe56e92e29d434c72b898cfd2a65f0b7e);

-- --------------------------------------------------------

--
-- Table structure for table `community_polls`
--

CREATE TABLE `community_polls` (
  `id` binary(16) NOT NULL,
  `community_id` binary(16) NOT NULL,
  `creator_id` binary(16) NOT NULL,
  `question` varchar(500) NOT NULL,
  `ends_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `community_poll_options`
--

CREATE TABLE `community_poll_options` (
  `id` binary(16) NOT NULL,
  `poll_id` binary(16) NOT NULL,
  `text` varchar(300) NOT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `votes_count` int DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `community_poll_votes`
--

CREATE TABLE `community_poll_votes` (
  `id` binary(16) NOT NULL,
  `poll_id` binary(16) NOT NULL,
  `option_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `conversations`
--

CREATE TABLE `conversations` (
  `id` binary(16) NOT NULL,
  `participant_one_id` binary(16) NOT NULL,
  `participant_two_id` binary(16) NOT NULL,
  `last_message_at` datetime(6) DEFAULT NULL,
  `last_message_preview` varchar(255) DEFAULT NULL,
  `is_buyer_seller_chat` bit(1) DEFAULT b'0',
  `product_id` binary(16) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `favorites`
--

CREATE TABLE `favorites` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `product_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `follows`
--

CREATE TABLE `follows` (
  `following_id` binary(16) NOT NULL,
  `follower_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `follows`
--

INSERT INTO `follows` (`following_id`, `follower_id`) VALUES
(0x2985ef03dcca48c79358408a4f589d0b, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x6815ee2a0b9a434c84ccf6e17c8973bc, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x7483d43b42bc432dacc7262e3c270902, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x786190a6cc07406480c9318e630b693f, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xe56e92e29d434c72b898cfd2a65f0b7e, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xf97655b1d77642efbd9514045a4e7270, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x1a531d9d25a54266a87e10f0ae60e187, 0x2985ef03dcca48c79358408a4f589d0b),
(0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x2985ef03dcca48c79358408a4f589d0b, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x6815ee2a0b9a434c84ccf6e17c8973bc, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x6e97e12bec3346a59d5853cda888ca4d, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x7483d43b42bc432dacc7262e3c270902, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x786190a6cc07406480c9318e630b693f, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x846276afb83244f8bcd78bbd67b5eba7, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xf97655b1d77642efbd9514045a4e7270, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x1a531d9d25a54266a87e10f0ae60e187, 0x6e97e12bec3346a59d5853cda888ca4d),
(0x2cecbeb064364e24ab4c525f9d7e5585, 0x6e97e12bec3346a59d5853cda888ca4d),
(0xf97655b1d77642efbd9514045a4e7270, 0x6e97e12bec3346a59d5853cda888ca4d),
(0xe56e92e29d434c72b898cfd2a65f0b7e, 0x786190a6cc07406480c9318e630b693f),
(0x1a531d9d25a54266a87e10f0ae60e187, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x2985ef03dcca48c79358408a4f589d0b, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x6815ee2a0b9a434c84ccf6e17c8973bc, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x7483d43b42bc432dacc7262e3c270902, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x786190a6cc07406480c9318e630b693f, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x846276afb83244f8bcd78bbd67b5eba7, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xf97655b1d77642efbd9514045a4e7270, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x1a531d9d25a54266a87e10f0ae60e187, 0xf97655b1d77642efbd9514045a4e7270);

-- --------------------------------------------------------

--
-- Table structure for table `friendships`
--

CREATE TABLE `friendships` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `status` enum('PENDING','ACCEPTED','DECLINED','BLOCKED') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `addressee_id` binary(16) NOT NULL,
  `requester_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `gift_transactions`
--

CREATE TABLE `gift_transactions` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `message` varchar(255) DEFAULT NULL,
  `quantity` int NOT NULL,
  `total_value` decimal(12,2) NOT NULL,
  `gift_id` binary(16) NOT NULL,
  `live_stream_id` binary(16) DEFAULT NULL,
  `receiver_id` binary(16) NOT NULL,
  `sender_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `hashtags`
--

CREATE TABLE `hashtags` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `name` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `live_streams`
--

CREATE TABLE `live_streams` (
  `id` binary(16) NOT NULL,
  `comments_count` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `duration_seconds` int DEFAULT NULL,
  `ended_at` datetime(6) DEFAULT NULL,
  `likes_count` int DEFAULT NULL,
  `peak_viewers` int DEFAULT NULL,
  `room_id` varchar(255) DEFAULT NULL,
  `scheduled_at` datetime(6) DEFAULT NULL,
  `started_at` datetime(6) DEFAULT NULL,
  `status` enum('SCHEDULED','LIVE','ENDED','CANCELLED') NOT NULL,
  `stream_key` varchar(255) DEFAULT NULL,
  `thumbnail_url` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `total_gifts_value` decimal(15,2) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `viewer_count` int DEFAULT NULL,
  `host_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `live_streams`
--

INSERT INTO `live_streams` (`id`, `comments_count`, `created_at`, `description`, `duration_seconds`, `ended_at`, `likes_count`, `peak_viewers`, `room_id`, `scheduled_at`, `started_at`, `status`, `stream_key`, `thumbnail_url`, `title`, `total_gifts_value`, `updated_at`, `viewer_count`, `host_id`) VALUES
(0x0c7c6ab1dcad4feabb6dc47749872421, 0, '2026-02-10 18:44:20.049471', 'football', 1657, '2026-02-10 19:11:57.563744', 1, 2, 'live_692afadd-4ee', NULL, '2026-02-10 18:44:20.022296', 'ENDED', '6ff3c9d324b8466c99c1a3b8eb78bd60', NULL, 'Video', 0.00, '2026-02-10 19:11:57.566332', 2, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x10e37c4b642145599a9f077b61ff43e4, 0, '2026-02-10 19:27:01.152217', 'hhh', 49, '2026-02-10 19:27:50.719666', 0, 1, 'live_963593ba-ebb', NULL, '2026-02-10 19:27:01.150619', 'ENDED', '537f9b43223447909b0976c6b02ac9e9', NULL, 'hhh', 0.00, '2026-02-10 19:27:50.723516', 0, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x26936690b5ef4ab8a1222a72a837406b, 0, '2026-02-10 18:28:25.090020', 'football', 935, '2026-02-10 18:44:00.760879', 2, 1, 'live_c572325c-22d', NULL, '2026-02-10 18:28:25.058656', 'ENDED', 'f7d54d108c3047b89cc9c5002c8f2f1c', NULL, 'Mama', 0.00, '2026-02-10 18:44:00.762051', 1, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x46a2130f01a04eff91b8897c12f8de3e, 0, '2026-02-10 19:12:10.973646', 'fffff', 214, '2026-02-10 19:15:45.494041', 0, 1, 'live_23daa8a5-ac7', NULL, '2026-02-10 19:12:10.969409', 'ENDED', 'd33edc82799d48cbaf8f910bc4dd86fd', NULL, 'ffff', 0.00, '2026-02-10 19:15:45.498170', 0, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x89b29cc0407b402ba55f5b4674920972, 0, '2026-02-10 19:29:37.373955', 'hhh', 674, '2026-02-10 19:40:52.285443', 2, 3, 'live_e9fcb138-ea4', NULL, '2026-02-10 19:29:37.312500', 'ENDED', '452bb9a6f108488f850a027eece6b91a', NULL, 'hhh', 0.00, '2026-02-10 19:40:52.287324', 3, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x8c61a705e6ef4d068816551502955e8d, 0, '2026-02-11 02:38:35.917858', 'dddddddddddd', 981, '2026-02-11 02:54:57.147654', 0, 1, 'live_18787ec1-ca6', NULL, '2026-02-11 02:38:35.901539', 'ENDED', '4e069dc01e49492181e589e3ba0cdfd2', NULL, 'pop', 0.00, '2026-02-11 02:54:57.150754', 0, 0x786190a6cc07406480c9318e630b693f),
(0xb34bd8a9949a44dca9bb9dc2fb70c991, 0, '2026-02-10 19:24:03.604206', 'kkkk', 162, '2026-02-10 19:26:45.943538', 0, 1, 'live_fb42b8aa-33c', NULL, '2026-02-10 19:24:03.602770', 'ENDED', '4cbd2c53c3bb4fcf9664f6b555882339', NULL, 'kkk', 0.00, '2026-02-10 19:26:45.946424', 0, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xb94f048992f7402aa26b99f65a224d5b, 0, '2026-02-10 19:22:24.147077', 'Coding vibe', 80, '2026-02-10 19:23:44.199995', 1, 1, 'live_180b6bf4-de1', NULL, '2026-02-10 19:22:24.115628', 'ENDED', 'ea42202f8a6e45a9955dd884f3a8bf4a', NULL, 'ERICKsky', 0.00, '2026-02-10 19:23:44.203092', 1, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xc4f3bfd96a7e433184d03b1272427bb5, 0, '2026-02-11 12:36:47.063155', 'Uje', NULL, NULL, 8, 2, 'live_e8bb5a4f-a6a', NULL, '2026-02-11 12:36:47.044525', 'LIVE', 'bea05738ea8b42deb4d84f3f448302a5', NULL, 'Poo', 0.00, '2026-02-11 18:27:50.151394', 1, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xf59acfcfac524d4f9d89f8ab80365d22, 0, '2026-02-10 18:04:28.059736', 'juuma', 119, '2026-02-10 18:06:27.899761', 1, 2, 'live_fd5c678d-cde', NULL, '2026-02-10 18:04:28.032439', 'ENDED', 'c0e8302975954c47bbc8b1d325e1a45d', NULL, 'erick', 0.00, '2026-02-10 18:06:27.903336', 1, 0x1a531d9d25a54266a87e10f0ae60e187);

-- --------------------------------------------------------

--
-- Table structure for table `live_stream_comments`
--

CREATE TABLE `live_stream_comments` (
  `id` binary(16) NOT NULL,
  `live_stream_id` binary(16) NOT NULL,
  `author_id` binary(16) NOT NULL,
  `content` varchar(500) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `live_stream_join_requests`
--

CREATE TABLE `live_stream_join_requests` (
  `id` binary(16) NOT NULL,
  `live_stream_id` binary(16) NOT NULL,
  `requester_id` binary(16) NOT NULL,
  `status` enum('PENDING','ACCEPTED','REJECTED') NOT NULL,
  `host_responded_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `guest_stream_key` varchar(255) DEFAULT NULL,
  UNIQUE KEY `UK_guest_stream_key` (`guest_stream_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `live_stream_join_requests`
--

INSERT INTO `live_stream_join_requests` (`id`, `live_stream_id`, `requester_id`, `status`, `host_responded_at`, `created_at`) VALUES
(0x43632c66f4fa459ebd0a4e2c5a65ba8f, 0xc4f3bfd96a7e433184d03b1272427bb5, 0x1a531d9d25a54266a87e10f0ae60e187, 'PENDING', NULL, '2026-02-11 14:42:25.700266'),
(0xa7a77a6ce1114347a142caf08553de72, 0x89b29cc0407b402ba55f5b4674920972, 0xe56e92e29d434c72b898cfd2a65f0b7e, 'ACCEPTED', '2026-02-10 19:39:22.740985', '2026-02-10 19:39:10.747300'),
(0xab783028641e447e899a5f144e8c303c, 0xb94f048992f7402aa26b99f65a224d5b, 0x1a531d9d25a54266a87e10f0ae60e187, 'PENDING', NULL, '2026-02-10 19:23:29.374935');

-- --------------------------------------------------------

--
-- Table structure for table `messages`
--

CREATE TABLE `messages` (
  `id` binary(16) NOT NULL,
  `content` text NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_read` bit(1) DEFAULT NULL,
  `media_url` varchar(255) DEFAULT NULL,
  `recipient_id` binary(16) NOT NULL,
  `sender_id` binary(16) NOT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `type` enum('TEXT','IMAGE','VIDEO','VOICE','DOCUMENT','PRODUCT_LINK','ORDER_REQUEST') DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT b'0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `messages`
--

INSERT INTO `messages` (`id`, `content`, `created_at`, `is_read`, `media_url`, `recipient_id`, `sender_id`, `read_at`, `type`, `is_deleted`) VALUES
(0x039d01ff22e943fbae6462c8e5d036e7, 'hi', '2026-02-11 08:41:44.741808', b'1', NULL, 0x2cecbeb064364e24ab4c525f9d7e5585, 0x6e97e12bec3346a59d5853cda888ca4d, NULL, 'TEXT', b'0'),
(0x1203664f4e7345689aadbbcba4f01b42, 'Profile unyama umetisha✅ Reels na post yake na ui ✅', '2026-02-11 08:32:44.071716', b'1', NULL, 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, 'TEXT', b'0'),
(0x39bee027cc2e420a8e39f1c0e1fb7ea2, 'saf vp apo', '2026-02-10 16:22:25.511857', b'1', NULL, 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, 'TEXT', b'0'),
(0x4063c62bf6c041beb66e7d13af944900, 'vp', '2026-02-10 16:22:12.429961', b'1', NULL, 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, 'TEXT', b'0'),
(0x59dcec1ca3c54e1ca9d00f182a7d4d7f, 'vp', '2026-02-11 06:14:46.646713', b'1', NULL, 0xf97655b1d77642efbd9514045a4e7270, 0x1a531d9d25a54266a87e10f0ae60e187, NULL, 'TEXT', b'0'),
(0x5e2c834d7c1744199a45829b3c611e28, 'good', '2026-02-10 03:43:21.600892', b'1', NULL, 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, 'TEXT', b'0'),
(0x7843bd84dff24c9ebb4b43f766eb7010, 'Mamb', '2026-02-10 03:41:28.784570', b'0', NULL, 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187, NULL, 'TEXT', b'0'),
(0x961032df987c47a9b5e8284414887735, 'Salama', '2026-02-10 03:43:38.629410', b'0', NULL, 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187, NULL, 'TEXT', b'0'),
(0xa3cfa164b74842cf870fced712b1d069, 'oui', '2026-02-11 01:50:02.994238', b'1', NULL, 0xf97655b1d77642efbd9514045a4e7270, 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, 'TEXT', b'0'),
(0xa52be73e46b64d9ea1a3413d534ecba7, 'ghhsss', '2026-02-11 08:41:57.431308', b'1', NULL, 0x2cecbeb064364e24ab4c525f9d7e5585, 0x6e97e12bec3346a59d5853cda888ca4d, NULL, 'TEXT', b'0'),
(0xab61124c253441569913c4754ecc2592, 'hi', '2026-02-11 02:03:25.026704', b'1', NULL, 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x786190a6cc07406480c9318e630b693f, NULL, 'TEXT', b'0'),
(0xacb7773d37124eee9afb0c880ce815e4, 'vp', '2026-02-10 05:31:50.576091', b'1', NULL, 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, 'TEXT', b'0'),
(0xbe85b7927eba4d999c6922ce7ac869d4, 'Uhakik', '2026-02-11 09:52:25.376066', b'1', NULL, 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, 'TEXT', b'0'),
(0xe14295132dcc4b34b7dd92bedf1b1309, 'Unym san', '2026-02-10 18:48:00.186352', b'1', NULL, 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, 'TEXT', b'0');

-- --------------------------------------------------------

--
-- Table structure for table `notifications`
--

CREATE TABLE `notifications` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `entity_id` binary(16) DEFAULT NULL,
  `is_read` bit(1) DEFAULT NULL,
  `message` varchar(255) NOT NULL,
  `type` enum('LIKE','COMMENT','SHARE','FRIEND_REQUEST','FRIEND_ACCEPT','FOLLOW','SYSTEM') NOT NULL,
  `actor_id` binary(16) DEFAULT NULL,
  `recipient_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `notifications`
--

INSERT INTO `notifications` (`id`, `created_at`, `entity_id`, `is_read`, `message`, `type`, `actor_id`, `recipient_id`) VALUES
(0x048885fcaa9d460e84997e578606b049, '2026-02-11 08:31:26.073593', 0x93bf9d65c25c4ce0a3fe362c11eb6e84, b'0', 'Erick salehe commented on your post', 'COMMENT', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x04f7f92fdf054a599a3a631909297784, '2026-02-10 12:14:44.036918', 0x609a9f5caab54b63b72bc7a13852bb6d, b'0', 'Enjo Elvin liked your post', 'LIKE', 0x2985ef03dcca48c79358408a4f589d0b, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x0cad521db0c74c13bb1f600247d719b0, '2026-02-11 01:43:01.588392', 0xf97655b1d77642efbd9514045a4e7270, b'1', 'IBRAHIM Z MFINANGA started following you', 'FOLLOW', 0xf97655b1d77642efbd9514045a4e7270, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x0d873b592f1a443389959d1f12d970f5, '2026-02-11 18:52:57.935437', 0x93bf9d65c25c4ce0a3fe362c11eb6e84, b'0', 'Ezekiel Salehe liked your post', 'LIKE', 0x1a531d9d25a54266a87e10f0ae60e187, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x0ef2ad874a234b3598cf871eb07df0f2, '2026-02-09 06:24:01.117330', 0xa55e69f9711e49c4a3c2f738057593dd, b'1', 'salim ashiraf liked your post', 'LIKE', 0x786190a6cc07406480c9318e630b693f, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x105af13fd8144f27b11975783810af08, '2026-02-10 05:39:52.496367', 0x1f0e3831eb004dec968a5eacf6fd9dc6, b'0', 'Ezekiel Salehe liked your post', 'LIKE', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2985ef03dcca48c79358408a4f589d0b),
(0x10caee63cbc24396b2d88d8348f59f30, '2026-02-10 05:37:14.532356', 0x1a531d9d25a54266a87e10f0ae60e187, b'0', 'Ezekiel Salehe started following you', 'FOLLOW', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2985ef03dcca48c79358408a4f589d0b),
(0x161868141880484990e9fe5b9c3f3ded, '2026-02-10 12:14:43.434505', 0x75c5bbadabd14229a8cdceb6ed321a55, b'0', 'Enjo Elvin liked your post', 'LIKE', 0x2985ef03dcca48c79358408a4f589d0b, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x18252609d64e4483a25bab4959a2ec3f, '2026-02-09 19:32:10.454635', 0xe56e92e29d434c72b898cfd2a65f0b7e, b'0', 'ibrahim ashiraf started following you', 'FOLLOW', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x1b55e590fc304b85a28d030c05444242, '2026-02-10 04:22:52.707752', 0x3fb24b08d68547f3987ce3eec7502153, b'0', 'ibrahim ashiraf commented on your post', 'COMMENT', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x1c41c3ca34b84ff783988002594e9216, '2026-02-10 12:14:44.656062', 0x14b2d1d1eedc4b5483d49d5bf99fae2b, b'0', 'Enjo Elvin liked your post', 'LIKE', 0x2985ef03dcca48c79358408a4f589d0b, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x1f26a3be31e64e8e94bab3d1c44c33b0, '2026-02-10 04:22:41.293988', 0x3fb24b08d68547f3987ce3eec7502153, b'0', 'ibrahim ashiraf commented on your post', 'COMMENT', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x21b34576765344d5b215985bdef43b09, '2026-02-11 08:31:16.289224', 0x93bf9d65c25c4ce0a3fe362c11eb6e84, b'0', 'Erick salehe liked your post', 'LIKE', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x255d2ac3e10141d39aee786b51899622, '2026-02-09 14:23:12.948784', 0xd670703817c24cbc975ae683f45988a2, b'1', 'Erick salehe liked your post', 'LIKE', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x786190a6cc07406480c9318e630b693f),
(0x29cefe8f8ee345dcaffb2102ef01e940, '2026-02-09 13:03:35.082661', 0xa55e69f9711e49c4a3c2f738057593dd, b'1', 'Erick salehe liked your post', 'LIKE', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x2bfa330a1ddd4946bd2592cef4371d40, '2026-02-11 02:26:11.758860', 0x93bf9d65c25c4ce0a3fe362c11eb6e84, b'0', 'salim ashiraf liked your post', 'LIKE', 0x786190a6cc07406480c9318e630b693f, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x2eaa1b82fb2a4befaa74e1bbc7ddb956, '2026-02-10 00:18:36.528771', 0x609a9f5caab54b63b72bc7a13852bb6d, b'0', 'Erick salehe liked your post', 'LIKE', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x32afb018c0324fcd81167a8dd3f95fbb, '2026-02-10 03:21:35.603492', 0x771a8b4087094a5f985c471c0e3109f3, b'1', 'Erick salehe commented on your post', 'COMMENT', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x35a45c629e7d46d8ad32a95b6c644d0e, '2026-02-10 00:13:57.112915', 0x771a8b4087094a5f985c471c0e3109f3, b'0', 'Ezekiel Salehe commented on your post', 'COMMENT', 0x1a531d9d25a54266a87e10f0ae60e187, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x3a4ff82dcbf545818e6c8a2caa47d7d7, '2026-02-10 23:05:54.820869', 0x2d9e09b17e284bfaa8e73c95ad8c96b4, b'0', 'Erick salehe liked your post', 'LIKE', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x3c9d859cc4b7451cbd15b53717f9043e, '2026-02-09 22:28:09.842994', 0xe56e92e29d434c72b898cfd2a65f0b7e, b'1', 'ibrahim ashiraf started following you', 'FOLLOW', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x786190a6cc07406480c9318e630b693f),
(0x3fef27c994954f96a5a6806a61ab620f, '2026-02-11 08:39:22.807883', 0x6e97e12bec3346a59d5853cda888ca4d, b'0', 'Justin John started following you', 'FOLLOW', 0x6e97e12bec3346a59d5853cda888ca4d, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x408b74e8ff1e4b4ca13072f2d6de0270, '2026-02-09 19:44:50.693705', 0x1a531d9d25a54266a87e10f0ae60e187, b'1', 'Ezekiel Salehe started following you', 'FOLLOW', 0x1a531d9d25a54266a87e10f0ae60e187, 0x786190a6cc07406480c9318e630b693f),
(0x4443940af7fd44618ae78096894a62e4, '2026-02-09 22:19:57.736236', 0xdcb0332044874f42a5ac575908054a0f, b'1', 'salim ashiraf liked your post', 'LIKE', 0x786190a6cc07406480c9318e630b693f, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x4471eb506e654c0f9790c198f58face0, '2026-02-10 16:21:59.808505', 0xe56e92e29d434c72b898cfd2a65f0b7e, b'0', 'ibrahim ashiraf started following you', 'FOLLOW', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2985ef03dcca48c79358408a4f589d0b),
(0x4743b361d1d3452394cc2d1503d02d4a, '2026-02-11 09:52:16.136967', 0xba39e4f8a5814ba5bd25566d4a46b011, b'0', 'ibrahim ashiraf liked your post', 'LIKE', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x48dcd2bf946e46d9b3ee6f84b6056381, '2026-02-09 13:03:37.665594', 0xc8f2f7462b864ad99dcdc029baf5f630, b'1', 'Erick salehe liked your post', 'LIKE', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x503332b43f67485c94b11c1e552ab7c5, '2026-02-11 00:10:04.962700', 0x1a531d9d25a54266a87e10f0ae60e187, b'0', 'Ezekiel Salehe started following you', 'FOLLOW', 0x1a531d9d25a54266a87e10f0ae60e187, 0x6815ee2a0b9a434c84ccf6e17c8973bc),
(0x5092ce2111f3439c974b14e02b3d3eeb, '2026-02-09 19:44:39.919601', 0x5ca6683d51b94dc084fdd22a5f49a4f0, b'0', 'Ezekiel Salehe liked your post', 'LIKE', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x54249de5b57a46bc969236db37dd8fa6, '2026-02-11 08:39:15.872574', 0x6e97e12bec3346a59d5853cda888ca4d, b'0', 'Justin John started following you', 'FOLLOW', 0x6e97e12bec3346a59d5853cda888ca4d, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x54fe1c3f09aa4e89a0cc8ea9256cc1ec, '2026-02-09 19:44:19.972460', 0x1a531d9d25a54266a87e10f0ae60e187, b'0', 'Ezekiel Salehe started following you', 'FOLLOW', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x5961dc15b8f649b3a985139ec319c9e6, '2026-02-11 01:43:57.023727', 0xe56e92e29d434c72b898cfd2a65f0b7e, b'1', 'ibrahim ashiraf started following you', 'FOLLOW', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0xf97655b1d77642efbd9514045a4e7270),
(0x61292af29bb0435fac2d23ab1c54c4b2, '2026-02-09 22:28:06.954808', 0xe56e92e29d434c72b898cfd2a65f0b7e, b'0', 'ibrahim ashiraf started following you', 'FOLLOW', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x6483ad772a4b4d9888802e572ea16baf, '2026-02-09 23:17:17.120240', 0x771a8b4087094a5f985c471c0e3109f3, b'0', 'Erick salehe liked your post', 'LIKE', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x6ad8ced3797a433389c1352ee22e616a, '2026-02-10 19:06:44.481822', 0x34b75a1485f0454ab6c196beb1e3f85d, b'0', 'ibrahim ashiraf commented on your post', 'COMMENT', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x6dba7f6b7cea4552a3313e8c5261d64f, '2026-02-10 05:36:36.627173', 0x2985ef03dcca48c79358408a4f589d0b, b'0', 'Enjo Elvin started following you', 'FOLLOW', 0x2985ef03dcca48c79358408a4f589d0b, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x76a394a813754e1594df8f465781fc76, '2026-02-10 00:13:46.094110', 0x771a8b4087094a5f985c471c0e3109f3, b'0', 'Ezekiel Salehe commented on your post', 'COMMENT', 0x1a531d9d25a54266a87e10f0ae60e187, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x78e8aed065ce42e68933fbae780883d6, '2026-02-09 18:59:29.355895', 0x2cecbeb064364e24ab4c525f9d7e5585, b'0', 'Erick salehe started following you', 'FOLLOW', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x6815ee2a0b9a434c84ccf6e17c8973bc),
(0x79f0e054e47c4ab9884237b0e6ae0c8d, '2026-02-10 00:13:03.208657', 0xb330c623920d49eebbfe82f2d919db9e, b'0', 'Ezekiel Salehe liked your post', 'LIKE', 0x1a531d9d25a54266a87e10f0ae60e187, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x7b2f6639fee746c2bd7d6527e373abe9, '2026-02-11 00:10:00.117976', 0x1a531d9d25a54266a87e10f0ae60e187, b'0', 'Ezekiel Salehe started following you', 'FOLLOW', 0x1a531d9d25a54266a87e10f0ae60e187, 0x7483d43b42bc432dacc7262e3c270902),
(0x7b87b35de2774f049ee3fb6347ec7b2c, '2026-02-11 06:18:11.877024', 0x1a531d9d25a54266a87e10f0ae60e187, b'0', 'Ezekiel Salehe started following you', 'FOLLOW', 0x1a531d9d25a54266a87e10f0ae60e187, 0xf97655b1d77642efbd9514045a4e7270),
(0x7ea51b150e6f4a06898e73595ba1f6b3, '2026-02-09 06:43:02.969556', 0xa55e69f9711e49c4a3c2f738057593dd, b'1', 'salim ashiraf commented on your post', 'COMMENT', 0x786190a6cc07406480c9318e630b693f, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x8bd165dbb3a44f3b98cfef047c896abd, '2026-02-11 06:59:39.443645', 0x778c5e32c8ef4d92acd6e2e65c33196c, b'0', 'Erick salehe liked your post', 'LIKE', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x8c93994e1809436a9cf2dd583df47b6c, '2026-02-11 00:09:03.400796', 0x93bf9d65c25c4ce0a3fe362c11eb6e84, b'0', 'Ezekiel Salehe liked your post', 'LIKE', 0x1a531d9d25a54266a87e10f0ae60e187, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x8dd7eff5ed4741638e6fc1373c396e3b, '2026-02-10 12:14:47.705346', 0x34b75a1485f0454ab6c196beb1e3f85d, b'0', 'Enjo Elvin liked your post', 'LIKE', 0x2985ef03dcca48c79358408a4f589d0b, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x987fd0b073cf47c19c028c636ed4fe87, '2026-02-09 19:44:42.011478', 0x5eb37da3b59c438a87d78aa5096a9778, b'0', 'Ezekiel Salehe liked your post', 'LIKE', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x98aca99e83854f7fbf3e78b5a25a9289, '2026-02-09 15:43:04.580860', 0x5ca6683d51b94dc084fdd22a5f49a4f0, b'0', 'salim ashiraf commented on your post', 'COMMENT', 0x786190a6cc07406480c9318e630b693f, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x994fc9eb94654c1a9a134d1633abd3a2, '2026-02-11 02:32:53.383354', 0x93bf9d65c25c4ce0a3fe362c11eb6e84, b'0', 'salim ashiraf liked your post', 'LIKE', 0x786190a6cc07406480c9318e630b693f, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x9bed95a1b8844952b9f107563851392a, '2026-02-10 01:16:40.266718', 0x34b75a1485f0454ab6c196beb1e3f85d, b'0', 'ibrahim ashiraf liked your post', 'LIKE', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xa7ba8d9a1021465f86ac398dd59d4caf, '2026-02-09 13:03:36.357715', 0xa55e69f9711e49c4a3c2f738057593dd, b'1', 'Erick salehe liked your post', 'LIKE', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xa88259f8352044f691cd086cd25b4734, '2026-02-09 18:59:33.709559', 0x2cecbeb064364e24ab4c525f9d7e5585, b'0', 'Erick salehe started following you', 'FOLLOW', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x846276afb83244f8bcd78bbd67b5eba7),
(0xaa57fb0111cf405590b4080e4c7f6f9f, '2026-02-11 00:14:34.812243', 0x771a8b4087094a5f985c471c0e3109f3, b'0', 'Ezekiel Salehe liked your post', 'LIKE', 0x1a531d9d25a54266a87e10f0ae60e187, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xab605d0009cc473580748acaa696f47e, '2026-02-10 00:16:10.186718', 0x8a218a947e694abb9b47dae84e3f6d32, b'0', 'Ezekiel Salehe liked your post', 'LIKE', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xad058a221da24b20b926f23e3da2e565, '2026-02-11 00:10:06.590078', 0x1a531d9d25a54266a87e10f0ae60e187, b'0', 'Ezekiel Salehe started following you', 'FOLLOW', 0x1a531d9d25a54266a87e10f0ae60e187, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xad54f600c0b0476496faa06e961879c8, '2026-02-09 19:44:58.265554', 0xd670703817c24cbc975ae683f45988a2, b'1', 'Ezekiel Salehe liked your post', 'LIKE', 0x1a531d9d25a54266a87e10f0ae60e187, 0x786190a6cc07406480c9318e630b693f),
(0xb23a141d9804442bb13b87851f2b55fa, '2026-02-10 00:13:02.312907', 0xdcb0332044874f42a5ac575908054a0f, b'0', 'Ezekiel Salehe liked your post', 'LIKE', 0x1a531d9d25a54266a87e10f0ae60e187, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xb2a5448d33d74e99828302b1e0952466, '2026-02-10 18:49:53.980301', 0xe56e92e29d434c72b898cfd2a65f0b7e, b'0', 'ibrahim ashiraf started following you', 'FOLLOW', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x7483d43b42bc432dacc7262e3c270902),
(0xb88eb9232ae74722a1467b736b506055, '2026-02-09 15:42:50.701698', 0x5ca6683d51b94dc084fdd22a5f49a4f0, b'0', 'salim ashiraf liked your post', 'LIKE', 0x786190a6cc07406480c9318e630b693f, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xbb6de7a5e104485798a86dd6a65f490a, '2026-02-09 06:25:31.786172', 0xc8f2f7462b864ad99dcdc029baf5f630, b'1', 'salim ashiraf liked your post', 'LIKE', 0x786190a6cc07406480c9318e630b693f, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xbd3a9450f5df476fae7c379851ffaa29, '2026-02-10 16:21:53.925229', 0xe56e92e29d434c72b898cfd2a65f0b7e, b'0', 'ibrahim ashiraf started following you', 'FOLLOW', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x6815ee2a0b9a434c84ccf6e17c8973bc),
(0xc01caa2d3fc1401bbb8a8d6e5cb077fc, '2026-02-10 02:54:18.056316', 0xf5e48bdbefea4a8a8cff86c2fd711c6c, b'0', 'Erick salehe liked your post', 'LIKE', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xc17cf1b09087449db49774f57cf8ec88, '2026-02-11 08:30:52.855130', 0x2cecbeb064364e24ab4c525f9d7e5585, b'0', 'Erick salehe started following you', 'FOLLOW', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xf97655b1d77642efbd9514045a4e7270),
(0xc197874b51604745a99bf4b83bdb4597, '2026-02-09 06:22:50.971171', 0xa55e69f9711e49c4a3c2f738057593dd, b'1', 'salim ashiraf liked your post', 'LIKE', 0x786190a6cc07406480c9318e630b693f, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xc416b7140a154668a8057ea682c19025, '2026-02-09 18:59:31.399013', 0x2cecbeb064364e24ab4c525f9d7e5585, b'0', 'Erick salehe started following you', 'FOLLOW', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x7483d43b42bc432dacc7262e3c270902),
(0xc519c406f74b491d881ff2c9371abdc0, '2026-02-09 20:36:57.157511', 0x2cecbeb064364e24ab4c525f9d7e5585, b'0', 'Erick salehe started following you', 'FOLLOW', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xc61f8095d174460c96dc11410a105f6e, '2026-02-10 19:06:35.571368', 0x34b75a1485f0454ab6c196beb1e3f85d, b'0', 'ibrahim ashiraf liked your post', 'LIKE', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xcf37542520824ec58914bc3ea79318c0, '2026-02-09 19:44:50.170451', 0x1a531d9d25a54266a87e10f0ae60e187, b'1', 'Ezekiel Salehe started following you', 'FOLLOW', 0x1a531d9d25a54266a87e10f0ae60e187, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xd5829f9537d944d48bd73968a93ce28e, '2026-02-09 18:59:27.516050', 0x2cecbeb064364e24ab4c525f9d7e5585, b'1', 'Erick salehe started following you', 'FOLLOW', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xd884e107751d40fcb282328db2e2c8d3, '2026-02-11 02:35:39.911970', 0x786190a6cc07406480c9318e630b693f, b'0', 'salim ashiraf started following you', 'FOLLOW', 0x786190a6cc07406480c9318e630b693f, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xdf27450de4d9424ba80660805ceac8ba, '2026-02-10 23:18:54.901608', 0x2d9e09b17e284bfaa8e73c95ad8c96b4, b'0', 'Ezekiel Salehe liked your post', 'LIKE', 0x1a531d9d25a54266a87e10f0ae60e187, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xe06a2da8bc9f4a6e95793bcf396f0aaf, '2026-02-09 18:59:28.493962', 0x2cecbeb064364e24ab4c525f9d7e5585, b'1', 'Erick salehe started following you', 'FOLLOW', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x786190a6cc07406480c9318e630b693f),
(0xe33c8f6717b84e68ae17e209ce5a82df, '2026-02-09 14:23:17.725449', 0xd670703817c24cbc975ae683f45988a2, b'1', 'Erick salehe liked your post', 'LIKE', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x786190a6cc07406480c9318e630b693f),
(0xe5de079a1b37466db7fe081b060cd421, '2026-02-09 15:56:07.168853', 0x5eb37da3b59c438a87d78aa5096a9778, b'0', 'ibrahim ashiraf liked your post', 'LIKE', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xe8d61e25e7bd485490785f07418e0933, '2026-02-09 20:37:06.087268', 0x34b75a1485f0454ab6c196beb1e3f85d, b'0', 'Erick salehe commented on your post', 'COMMENT', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xed8bcb1e24ef4e488f6c9e7040410759, '2026-02-11 08:20:04.530048', 0x2d9e09b17e284bfaa8e73c95ad8c96b4, b'0', 'IBRAHIM Z MFINANGA liked your post', 'LIKE', 0xf97655b1d77642efbd9514045a4e7270, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xee8678322e044af280420a9656c97b96, '2026-02-09 19:44:38.043275', 0x246d222ba96340c585586d9437e0be2a, b'0', 'Ezekiel Salehe liked your post', 'LIKE', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xeec2625e78a84317ab3a1176e8170200, '2026-02-09 19:44:43.226565', 0x574ca4da24fa44c49af728e68e7d118d, b'0', 'Ezekiel Salehe liked your post', 'LIKE', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xf0609d1b2dde4f8da058c2b7952382bb, '2026-02-10 22:58:39.150029', 0x3fb24b08d68547f3987ce3eec7502153, b'0', 'ibrahim ashiraf liked your post', 'LIKE', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xf1787c34d27a4e369627fa6d258526e4, '2026-02-09 15:56:28.792112', 0x574ca4da24fa44c49af728e68e7d118d, b'0', 'salim ashiraf liked your post', 'LIKE', 0x786190a6cc07406480c9318e630b693f, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xf187bcdf3e964733a0f73c2afd47ea70, '2026-02-09 19:44:44.571242', 0x5105bc47e23748c6938d0705430b8f5e, b'1', 'Ezekiel Salehe liked your post', 'LIKE', 0x1a531d9d25a54266a87e10f0ae60e187, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xf5e443f0ff37487c93de164b878b7fc2, '2026-02-10 16:21:56.050729', 0x2cecbeb064364e24ab4c525f9d7e5585, b'0', 'Erick salehe started following you', 'FOLLOW', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x2985ef03dcca48c79358408a4f589d0b),
(0xf90ada7174c142dfa7236275bd89ab8b, '2026-02-10 18:49:51.606607', 0xe56e92e29d434c72b898cfd2a65f0b7e, b'0', 'ibrahim ashiraf started following you', 'FOLLOW', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x846276afb83244f8bcd78bbd67b5eba7),
(0xf9967a3186904585962d0ad1fac5c704, '2026-02-11 08:39:33.794614', 0x6e97e12bec3346a59d5853cda888ca4d, b'0', 'Justin John started following you', 'FOLLOW', 0x6e97e12bec3346a59d5853cda888ca4d, 0xf97655b1d77642efbd9514045a4e7270),
(0xfa6c0aa2fa7c4d89b07231765914682e, '2026-02-10 00:18:55.032287', 0x771a8b4087094a5f985c471c0e3109f3, b'0', 'Erick salehe commented on your post', 'COMMENT', 0x2cecbeb064364e24ab4c525f9d7e5585, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xfa7448585ee3482abd344845dee73c30, '2026-02-09 20:28:59.801776', 0x2cecbeb064364e24ab4c525f9d7e5585, b'0', 'Erick salehe started following you', 'FOLLOW', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xfcde4642881244579ee9e22f5f6d9e4c, '2026-02-11 08:40:31.476055', 0x2cecbeb064364e24ab4c525f9d7e5585, b'0', 'Erick salehe started following you', 'FOLLOW', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x6e97e12bec3346a59d5853cda888ca4d),
(0xfdab10e851ea48ee8b36f1cc10d25635, '2026-02-09 20:29:13.527527', 0x34b75a1485f0454ab6c196beb1e3f85d, b'0', 'Erick salehe liked your post', 'LIKE', 0x2cecbeb064364e24ab4c525f9d7e5585, 0x1a531d9d25a54266a87e10f0ae60e187);

-- --------------------------------------------------------

--
-- Table structure for table `orders`
--

CREATE TABLE `orders` (
  `id` binary(16) NOT NULL,
  `cancellation_reason` varchar(255) DEFAULT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `customer_notes` text,
  `delivered_at` datetime(6) DEFAULT NULL,
  `delivery_address` text,
  `delivery_district` varchar(255) DEFAULT NULL,
  `delivery_fee` decimal(12,2) DEFAULT NULL,
  `delivery_name` varchar(255) DEFAULT NULL,
  `delivery_phone` varchar(255) DEFAULT NULL,
  `delivery_region` varchar(255) DEFAULT NULL,
  `discount` decimal(12,2) DEFAULT NULL,
  `is_paid` bit(1) DEFAULT NULL,
  `order_number` varchar(255) NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `payment_method` varchar(255) DEFAULT NULL,
  `seller_notes` text,
  `shipped_at` datetime(6) DEFAULT NULL,
  `status` enum('PENDING','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','COMPLETED','CANCELLED','REFUNDED') DEFAULT NULL,
  `subtotal` decimal(12,2) NOT NULL,
  `total` decimal(12,2) NOT NULL,
  `tracking_number` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `business_id` binary(16) NOT NULL,
  `buyer_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `order_items`
--

CREATE TABLE `order_items` (
  `id` binary(16) NOT NULL,
  `product_image` varchar(255) DEFAULT NULL,
  `product_name` varchar(255) NOT NULL,
  `quantity` int NOT NULL,
  `total` decimal(12,2) NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `order_id` binary(16) NOT NULL,
  `product_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `payments`
--

CREATE TABLE `payments` (
  `id` binary(16) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `external_reference` varchar(255) DEFAULT NULL,
  `method` enum('MPESA','TIGOPESA','AIRTELMONEY','HALOPESA','BANK_TRANSFER','CARD') DEFAULT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `payment_phone` varchar(255) DEFAULT NULL,
  `provider_response` text,
  `related_entity_id` binary(16) DEFAULT NULL,
  `related_entity_type` varchar(255) DEFAULT NULL,
  `status` enum('PENDING','PROCESSING','SUCCESS','FAILED','CANCELLED','REFUNDED') DEFAULT NULL,
  `transaction_id` varchar(255) DEFAULT NULL,
  `type` enum('SUBSCRIPTION','AGENT_REGISTRATION','BUSINESS_ACTIVATION','PROMOTION','ORDER','WITHDRAWAL','COIN_PURCHASE') NOT NULL,
  `user_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `posts`
--

CREATE TABLE `posts` (
  `id` binary(16) NOT NULL,
  `caption` text,
  `created_at` datetime(6) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  `post_type` enum('POST','REEL','STORY') DEFAULT NULL,
  `shares_count` int DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `views_count` int DEFAULT NULL,
  `visibility` enum('PUBLIC','FOLLOWERS','PRIVATE') DEFAULT NULL,
  `author_id` binary(16) NOT NULL,
  `community_id` binary(16) DEFAULT NULL,
  `original_post_id` binary(16) DEFAULT NULL,
  `location` varchar(500) DEFAULT NULL,
  `feeling_activity` varchar(200) DEFAULT NULL,
  `is_pinned` tinyint(1) NOT NULL DEFAULT '0',
  `pinned_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `posts`
--

INSERT INTO `posts` (`id`, `caption`, `created_at`, `is_deleted`, `post_type`, `shares_count`, `updated_at`, `views_count`, `visibility`, `author_id`, `community_id`, `original_post_id`, `location`, `feeling_activity`, `is_pinned`, `pinned_at`) VALUES
(0x0ffcc129167c43cba7fdcbc3f39f3a7d, 'dddd', '2026-02-10 02:45:07.724142', b'0', 'STORY', 0, '2026-02-10 23:28:57.225362', 2, 'PUBLIC', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, NULL, NULL, NULL, 0, NULL),
(0x14b2d1d1eedc4b5483d49d5bf99fae2b, NULL, '2026-02-10 00:16:03.622180', b'0', 'STORY', 0, '2026-02-10 12:14:31.377034', 3, 'PUBLIC', 0x1a531d9d25a54266a87e10f0ae60e187, NULL, 0x8a218a947e694abb9b47dae84e3f6d32, NULL, NULL, 0, NULL),
(0x1f0e3831eb004dec968a5eacf6fd9dc6, '', '2026-02-10 05:39:34.155126', b'0', 'STORY', 0, '2026-02-10 19:28:10.021090', 3, 'PUBLIC', 0x2985ef03dcca48c79358408a4f589d0b, NULL, NULL, NULL, NULL, 0, NULL),
(0x246d222ba96340c585586d9437e0be2a, 'hi pealpo', '2026-02-09 17:36:59.887497', b'0', 'STORY', 0, '2026-02-10 00:14:56.532165', 2, 'PUBLIC', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, NULL, NULL, NULL, 0, NULL),
(0x259da1f065924ec598d21521fe263147, '', '2026-02-10 01:26:40.157770', b'0', 'STORY', 0, '2026-02-10 23:28:48.082442', 2, 'PUBLIC', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, NULL, NULL, NULL, 0, NULL),
(0x2d9e09b17e284bfaa8e73c95ad8c96b4, 'pop', '2026-02-10 22:59:37.427318', b'0', 'REEL', 0, '2026-02-10 22:59:37.427395', 0, 'PUBLIC', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, NULL, NULL, NULL, 0, NULL),
(0x2eaf35878e1944638773fff590115cc9, '', '2026-02-10 00:22:03.737953', b'0', 'STORY', 0, '2026-02-10 00:23:21.844979', 2, 'PUBLIC', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, NULL, NULL, NULL, 0, NULL),
(0x34b75a1485f0454ab6c196beb1e3f85d, 'Ho guys', '2026-02-09 19:47:02.492988', b'0', 'STORY', 0, '2026-02-10 12:14:32.485872', 3, 'PUBLIC', 0x1a531d9d25a54266a87e10f0ae60e187, NULL, NULL, NULL, NULL, 0, NULL),
(0x3fb24b08d68547f3987ce3eec7502153, 'hiiii', '2026-02-10 02:44:21.261576', b'0', 'POST', 1, '2026-02-10 04:23:11.961892', 0, 'PUBLIC', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, NULL, NULL, NULL, 0, NULL),
(0x5105bc47e23748c6938d0705430b8f5e, 'dddd', '2026-02-09 13:04:01.644418', b'0', 'POST', 0, '2026-02-09 13:04:01.644461', 0, 'PUBLIC', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, NULL, NULL, NULL, 0, NULL),
(0x574ca4da24fa44c49af728e68e7d118d, 'test', '2026-02-09 14:23:46.208580', b'0', 'POST', 0, '2026-02-09 14:23:46.208607', 0, 'PUBLIC', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, NULL, NULL, NULL, 0, NULL),
(0x5ca6683d51b94dc084fdd22a5f49a4f0, '', '2026-02-09 15:20:09.109617', b'0', 'STORY', 0, '2026-02-10 00:22:58.453139', 2, 'PUBLIC', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, NULL, NULL, NULL, 0, NULL),
(0x5eb37da3b59c438a87d78aa5096a9778, 'Thanks God', '2026-02-09 14:41:29.151842', b'0', 'POST', 0, '2026-02-09 14:41:29.152015', 0, 'PUBLIC', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, NULL, NULL, NULL, 0, NULL),
(0x609a9f5caab54b63b72bc7a13852bb6d, '', '2026-02-10 00:16:14.526918', b'0', 'POST', 0, '2026-02-10 00:16:14.526953', 0, 'PUBLIC', 0x1a531d9d25a54266a87e10f0ae60e187, NULL, 0x8a218a947e694abb9b47dae84e3f6d32, NULL, NULL, 0, NULL),
(0x71825ea5559c45309e5a71f9df8ea200, NULL, '2026-02-10 04:23:11.956330', b'0', 'STORY', 0, '2026-02-10 19:28:08.727383', 2, 'PUBLIC', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, 0x3fb24b08d68547f3987ce3eec7502153, NULL, NULL, 0, NULL),
(0x75c5bbadabd14229a8cdceb6ed321a55, '', '2026-02-10 02:42:09.367189', b'0', 'STORY', 0, '2026-02-10 18:48:26.017866', 3, 'PUBLIC', 0x1a531d9d25a54266a87e10f0ae60e187, NULL, NULL, NULL, NULL, 0, NULL),
(0x7662961193ca4c3e8439c9448ed9dbec, 'New laravel version', '2026-02-11 07:01:06.418451', b'0', 'REEL', 0, '2026-02-11 07:01:06.418502', 0, 'PUBLIC', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, NULL, NULL, NULL, 0, NULL),
(0x771a8b4087094a5f985c471c0e3109f3, 'Po', '2026-02-09 22:36:51.682397', b'0', 'POST', 0, '2026-02-09 22:36:51.682489', 0, 'PUBLIC', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x1a25329af10845009fd6410c97ae188a, NULL, NULL, NULL, 0, NULL),
(0x778c5e32c8ef4d92acd6e2e65c33196c, 'Hi guys welcome to laravel', '2026-02-11 06:20:25.087671', b'0', 'STORY', 0, '2026-02-11 08:40:00.365774', 2, 'PUBLIC', 0x1a531d9d25a54266a87e10f0ae60e187, NULL, NULL, NULL, NULL, 0, NULL),
(0x7f02749d4d6b4c55862a201d7d1f0b6f, '', '2026-02-10 00:22:58.519715', b'0', 'STORY', 0, '2026-02-10 00:23:21.086256', 1, 'PUBLIC', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, NULL, NULL, NULL, 0, NULL),
(0x7f5edff61c4e4fcaa99cb85d53f63f2c, '', '2026-02-10 01:43:08.725210', b'0', 'STORY', 0, '2026-02-10 02:43:10.437288', 2, 'PUBLIC', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, NULL, NULL, NULL, 0, NULL),
(0x8a218a947e694abb9b47dae84e3f6d32, 'GGFFFDDF', '2026-02-09 23:18:00.821187', b'0', 'STORY', 3, '2026-02-10 02:07:23.644268', 2, 'PUBLIC', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, NULL, NULL, NULL, 0, NULL),
(0x93bf9d65c25c4ce0a3fe362c11eb6e84, 'gerfgvrafvwsfffsad', '2026-02-10 23:34:03.953680', b'0', 'REEL', 0, '2026-02-10 23:34:03.953749', 0, 'PUBLIC', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, NULL, NULL, NULL, 0, NULL),
(0x94aefa819f7244e7abb4ea435e0c6324, '', '2026-02-10 02:40:29.753238', b'0', 'STORY', 0, '2026-02-10 18:48:22.043892', 2, 'PUBLIC', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, NULL, NULL, NULL, 0, NULL),
(0xa55e69f9711e49c4a3c2f738057593dd, 'hello guys', '2026-02-09 06:01:07.131665', b'0', 'POST', 0, '2026-02-09 06:01:07.131714', 0, 'PUBLIC', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, NULL, NULL, NULL, 0, NULL),
(0xb330c623920d49eebbfe82f2d919db9e, 'zxczxczx', '2026-02-09 21:47:16.085250', b'0', 'POST', 0, '2026-02-09 21:47:16.085294', 0, 'PUBLIC', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x1a25329af10845009fd6410c97ae188a, NULL, NULL, NULL, 0, NULL),
(0xba39e4f8a5814ba5bd25566d4a46b011, 'Best language backend right now', '2026-02-11 08:30:06.080162', b'0', 'STORY', 0, '2026-02-11 08:39:56.662014', 1, 'PUBLIC', 0x2cecbeb064364e24ab4c525f9d7e5585, NULL, NULL, NULL, NULL, 0, NULL),
(0xc8f2f7462b864ad99dcdc029baf5f630, 'haha', '2026-02-09 05:54:00.680608', b'0', 'POST', 0, '2026-02-09 05:54:00.680707', 0, 'PUBLIC', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, NULL, NULL, NULL, 0, NULL),
(0xca7cec00eb8345279bc4e826876ae93d, 'Pop', '2026-02-10 01:44:10.978265', b'0', 'POST', 0, '2026-02-10 01:44:10.978302', 0, 'PUBLIC', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, NULL, NULL, NULL, 0, NULL),
(0xd670703817c24cbc975ae683f45988a2, 'what is this', '2026-02-09 07:01:18.073969', b'0', 'POST', 0, '2026-02-09 07:01:18.074015', 0, 'PUBLIC', 0x786190a6cc07406480c9318e630b693f, NULL, NULL, NULL, NULL, 0, NULL),
(0xdcb0332044874f42a5ac575908054a0f, 'hi', '2026-02-09 22:06:54.386576', b'0', 'POST', 0, '2026-02-09 22:06:54.386698', 0, 'PUBLIC', 0xe56e92e29d434c72b898cfd2a65f0b7e, 0x1a25329af10845009fd6410c97ae188a, NULL, NULL, NULL, 0, NULL),
(0xf5e48bdbefea4a8a8cff86c2fd711c6c, 'pop', '2026-02-10 01:23:19.977193', b'0', 'STORY', 0, '2026-02-10 23:28:48.933336', 2, 'PUBLIC', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, NULL, NULL, NULL, 0, NULL),
(0xf6813a1afdf64fb1a3225c6630fa50c1, NULL, '2026-02-10 02:07:23.641643', b'0', 'STORY', 0, '2026-02-10 23:28:47.334517', 2, 'PUBLIC', 0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, 0x8a218a947e694abb9b47dae84e3f6d32, NULL, NULL, 0, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `post_hashtags`
--

CREATE TABLE `post_hashtags` (
  `post_id` binary(16) NOT NULL,
  `hashtag_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `post_likes`
--

CREATE TABLE `post_likes` (
  `post_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `post_media`
--

CREATE TABLE `post_media` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `display_order` int DEFAULT NULL,
  `duration_seconds` int DEFAULT NULL,
  `thumbnail_url` varchar(255) DEFAULT NULL,
  `type` enum('IMAGE','VIDEO') NOT NULL,
  `url` varchar(255) NOT NULL,
  `post_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `post_media`
--

INSERT INTO `post_media` (`id`, `created_at`, `display_order`, `duration_seconds`, `thumbnail_url`, `type`, `url`, `post_id`) VALUES
(0x03af0aa0694e4532bbcc552957b4b194, '2026-02-10 01:23:19.987052', 0, NULL, NULL, 'VIDEO', 'https://storage.wakilfy.com/posts/03ee799c-99bb-42e1-8bd4-188d71fe7bc1.mp4', 0xf5e48bdbefea4a8a8cff86c2fd711c6c),
(0x1f2b5eeef4924578889144e6fe830b05, '2026-02-11 07:01:06.423617', 0, NULL, NULL, 'VIDEO', 'https://storage.wakilfy.com/posts/0eeb7f03-be99-424a-b871-f312f67d3d54.mp4', 0x7662961193ca4c3e8439c9448ed9dbec),
(0x27041a9a56c649dcad48a5c22909830f, '2026-02-10 02:42:09.371991', 0, NULL, NULL, 'VIDEO', 'https://storage.wakilfy.com/posts/1a481d07-31a5-41b0-b0a6-72bc29caf814.mp4', 0x75c5bbadabd14229a8cdceb6ed321a55),
(0x3d64dc7a36d94ea0aa47cc43660c1ba7, '2026-02-10 00:22:58.522575', 0, NULL, NULL, 'VIDEO', 'https://storage.wakilfy.com/posts/fc408207-7eb1-4c4a-ab08-2bb8a0d72d18.mp4', 0x7f02749d4d6b4c55862a201d7d1f0b6f),
(0x720511da538e465d9083471f7b2695a8, '2026-02-10 01:26:40.163793', 0, NULL, NULL, 'VIDEO', 'https://storage.wakilfy.com/posts/0bf88c61-295a-40fb-9756-cfafa96a61e5.mp4', 0x259da1f065924ec598d21521fe263147),
(0x8752b33b4590483aa40fec0775957aa1, '2026-02-10 02:40:29.796865', 0, NULL, NULL, 'VIDEO', 'https://storage.wakilfy.com/posts/ed428d3d-2df9-4d83-b8aa-e63ddc804fd7.mp4', 0x94aefa819f7244e7abb4ea435e0c6324),
(0x8dd720251b5b4fa785ab393cb8452d9d, '2026-02-10 05:39:34.159921', 0, NULL, NULL, 'IMAGE', 'https://storage.wakilfy.com/posts/c55f86dd-8273-482f-acbe-88c17efb8e2c.jpg', 0x1f0e3831eb004dec968a5eacf6fd9dc6),
(0x901857ed7cb04f6cb3cfe2f4e11ae071, '2026-02-10 22:59:37.435635', 0, NULL, NULL, 'VIDEO', 'https://storage.wakilfy.com/posts/e6023660-99ec-4cae-88fe-6c5221ecf488.mp4', 0x2d9e09b17e284bfaa8e73c95ad8c96b4),
(0x9729279ec55f45fdabde38fa577f2b60, '2026-02-09 17:36:59.893073', 0, NULL, NULL, 'IMAGE', 'https://storage.wakilfy.com/posts/734c9bb4-af41-415d-ad4d-6dbe5c48e37b.jpeg', 0x246d222ba96340c585586d9437e0be2a),
(0x9785bf78970a489c83e1348a0f6da2d2, '2026-02-09 22:36:51.740717', 0, NULL, NULL, 'IMAGE', 'https://storage.wakilfy.com/posts/20b46076-8d6b-4832-b54f-2f0918719656.jpg', 0x771a8b4087094a5f985c471c0e3109f3),
(0x9c16484a7dfe4346b222823319a3e102, '2026-02-10 23:34:03.961256', 0, NULL, NULL, 'VIDEO', 'https://storage.wakilfy.com/posts/8568575c-1465-4aa0-9178-92dc087e7ac6.mp4', 0x93bf9d65c25c4ce0a3fe362c11eb6e84),
(0xa2fb248cd6344070bd2c13c7f89ebede, '2026-02-09 14:23:46.212517', 0, NULL, NULL, 'IMAGE', 'https://storage.wakilfy.com/posts/aa330a09-c890-40ea-b7a5-3db64df32ccb.jpeg', 0x574ca4da24fa44c49af728e68e7d118d),
(0xa62a2019c95d4855bbe45848a4181648, '2026-02-10 01:43:08.804670', 0, NULL, NULL, 'VIDEO', 'https://storage.wakilfy.com/posts/ec214a3c-0961-4385-af15-70b6ec286a98.mp4', 0x7f5edff61c4e4fcaa99cb85d53f63f2c),
(0xb3b29b1cbc5245d490e81f94a111ccc0, '2026-02-09 13:04:01.650222', 0, NULL, NULL, 'IMAGE', 'https://storage.wakilfy.com/posts/8d9933f8-e9f1-4669-a2ad-1a166d3d008b.jpeg', 0x5105bc47e23748c6938d0705430b8f5e),
(0xb80debfd8fbd4ce981155e6efd6080d2, '2026-02-09 15:20:09.140456', 0, NULL, NULL, 'IMAGE', 'https://storage.wakilfy.com/posts/35420e79-7dcf-404d-b175-3de633f15296.jpg', 0x5ca6683d51b94dc084fdd22a5f49a4f0),
(0xc6b3f8b34ce74c089b4db0b0b5bff7ff, '2026-02-10 01:44:10.986866', 0, NULL, NULL, 'IMAGE', 'https://storage.wakilfy.com/posts/c980ac70-c558-4551-bbda-415ed970397a.jpg', 0xca7cec00eb8345279bc4e826876ae93d),
(0xd3f0ad60e3254f88a7cd3f4840f5b533, '2026-02-10 02:44:21.265781', 0, NULL, NULL, 'VIDEO', 'https://storage.wakilfy.com/posts/10d759cc-e3c3-4567-9008-c6e85cab8104.mp4', 0x3fb24b08d68547f3987ce3eec7502153),
(0xd93d4d0cfb754d1497ff4f0c3029aa3a, '2026-02-11 06:20:25.092316', 0, NULL, NULL, 'VIDEO', 'https://storage.wakilfy.com/posts/2cd4bee9-e985-41cc-9bca-7e9303102b53.mp4', 0x778c5e32c8ef4d92acd6e2e65c33196c),
(0xdaa8f74e82834e27b933f6a079fac5d8, '2026-02-09 07:01:18.084870', 0, NULL, NULL, 'IMAGE', 'https://storage.wakilfy.com/posts/310566e0-2947-44fb-ba67-2f5fd8b7b4a9.png', 0xd670703817c24cbc975ae683f45988a2),
(0xdce88643d04241b0baf0501c993fa2e1, '2026-02-10 02:45:07.730181', 0, NULL, NULL, 'IMAGE', 'https://storage.wakilfy.com/posts/64ceb07a-6144-49f8-b0eb-9373e05cd4d3.png', 0x0ffcc129167c43cba7fdcbc3f39f3a7d),
(0xe2549e302bda4572b750dd2f3ddd3e63, '2026-02-11 08:30:06.086162', 0, NULL, NULL, 'IMAGE', 'https://storage.wakilfy.com/posts/8206e702-519f-42a0-be63-54fd5ac5a538.png', 0xba39e4f8a5814ba5bd25566d4a46b011),
(0xe9a6966a3ca642eca3019f67f441ffb0, '2026-02-10 00:22:03.743869', 0, NULL, NULL, 'VIDEO', 'https://storage.wakilfy.com/posts/df5d57d4-f79e-4705-ac79-621f0a5139f6.mp4', 0x2eaf35878e1944638773fff590115cc9),
(0xebb84930b7a5481cbe23431dc13f6bba, '2026-02-09 14:41:29.176314', 0, NULL, NULL, 'IMAGE', 'https://storage.wakilfy.com/posts/95aa2cbc-442c-4b2e-b61f-92ea057232d8.jpeg', 0x5eb37da3b59c438a87d78aa5096a9778),
(0xefb9ae80d39549b6aabefc0607cc2ab9, '2026-02-09 19:47:02.498999', 0, NULL, NULL, 'IMAGE', 'https://storage.wakilfy.com/posts/fa53c537-abaf-4b08-a62d-8d0f2f68b954.jpg', 0x34b75a1485f0454ab6c196beb1e3f85d);

-- --------------------------------------------------------

--
-- Table structure for table `post_product_tags`
--

CREATE TABLE `post_product_tags` (
  `post_id` binary(16) NOT NULL,
  `product_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `post_reactions`
--

CREATE TABLE `post_reactions` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `type` enum('LIKE','LOVE','HAHA','WOW','SAD','ANGRY') NOT NULL,
  `post_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `post_reactions`
--

INSERT INTO `post_reactions` (`id`, `created_at`, `type`, `post_id`, `user_id`) VALUES
(0x090af36f5b2347b99501a9cc5bfc16ef, '2026-02-10 00:16:10.183974', 'LIKE', 0x8a218a947e694abb9b47dae84e3f6d32, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x111f8ec7ae144c1fb375b748b179e516, '2026-02-10 19:06:35.567780', 'LIKE', 0x34b75a1485f0454ab6c196beb1e3f85d, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x249dc97fe4804e80b43a315e5651b4b4, '2026-02-10 03:21:55.681871', 'LIKE', 0x3fb24b08d68547f3987ce3eec7502153, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x26de0559c0364dccaaf5c7870b4b3566, '2026-02-11 06:59:39.440348', 'LIKE', 0x778c5e32c8ef4d92acd6e2e65c33196c, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x298084b3de1a46499d906f0eff292302, '2026-02-11 02:18:49.293433', 'LIKE', 0x93bf9d65c25c4ce0a3fe362c11eb6e84, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x2a9260a0acc84a12866512a7844bf93b, '2026-02-09 19:44:42.010158', 'LIKE', 0x5eb37da3b59c438a87d78aa5096a9778, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x2bfd87c1f49c4f74af54163e92f93a9e, '2026-02-11 08:20:04.526631', 'LIKE', 0x2d9e09b17e284bfaa8e73c95ad8c96b4, 0xf97655b1d77642efbd9514045a4e7270),
(0x2f089170660a45ab9dc48c15bc793cf6, '2026-02-10 00:18:31.250977', 'LIKE', 0x609a9f5caab54b63b72bc7a13852bb6d, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x30c4a22c72154394926ba4fb905c0ba3, '2026-02-10 12:14:43.431459', 'LIKE', 0x75c5bbadabd14229a8cdceb6ed321a55, 0x2985ef03dcca48c79358408a4f589d0b),
(0x32d190dd2e8540548ddc98fde0279361, '2026-02-09 22:19:57.733139', 'LIKE', 0xdcb0332044874f42a5ac575908054a0f, 0x786190a6cc07406480c9318e630b693f),
(0x33c7b3cf5c194a1c9e53cd574b4805b1, '2026-02-10 02:45:17.873385', 'LIKE', 0x0ffcc129167c43cba7fdcbc3f39f3a7d, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x38c4b2a3a79e492598ccc5a42e71ed2d, '2026-02-10 02:54:18.052531', 'LIKE', 0xf5e48bdbefea4a8a8cff86c2fd711c6c, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x3a93d3652dde4c0c87e375c4ecae3155, '2026-02-11 00:14:34.807853', 'LIKE', 0x771a8b4087094a5f985c471c0e3109f3, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x3b081025ce5f44ef85afd409841a39f4, '2026-02-09 15:56:28.788922', 'LIKE', 0x574ca4da24fa44c49af728e68e7d118d, 0x786190a6cc07406480c9318e630b693f),
(0x4063ac339a3444dbbbc669a2d31f4f1c, '2026-02-10 05:39:39.589136', 'LIKE', 0x1f0e3831eb004dec968a5eacf6fd9dc6, 0x2985ef03dcca48c79358408a4f589d0b),
(0x439ffb3cbc2e4afbac32ab475ebcd83c, '2026-02-09 19:45:23.458695', 'LIKE', 0x246d222ba96340c585586d9437e0be2a, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x4482abe5099e4d12a630d353f5054e02, '2026-02-09 13:03:37.663863', 'LIKE', 0xc8f2f7462b864ad99dcdc029baf5f630, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x4c47fb45cd0248c3bc90d74970bd0469, '2026-02-10 23:05:54.814793', 'LIKE', 0x2d9e09b17e284bfaa8e73c95ad8c96b4, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x53e309e79d6142d4864a7a6908713726, '2026-02-09 14:29:03.571929', 'LIKE', 0x574ca4da24fa44c49af728e68e7d118d, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x55f9d94e450843319487533e26e6860a, '2026-02-10 12:14:44.031403', 'LIKE', 0x609a9f5caab54b63b72bc7a13852bb6d, 0x2985ef03dcca48c79358408a4f589d0b),
(0x62e08a07f63542c6802ddbda1485b671, '2026-02-10 00:18:36.517516', 'LIKE', 0x609a9f5caab54b63b72bc7a13852bb6d, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x6585e784f0ad49a0a2fd97d1a55a01ed, '2026-02-09 23:17:17.115559', 'LIKE', 0x771a8b4087094a5f985c471c0e3109f3, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x79acee0e0519417ba546b8e25417d2ce, '2026-02-09 19:44:39.917908', 'LIKE', 0x5ca6683d51b94dc084fdd22a5f49a4f0, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x8bcf4e189e444cab8a56c5ff474812a1, '2026-02-10 12:14:47.703309', 'LIKE', 0x34b75a1485f0454ab6c196beb1e3f85d, 0x2985ef03dcca48c79358408a4f589d0b),
(0x934d2ce5f23e4bfba28960ab96166b91, '2026-02-11 02:32:53.380509', 'LIKE', 0x93bf9d65c25c4ce0a3fe362c11eb6e84, 0x786190a6cc07406480c9318e630b693f),
(0x9910e99ae8b94643b711132a48f7aed7, '2026-02-09 13:03:36.355466', 'LIKE', 0xa55e69f9711e49c4a3c2f738057593dd, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x9ea80a4cc5e64b99b4cf69feea451be5, '2026-02-09 15:56:07.165981', 'LIKE', 0x5eb37da3b59c438a87d78aa5096a9778, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xa4a919da3e0343dcb1c0981dfef9f53d, '2026-02-11 08:31:16.286570', 'LIKE', 0x93bf9d65c25c4ce0a3fe362c11eb6e84, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xaa2752e119844524ab7fa63dd4971edd, '2026-02-10 12:14:44.644087', 'LIKE', 0x14b2d1d1eedc4b5483d49d5bf99fae2b, 0x2985ef03dcca48c79358408a4f589d0b),
(0xabd59a539bba42f798c2faa5006f1eef, '2026-02-11 06:21:47.465894', 'LIKE', 0x778c5e32c8ef4d92acd6e2e65c33196c, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xabf13d385911481db39e795b8d63e3b3, '2026-02-09 19:44:38.036777', 'LIKE', 0x246d222ba96340c585586d9437e0be2a, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xacc51b1485b84406aea316d04c8e0831, '2026-02-09 14:23:17.722275', 'LIKE', 0xd670703817c24cbc975ae683f45988a2, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xb1e4bef1e4ab419096d0bee77834d794, '2026-02-10 00:12:02.586025', 'LIKE', 0x771a8b4087094a5f985c471c0e3109f3, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xb36308d0f0c548cab2aebcb82437e15b, '2026-02-09 19:44:58.263395', 'LIKE', 0xd670703817c24cbc975ae683f45988a2, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xb4cb13ccf84744528a5cf0b8aad525a8, '2026-02-09 20:29:13.523057', 'LIKE', 0x34b75a1485f0454ab6c196beb1e3f85d, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xb669f033daf14381bac617e843872a70, '2026-02-09 06:24:01.115420', 'LIKE', 0xa55e69f9711e49c4a3c2f738057593dd, 0x786190a6cc07406480c9318e630b693f),
(0xbac954a1987d4ea0a9a2829814c3e4fd, '2026-02-11 08:30:16.932663', 'LIKE', 0xba39e4f8a5814ba5bd25566d4a46b011, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xc42392b2700640e880d1c0542ae16758, '2026-02-09 14:41:33.729883', 'LIKE', 0x5eb37da3b59c438a87d78aa5096a9778, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xc5057e2c5051432da72428aedecc943d, '2026-02-09 19:47:07.857785', 'LIKE', 0x34b75a1485f0454ab6c196beb1e3f85d, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xc55f8065a83a42a58fa4cb51370e0783, '2026-02-10 05:39:52.493806', 'LIKE', 0x1f0e3831eb004dec968a5eacf6fd9dc6, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xc56001435a004015b03771bdbf8e1513, '2026-02-11 07:01:31.512731', 'LIKE', 0x7662961193ca4c3e8439c9448ed9dbec, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xc8a6c05f9ca74b44a519a68aa311a753, '2026-02-09 22:37:45.786592', 'LIKE', 0xdcb0332044874f42a5ac575908054a0f, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xcb1221c5e53446949fb0fd34fe02208d, '2026-02-09 14:29:05.511573', 'LIKE', 0x5105bc47e23748c6938d0705430b8f5e, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xcbc2f63899a0456a87ff95b378618418, '2026-02-11 04:45:37.395516', 'LIKE', 0x2d9e09b17e284bfaa8e73c95ad8c96b4, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xd3aba6d0b72a4f768e7496c2e0756e99, '2026-02-10 00:13:03.206176', 'LIKE', 0xb330c623920d49eebbfe82f2d919db9e, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xd623a8067ffa441aaa5cb74f062a54f1, '2026-02-09 19:44:44.569622', 'LIKE', 0x5105bc47e23748c6938d0705430b8f5e, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xe1056b3d008b43a8981c6bda2b105f69, '2026-02-09 06:25:31.783270', 'LIKE', 0xc8f2f7462b864ad99dcdc029baf5f630, 0x786190a6cc07406480c9318e630b693f),
(0xe7f742d834504320a4b0bfe2fe12548e, '2026-02-11 09:52:16.104521', 'LIKE', 0xba39e4f8a5814ba5bd25566d4a46b011, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xea541505f36f445e82bee8b33ac8d154, '2026-02-09 15:42:50.697622', 'LIKE', 0x5ca6683d51b94dc084fdd22a5f49a4f0, 0x786190a6cc07406480c9318e630b693f),
(0xec56e52522404936b325d852411e2c56, '2026-02-09 15:21:29.834517', 'LIKE', 0x5ca6683d51b94dc084fdd22a5f49a4f0, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xecaaf617dd094b858f90c92925c663a5, '2026-02-10 23:18:54.897356', 'LIKE', 0x2d9e09b17e284bfaa8e73c95ad8c96b4, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xf1a16aabdcb34e47ae0f5e44e55be69d, '2026-02-10 00:13:02.306764', 'LIKE', 0xdcb0332044874f42a5ac575908054a0f, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xf2d9641559c54cfe8ffaeef7d6380c9c, '2026-02-11 18:52:57.931447', 'LIKE', 0x93bf9d65c25c4ce0a3fe362c11eb6e84, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xf83eb35bea624f2d90804f263a1236d1, '2026-02-09 19:44:43.224337', 'LIKE', 0x574ca4da24fa44c49af728e68e7d118d, 0x1a531d9d25a54266a87e10f0ae60e187);

-- --------------------------------------------------------

--
-- Table structure for table `post_tagged_users`
--

CREATE TABLE `post_tagged_users` (
  `post_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `products`
--

CREATE TABLE `products` (
  `id` binary(16) NOT NULL,
  `category` varchar(255) DEFAULT NULL,
  `compare_at_price` decimal(12,2) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `is_active` bit(1) DEFAULT NULL,
  `is_featured` bit(1) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `orders_count` int DEFAULT NULL,
  `price` decimal(12,2) NOT NULL,
  `rating` double DEFAULT NULL,
  `reviews_count` int DEFAULT NULL,
  `sku` varchar(255) DEFAULT NULL,
  `stock_quantity` int DEFAULT NULL,
  `thumbnail` varchar(255) DEFAULT NULL,
  `track_stock` bit(1) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `views_count` int DEFAULT NULL,
  `business_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `product_images`
--

CREATE TABLE `product_images` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `display_order` int DEFAULT NULL,
  `is_primary` bit(1) DEFAULT NULL,
  `url` varchar(255) NOT NULL,
  `product_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `product_reviews`
--

CREATE TABLE `product_reviews` (
  `id` binary(16) NOT NULL,
  `comment` text,
  `created_at` datetime(6) DEFAULT NULL,
  `rating` int NOT NULL,
  `product_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `promotions`
--

CREATE TABLE `promotions` (
  `id` binary(16) NOT NULL,
  `budget` decimal(12,2) NOT NULL,
  `clicks` bigint DEFAULT NULL,
  `conversions` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `daily_budget` decimal(12,2) DEFAULT NULL,
  `description` text,
  `end_date` datetime(6) NOT NULL,
  `impressions` bigint DEFAULT NULL,
  `is_paid` bit(1) DEFAULT NULL,
  `payment_id` binary(16) DEFAULT NULL,
  `reach` bigint DEFAULT NULL,
  `spent_amount` decimal(12,2) DEFAULT NULL,
  `start_date` datetime(6) NOT NULL,
  `status` enum('PENDING','ACTIVE','PAUSED','COMPLETED','CANCELLED','REJECTED') NOT NULL,
  `target_age_max` int DEFAULT NULL,
  `target_age_min` int DEFAULT NULL,
  `target_gender` varchar(255) DEFAULT NULL,
  `target_id` binary(16) DEFAULT NULL,
  `target_regions` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `type` enum('POST','PRODUCT','BUSINESS') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `business_id` binary(16) DEFAULT NULL,
  `user_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `promotion_packages`
--

CREATE TABLE `promotion_packages` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `daily_reach` bigint DEFAULT NULL,
  `description` text,
  `duration_days` int NOT NULL,
  `includes_analytics` bit(1) DEFAULT NULL,
  `includes_targeting` bit(1) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `price` decimal(12,2) NOT NULL,
  `promotion_type` enum('POST','PRODUCT','BUSINESS') DEFAULT NULL,
  `sort_order` int DEFAULT NULL,
  `total_impressions` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `agent_packages`
--

CREATE TABLE `agent_packages` (
  `id` binary(16) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text,
  `price` decimal(12,2) NOT NULL,
  `number_of_businesses` int NOT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `is_popular` bit(1) DEFAULT NULL,
  `sort_order` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `reports`
--

CREATE TABLE `reports` (
  `id` binary(16) NOT NULL,
  `action_taken` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `reason` enum('SPAM','HARASSMENT','HATE_SPEECH','VIOLENCE','NUDITY','SCAM','FAKE_ACCOUNT','INTELLECTUAL_PROPERTY','OTHER') NOT NULL,
  `resolution_notes` text,
  `resolved_at` datetime(6) DEFAULT NULL,
  `status` enum('PENDING','UNDER_REVIEW','RESOLVED','DISMISSED') NOT NULL,
  `target_id` binary(16) NOT NULL,
  `type` enum('USER','POST','COMMENT','PRODUCT','BUSINESS','MESSAGE') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `reporter_id` binary(16) NOT NULL,
  `resolved_by` binary(16) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `saved_posts`
--

CREATE TABLE `saved_posts` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `post_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `saved_posts`
--

INSERT INTO `saved_posts` (`id`, `created_at`, `post_id`, `user_id`) VALUES
(0x1912037610b349dc815f5f2a74c71ac6, '2026-02-10 03:46:37.803537', 0x75c5bbadabd14229a8cdceb6ed321a55, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x2be0543557ee432c8199034f9a1fb3d6, '2026-02-11 01:40:01.648842', 0x2d9e09b17e284bfaa8e73c95ad8c96b4, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x41be54f949a84d9ab3a5a91983df4c36, '2026-02-10 00:16:07.135975', 0x8a218a947e694abb9b47dae84e3f6d32, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x49dc53314dc64fc6a3cfbffe03a2b609, '2026-02-11 01:35:37.847136', 0x93bf9d65c25c4ce0a3fe362c11eb6e84, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x53dfe90171ac4919a186c995d10e320a, '2026-02-11 06:07:28.897374', 0x93bf9d65c25c4ce0a3fe362c11eb6e84, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x72cd8ff47f4343c2b698806d37e7da50, '2026-02-11 06:21:57.915941', 0x778c5e32c8ef4d92acd6e2e65c33196c, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xf50fc3fe119a4d209cfe7fc06e3e0a07, '2026-02-10 18:48:48.304266', 0x1f0e3831eb004dec968a5eacf6fd9dc6, 0xe56e92e29d434c72b898cfd2a65f0b7e);

-- --------------------------------------------------------

--
-- Table structure for table `story_views`
--

CREATE TABLE `story_views` (
  `id` binary(16) NOT NULL,
  `viewed_at` datetime(6) DEFAULT NULL,
  `post_id` binary(16) NOT NULL,
  `viewer_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `story_views`
--

INSERT INTO `story_views` (`id`, `viewed_at`, `post_id`, `viewer_id`) VALUES
(0x0025889a04874649955ac9bbe2c83d5f, '2026-02-10 12:16:40.181476', 0x71825ea5559c45309e5a71f9df8ea200, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x10eeb68b5da149e8ab708a4039c46928, '2026-02-10 02:42:34.448960', 0x94aefa819f7244e7abb4ea435e0c6324, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x1939fec8ec8243c38f0623d8613ff106, '2026-02-10 19:28:08.721916', 0x71825ea5559c45309e5a71f9df8ea200, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x2215a9087977400d95e30fa337759f91, '2026-02-10 00:09:31.276592', 0x8a218a947e694abb9b47dae84e3f6d32, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x335c5b731ab1426498e4e9c8edb91f4d, '2026-02-10 18:48:22.042160', 0x94aefa819f7244e7abb4ea435e0c6324, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x3819a74095d04c7ab68b3220221e740f, '2026-02-11 07:01:46.322776', 0x778c5e32c8ef4d92acd6e2e65c33196c, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x39bb096ea382498891c54f32d28bcfdc, '2026-02-10 23:28:57.223417', 0x0ffcc129167c43cba7fdcbc3f39f3a7d, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x3ddc2d7aee7749dc98237f1e477c4443, '2026-02-10 18:48:26.015804', 0x75c5bbadabd14229a8cdceb6ed321a55, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x4495425be903497aa0ecd5954e825a5d, '2026-02-10 12:14:32.484452', 0x34b75a1485f0454ab6c196beb1e3f85d, 0x2985ef03dcca48c79358408a4f589d0b),
(0x483b7274c3a54e55bc522d4f4e83828d, '2026-02-10 00:22:58.444804', 0x5ca6683d51b94dc084fdd22a5f49a4f0, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x4ea466e1a5224622950d634f1ca03b23, '2026-02-10 00:23:21.843197', 0x2eaf35878e1944638773fff590115cc9, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x56c3abd6f6384114be783f726b311345, '2026-02-10 01:25:02.456754', 0xf5e48bdbefea4a8a8cff86c2fd711c6c, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x61e8b1cbbca54a7ebcc2f9e3512dd5ee, '2026-02-10 01:45:10.923160', 0x7f5edff61c4e4fcaa99cb85d53f63f2c, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0x637ea296a92b4b8d874a25a6462e09c4, '2026-02-10 02:42:56.411888', 0xf6813a1afdf64fb1a3225c6630fa50c1, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x68d7420258584fe99d4d86d28677c360, '2026-02-11 08:39:56.660216', 0xba39e4f8a5814ba5bd25566d4a46b011, 0x6e97e12bec3346a59d5853cda888ca4d),
(0x6d4e60052bb842ffad4707cd66a134db, '2026-02-10 01:26:53.518077', 0x259da1f065924ec598d21521fe263147, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x739d16b6324b4c84a839846d1ff159dd, '2026-02-10 19:28:10.018246', 0x1f0e3831eb004dec968a5eacf6fd9dc6, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x7444b182ae784445a01e60ce1c7409f2, '2026-02-10 12:16:35.187244', 0x1f0e3831eb004dec968a5eacf6fd9dc6, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x85025202be574eb18eed8ffa44d7cbb9, '2026-02-10 23:28:48.079325', 0x259da1f065924ec598d21521fe263147, 0x1a531d9d25a54266a87e10f0ae60e187),
(0x956b1b1cc6df47259290f97dad3eb3cc, '2026-02-10 05:37:21.012495', 0x75c5bbadabd14229a8cdceb6ed321a55, 0x2985ef03dcca48c79358408a4f589d0b),
(0x9b42678ed75e4a39af82115028bbb818, '2026-02-10 04:23:23.124405', 0x0ffcc129167c43cba7fdcbc3f39f3a7d, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xa0506807bcc84113b92503982fcc56bc, '2026-02-10 00:09:42.091626', 0x246d222ba96340c585586d9437e0be2a, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xa37532df9fa94bc3a3208875bb6e45cc, '2026-02-10 12:14:31.336790', 0x14b2d1d1eedc4b5483d49d5bf99fae2b, 0x2985ef03dcca48c79358408a4f589d0b),
(0xa7aba42e73d043c4a8886294f1d247e4, '2026-02-10 00:09:47.114745', 0x5ca6683d51b94dc084fdd22a5f49a4f0, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xa8b97ecfbc804319a448d07ae2265034, '2026-02-10 23:28:48.932043', 0xf5e48bdbefea4a8a8cff86c2fd711c6c, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xa914698b6bb943d1a40a3e672ccc0cc9, '2026-02-10 00:14:56.530414', 0x246d222ba96340c585586d9437e0be2a, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xa9ba3e9cf7e24685b9c7251f712a0526, '2026-02-10 18:48:19.434377', 0x1f0e3831eb004dec968a5eacf6fd9dc6, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xaae9f1fdbc014e2098503fa0b82a7c59, '2026-02-10 02:43:10.434287', 0x7f5edff61c4e4fcaa99cb85d53f63f2c, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xab477c449fbb40f6aa96b3e3af63afe7, '2026-02-11 08:40:00.364370', 0x778c5e32c8ef4d92acd6e2e65c33196c, 0x6e97e12bec3346a59d5853cda888ca4d),
(0xbc4adf53ae354a2d972497e8db16e89f, '2026-02-10 00:14:51.512162', 0x8a218a947e694abb9b47dae84e3f6d32, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xbe488786a8ba4dfca224f73172e3072d, '2026-02-10 00:09:52.111331', 0x34b75a1485f0454ab6c196beb1e3f85d, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xc379ecba33a64f7db95cbd1560de51ba, '2026-02-10 00:18:17.011546', 0x14b2d1d1eedc4b5483d49d5bf99fae2b, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xcecd3205a7cd4799bc7275d1b6634f23, '2026-02-10 00:22:21.646856', 0x2eaf35878e1944638773fff590115cc9, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xd452b45abfe24874bb2849b157eeeff4, '2026-02-09 23:18:12.871905', 0x34b75a1485f0454ab6c196beb1e3f85d, 0x2cecbeb064364e24ab4c525f9d7e5585),
(0xdbe982a2923745399e7842055e97ba87, '2026-02-10 00:23:38.103936', 0x14b2d1d1eedc4b5483d49d5bf99fae2b, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xe24b6ee2df1d452daef87e8574168cbf, '2026-02-10 00:23:21.077780', 0x7f02749d4d6b4c55862a201d7d1f0b6f, 0xe56e92e29d434c72b898cfd2a65f0b7e),
(0xf3537f9ad01a4df4b57c717f1bacf528, '2026-02-10 23:28:47.321706', 0xf6813a1afdf64fb1a3225c6630fa50c1, 0x1a531d9d25a54266a87e10f0ae60e187),
(0xf74aaca2b42d49128b88a2ec531adc9d, '2026-02-10 02:42:30.089246', 0x75c5bbadabd14229a8cdceb6ed321a55, 0x2cecbeb064364e24ab4c525f9d7e5585);

-- --------------------------------------------------------

--
-- Table structure for table `subscriptions`
--

CREATE TABLE `subscriptions` (
  `id` binary(16) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `auto_renew` bit(1) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `end_date` datetime(6) NOT NULL,
  `plan` enum('WEEKLY','MONTHLY','QUARTERLY','ANNUAL') NOT NULL,
  `reminder_sent_1_day` bit(1) DEFAULT NULL,
  `reminder_sent_3_days` bit(1) DEFAULT NULL,
  `reminder_sent_7_days` bit(1) DEFAULT NULL,
  `start_date` datetime(6) NOT NULL,
  `status` enum('PENDING','ACTIVE','GRACE','EXPIRED','CANCELLED') DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `business_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` binary(16) NOT NULL,
  `bio` text,
  `cover_pic` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `current_city` varchar(255) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `education` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `gender` varchar(255) DEFAULT NULL,
  `hometown` varchar(255) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `is_verified` bit(1) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `otp_code` varchar(255) DEFAULT NULL,
  `otp_expires_at` datetime(6) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `phone` varchar(255) NOT NULL,
  `profile_pic` varchar(255) DEFAULT NULL,
  `referred_by_agent_code` varchar(255) DEFAULT NULL,
  `relationship_status` varchar(255) DEFAULT NULL,
  `role` enum('VISITOR','USER','BUSINESS','AGENT','ADMIN') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `work` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `interests` text,
  `region` varchar(255) DEFAULT NULL,
  `profile_visibility` enum('PUBLIC','FOLLOWERS','PRIVATE') DEFAULT NULL,
  `following_list_visibility` enum('PUBLIC','FOLLOWERS','PRIVATE') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `bio`, `cover_pic`, `created_at`, `current_city`, `date_of_birth`, `education`, `email`, `gender`, `hometown`, `is_active`, `is_verified`, `name`, `otp_code`, `otp_expires_at`, `password`, `phone`, `profile_pic`, `referred_by_agent_code`, `relationship_status`, `role`, `updated_at`, `website`, `work`, `country`, `interests`, `region`, `profile_visibility`, `following_list_visibility`) VALUES
(0x1a531d9d25a54266a87e10f0ae60e187, NULL, 'https://storage.wakilfy.com/covers/be5ef1fe-22e4-4444-a45e-b338a4be8b54.png', '2026-02-09 19:43:11.050471', NULL, '2002-10-22', NULL, 'erickesn001@gmail.com', NULL, NULL, b'1', b'1', 'Ezekiel Salehe', NULL, NULL, '$2a$10$B0sMQNxwjJj60epZ24h1EOHUOqfUaqN4RM4UQMWWLZfDucegp/iGK', '+255682818866', 'https://storage.wakilfy.com/profile/4f2c6071-be75-4bf6-a18d-a192c5fd47c3.jpg', NULL, NULL, 'USER', '2026-02-09 19:46:38.434660', NULL, NULL, 'Tanzania', 'developers', 'Dar es Salaam', 'PUBLIC', 'PUBLIC'),
(0x2985ef03dcca48c79358408a4f589d0b, NULL, 'https://storage.wakilfy.com/covers/e7405341-6c6f-448d-9561-67ce399d63f0.jpg', '2026-02-10 05:35:39.085274', NULL, '1994-10-08', NULL, 'mrsecondchance001@gmail.com', NULL, NULL, b'1', b'1', 'Enjo Elvin', NULL, NULL, '$2a$10$sKYMZt6eN9qz7N2uFMhV6ORJsbitVRdkqMRmImCrueapqPCoUwOum', '+255682818869', 'https://storage.wakilfy.com/profile/0d5f7fbe-066b-4806-bc71-33722ea7731f.jpg', NULL, NULL, 'USER', '2026-02-10 05:39:18.973170', NULL, NULL, 'Uganda', 'Football', 'Mbeya', 'PUBLIC', 'PUBLIC'),
(0x2cecbeb064364e24ab4c525f9d7e5585, NULL, 'https://storage.wakilfy.com/covers/b5812ce0-b21e-474e-b26d-f5c39568993b.jpg', '2026-02-08 21:57:52.610498', NULL, NULL, NULL, 'ezekielsalehe00@gmail.com', NULL, NULL, b'1', b'1', 'Erick salehe', NULL, NULL, '$2a$10$SmIMR6icekil.w7Rm8UBBegis2.y4kMvNg5z/jST9JzTNgWHQL4ha', '+255750599412', 'https://storage.wakilfy.com/profile/deafd2c7-ba85-44a4-adca-7f4f88067b71.jpg', NULL, NULL, 'USER', '2026-02-10 00:23:31.179144', NULL, NULL, NULL, NULL, NULL, 'PUBLIC', 'PUBLIC'),
(0x6815ee2a0b9a434c84ccf6e17c8973bc, NULL, NULL, '2026-02-08 14:32:06.108271', NULL, NULL, NULL, 'iwwbra@example.com', NULL, NULL, b'1', b'0', 'John Doe', '815708', '2026-02-08 14:42:05.531453', '$2a$10$BASs.MvoOjypjoHX1m7SUeOcwnda4avrpV4XPARyi/H0.A8PfRFDS', '+255653494745', NULL, NULL, NULL, 'USER', '2026-02-08 14:32:06.108325', NULL, NULL, NULL, NULL, NULL, 'PUBLIC', 'PUBLIC'),
(0x6e97e12bec3346a59d5853cda888ca4d, NULL, NULL, '2026-02-11 08:37:43.013871', NULL, '2005-05-07', NULL, 'machajustin666@gmail.com', NULL, NULL, b'1', b'1', 'Justin John', NULL, NULL, '$2a$10$BC.pgRbNiMvcalwUy3Z3Seosgjhywv/7v8CrD3eoQAEnTDzblfIPS', '+255785586190', NULL, NULL, NULL, 'USER', '2026-02-11 08:38:28.794761', NULL, NULL, 'Tanzania', 'Music Games Football', 'Dar es Salaam', 'PUBLIC', 'PUBLIC'),
(0x7483d43b42bc432dacc7262e3c270902, NULL, NULL, '2026-02-08 01:23:39.895667', NULL, NULL, NULL, 'test@wakilfy.com', NULL, NULL, b'1', b'1', 'Test User', NULL, NULL, '$2a$10$BL/ii8gsds6gZ1MglvHFeeYLd31AmOgPbC3D7f2vnuUXaENsoGtL.', '255712000000', NULL, NULL, NULL, 'USER', '2026-02-08 01:23:39.895707', NULL, NULL, NULL, NULL, NULL, 'PUBLIC', 'PUBLIC'),
(0x786190a6cc07406480c9318e630b693f, NULL, 'https://storage.wakilfy.com/covers/8b1ccca2-d0f7-4202-bc29-2165673c078b.png', '2026-02-09 06:02:47.930522', NULL, NULL, NULL, 'doniaparoma@gmail.com', NULL, NULL, b'1', b'1', 'salim ashiraf', NULL, NULL, '$2a$10$iztpVVkAaib7qQSv.0moZe2xNuShf/rnojJpsYdSGT5W7r/BqGAJC', '+255696646570', NULL, NULL, NULL, 'USER', '2026-02-09 23:31:55.218734', NULL, NULL, NULL, NULL, NULL, 'PUBLIC', 'PUBLIC'),
(0x846276afb83244f8bcd78bbd67b5eba7, NULL, NULL, '2026-02-08 13:47:43.607282', NULL, NULL, NULL, 'test@example.com', NULL, NULL, b'1', b'0', 'Test User', '856121', '2026-02-08 13:57:43.601546', '$2a$10$fgfSAVsu1p1aJF.pUYef/uZhrJkA6qOR/8ZTZNhp2TJ0IIh9T4wdS', '+255712345678', NULL, NULL, NULL, 'USER', '2026-02-08 13:47:43.607327', NULL, NULL, NULL, NULL, NULL, 'PUBLIC', 'PUBLIC'),
(0xe56e92e29d434c72b898cfd2a65f0b7e, NULL, 'https://storage.wakilfy.com/covers/14b5d2a3-037f-4cdb-8ade-494fe8e38aeb.jpeg', '2026-02-08 21:09:25.696714', NULL, NULL, NULL, 'doniaparoma99@gmail.com', NULL, NULL, b'1', b'1', 'ibrahim ashiraf', NULL, NULL, '$2a$10$SRyZetfrBtIAuHMlYqVkTeMX5DDNDUBaOXwMhwkG//ug.alCOv64K', '+255628042409', 'https://storage.wakilfy.com/profile/0a6849e6-45ff-4103-9938-cccda7c7dc08.jpg', NULL, NULL, 'AGENT', '2026-02-10 23:10:04.154833', NULL, NULL, NULL, NULL, NULL, 'PUBLIC', 'PUBLIC'),
(0xf97655b1d77642efbd9514045a4e7270, NULL, NULL, '2026-02-11 01:42:00.314473', NULL, '2001-03-04', NULL, 'ibrahimzuberimfinanga@gmail.com', NULL, NULL, b'1', b'1', 'IBRAHIM Z MFINANGA', NULL, NULL, '$2a$10$8QcOPaGKN0OhhsiAejQ47eGo49kp0QHsF4pARFLXB4D1x3xxtfnFG', '+255779977578', NULL, NULL, NULL, 'USER', '2026-02-11 01:42:21.383263', NULL, NULL, 'Tanzania', 'Sports', 'Dar es Salaam', 'PUBLIC', 'PUBLIC');

-- --------------------------------------------------------

--
-- Table structure for table `user_archived_conversations`
--

CREATE TABLE `user_archived_conversations` (
  `id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `other_user_id` binary(16) NOT NULL,
  `archived_at` datetime(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user_blocks`
--

CREATE TABLE `user_blocks` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `blocked_id` binary(16) NOT NULL,
  `blocker_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user_cash_withdrawals`
--

CREATE TABLE `user_cash_withdrawals` (
  `id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `payment_method` varchar(50) DEFAULT NULL,
  `payment_phone` varchar(20) DEFAULT NULL,
  `payment_name` varchar(100) DEFAULT NULL,
  `status` enum('PENDING','PROCESSING','COMPLETED','FAILED','REJECTED') NOT NULL,
  `transaction_id` varchar(100) DEFAULT NULL,
  `rejection_reason` varchar(255) DEFAULT NULL,
  `processed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user_contact_hashes`
--

CREATE TABLE `user_contact_hashes` (
  `id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `contact_type` enum('PHONE','EMAIL') NOT NULL,
  `hash` varchar(64) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user_muted_notifications`
--

CREATE TABLE `user_muted_notifications` (
  `id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `muted_user_id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user_notification_settings`
--

CREATE TABLE `user_notification_settings` (
  `id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `type` enum('LIKE','COMMENT','SHARE','FRIEND_REQUEST','FRIEND_ACCEPT','FOLLOW','COMMUNITY_INVITE','SYSTEM') NOT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user_restrictions`
--

CREATE TABLE `user_restrictions` (
  `id` binary(16) NOT NULL,
  `restricter_id` binary(16) NOT NULL,
  `restricted_id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user_wallets`
--

CREATE TABLE `user_wallets` (
  `id` binary(16) NOT NULL,
  `cash_balance` decimal(15,2) DEFAULT NULL,
  `coin_balance` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `total_coins_purchased` int DEFAULT NULL,
  `total_coins_spent` int DEFAULT NULL,
  `total_gifts_received` decimal(15,2) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `user_wallets`
--

INSERT INTO `user_wallets` (`id`, `cash_balance`, `coin_balance`, `created_at`, `total_coins_purchased`, `total_coins_spent`, `total_gifts_received`, `updated_at`, `user_id`) VALUES
(0x48cf5b6516c74543a15b9ebb5a1b75e5, 0.00, 0, '2026-02-11 17:40:00.693390', 0, 0, 0.00, '2026-02-11 17:40:00.693422', 0x2cecbeb064364e24ab4c525f9d7e5585),
(0x52dc5e640c614639891b2e3ac2ed61e0, 0.00, 0, '2026-02-10 18:28:54.962478', 0, 0, 0.00, '2026-02-10 18:28:54.962508', 0x1a531d9d25a54266a87e10f0ae60e187);

-- --------------------------------------------------------

--
-- Table structure for table `virtual_gifts`
--

CREATE TABLE `virtual_gifts` (
  `id` binary(16) NOT NULL,
  `animation_url` varchar(255) DEFAULT NULL,
  `coin_value` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `icon_url` varchar(255) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `is_premium` bit(1) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `sort_order` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `virtual_gifts`
--

INSERT INTO `virtual_gifts` (`id`, `animation_url`, `coin_value`, `created_at`, `description`, `icon_url`, `is_active`, `is_premium`, `name`, `price`, `sort_order`) VALUES
(0x3ea07534650c4cae851a5ce165464836, NULL, 300, '2026-02-08 01:23:39.702364', 'Shine bright like a diamond', 'https://api.wakify.com/assets/gifts/diamond.png', b'1', b'1', 'Diamond', 10000.00, 4),
(0x988eca35496a4c39b67e07e39434f173, NULL, 150, '2026-02-08 01:23:39.700563', 'For the king/queen', 'https://api.wakify.com/assets/gifts/crown.png', b'1', b'1', 'Crown', 5000.00, 3),
(0xb2272c8640bc40dfb461d841c8a4180a, NULL, 15, '2026-02-08 01:23:39.698365', 'Love is in the air', 'https://api.wakify.com/assets/gifts/heart.png', b'1', b'0', 'Heart', 500.00, 2),
(0xcf2fc510d8ee4eff9ad058adccdf8c83, NULL, 5, '2026-02-08 01:23:39.681537', 'A beautiful red rose', 'https://api.wakify.com/assets/gifts/rose.png', b'1', b'0', 'Rose', 200.00, 1);

-- --------------------------------------------------------

--
-- Table structure for table `withdrawals`
--

CREATE TABLE `withdrawals` (
  `id` binary(16) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `notes` text,
  `payment_method` enum('MPESA','TIGOPESA','AIRTELMONEY','HALOPESA','BANK_TRANSFER','CARD') NOT NULL,
  `payment_name` varchar(255) DEFAULT NULL,
  `payment_phone` varchar(255) NOT NULL,
  `processed_at` datetime(6) DEFAULT NULL,
  `provider_response` text,
  `rejection_reason` varchar(255) DEFAULT NULL,
  `status` enum('PENDING','PROCESSING','COMPLETED','FAILED','REJECTED') NOT NULL,
  `transaction_id` varchar(255) DEFAULT NULL,
  `agent_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `ads`
--
ALTER TABLE `ads`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKqla30jfamq5w2lucnijvhg35` (`business_id`);

--
-- Indexes for table `agents`
--
ALTER TABLE `agents`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_ica2nnf6jymrt09bvuv7e4mai` (`user_id`),
  ADD UNIQUE KEY `UK_cu2bnqu9g7y26c0937qafj0hx` (`agent_code`);

--
-- Indexes for table `audit_logs`
--
ALTER TABLE `audit_logs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKjs4iimve3y0xssbtve5ysyef0` (`user_id`);

--
-- Indexes for table `auth_events`
--
ALTER TABLE `auth_events`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_auth_events_user_id` (`user_id`),
  ADD KEY `idx_auth_events_created_at` (`created_at`),
  ADD KEY `idx_auth_events_ip` (`ip_address`);

--
-- Indexes for table `businesses`
--
ALTER TABLE `businesses`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_apekaxbj4a9dsrurxxv03mfer` (`owner_id`),
  ADD KEY `FKqb177wmta0qqd1ucpx3ralafs` (`agent_id`);

--
-- Indexes for table `business_follows`
--
ALTER TABLE `business_follows`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_user_business` (`user_id`,`business_id`),
  ADD UNIQUE KEY `UKfn3qq5m0n6gh4fusf51nu2fyl` (`user_id`,`business_id`),
  ADD KEY `FKjqr4h5y8ghss8htuh3ijcaqbw` (`business_id`);

--
-- Indexes for table `calls`
--
ALTER TABLE `calls`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKd982s1pgb8m50bf9wvu08hpfk` (`caller_id`),
  ADD KEY `FKcolig0jgdegfjcx3hv5ticf3j` (`receiver_id`);

--
-- Indexes for table `carts`
--
ALTER TABLE `carts`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_64t7ox312pqal3p7fg9o503c2` (`user_id`);

--
-- Indexes for table `cart_items`
--
ALTER TABLE `cart_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKpcttvuq4mxppo8sxggjtn5i2c` (`cart_id`),
  ADD KEY `FK1re40cjegsfvw58xrkdp6bac6` (`product_id`);

--
-- Indexes for table `coin_packages`
--
ALTER TABLE `coin_packages`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `comments`
--
ALTER TABLE `comments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKn2na60ukhs76ibtpt9burkm27` (`author_id`),
  ADD KEY `FKlri30okf66phtcgbe5pok7cc0` (`parent_id`),
  ADD KEY `FKh4c7lvsc298whoyd4w9ta25cr` (`post_id`);

--
-- Indexes for table `comment_likes`
--
ALTER TABLE `comment_likes`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKgu1pee3567af29uutdfy0fcjd` (`comment_id`,`user_id`),
  ADD KEY `FK6h3lbneryl5pyb9ykaju7werx` (`user_id`);

--
-- Indexes for table `commissions`
--
ALTER TABLE `commissions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKd8v22tv0b7qw3b2r2eme5uidd` (`agent_id`),
  ADD KEY `FKltjjdssjptx4k2qvtvi5uamo5` (`business_id`);

--
-- Indexes for table `communities`
--
ALTER TABLE `communities`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKehds1lhi1y9a8rweslp1esncn` (`creator_id`);

--
-- Indexes for table `community_events`
--
ALTER TABLE `community_events`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKkehlu98g8i1kmaxn5fdtjra8b` (`community_id`),
  ADD KEY `FK6sgare84u5kxwvle0p355mh3` (`creator_id`);

--
-- Indexes for table `community_invites`
--
ALTER TABLE `community_invites`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_community_invitee` (`community_id`,`invitee_id`),
  ADD UNIQUE KEY `UK8ff8c4371stge56rhfi0wkqge` (`community_id`,`invitee_id`),
  ADD KEY `FK3ouhbbstqqr3px4e0840au8ip` (`invitee_id`),
  ADD KEY `FKdwiwgnbxj13naqt168n3cb8ml` (`inviter_id`);

--
-- Indexes for table `community_members`
--
ALTER TABLE `community_members`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKfr78n3lo0omh8or6dipyem6ii` (`community_id`,`user_id`),
  ADD KEY `FKme7k1stbnwi6cpmm8a6sgcikn` (`user_id`);

--
-- Indexes for table `community_polls`
--
ALTER TABLE `community_polls`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKh94fk11av968rgl8ib6xcmj68` (`community_id`),
  ADD KEY `FKh2w9gf4n75v9889n508cqtell` (`creator_id`);

--
-- Indexes for table `community_poll_options`
--
ALTER TABLE `community_poll_options`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKp2rb17fu6ujpx86aa66t08t3i` (`poll_id`);

--
-- Indexes for table `community_poll_votes`
--
ALTER TABLE `community_poll_votes`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_poll_user` (`poll_id`,`user_id`),
  ADD UNIQUE KEY `UKap0un4y88wgd8u0dcrl55xqpu` (`poll_id`,`user_id`),
  ADD KEY `FKo4xh8aox5x8i2tdhlhpdaaihj` (`option_id`),
  ADD KEY `FKgng8ywn7dwlm66ohnoxp02514` (`user_id`);

--
-- Indexes for table `conversations`
--
ALTER TABLE `conversations`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK_conversations_participant_one` (`participant_one_id`),
  ADD KEY `FK_conversations_participant_two` (`participant_two_id`);

--
-- Indexes for table `favorites`
--
ALTER TABLE `favorites`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKgh1s14hhb9qb8p2do933hscsf` (`user_id`,`product_id`),
  ADD KEY `FK6sgu5npe8ug4o42bf9j71x20c` (`product_id`);

--
-- Indexes for table `follows`
--
ALTER TABLE `follows`
  ADD PRIMARY KEY (`following_id`,`follower_id`),
  ADD KEY `FKqnkw0cwwh6572nyhvdjqlr163` (`follower_id`);

--
-- Indexes for table `friendships`
--
ALTER TABLE `friendships`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK6whjqep1tn7y88ds8ja3xtaey` (`requester_id`,`addressee_id`),
  ADD KEY `FKeq5r8dvxs43wkt7or9pdno9av` (`addressee_id`);

--
-- Indexes for table `gift_transactions`
--
ALTER TABLE `gift_transactions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK2ilbma6vb4s8aqpp1cyux79s2` (`gift_id`),
  ADD KEY `FKdmwtg483syhcx0i6kiu87ub22` (`live_stream_id`),
  ADD KEY `FK4suohwb1v2ra8x7919busnlin` (`receiver_id`),
  ADD KEY `FKt8cymgp0y0nd6ciw9al5dai6d` (`sender_id`);

--
-- Indexes for table `hashtags`
--
ALTER TABLE `hashtags`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKoed8qhhrhflqj7olh3oeii6ym` (`name`);

--
-- Indexes for table `live_streams`
--
ALTER TABLE `live_streams`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_nt5tudetduucnjgvl9vgmh7n5` (`room_id`),
  ADD KEY `FK9i9n9pbop6auin3iunmajsh0r` (`host_id`);

--
-- Indexes for table `live_stream_comments`
--
ALTER TABLE `live_stream_comments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_live_stream_id` (`live_stream_id`),
  ADD KEY `idx_author_id` (`author_id`);

--
-- Indexes for table `live_stream_join_requests`
--
ALTER TABLE `live_stream_join_requests`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_live_stream_requester` (`live_stream_id`,`requester_id`),
  ADD UNIQUE KEY `UKgsk1o69s946m56f2hkfwvx6d` (`live_stream_id`,`requester_id`),
  ADD KEY `FK_join_request_live_stream` (`live_stream_id`),
  ADD KEY `FK_join_request_requester` (`requester_id`);

--
-- Indexes for table `messages`
--
ALTER TABLE `messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKhdkwfnspwb3s60j27vpg0rpg6` (`recipient_id`),
  ADD KEY `FK4ui4nnwntodh6wjvck53dbk9m` (`sender_id`);

--
-- Indexes for table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK4sd9fik0uthbk6d9rsxco4uja` (`actor_id`),
  ADD KEY `FKqqnsjxlwleyjbxlmm213jaj3f` (`recipient_id`);

--
-- Indexes for table `orders`
--
ALTER TABLE `orders`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_nthkiu7pgmnqnu86i2jyoe2v7` (`order_number`),
  ADD KEY `FKqac135o6f6wifn3tlfkdgmxey` (`business_id`),
  ADD KEY `FKhtx3insd5ge6w486omk4fnk54` (`buyer_id`);

--
-- Indexes for table `order_items`
--
ALTER TABLE `order_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKbioxgbv59vetrxe0ejfubep1w` (`order_id`),
  ADD KEY `FKocimc7dtr037rh4ls4l95nlfi` (`product_id`);

--
-- Indexes for table `payments`
--
ALTER TABLE `payments`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_lryndveuwa4k5qthti0pkmtlx` (`transaction_id`),
  ADD KEY `FKj94hgy9v5fw1munb90tar2eje` (`user_id`);

--
-- Indexes for table `posts`
--
ALTER TABLE `posts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK6xvn0811tkyo3nfjk2xvqx6ns` (`author_id`),
  ADD KEY `FK7rk45ficmsfhe8n1dojvqt6ui` (`community_id`),
  ADD KEY `FK9ghkwednnii55uyx3f4j7o9fg` (`original_post_id`);

--
-- Indexes for table `post_hashtags`
--
ALTER TABLE `post_hashtags`
  ADD PRIMARY KEY (`post_id`,`hashtag_id`),
  ADD KEY `FKb8j4xx456a7584d8blc604pqg` (`hashtag_id`);

--
-- Indexes for table `post_likes`
--
ALTER TABLE `post_likes`
  ADD PRIMARY KEY (`post_id`,`user_id`),
  ADD KEY `FKkgau5n0nlewg6o9lr4yibqgxj` (`user_id`);

--
-- Indexes for table `post_media`
--
ALTER TABLE `post_media`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK1urcum9dtf0vgul7k405f4r2d` (`post_id`);

--
-- Indexes for table `post_product_tags`
--
ALTER TABLE `post_product_tags`
  ADD PRIMARY KEY (`post_id`,`product_id`),
  ADD KEY `FK7wcmkafchsrffveq0jw9aimgi` (`product_id`);

--
-- Indexes for table `post_reactions`
--
ALTER TABLE `post_reactions`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKqx1p0wtweq8mgrtenw93jdqns` (`post_id`,`user_id`),
  ADD KEY `FKptar8f3u0qt7ssjksu2hxme03` (`user_id`);

--
-- Indexes for table `post_tagged_users`
--
ALTER TABLE `post_tagged_users`
  ADD PRIMARY KEY (`post_id`,`user_id`),
  ADD KEY `FK8395p16bg7yhka3t1w851ugy6` (`user_id`);

--
-- Indexes for table `products`
--
ALTER TABLE `products`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKi65q12p725kxcn1jcosg2x9y4` (`business_id`);

--
-- Indexes for table `product_images`
--
ALTER TABLE `product_images`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKqnq71xsohugpqwf3c9gxmsuy` (`product_id`);

--
-- Indexes for table `product_reviews`
--
ALTER TABLE `product_reviews`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK35kxxqe2g9r4mww80w9e3tnw9` (`product_id`),
  ADD KEY `FK58i39bhws2hss3tbcvdmrm60f` (`user_id`);

--
-- Indexes for table `promotions`
--
ALTER TABLE `promotions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKfpdo2s6bpv0jff255qnvb8y4f` (`business_id`),
  ADD KEY `FKa01xhxywwnp12j0dbes5ibokt` (`user_id`);

--
-- Indexes for table `promotion_packages`
--
ALTER TABLE `promotion_packages`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `reports`
--
ALTER TABLE `reports`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKd3qiw2om5d2oh5xb7fbdcq225` (`reporter_id`),
  ADD KEY `FKru383gq6s2hvqp6jf4q3itays` (`resolved_by`);

--
-- Indexes for table `saved_posts`
--
ALTER TABLE `saved_posts`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKrp4caf9aruyad4113wv29bowp` (`user_id`,`post_id`),
  ADD KEY `FK9poxgdc1595vxdxkyg202x4ge` (`post_id`);

--
-- Indexes for table `story_views`
--
ALTER TABLE `story_views`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKqipvffnu5ftwn5e5vecpx6xgv` (`viewer_id`,`post_id`),
  ADD KEY `FKp7sni7n1627qlk1b3i3prh0j1` (`post_id`);

--
-- Indexes for table `subscriptions`
--
ALTER TABLE `subscriptions`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_r13wticerot66fejnp4s310f3` (`business_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_du5v5sr43g5bfnji4vb8hg5s3` (`phone`),
  ADD UNIQUE KEY `UK_6dotkott2kjsp8vw4d0m25fb7` (`email`);

--
-- Indexes for table `user_archived_conversations`
--
ALTER TABLE `user_archived_conversations`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_user_other` (`user_id`,`other_user_id`),
  ADD UNIQUE KEY `UKokqev6sg7m89x6d9gxox6inch` (`user_id`,`other_user_id`),
  ADD KEY `FKlmxqqk24bgf9803mr0u9ywnw9` (`other_user_id`);

--
-- Indexes for table `user_blocks`
--
ALTER TABLE `user_blocks`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK6kwyqs53ciqfxmlhquyq5socr` (`blocker_id`,`blocked_id`),
  ADD KEY `FK7k3mfgb03bnmwh81vqb1u5h80` (`blocked_id`);

--
-- Indexes for table `user_cash_withdrawals`
--
ALTER TABLE `user_cash_withdrawals`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_user_id` (`user_id`),
  ADD KEY `idx_status` (`status`);

--
-- Indexes for table `user_contact_hashes`
--
ALTER TABLE `user_contact_hashes`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_user_contact` (`user_id`,`contact_type`,`hash`),
  ADD UNIQUE KEY `UKafyi5txv3jgq6wredr0s6v29s` (`user_id`,`contact_type`,`hash`),
  ADD KEY `idx_user_id` (`user_id`),
  ADD KEY `idx_hash` (`hash`);

--
-- Indexes for table `user_muted_notifications`
--
ALTER TABLE `user_muted_notifications`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_user_muted` (`user_id`,`muted_user_id`),
  ADD UNIQUE KEY `UKc0ba568ek6ngl11d4o3hk1um5` (`user_id`,`muted_user_id`),
  ADD KEY `FKtmln0gryhi1k85e3m7mtyw813` (`muted_user_id`);

--
-- Indexes for table `user_notification_settings`
--
ALTER TABLE `user_notification_settings`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_user_type` (`user_id`,`type`),
  ADD UNIQUE KEY `UK8as16bwtxsa9opm9e1la6st4y` (`user_id`,`type`);

--
-- Indexes for table `user_restrictions`
--
ALTER TABLE `user_restrictions`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_restricter_restricted` (`restricter_id`,`restricted_id`),
  ADD UNIQUE KEY `UKtems4b92x4xtompsmtf4897fu` (`restricter_id`,`restricted_id`),
  ADD KEY `FKn8y390p621ytr5urjv9hsjrgd` (`restricted_id`);

--
-- Indexes for table `user_wallets`
--
ALTER TABLE `user_wallets`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_blgu1m2imxhvys2sy1a0d7ut7` (`user_id`);

--
-- Indexes for table `virtual_gifts`
--
ALTER TABLE `virtual_gifts`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_er32qy3a2d7vc3ioy3e423ynl` (`name`);

--
-- Indexes for table `withdrawals`
--
ALTER TABLE `withdrawals`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKocp650yecg6tnnypb5qtcclok` (`agent_id`);

--
-- Constraints for dumped tables
--

--
-- Constraints for table `ads`
--
ALTER TABLE `ads`
  ADD CONSTRAINT `FKqla30jfamq5w2lucnijvhg35` FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`);

--
-- Constraints for table `agents`
--
ALTER TABLE `agents`
  ADD CONSTRAINT `FK2vh8rg4inh3scgcguimya35my` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `audit_logs`
--
ALTER TABLE `audit_logs`
  ADD CONSTRAINT `FKjs4iimve3y0xssbtve5ysyef0` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `auth_events`
--
ALTER TABLE `auth_events`
  ADD CONSTRAINT `FK3s4gjagbegfvemg8s22jk25cn` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `businesses`
--
ALTER TABLE `businesses`
  ADD CONSTRAINT `FKdh1y7wew1fqwy531d5ojohod5` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKqb177wmta0qqd1ucpx3ralafs` FOREIGN KEY (`agent_id`) REFERENCES `agents` (`id`);

--
-- Constraints for table `business_follows`
--
ALTER TABLE `business_follows`
  ADD CONSTRAINT `FKjqr4h5y8ghss8htuh3ijcaqbw` FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`),
  ADD CONSTRAINT `FKtq2pleng0ykfd0xtdw1ux3ieo` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `calls`
--
ALTER TABLE `calls`
  ADD CONSTRAINT `FKcolig0jgdegfjcx3hv5ticf3j` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKd982s1pgb8m50bf9wvu08hpfk` FOREIGN KEY (`caller_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `carts`
--
ALTER TABLE `carts`
  ADD CONSTRAINT `FKb5o626f86h46m4s7ms6ginnop` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `cart_items`
--
ALTER TABLE `cart_items`
  ADD CONSTRAINT `FK1re40cjegsfvw58xrkdp6bac6` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  ADD CONSTRAINT `FKpcttvuq4mxppo8sxggjtn5i2c` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`id`);

--
-- Constraints for table `comments`
--
ALTER TABLE `comments`
  ADD CONSTRAINT `FKh4c7lvsc298whoyd4w9ta25cr` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`),
  ADD CONSTRAINT `FKlri30okf66phtcgbe5pok7cc0` FOREIGN KEY (`parent_id`) REFERENCES `comments` (`id`),
  ADD CONSTRAINT `FKn2na60ukhs76ibtpt9burkm27` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `comment_likes`
--
ALTER TABLE `comment_likes`
  ADD CONSTRAINT `FK3wa5u7bs1p1o9hmavtgdgk1go` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`),
  ADD CONSTRAINT `FK6h3lbneryl5pyb9ykaju7werx` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `commissions`
--
ALTER TABLE `commissions`
  ADD CONSTRAINT `FKd8v22tv0b7qw3b2r2eme5uidd` FOREIGN KEY (`agent_id`) REFERENCES `agents` (`id`),
  ADD CONSTRAINT `FKltjjdssjptx4k2qvtvi5uamo5` FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`);

--
-- Constraints for table `communities`
--
ALTER TABLE `communities`
  ADD CONSTRAINT `FKehds1lhi1y9a8rweslp1esncn` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `community_events`
--
ALTER TABLE `community_events`
  ADD CONSTRAINT `FK6sgare84u5kxwvle0p355mh3` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKkehlu98g8i1kmaxn5fdtjra8b` FOREIGN KEY (`community_id`) REFERENCES `communities` (`id`);

--
-- Constraints for table `community_invites`
--
ALTER TABLE `community_invites`
  ADD CONSTRAINT `FK3ouhbbstqqr3px4e0840au8ip` FOREIGN KEY (`invitee_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKdwiwgnbxj13naqt168n3cb8ml` FOREIGN KEY (`inviter_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKh165prdkpw4u4m6dxcobjhadr` FOREIGN KEY (`community_id`) REFERENCES `communities` (`id`);

--
-- Constraints for table `community_members`
--
ALTER TABLE `community_members`
  ADD CONSTRAINT `FKme7k1stbnwi6cpmm8a6sgcikn` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKqn9g17tqcwnoy41o2am9fnlep` FOREIGN KEY (`community_id`) REFERENCES `communities` (`id`);

--
-- Constraints for table `community_polls`
--
ALTER TABLE `community_polls`
  ADD CONSTRAINT `FKh2w9gf4n75v9889n508cqtell` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKh94fk11av968rgl8ib6xcmj68` FOREIGN KEY (`community_id`) REFERENCES `communities` (`id`);

--
-- Constraints for table `community_poll_options`
--
ALTER TABLE `community_poll_options`
  ADD CONSTRAINT `FKp2rb17fu6ujpx86aa66t08t3i` FOREIGN KEY (`poll_id`) REFERENCES `community_polls` (`id`);

--
-- Constraints for table `community_poll_votes`
--
ALTER TABLE `community_poll_votes`
  ADD CONSTRAINT `FKe6f1wjtdlb3jttyxlxiv0e4m1` FOREIGN KEY (`poll_id`) REFERENCES `community_polls` (`id`),
  ADD CONSTRAINT `FKgng8ywn7dwlm66ohnoxp02514` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKo4xh8aox5x8i2tdhlhpdaaihj` FOREIGN KEY (`option_id`) REFERENCES `community_poll_options` (`id`);

--
-- Constraints for table `conversations`
--
ALTER TABLE `conversations`
  ADD CONSTRAINT `FK_conversations_participant_one` FOREIGN KEY (`participant_one_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_conversations_participant_two` FOREIGN KEY (`participant_two_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `favorites`
--
ALTER TABLE `favorites`
  ADD CONSTRAINT `FK6sgu5npe8ug4o42bf9j71x20c` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  ADD CONSTRAINT `FKk7du8b8ewipawnnpg76d55fus` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `follows`
--
ALTER TABLE `follows`
  ADD CONSTRAINT `FKonkdkae2ngtx70jqhsh7ol6uq` FOREIGN KEY (`following_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKqnkw0cwwh6572nyhvdjqlr163` FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `friendships`
--
ALTER TABLE `friendships`
  ADD CONSTRAINT `FKas6bp8so5n3pfcqtfxt72e1ii` FOREIGN KEY (`requester_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKeq5r8dvxs43wkt7or9pdno9av` FOREIGN KEY (`addressee_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `gift_transactions`
--
ALTER TABLE `gift_transactions`
  ADD CONSTRAINT `FK2ilbma6vb4s8aqpp1cyux79s2` FOREIGN KEY (`gift_id`) REFERENCES `virtual_gifts` (`id`),
  ADD CONSTRAINT `FK4suohwb1v2ra8x7919busnlin` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKdmwtg483syhcx0i6kiu87ub22` FOREIGN KEY (`live_stream_id`) REFERENCES `live_streams` (`id`),
  ADD CONSTRAINT `FKt8cymgp0y0nd6ciw9al5dai6d` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `live_streams`
--
ALTER TABLE `live_streams`
  ADD CONSTRAINT `FK9i9n9pbop6auin3iunmajsh0r` FOREIGN KEY (`host_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `live_stream_comments`
--
ALTER TABLE `live_stream_comments`
  ADD CONSTRAINT `FK2d68xqql2xfx23s766mfrx773` FOREIGN KEY (`live_stream_id`) REFERENCES `live_streams` (`id`),
  ADD CONSTRAINT `FKowyheh97hqad1xe5jk47w03d7` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `live_stream_join_requests`
--
ALTER TABLE `live_stream_join_requests`
  ADD CONSTRAINT `FK_join_request_live_stream` FOREIGN KEY (`live_stream_id`) REFERENCES `live_streams` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_join_request_requester` FOREIGN KEY (`requester_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `messages`
--
ALTER TABLE `messages`
  ADD CONSTRAINT `FK4ui4nnwntodh6wjvck53dbk9m` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKhdkwfnspwb3s60j27vpg0rpg6` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `FK4sd9fik0uthbk6d9rsxco4uja` FOREIGN KEY (`actor_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKqqnsjxlwleyjbxlmm213jaj3f` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `orders`
--
ALTER TABLE `orders`
  ADD CONSTRAINT `FKhtx3insd5ge6w486omk4fnk54` FOREIGN KEY (`buyer_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKqac135o6f6wifn3tlfkdgmxey` FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`);

--
-- Constraints for table `order_items`
--
ALTER TABLE `order_items`
  ADD CONSTRAINT `FKbioxgbv59vetrxe0ejfubep1w` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  ADD CONSTRAINT `FKocimc7dtr037rh4ls4l95nlfi` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`);

--
-- Constraints for table `payments`
--
ALTER TABLE `payments`
  ADD CONSTRAINT `FKj94hgy9v5fw1munb90tar2eje` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `posts`
--
ALTER TABLE `posts`
  ADD CONSTRAINT `FK6xvn0811tkyo3nfjk2xvqx6ns` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FK7rk45ficmsfhe8n1dojvqt6ui` FOREIGN KEY (`community_id`) REFERENCES `communities` (`id`),
  ADD CONSTRAINT `FK9ghkwednnii55uyx3f4j7o9fg` FOREIGN KEY (`original_post_id`) REFERENCES `posts` (`id`);

--
-- Constraints for table `post_hashtags`
--
ALTER TABLE `post_hashtags`
  ADD CONSTRAINT `FKb8j4xx456a7584d8blc604pqg` FOREIGN KEY (`hashtag_id`) REFERENCES `hashtags` (`id`),
  ADD CONSTRAINT `FKrrlq793bvaswhomm900i71ac5` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`);

--
-- Constraints for table `post_likes`
--
ALTER TABLE `post_likes`
  ADD CONSTRAINT `FKa5wxsgl4doibhbed9gm7ikie2` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`),
  ADD CONSTRAINT `FKkgau5n0nlewg6o9lr4yibqgxj` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `post_media`
--
ALTER TABLE `post_media`
  ADD CONSTRAINT `FK1urcum9dtf0vgul7k405f4r2d` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`);

--
-- Constraints for table `post_product_tags`
--
ALTER TABLE `post_product_tags`
  ADD CONSTRAINT `FK7wcmkafchsrffveq0jw9aimgi` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  ADD CONSTRAINT `FKkld089tnqj2ml22geqnbt4psa` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`);

--
-- Constraints for table `post_reactions`
--
ALTER TABLE `post_reactions`
  ADD CONSTRAINT `FKptar8f3u0qt7ssjksu2hxme03` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKq9ivjiqt8flog43og7gtmoyqw` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`);

--
-- Constraints for table `post_tagged_users`
--
ALTER TABLE `post_tagged_users`
  ADD CONSTRAINT `FK8395p16bg7yhka3t1w851ugy6` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKn9yo1lweapbbkqkyyd8yctf59` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`);

--
-- Constraints for table `products`
--
ALTER TABLE `products`
  ADD CONSTRAINT `FKi65q12p725kxcn1jcosg2x9y4` FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`);

--
-- Constraints for table `product_images`
--
ALTER TABLE `product_images`
  ADD CONSTRAINT `FKqnq71xsohugpqwf3c9gxmsuy` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`);

--
-- Constraints for table `product_reviews`
--
ALTER TABLE `product_reviews`
  ADD CONSTRAINT `FK35kxxqe2g9r4mww80w9e3tnw9` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  ADD CONSTRAINT `FK58i39bhws2hss3tbcvdmrm60f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `promotions`
--
ALTER TABLE `promotions`
  ADD CONSTRAINT `FKa01xhxywwnp12j0dbes5ibokt` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKfpdo2s6bpv0jff255qnvb8y4f` FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`);

--
-- Constraints for table `reports`
--
ALTER TABLE `reports`
  ADD CONSTRAINT `FKd3qiw2om5d2oh5xb7fbdcq225` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKru383gq6s2hvqp6jf4q3itays` FOREIGN KEY (`resolved_by`) REFERENCES `users` (`id`);

--
-- Constraints for table `saved_posts`
--
ALTER TABLE `saved_posts`
  ADD CONSTRAINT `FK9poxgdc1595vxdxkyg202x4ge` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`),
  ADD CONSTRAINT `FKs9a5ulcshnympbu557ps3qdlv` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `story_views`
--
ALTER TABLE `story_views`
  ADD CONSTRAINT `FK2y4acyhro3w5xrmku47osiry9` FOREIGN KEY (`viewer_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKp7sni7n1627qlk1b3i3prh0j1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`);

--
-- Constraints for table `subscriptions`
--
ALTER TABLE `subscriptions`
  ADD CONSTRAINT `FKd75uwj5b3erhwwt5flxnevr7o` FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`);

--
-- Constraints for table `user_archived_conversations`
--
ALTER TABLE `user_archived_conversations`
  ADD CONSTRAINT `FKgmeoueqicj8s3rs2tr78xh2xk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKlmxqqk24bgf9803mr0u9ywnw9` FOREIGN KEY (`other_user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `user_blocks`
--
ALTER TABLE `user_blocks`
  ADD CONSTRAINT `FK7k3mfgb03bnmwh81vqb1u5h80` FOREIGN KEY (`blocked_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKgvu85oyjrfafwttb7iphgmm0v` FOREIGN KEY (`blocker_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `user_cash_withdrawals`
--
ALTER TABLE `user_cash_withdrawals`
  ADD CONSTRAINT `FK7fvnmo420f9wulthryd7jlw08` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `user_muted_notifications`
--
ALTER TABLE `user_muted_notifications`
  ADD CONSTRAINT `FKlhr4c6kmg508l5r432bpf3cl4` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKtmln0gryhi1k85e3m7mtyw813` FOREIGN KEY (`muted_user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `user_notification_settings`
--
ALTER TABLE `user_notification_settings`
  ADD CONSTRAINT `FKs9tjvxu8ko31ivjlq9l9duh9y` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `user_restrictions`
--
ALTER TABLE `user_restrictions`
  ADD CONSTRAINT `FKn8y390p621ytr5urjv9hsjrgd` FOREIGN KEY (`restricted_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKra5rhs68xtur2r3ausb0s33jn` FOREIGN KEY (`restricter_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `user_wallets`
--
ALTER TABLE `user_wallets`
  ADD CONSTRAINT `FK423n8ap6gdudl8fcab7ugv3qt` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `withdrawals`
--
ALTER TABLE `withdrawals`
  ADD CONSTRAINT `FKocp650yecg6tnnypb5qtcclok` FOREIGN KEY (`agent_id`) REFERENCES `agents` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;