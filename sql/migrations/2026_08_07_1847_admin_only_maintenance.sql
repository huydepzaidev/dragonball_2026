-- Add admin-only maintenance commands and expose the in-memory mode to web2026.
-- Rerunnable: DDL converges to the same enum and guarded runtime column.
SET NAMES utf8mb4;
START TRANSACTION;

ALTER TABLE `game_server_command`
  MODIFY COLUMN `command_type`
    enum('RELOAD_CONFIG','RESPAWN_BOSS','RESPAWN_ALL','START_MAINTENANCE','STOP_MAINTENANCE') NOT NULL;

ALTER TABLE `game_server_runtime`
  ADD COLUMN IF NOT EXISTS `admin_only_mode` tinyint(1) NOT NULL DEFAULT 0
  AFTER `boss_count`;

COMMIT;
