-- Sell the mystery capsule detector in the future Bulma shop for 10 rubies.
-- Access control, stacking time, drop rate, and reward odds are enforced by the server.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `item_shop` shop_item
INNER JOIN `tab_shop` shop_tab ON shop_tab.`id`=shop_item.`tab_id`
INNER JOIN `shop` future_shop ON future_shop.`id`=shop_tab.`shop_id`
SET shop_item.`type_sell`=3,
    shop_item.`cost`=10,
    shop_item.`is_sell`=1
WHERE future_shop.`tag_name`='BUNMA_FUTURE'
  AND shop_item.`temp_id`=379;

COMMIT;
