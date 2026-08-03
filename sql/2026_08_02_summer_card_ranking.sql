ALTER TABLE `player`
    ADD COLUMN IF NOT EXISTS `point_summer_cards` BIGINT NOT NULL DEFAULT 0
    AFTER `point_sukien2`,
    ADD COLUMN IF NOT EXISTS `summer_card_points_migrated` TINYINT(1) NOT NULL DEFAULT 0
    AFTER `point_summer_cards`;
