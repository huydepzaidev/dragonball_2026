-- Summer beach event item metadata and Chi Chi shop inventory.
-- These items belong to Chi Chi, never Santa's limited-duration shop.

INSERT INTO item_option_template (id, NAME)
SELECT 251, 'Vật phẩm sự kiện hè, quy đổi tại Quy Lão Kame'
WHERE NOT EXISTS (
    SELECT 1 FROM item_option_template WHERE id = 251
);

UPDATE item_option_template
SET NAME = 'Vật phẩm sự kiện hè, quy đổi tại Quy Lão Kame'
WHERE id = 251;

DELETE option_row
FROM item_shop_option option_row
JOIN item_shop shop_item ON shop_item.id = option_row.item_shop_id
WHERE shop_item.tab_id = 47
  AND shop_item.temp_id IN (691, 692, 693, 694);

DELETE FROM item_shop
WHERE tab_id = 47
  AND temp_id IN (691, 692, 693, 694);

INSERT INTO item_shop (tab_id, temp_id, is_new, is_sell, type_sell, cost, icon_spec, create_time)
SELECT 58, beach_item.temp_id, 1, 1, 1, 10, 0, CURRENT_TIMESTAMP
FROM (
    SELECT 691 AS temp_id
    UNION ALL SELECT 692
    UNION ALL SELECT 693
    UNION ALL SELECT 694
) AS beach_item
WHERE NOT EXISTS (
    SELECT 1
    FROM item_shop existing
    WHERE existing.tab_id = 58 AND existing.temp_id = beach_item.temp_id
);

INSERT INTO item_shop_option (item_shop_id, option_id, param)
SELECT shop_item.id, required_option.option_id, required_option.param
FROM item_shop shop_item
JOIN (
    SELECT 93 AS option_id, 30 AS param
    UNION ALL SELECT 30, 0
) AS required_option
WHERE shop_item.tab_id = 58
  AND shop_item.temp_id IN (691, 692, 693, 694)
  AND NOT EXISTS (
      SELECT 1
      FROM item_shop_option existing_option
      WHERE existing_option.item_shop_id = shop_item.id
        AND existing_option.option_id = required_option.option_id
  );

INSERT INTO item_shop_option (item_shop_id, option_id, param)
SELECT shop_item.id, 158, 0
FROM item_shop shop_item
WHERE shop_item.tab_id = 58
  AND shop_item.temp_id IN (691, 692, 693)
  AND NOT EXISTS (
      SELECT 1
      FROM item_shop_option existing_option
      WHERE existing_option.item_shop_id = shop_item.id
        AND existing_option.option_id = 158
  );

UPDATE item_shop
SET is_new = 1,
    is_sell = 1,
    type_sell = 1,
    cost = 10,
    icon_spec = 0
WHERE tab_id = 58
  AND temp_id IN (691, 692, 693, 694);
