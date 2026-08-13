-- Change every Bill destroy-equipment purchase to a fixed 500 Ruby price.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `item_shop` item
INNER JOIN `tab_shop` tab ON tab.`id`=item.`tab_id`
INNER JOIN `shop` bill_shop ON bill_shop.`id`=tab.`shop_id`
SET item.`type_sell`=3,
    item.`cost`=500,
    item.`icon_spec`=0
WHERE bill_shop.`tag_name`='BILL'
  AND item.`temp_id` BETWEEN 650 AND 662;

COMMIT;
