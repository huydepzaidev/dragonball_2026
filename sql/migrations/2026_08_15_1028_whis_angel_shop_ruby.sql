-- Price every item in Whis's Angel shop at 100 ruby instead of gold bars.
-- Re-runnable: the same 16 Angel materials and recipes are reset to the canonical price.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `item_shop` AS `item`
JOIN `tab_shop` AS `tab` ON `tab`.`id`=`item`.`tab_id`
JOIN `shop` AS `shop` ON `shop`.`id`=`tab`.`shop_id`
JOIN `item_template` AS `ruby` ON `ruby`.`id`=861
SET `item`.`type_sell`=3,
    `item`.`cost`=100,
    `item`.`icon_spec`=`ruby`.`icon_id`
WHERE `shop`.`npc_id`=56
  AND `shop`.`tag_name`='THIEN_SU'
  AND `item`.`temp_id` BETWEEN 1071 AND 1086;

COMMIT;
