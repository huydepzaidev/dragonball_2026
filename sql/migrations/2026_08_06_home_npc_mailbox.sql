-- Move mailbox access to the three home mentors and remove Bo Mong from home maps.
-- Also creates the shared web/game mailbox and Top 1-3 command tables.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `map_template`
SET `npcs`=CASE
    WHEN TRIM(`npcs`)='[[17,573,336]]' THEN '[]'
    ELSE REPLACE(REPLACE(`npcs`,',[17,573,336]',''),'[17,573,336],','')
END
WHERE `id`=21 AND `npcs` LIKE '%[17,573,336]%';

UPDATE `map_template`
SET `npcs`=CASE
    WHEN TRIM(`npcs`)='[[17,105,336]]' THEN '[]'
    ELSE REPLACE(REPLACE(`npcs`,',[17,105,336]',''),'[17,105,336],','')
END
WHERE `id`=22 AND `npcs` LIKE '%[17,105,336]%';

UPDATE `map_template`
SET `npcs`=CASE
    WHEN TRIM(`npcs`)='[[17,570,336]]' THEN '[]'
    ELSE REPLACE(REPLACE(`npcs`,',[17,570,336]',''),'[17,570,336],','')
END
WHERE `id`=23 AND `npcs` LIKE '%[17,570,336]%';

CREATE TABLE IF NOT EXISTS `top_reward_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `ranking_key` varchar(40) NOT NULL,
  `rank_position` tinyint unsigned NOT NULL,
  `title` varchar(120) NOT NULL,
  `message` varchar(500) NOT NULL DEFAULT '',
  `sender_name` varchar(50) NOT NULL DEFAULT 'Admin',
  `rewards_json` longtext NOT NULL,
  `updated_by` int DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_top_reward_config_rank` (`ranking_key`,`rank_position`),
  CONSTRAINT `chk_top_reward_config_rank` CHECK (`rank_position` BETWEEN 1 AND 3),
  CONSTRAINT `chk_top_reward_config_json` CHECK (JSON_VALID(`rewards_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `top_reward_command` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `ranking_key` varchar(40) NOT NULL,
  `batch_key` varchar(80) NOT NULL,
  `batch_title` varchar(120) NOT NULL,
  `requested_by` int DEFAULT NULL,
  `requested_by_name` varchar(20) NOT NULL,
  `status` enum('PENDING','PROCESSING','DONE','FAILED') NOT NULL DEFAULT 'PENDING',
  `config_snapshot_json` longtext DEFAULT NULL,
  `result_message` varchar(500) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_top_reward_command_batch` (`ranking_key`,`batch_key`),
  KEY `idx_top_reward_command_queue` (`status`,`id`),
  CONSTRAINT `chk_top_reward_command_snapshot` CHECK (`config_snapshot_json` IS NULL OR JSON_VALID(`config_snapshot_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `player_mailbox` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `account_id` int NOT NULL,
  `player_id` int NOT NULL,
  `title` varchar(120) NOT NULL,
  `message` varchar(500) NOT NULL DEFAULT '',
  `sender_name` varchar(50) NOT NULL DEFAULT 'Admin',
  `rank_position` tinyint unsigned DEFAULT NULL,
  `rewards_json` longtext NOT NULL,
  `status` enum('PENDING','PROCESSING','CLAIMED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  `source_command_id` bigint unsigned DEFAULT NULL,
  `created_by` int DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `processing_at` datetime DEFAULT NULL,
  `claimed_at` datetime DEFAULT NULL,
  `cancelled_at` datetime DEFAULT NULL,
  `failure_reason` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_player_mailbox_command_rank` (`source_command_id`,`rank_position`),
  KEY `idx_player_mailbox_recipient` (`player_id`,`account_id`,`status`,`id`),
  KEY `idx_player_mailbox_status` (`status`,`id`),
  CONSTRAINT `fk_player_mailbox_command` FOREIGN KEY (`source_command_id`) REFERENCES `top_reward_command` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_player_mailbox_rank` CHECK (`rank_position` IS NULL OR `rank_position` BETWEEN 1 AND 3),
  CONSTRAINT `chk_player_mailbox_rewards` CHECK (JSON_VALID(`rewards_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `top_reward_winner` (
  `command_id` bigint unsigned NOT NULL,
  `rank_position` tinyint unsigned NOT NULL,
  `account_id` int NOT NULL,
  `player_id` int NOT NULL,
  `player_name` varchar(20) NOT NULL,
  `score` decimal(30,0) unsigned NOT NULL DEFAULT 0,
  `mailbox_id` bigint unsigned NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`command_id`,`rank_position`),
  UNIQUE KEY `uq_top_reward_winner_mailbox` (`mailbox_id`),
  KEY `idx_top_reward_winner_player` (`player_id`,`created_at`),
  CONSTRAINT `fk_top_reward_winner_command` FOREIGN KEY (`command_id`) REFERENCES `top_reward_command` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_top_reward_winner_mailbox` FOREIGN KEY (`mailbox_id`) REFERENCES `player_mailbox` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_top_reward_winner_rank` CHECK (`rank_position` BETWEEN 1 AND 3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `activation_reward_config` (
  `planet` tinyint NOT NULL,
  `planet_name` varchar(30) NOT NULL,
  `activation_options_json` longtext NOT NULL,
  `activation_weights_json` longtext NOT NULL,
  `bonus_options_json` longtext NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `updated_by` int DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`planet`),
  CONSTRAINT `chk_activation_reward_planet` CHECK (`planet` BETWEEN 0 AND 2),
  CONSTRAINT `chk_activation_reward_options` CHECK (JSON_VALID(`activation_options_json`)),
  CONSTRAINT `chk_activation_reward_weights` CHECK (JSON_VALID(`activation_weights_json`)),
  CONSTRAINT `chk_activation_reward_bonus` CHECK (JSON_VALID(`bonus_options_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

COMMIT;
