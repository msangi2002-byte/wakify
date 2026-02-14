-- Migration: Add agent_package_id column to agents table
-- This allows agents to be associated with agent packages
-- Run this script on your existing database to add the package relationship

-- Step 1: Add the agent_package_id column to the agents table
-- (Skip this if the column already exists)
ALTER TABLE `agents`
  ADD COLUMN `agent_package_id` binary(16) DEFAULT NULL AFTER `user_id`;

-- Step 2: Add foreign key constraint (this will automatically create an index)
-- (Skip this if the constraint already exists)
ALTER TABLE `agents`
  ADD CONSTRAINT `FK_agent_package` FOREIGN KEY (`agent_package_id`) REFERENCES `agent_packages` (`id`);

-- Verification queries (optional - run these to check):
-- Check if column exists: SHOW COLUMNS FROM `agents` LIKE 'agent_package_id';
-- Check if constraint exists: SELECT * FROM information_schema.KEY_COLUMN_USAGE 
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agents' AND CONSTRAINT_NAME = 'FK_agent_package';
-- Check if agent_packages table exists: SHOW TABLES LIKE 'agent_packages';

-- If you need to remove the column (rollback):
-- ALTER TABLE `agents` DROP FOREIGN KEY `FK_agent_package`;
-- ALTER TABLE `agents` DROP COLUMN `agent_package_id`;
