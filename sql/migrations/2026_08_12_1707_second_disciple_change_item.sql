-- Refresh the normal disciple changer icon and add a guarded second-disciple changer.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `item_template`
SET `icon_id`=25002
WHERE `id`=401;

INSERT INTO `item_template`
(`id`,`TYPE`,`gender`,`NAME`,`description`,`level`,`icon_id`,`part`,
 `is_up_to_up`,`power_require`,`gold`,`gem`,`head`,`body`,`leg`) VALUES
(2007,27,3,'Đổi đệ tử 2',
 'Chỉ dùng khi đang có Đệ tử 2; đổi ngẫu nhiên sang một Đệ tử 2 khác',
 1,25001,-1,1,0,0,0,-1,-1,-1)
ON DUPLICATE KEY UPDATE
`TYPE`=VALUES(`TYPE`),
`gender`=VALUES(`gender`),
`NAME`=VALUES(`NAME`),
`description`=VALUES(`description`),
`level`=VALUES(`level`),
`icon_id`=VALUES(`icon_id`),
`part`=VALUES(`part`),
`is_up_to_up`=VALUES(`is_up_to_up`),
`power_require`=VALUES(`power_require`),
`gold`=VALUES(`gold`),
`gem`=VALUES(`gem`),
`head`=VALUES(`head`),
`body`=VALUES(`body`),
`leg`=VALUES(`leg`);

INSERT INTO `item_shop`
(`tab_id`,`temp_id`,`is_new`,`is_sell`,`type_sell`,`cost`,`icon_spec`,`create_time`)
SELECT 64,2007,1,1,3,2000,0,CURRENT_TIMESTAMP
WHERE EXISTS (
    SELECT 1
    FROM `tab_shop` tab
    INNER JOIN `shop` ruby_shop ON ruby_shop.id=tab.shop_id
    WHERE tab.id=64
      AND ruby_shop.tag_name='SATAN_RUBY'
)
AND EXISTS (
    SELECT 1 FROM `item_template` WHERE id=2007
)
AND NOT EXISTS (
    SELECT 1
    FROM `item_shop`
    WHERE tab_id=64 AND temp_id=2007
);

UPDATE `item_shop`
SET `is_new`=1,
    `is_sell`=1,
    `type_sell`=3,
    `cost`=2000,
    `icon_spec`=0
WHERE `tab_id`=64
  AND `temp_id`=2007;

UPDATE `item_shop_option` option_row
INNER JOIN `item_shop` shop_item ON shop_item.id=option_row.item_shop_id
SET option_row.param=0
WHERE shop_item.tab_id=64
  AND shop_item.temp_id=2007
  AND option_row.option_id=30;

INSERT INTO `item_shop_option` (`item_shop_id`,`option_id`,`param`)
SELECT shop_item.id,30,0
FROM `item_shop` shop_item
WHERE shop_item.tab_id=64
  AND shop_item.temp_id=2007
  AND NOT EXISTS (
      SELECT 1
      FROM `item_shop_option` existing_option
      WHERE existing_option.item_shop_id=shop_item.id
        AND existing_option.option_id=30
  );

COMMIT;
