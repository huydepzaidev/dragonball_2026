-- BEGIN MIGRATION: 2026_08_26_2130_fix_satan_ruby_shop_currency_icon.sql
-- Update Santa's Ruby Shop (SATAN_RUBY / Shop 37 / Tab 64) to SPEC_SHOP (type_shop = 3)
-- and set icon_spec = 7743 (item 861 Hồng ngọc) so items display the red/pink ruby currency icon
-- instead of green gem in both the shop item list and the purchase confirmation dialog.
SET NAMES utf8mb4;
START TRANSACTION;

-- 1. Update shop type to SPEC_SHOP (3)
UPDATE `shop`
SET `type_shop` = 3
WHERE `id` = 37 OR `tag_name` = 'SATAN_RUBY';

-- 2. Update icon_spec = 7743 (Hồng ngọc icon) for all items in Tab 64 (Cửa hàng Hồng Ngọc)
UPDATE `item_shop` AS `item`
JOIN `tab_shop` AS `tab` ON `tab`.`id` = `item`.`tab_id`
JOIN `shop` AS `shop` ON `shop`.`id` = `tab`.`shop_id`
JOIN `item_template` AS `ruby` ON `ruby`.`id` = 861
SET `item`.`type_sell` = 3,
    `item`.`icon_spec` = `ruby`.`icon_id`
WHERE `shop`.`tag_name` = 'SATAN_RUBY'
   OR `tab`.`id` = 64;

COMMIT;
-- END MIGRATION: 2026_08_26_2130_fix_satan_ruby_shop_currency_icon.sql
