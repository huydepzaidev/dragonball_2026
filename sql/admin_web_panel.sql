SET NAMES utf8mb4;

-- Bảng nhật ký dành riêng cho control panel web.
-- Không thay đổi cấu trúc dữ liệu game hiện có.
CREATE TABLE IF NOT EXISTS `admin_audit_log` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `admin_id` int(11) NOT NULL,
  `admin_username` varchar(20) NOT NULL,
  `action_name` varchar(80) NOT NULL,
  `target_type` varchar(40) NOT NULL,
  `target_id` int(11) DEFAULT NULL,
  `detail_json` longtext DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_admin_created` (`admin_id`, `created_at`),
  KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Cấu hình vận hành game server. Server đọc lại định kỳ, không cần sửa file properties.
CREATE TABLE IF NOT EXISTS `game_server_config` (
  `id` tinyint(3) unsigned NOT NULL DEFAULT 1,
  `exp_rate` smallint(5) unsigned NOT NULL DEFAULT 3,
  `drop_rate_percent` smallint(5) unsigned NOT NULL DEFAULT 100,
  `auto_maintenance_enabled` tinyint(1) NOT NULL DEFAULT 0,
  `maintenance_time` time NOT NULL DEFAULT '04:30:00',
  `maintenance_countdown_seconds` smallint(5) unsigned NOT NULL DEFAULT 300,
  `boss_watchdog_enabled` tinyint(1) NOT NULL DEFAULT 1,
  `boss_stuck_seconds` smallint(5) unsigned NOT NULL DEFAULT 120,
  `config_refresh_seconds` tinyint(3) unsigned NOT NULL DEFAULT 5,
  `updated_by` varchar(20) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_game_server_config_singleton` CHECK (`id` = 1),
  CONSTRAINT `chk_game_server_exp_rate` CHECK (`exp_rate` BETWEEN 1 AND 100),
  CONSTRAINT `chk_game_server_drop_rate` CHECK (`drop_rate_percent` BETWEEN 0 AND 1000),
  CONSTRAINT `chk_game_server_countdown` CHECK (`maintenance_countdown_seconds` BETWEEN 10 AND 3600),
  CONSTRAINT `chk_game_server_stuck` CHECK (`boss_stuck_seconds` BETWEEN 10 AND 3600),
  CONSTRAINT `chk_game_server_refresh` CHECK (`config_refresh_seconds` BETWEEN 2 AND 60)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `game_server_config` (`id`, `exp_rate`)
VALUES (1, 3)
ON DUPLICATE KEY UPDATE `id` = VALUES(`id`);

