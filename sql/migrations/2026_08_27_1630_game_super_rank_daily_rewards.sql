-- Migration: 2026_08_27_1630_game_super_rank_daily_rewards
-- Description: Super rank daily rewards tables and player mailbox idempotency key

CREATE TABLE IF NOT EXISTS `super_rank_reward_cycle` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `reward_date` date NOT NULL,
  `snapshot_at` datetime NOT NULL,
  `status` enum('SNAPSHOT_TAKING','SNAPSHOT_TAKEN','DELIVERING','CLOSED_SNAPSHOT','FAILED') NOT NULL DEFAULT 'SNAPSHOT_TAKING',
  `snapshot_count` int DEFAULT NULL,
  `snapshot_checksum` varchar(64) DEFAULT NULL,
  `total_winners` int NOT NULL DEFAULT 0,
  `processed_winners` int NOT NULL DEFAULT 0,
  `attempts` int NOT NULL DEFAULT 1,
  `last_attempt_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `error_message` varchar(500) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_super_rank_cycle_date` (`reward_date`),
  KEY `idx_super_rank_cycle_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `super_rank_reward_ledger` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `cycle_id` bigint unsigned NOT NULL,
  `reward_date` date NOT NULL,
  `player_id` int NOT NULL,
  `account_id` int NOT NULL,
  `rank_position` int NOT NULL,
  `ruby_reward` int NOT NULL,
  `ruby_granted` int NOT NULL DEFAULT 0,
  `ruby_status` enum('PENDING','CLAIMED','FAILED') NOT NULL DEFAULT 'PENDING',
  `ruby_claimed_at` datetime DEFAULT NULL,
  `badge_status` enum('NONE','PENDING','CLAIMED') NOT NULL DEFAULT 'NONE',
  `badge_claimed_at` datetime DEFAULT NULL,
  `mailbox_id` bigint unsigned DEFAULT NULL,
  `mailbox_status` enum('NONE','PENDING','SENT','FAILED') NOT NULL DEFAULT 'NONE',
  `error_message` varchar(500) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_super_rank_ledger_date_player` (`reward_date`,`player_id`),
  KEY `idx_super_rank_ledger_claim` (`player_id`,`ruby_status`),
  KEY `idx_super_rank_ledger_cycle` (`cycle_id`,`rank_position`),
  CONSTRAINT `fk_super_rank_ledger_cycle` FOREIGN KEY (`cycle_id`) REFERENCES `super_rank_reward_cycle` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'player_mailbox' AND COLUMN_NAME = 'idempotency_key');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `player_mailbox` ADD COLUMN `idempotency_key` VARCHAR(100) DEFAULT NULL AFTER `source_command_id`, ADD UNIQUE KEY `uq_player_mailbox_idempotency` (`idempotency_key`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
