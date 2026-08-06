-- Expand automatic Top rewards to ranks 1-10 and persist the exact reward period.
-- Safe to run more than once on MariaDB 10.4+.
SET NAMES utf8mb4;

ALTER TABLE `top_reward_command`
  ADD COLUMN IF NOT EXISTS `period_type` enum('LIFETIME','WEEKLY','MANUAL') NOT NULL DEFAULT 'MANUAL' AFTER `ranking_key`,
  ADD COLUMN IF NOT EXISTS `ranking_date` date DEFAULT NULL AFTER `batch_title`;

SET @sql_drop_top_reward_config_rank = (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `top_reward_config` DROP CONSTRAINT `chk_top_reward_config_rank`',
    'DO 0')
  FROM information_schema.table_constraints
  WHERE constraint_schema=DATABASE()
    AND table_name='top_reward_config'
    AND constraint_name='chk_top_reward_config_rank'
);
PREPARE stmt_drop_top_reward_config_rank FROM @sql_drop_top_reward_config_rank;
EXECUTE stmt_drop_top_reward_config_rank;
DEALLOCATE PREPARE stmt_drop_top_reward_config_rank;

SET @sql_add_top_reward_config_rank = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `top_reward_config` ADD CONSTRAINT `chk_top_reward_config_rank` CHECK (`rank_position` BETWEEN 1 AND 10)',
    'DO 0')
  FROM information_schema.table_constraints
  WHERE constraint_schema=DATABASE()
    AND table_name='top_reward_config'
    AND constraint_name='chk_top_reward_config_rank'
);
PREPARE stmt_add_top_reward_config_rank FROM @sql_add_top_reward_config_rank;
EXECUTE stmt_add_top_reward_config_rank;
DEALLOCATE PREPARE stmt_add_top_reward_config_rank;

SET @sql_drop_player_mailbox_rank = (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `player_mailbox` DROP CONSTRAINT `chk_player_mailbox_rank`',
    'DO 0')
  FROM information_schema.table_constraints
  WHERE constraint_schema=DATABASE()
    AND table_name='player_mailbox'
    AND constraint_name='chk_player_mailbox_rank'
);
PREPARE stmt_drop_player_mailbox_rank FROM @sql_drop_player_mailbox_rank;
EXECUTE stmt_drop_player_mailbox_rank;
DEALLOCATE PREPARE stmt_drop_player_mailbox_rank;

SET @sql_add_player_mailbox_rank = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `player_mailbox` ADD CONSTRAINT `chk_player_mailbox_rank` CHECK (`rank_position` IS NULL OR `rank_position` BETWEEN 1 AND 10)',
    'DO 0')
  FROM information_schema.table_constraints
  WHERE constraint_schema=DATABASE()
    AND table_name='player_mailbox'
    AND constraint_name='chk_player_mailbox_rank'
);
PREPARE stmt_add_player_mailbox_rank FROM @sql_add_player_mailbox_rank;
EXECUTE stmt_add_player_mailbox_rank;
DEALLOCATE PREPARE stmt_add_player_mailbox_rank;

SET @sql_drop_top_reward_winner_rank = (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `top_reward_winner` DROP CONSTRAINT `chk_top_reward_winner_rank`',
    'DO 0')
  FROM information_schema.table_constraints
  WHERE constraint_schema=DATABASE()
    AND table_name='top_reward_winner'
    AND constraint_name='chk_top_reward_winner_rank'
);
PREPARE stmt_drop_top_reward_winner_rank FROM @sql_drop_top_reward_winner_rank;
EXECUTE stmt_drop_top_reward_winner_rank;
DEALLOCATE PREPARE stmt_drop_top_reward_winner_rank;

SET @sql_add_top_reward_winner_rank = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `top_reward_winner` ADD CONSTRAINT `chk_top_reward_winner_rank` CHECK (`rank_position` BETWEEN 1 AND 10)',
    'DO 0')
  FROM information_schema.table_constraints
  WHERE constraint_schema=DATABASE()
    AND table_name='top_reward_winner'
    AND constraint_name='chk_top_reward_winner_rank'
);
PREPARE stmt_add_top_reward_winner_rank FROM @sql_add_top_reward_winner_rank;
EXECUTE stmt_add_top_reward_winner_rank;
DEALLOCATE PREPARE stmt_add_top_reward_winner_rank;

SET @sql_add_top_reward_command_period = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `top_reward_command` ADD CONSTRAINT `chk_top_reward_command_period` CHECK ((`period_type`=''WEEKLY'' AND `ranking_date` IS NOT NULL) OR (`period_type`<>''WEEKLY'' AND `ranking_date` IS NULL))',
    'DO 0')
  FROM information_schema.table_constraints
  WHERE constraint_schema=DATABASE()
    AND table_name='top_reward_command'
    AND constraint_name='chk_top_reward_command_period'
);
PREPARE stmt_add_top_reward_command_period FROM @sql_add_top_reward_command_period;
EXECUTE stmt_add_top_reward_command_period;
DEALLOCATE PREPARE stmt_add_top_reward_command_period;
