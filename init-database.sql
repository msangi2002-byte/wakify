-- ============================================
-- WAKILFLY DATABASE INITIALIZATION SCRIPT
-- ============================================
-- Run this script to create the database
-- mysql -u root -p < init-database.sql
-- ============================================

-- Create database
CREATE DATABASE IF NOT EXISTS wakilfly_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Use the database
USE wakilfly_db;

-- ============================================
-- NOTE: Tables will be auto-created by JPA/Hibernate
-- with spring.jpa.hibernate.ddl-auto=update
-- ============================================

-- But here's the complete schema for reference:

-- ============================================
-- USERS TABLE
-- ============================================
-- CREATE TABLE IF NOT EXISTS users (
--     id BINARY(16) PRIMARY KEY,
--     name VARCHAR(255) NOT NULL,
--     email VARCHAR(255) UNIQUE,
--     phone VARCHAR(20) NOT NULL UNIQUE,
--     password VARCHAR(255) NOT NULL,
--     bio TEXT,
--     profile_pic VARCHAR(500),
--     cover_pic VARCHAR(500),
--     role ENUM('VISITOR', 'USER', 'BUSINESS', 'AGENT', 'ADMIN') DEFAULT 'USER',
--     is_verified BOOLEAN DEFAULT FALSE,
--     is_active BOOLEAN DEFAULT TRUE,
--     otp_code VARCHAR(10),
--     otp_expires_at DATETIME,
--     created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
--     updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
-- );

-- ============================================
-- Create Admin User (Optional - for testing)
-- Password: admin123 (BCrypt hashed)
-- ============================================
-- INSERT INTO users (id, name, email, phone, password, role, is_verified, is_active, created_at, updated_at)
-- VALUES (
--     UUID_TO_BIN(UUID()),
--     'Admin User',
--     'admin@wakilfly.com',
--     '+255700000000',
--     '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', -- admin123
--     'ADMIN',
--     TRUE,
--     TRUE,
--     NOW(),
--     NOW()
-- );

SELECT 'Database wakilfly_db created successfully!' AS Status;
