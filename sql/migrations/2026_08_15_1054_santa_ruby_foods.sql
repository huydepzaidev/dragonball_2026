-- Add the three collaboration foods to Santa's Ruby shop for 10 ruby each.
-- Safe to run more than once: missing rows are inserted and existing rows are normalized.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO `item_shop`
(`tab_id`,`temp_id`,`is_new`,`is_sell`,`type_sell`,`cost`,`icon_spec`,`create_time`)
SELECT 64, desired.temp_id, 1, 1, 3, 10, 0, CURRENT_TIMESTAMP
FROM (
    SELECT 880 AS temp_id
    UNION ALL SELECT 881
    UNION ALL SELECT 882
) AS desired
INNER JOIN `item_template` template ON template.id=desired.temp_id
WHERE EXISTS (
    SELECT 1
    FROM `tab_shop` tab
    INNER JOIN `shop` ruby_shop ON ruby_shop.id=tab.shop_id
    WHERE tab.id=64
      AND ruby_shop.npc_id=39
      AND ruby_shop.tag_name='SATAN_RUBY'
)
AND NOT EXISTS (
    SELECT 1
    FROM `item_shop` existing
    WHERE existing.tab_id=64
      AND existing.temp_id=desired.temp_id
);

UPDATE `item_shop` item
INNER JOIN `tab_shop` tab ON tab.id=item.tab_id
INNER JOIN `shop` ruby_shop ON ruby_shop.id=tab.shop_id
SET item.is_new=1,
    item.is_sell=1,
    item.type_sell=3,
    item.cost=10,
    item.icon_spec=0
WHERE tab.id=64
  AND ruby_shop.npc_id=39
  AND ruby_shop.tag_name='SATAN_RUBY'
  AND item.temp_id IN (880,881,882);

COMMIT;
