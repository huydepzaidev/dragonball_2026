-- Add the two Dragon Ball flag radars to Santa's Ruby shop.
-- Item IDs are 1822/1823; icon IDs are 15002/15003.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `item_template`
SET `NAME`='Rada cờ ngọc rồng',
    `description`='Mở cờ 1-7 sao, không có Super; 1% nhận cờ vĩnh viễn',
    `is_up_to_up`=1
WHERE `id`=1822;

UPDATE `item_template`
SET `NAME`='Rada cờ ngọc rồng VIP',
    `description`='Mở đủ 8 cờ, có cờ Super; 10% nhận cờ vĩnh viễn',
    `is_up_to_up`=1
WHERE `id`=1823;

INSERT INTO `item_shop`
(`tab_id`,`temp_id`,`is_new`,`is_sell`,`type_sell`,`cost`,`icon_spec`,`create_time`)
SELECT 64,1822,1,1,3,50,0,CURRENT_TIMESTAMP
WHERE EXISTS (
    SELECT 1
    FROM `tab_shop` tab
    INNER JOIN `shop` ruby_shop ON ruby_shop.id=tab.shop_id
    WHERE tab.id=64
      AND ruby_shop.tag_name='SATAN_RUBY'
)
AND EXISTS (SELECT 1 FROM `item_template` WHERE id=1822)
AND NOT EXISTS (
    SELECT 1 FROM `item_shop` WHERE tab_id=64 AND temp_id=1822
);

INSERT INTO `item_shop`
(`tab_id`,`temp_id`,`is_new`,`is_sell`,`type_sell`,`cost`,`icon_spec`,`create_time`)
SELECT 64,1823,1,1,3,100,0,CURRENT_TIMESTAMP
WHERE EXISTS (
    SELECT 1
    FROM `tab_shop` tab
    INNER JOIN `shop` ruby_shop ON ruby_shop.id=tab.shop_id
    WHERE tab.id=64
      AND ruby_shop.tag_name='SATAN_RUBY'
)
AND EXISTS (SELECT 1 FROM `item_template` WHERE id=1823)
AND NOT EXISTS (
    SELECT 1 FROM `item_shop` WHERE tab_id=64 AND temp_id=1823
);

UPDATE `item_shop` shop_item
INNER JOIN `tab_shop` shop_tab ON shop_tab.id=shop_item.tab_id
INNER JOIN `shop` ruby_shop ON ruby_shop.id=shop_tab.shop_id
SET shop_item.is_new=1,
    shop_item.is_sell=1,
    shop_item.type_sell=3,
    shop_item.cost=CASE shop_item.temp_id WHEN 1822 THEN 50 ELSE 100 END,
    shop_item.icon_spec=0
WHERE ruby_shop.tag_name='SATAN_RUBY'
  AND shop_tab.id=64
  AND shop_item.temp_id IN (1822,1823);

COMMIT;
