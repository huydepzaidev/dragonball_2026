-- Migration: 2026_08_30_1430_expand_account_password
-- Description: Expand account.password column to VARCHAR(255) for PBKDF2 hash storage
-- Safe to run multiple times.

SET NAMES utf8mb4;

SET @col_len = (
  SELECT CHARACTER_MAXIMUM_LENGTH
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'account'
    AND COLUMN_NAME = 'password'
);

SET @sql = IF(@col_len < 255, 'ALTER TABLE `account` MODIFY COLUMN `password` VARCHAR(255) NOT NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
