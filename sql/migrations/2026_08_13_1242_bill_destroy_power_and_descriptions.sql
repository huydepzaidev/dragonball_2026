-- Require 20 billion power for Bill destroy equipment and standardize its descriptions.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `item_template`
SET `description`=CASE `TYPE`
    WHEN 0 THEN 'Giúp bạn tăng giáp'
    WHEN 1 THEN 'Giúp bạn tăng HP'
    WHEN 2 THEN 'Giúp bạn tăng sức đánh'
    WHEN 3 THEN 'Giúp bạn tăng KI'
    WHEN 4 THEN 'Giúp bạn tăng Chí Mạng'
    ELSE `description`
END
WHERE `id` BETWEEN 650 AND 662;

UPDATE `item_shop_option` shop_option
INNER JOIN `item_shop` item ON item.`id`=shop_option.`item_shop_id`
INNER JOIN `tab_shop` tab ON tab.`id`=item.`tab_id`
INNER JOIN `shop` bill_shop ON bill_shop.`id`=tab.`shop_id`
SET shop_option.`param`=20
WHERE bill_shop.`tag_name`='BILL'
  AND item.`temp_id` BETWEEN 650 AND 662
  AND shop_option.`option_id`=21;

INSERT INTO `item_shop_option` (`item_shop_id`,`option_id`,`param`)
SELECT item.`id`,21,20
FROM `item_shop` item
INNER JOIN `tab_shop` tab ON tab.`id`=item.`tab_id`
INNER JOIN `shop` bill_shop ON bill_shop.`id`=tab.`shop_id`
WHERE bill_shop.`tag_name`='BILL'
  AND item.`temp_id` BETWEEN 650 AND 662
  AND NOT EXISTS (
      SELECT 1
      FROM `item_shop_option` existing_option
      WHERE existing_option.`item_shop_id`=item.`id`
        AND existing_option.`option_id`=21
  );

COMMIT;
