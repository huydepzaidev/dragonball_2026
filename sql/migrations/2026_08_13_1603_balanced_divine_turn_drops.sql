-- Centralize divine-equipment drops by boss turn and persist anti-dry-streak state.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

CREATE TABLE IF NOT EXISTS `game_divine_turn_config` (
  `id` tinyint unsigned NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `one_zero_bp` smallint unsigned NOT NULL DEFAULT 6500,
  `one_one_bp` smallint unsigned NOT NULL DEFAULT 3000,
  `one_two_bp` smallint unsigned NOT NULL DEFAULT 500,
  `two_zero_bp` smallint unsigned NOT NULL DEFAULT 5000,
  `two_one_bp` smallint unsigned NOT NULL DEFAULT 3500,
  `two_two_bp` smallint unsigned NOT NULL DEFAULT 1500,
  `multi_zero_bp` smallint unsigned NOT NULL DEFAULT 4000,
  `multi_one_bp` smallint unsigned NOT NULL DEFAULT 3500,
  `multi_two_bp` smallint unsigned NOT NULL DEFAULT 2000,
  `multi_three_bp` smallint unsigned NOT NULL DEFAULT 500,
  `pity_blank_turns` tinyint unsigned NOT NULL DEFAULT 5,
  `updated_by` varchar(50) NOT NULL DEFAULT 'migration',
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `game_divine_turn_config`
(`id`,`enabled`,`one_zero_bp`,`one_one_bp`,`one_two_bp`,
 `two_zero_bp`,`two_one_bp`,`two_two_bp`,
 `multi_zero_bp`,`multi_one_bp`,`multi_two_bp`,`multi_three_bp`,
 `pity_blank_turns`,`updated_by`)
VALUES (1,1,6500,3000,500,5000,3500,1500,4000,3500,2000,500,5,'migration')
ON DUPLICATE KEY UPDATE `id`=VALUES(`id`);

CREATE TABLE IF NOT EXISTS `game_divine_turn_pity` (
  `encounter_key` varchar(64) NOT NULL,
  `map_id` int NOT NULL,
  `zone_id` int NOT NULL,
  `blank_turns` tinyint unsigned NOT NULL DEFAULT 0,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`encounter_key`,`map_id`,`zone_id`),
  KEY `idx_game_divine_turn_pity_updated` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Only the turn service may reward eligible bosses outside the 12h/14h encounters.
-- Disable old per-kill rules for both eligible bosses and explicitly excluded
-- bosses so the admin panel cannot accidentally restore an independent roll.
UPDATE `game_boss_drop`
SET `enabled`=0
WHERE `drop_kind`='DIVINE_RANDOM'
  AND `boss_id` IN (
    -20,-21,-22,-23,-24,-25,-26,-27,
    -311,-312,-313,-314,-315,
    -28,-29,-30,-31,-35,-36,-37,
    -100,-101,-102,-103,-104,-105,-106,-107,-108,
    -203,-203999,-925,
    -372,-373,-374,-375,-376,-377,-378,-379,-380,-381,-382
  );

COMMIT;