-- Danh mục boss được server đồng bộ bằng ID thật trong code.
CREATE TABLE IF NOT EXISTS `game_boss_catalog` (
  `boss_id` int(11) NOT NULL,
  `boss_key` varchar(64) DEFAULT NULL,
  `boss_name` varchar(100) NOT NULL,
  `boss_group` varchar(24) NOT NULL DEFAULT 'RUNTIME',
  `active_instances` smallint(5) unsigned NOT NULL DEFAULT 0,
  `last_seen_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`boss_id`),
  UNIQUE KEY `uk_game_boss_key` (`boss_key`),
  KEY `idx_game_boss_group` (`boss_group`),
  KEY `idx_game_boss_name` (`boss_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `game_boss_catalog`
  ADD COLUMN IF NOT EXISTS `boss_key` varchar(64) DEFAULT NULL AFTER `boss_id`,
  ADD COLUMN IF NOT EXISTS `boss_group` varchar(24) NOT NULL DEFAULT 'RUNTIME' AFTER `boss_name`;

ALTER TABLE `game_boss_catalog`
  ADD UNIQUE INDEX IF NOT EXISTS `uk_game_boss_key` (`boss_key`),
  ADD INDEX IF NOT EXISTS `idx_game_boss_group` (`boss_group`);

-- Một boss có thể có nhiều dòng vật phẩm. chance_bp dùng 1/100 phần trăm:
-- 10000 = 100%, 100 = 1%, 1 = 0,01%.
CREATE TABLE IF NOT EXISTS `game_boss_drop` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `boss_id` int(11) NOT NULL,
  `drop_kind` enum('ITEM','DIVINE_RANDOM') NOT NULL DEFAULT 'ITEM',
  `item_id` int(11) DEFAULT NULL,
  `chance_bp` smallint(5) unsigned NOT NULL DEFAULT 10000,
  `quantity_min` int(10) unsigned NOT NULL DEFAULT 1,
  `quantity_max` int(10) unsigned NOT NULL DEFAULT 1,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `created_by` varchar(20) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_game_boss_drop_lookup` (`boss_id`, `enabled`),
  KEY `idx_game_boss_drop_item` (`item_id`),
  CONSTRAINT `fk_game_boss_drop_item` FOREIGN KEY (`item_id`) REFERENCES `item_template` (`id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `chk_game_boss_drop_chance` CHECK (`chance_bp` BETWEEN 0 AND 10000),
  CONSTRAINT `chk_game_boss_drop_quantity` CHECK (`quantity_min` BETWEEN 1 AND 9999 AND `quantity_max` BETWEEN `quantity_min` AND 9999),
  CONSTRAINT `chk_game_boss_drop_kind_item` CHECK (
    (`drop_kind` = 'ITEM' AND `item_id` IS NOT NULL)
    OR (`drop_kind` = 'DIVINE_RANDOM' AND `item_id` IS NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Hàng đợi lệnh an toàn giữa web và game server.
CREATE TABLE IF NOT EXISTS `game_server_command` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `command_type` enum('RELOAD_CONFIG','RESPAWN_BOSS','RESPAWN_ALL') NOT NULL,
  `boss_id` int(11) DEFAULT NULL,
  `status` enum('PENDING','PROCESSING','DONE','FAILED') NOT NULL DEFAULT 'PENDING',
  `requested_by` varchar(20) NOT NULL,
  `result_message` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `started_at` timestamp NULL DEFAULT NULL,
  `finished_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_game_server_command_queue` (`status`, `id`),
  CONSTRAINT `chk_game_server_command_boss` CHECK (
    (`command_type` = 'RESPAWN_BOSS' AND `boss_id` IS NOT NULL)
    OR (`command_type` <> 'RESPAWN_BOSS' AND `boss_id` IS NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `game_server_runtime` (
  `id` tinyint(3) unsigned NOT NULL DEFAULT 1,
  `server_online` tinyint(1) NOT NULL DEFAULT 0,
  `boss_count` int(10) unsigned NOT NULL DEFAULT 0,
  `last_heartbeat` timestamp NULL DEFAULT NULL,
  `last_config_load` timestamp NULL DEFAULT NULL,
  `last_error` varchar(500) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_game_server_runtime_singleton` CHECK (`id` = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `game_server_runtime` (`id`, `server_online`)
VALUES (1, 0)
ON DUPLICATE KEY UPDATE `id` = VALUES(`id`);

-- Dữ liệu dự phòng để trang quản trị có danh sách trước lần khởi động server kế tiếp.
INSERT INTO `game_boss_catalog` (`boss_id`, `boss_name`) VALUES
-- Dùng literal hex UTF-8 để không phụ thuộc code page của mysql.exe trên Windows.
(-27, CONVERT(0x5469E1BB837520C491E1BB9969207472C6B0E1BB9F6E67 USING utf8mb4)),
(-315, CONVERT(0x5469E1BB837520C491E1BB9969207472C6B0E1BB9F6E67204E616D656B USING utf8mb4)),
(-320, CONVERT(0x426F6A61636B USING utf8mb4)),
(-321, CONVERT(0x537570657220426F6A61636B USING utf8mb4)),
(-37, CONVERT(0x4B696E67204B6F6E67 USING utf8mb4)),
(-100, CONVERT(0x58C3AA6E2062E1BB8D2068756E67 USING utf8mb4)),
(-101, CONVERT(0x5369C3AA752062E1BB8D2068756E67 USING utf8mb4)),
(-20, CONVERT(0x4B756B75 USING utf8mb4)),
(-21, CONVERT(0x4DE1BAAD7020C491E1BAA77520C491696E68 USING utf8mb4)),
(-22, CONVERT(0x52616D626F USING utf8mb4)),
(-28, CONVERT(0x46696465 USING utf8mb4)),
(-33, CONVERT(0x416E64726F6964203134 USING utf8mb4)),
(-31, CONVERT(0x44722E204BC3B472C3AA USING utf8mb4)),
(-203999, CONVERT(0x43756D626572 USING utf8mb4)),
(-29, CONVERT(0x436F6F6C6572 USING utf8mb4)),
(-203, CONVERT(0x426C61636B20476F6B75 USING utf8mb4)),
(-502, CONVERT(0x476F6C64656E20467269657A61 USING utf8mb4)),
(-77, CONVERT(0x53C3B3692048E1BAB963205175796E USING utf8mb4)),
(-365, CONVERT(0xC4826E207472E1BB996D USING utf8mb4)),
(-78, CONVERT(0xC39420C490C3B4 USING utf8mb4)),
(-925, CONVERT(0x42616279 USING utf8mb4)),
(-799, CONVERT(0x4DE1BAB774205472E1BB9D69 USING utf8mb4))
ON DUPLICATE KEY UPDATE `boss_name` = VALUES(`boss_name`);

-- Chỉ nạp các ID có nhánh khởi tạo thật trong BossManager.createBoss().
-- boss_name dùng key ASCII làm tên dự phòng; web sẽ định dạng dễ đọc và server
-- sẽ thay bằng tên thật khi boss có instance runtime.
INSERT INTO `game_boss_catalog` (`boss_id`, `boss_key`, `boss_name`, `boss_group`) VALUES
(-1822, 'BROLY', 'BROLY', 'GLOBAL'),
(-322, 'TAP_SU_0', 'TAP_SU_0', 'YARDART'),
(-323, 'TAP_SU_1', 'TAP_SU_1', 'YARDART'),
(-324, 'TAP_SU_2', 'TAP_SU_2', 'YARDART'),
(-325, 'TAP_SU_3', 'TAP_SU_3', 'YARDART'),
(-326, 'TAP_SU_4', 'TAP_SU_4', 'YARDART'),
(-327, 'TAN_BINH_5', 'TAN_BINH_5', 'YARDART'),
(-328, 'TAN_BINH_0', 'TAN_BINH_0', 'YARDART'),
(-329, 'TAN_BINH_1', 'TAN_BINH_1', 'YARDART'),
(-330, 'TAN_BINH_2', 'TAN_BINH_2', 'YARDART'),
(-331, 'TAN_BINH_3', 'TAN_BINH_3', 'YARDART'),
(-332, 'TAN_BINH_4', 'TAN_BINH_4', 'YARDART'),
(-333, 'CHIEN_BINH_5', 'CHIEN_BINH_5', 'YARDART'),
(-334, 'CHIEN_BINH_0', 'CHIEN_BINH_0', 'YARDART'),
(-335, 'CHIEN_BINH_1', 'CHIEN_BINH_1', 'YARDART'),
(-336, 'CHIEN_BINH_2', 'CHIEN_BINH_2', 'YARDART'),
(-337, 'CHIEN_BINH_3', 'CHIEN_BINH_3', 'YARDART'),
(-338, 'CHIEN_BINH_4', 'CHIEN_BINH_4', 'YARDART'),
(-339, 'DOI_TRUONG_5', 'DOI_TRUONG_5', 'YARDART'),
(-23, 'SO_4', 'SO_4', 'GLOBAL'),
(-24, 'SO_3', 'SO_3', 'GLOBAL'),
(-25, 'SO_2', 'SO_2', 'GLOBAL'),
(-26, 'SO_1', 'SO_1', 'GLOBAL'),
(-27, 'TIEU_DOI_TRUONG', 'TIEU_DOI_TRUONG', 'GLOBAL'),
(-311, 'SO_4_NM', 'SO_4_NM', 'NAMEK'),
(-312, 'SO_3_NM', 'SO_3_NM', 'NAMEK'),
(-313, 'SO_2_NM', 'SO_2_NM', 'NAMEK'),
(-314, 'SO_1_NM', 'SO_1_NM', 'NAMEK'),
(-315, 'TIEU_DOI_TRUONG_NM', 'TIEU_DOI_TRUONG_NM', 'NAMEK'),
(-316, 'BUJIN', 'BUJIN', 'BOJACK'),
(-317, 'KOGU', 'KOGU', 'BOJACK'),
(-318, 'ZANGYA', 'ZANGYA', 'BOJACK'),
(-319, 'BIDO', 'BIDO', 'BOJACK'),
(-320, 'BOJACK', 'BOJACK', 'BOJACK'),
(-321, 'SUPER_BOJACK', 'SUPER_BOJACK', 'BOJACK'),
(-20, 'KUKU', 'KUKU', 'GLOBAL'),
(-21, 'MAP_DAU_DINH', 'MAP_DAU_DINH', 'GLOBAL'),
(-22, 'RAMBO', 'RAMBO', 'GLOBAL'),
(-308, 'TAU_PAY_PAY_DONG_NAM_KARIN', 'TAU_PAY_PAY_DONG_NAM_KARIN', 'GLOBAL'),
(-233, 'DRABURA', 'DRABURA', 'BOSS_12H'),
(-234, 'BUI_BUI', 'BUI_BUI', 'BOSS_12H'),
(-238, 'BUI_BUI_2', 'BUI_BUI_2', 'BOSS_12H'),
(-235, 'YA_CON', 'YA_CON', 'BOSS_12H'),
(-237, 'DRABURA_2', 'DRABURA_2', 'BOSS_12H'),
(-341, 'GOKU', 'GOKU', 'BOSS_12H'),
(-342, 'CADIC', 'CADIC', 'BOSS_12H'),
(-236, 'MABU_12H', 'MABU_12H', 'BOSS_12H'),
(-343, 'DRABURA_3', 'DRABURA_3', 'BOSS_12H'),
(-214, 'MABU', 'MABU', 'MAJIN'),
(-348, 'SUPERBU', 'SUPERBU', 'MAJIN'),
(-28, 'FIDE', 'FIDE', 'GLOBAL'),
(-31, 'DR_KORE', 'DR_KORE', 'ANDROID'),
(-30, 'ANDROID_19', 'ANDROID_19', 'ANDROID'),
(-32, 'ANDROID_13', 'ANDROID_13', 'ANDROID'),
(-33, 'ANDROID_14', 'ANDROID_14', 'ANDROID'),
(-34, 'ANDROID_15', 'ANDROID_15', 'ANDROID'),
(-35, 'PIC', 'PIC', 'ANDROID'),
(-36, 'POC', 'POC', 'ANDROID'),
(-37, 'KING_KONG', 'KING_KONG', 'ANDROID'),
(-100, 'XEN_BO_HUNG', 'XEN_BO_HUNG', 'CELL'),
(-101, 'SIEU_BO_HUNG', 'SIEU_BO_HUNG', 'CELL'),
(-102, 'XEN_CON_1', 'XEN_CON_1', 'CELL'),
(-103, 'XEN_CON_2', 'XEN_CON_2', 'CELL'),
(-104, 'XEN_CON_3', 'XEN_CON_3', 'CELL'),
(-105, 'XEN_CON_4', 'XEN_CON_4', 'CELL'),
(-106, 'XEN_CON_5', 'XEN_CON_5', 'CELL'),
(-107, 'XEN_CON_6', 'XEN_CON_6', 'CELL'),
(-108, 'XEN_CON_7', 'XEN_CON_7', 'CELL'),
(-29, 'COOLER', 'COOLER', 'GLOBAL'),
(-344, 'KHIDOT', 'KHIDOT', 'EVENT'),
(-345, 'NGUYETTHAN', 'NGUYETTHAN', 'EVENT'),
(-346, 'NHATTHAN', 'NHATTHAN', 'EVENT'),
(-502, 'GOLDEN_FRIEZA', 'GOLDEN_FRIEZA', 'EVENT'),
(-609, 'DEATH_BEAM_1', 'DEATH_BEAM_1', 'EVENT'),
(-610, 'DEATH_BEAM_2', 'DEATH_BEAM_2', 'EVENT'),
(-611, 'DEATH_BEAM_3', 'DEATH_BEAM_3', 'EVENT'),
(-612, 'DEATH_BEAM_4', 'DEATH_BEAM_4', 'EVENT'),
(-613, 'DEATH_BEAM_5', 'DEATH_BEAM_5', 'EVENT'),
(-351, 'BIMA', 'BIMA', 'EVENT'),
(-349, 'MATROI', 'MATROI', 'EVENT'),
(-350, 'DOI', 'DOI', 'EVENT'),
(-353, 'ONG_GIA_NOEL', 'ONG_GIA_NOEL', 'EVENT'),
(-354, 'SON_TINH', 'SON_TINH', 'EVENT'),
(-355, 'THUY_TINH', 'THUY_TINH', 'EVENT'),
(-371, 'LAN_CON', 'LAN_CON', 'EVENT'),
(-77, 'SOI_HEC_QUYN1', 'SOI_HEC_QUYN1', 'MINI'),
(-78, 'O_DO1', 'O_DO1', 'MINI'),
(-79, 'Virut', 'Virut', 'MINI'),
(-799, 'MAT_TROI', 'MAT_TROI', 'MINI'),
(-203, 'BLACK_GOKU', 'BLACK_GOKU', 'GLOBAL'),
(-203999, 'CUMBER', 'CUMBER', 'GLOBAL'),
(-365, 'AN_TROM', 'AN_TROM', 'MINI'),
(-386998, 'RONG_NHI', 'RONG_NHI', 'MINI'),
(-925, 'BABY', 'BABY', 'GLOBAL')
ON DUPLICATE KEY UPDATE
  `boss_key` = VALUES(`boss_key`),
  `boss_group` = VALUES(`boss_group`),
  `boss_name` = IF(`game_boss_catalog`.`boss_name` = '' OR `game_boss_catalog`.`boss_name` IS NULL,
                   VALUES(`boss_name`), `game_boss_catalog`.`boss_name`);
