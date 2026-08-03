SET NAMES utf8mb4;

ALTER TABLE `game_server_config`
  ADD COLUMN IF NOT EXISTS `login_notice_enabled` tinyint(1) NOT NULL DEFAULT 1 AFTER `config_refresh_seconds`,
  ADD COLUMN IF NOT EXISTS `login_notice_text` varchar(1000) NOT NULL DEFAULT 'X3 Kinh nghiệm đến hết ngày 11/5.\nSự kiện Goku Day.\nĐua TOP nhận quà cực khủng.\nTích điểm đổi quà.\nChi tiết xem tại diễn đàn, fanpage.' AFTER `login_notice_enabled`;

UPDATE `game_server_config`
SET `login_notice_enabled` = 1,
    `login_notice_text` = 'X3 Kinh nghiệm đến hết ngày 11/5.\nSự kiện Goku Day.\nĐua TOP nhận quà cực khủng.\nTích điểm đổi quà.\nChi tiết xem tại diễn đàn, fanpage.'
WHERE `id` = 1
  AND (`login_notice_text` IS NULL OR TRIM(`login_notice_text`) = '');
