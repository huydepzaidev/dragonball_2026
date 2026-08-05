-- Give each wolf costume its requested unique combat bonus.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO `item_option_template` (`id`,`NAME`) VALUES
(251,'Vật phẩm sự kiện hè, quy đổi tại Quy Lão Kame'),
(252,'+#% sát thương chiêu tự sát'),
(253,'+#% sát thương chiêu laze')
ON DUPLICATE KEY UPDATE `NAME`=VALUES(`NAME`);

UPDATE `item_template`
SET `description`=CASE `id`
    WHEN 2001 THEN 'HP, KI, sức đánh +35%; +25% sức đánh chí mạng'
    WHEN 2002 THEN 'HP, KI, sức đánh +35%; +30% sát thương chiêu tự sát'
    WHEN 2003 THEN 'HP, KI, sức đánh +35%; +30% sát thương chiêu laze'
END
WHERE `id` IN (2001,2002,2003);

DELETE shop_option
FROM `item_shop_option` shop_option
INNER JOIN `item_shop` shop_item ON shop_item.id=shop_option.item_shop_id
WHERE shop_item.tab_id=64
  AND shop_item.temp_id IN (2002,2003)
  AND shop_option.option_id IN (5,252,253);

INSERT INTO `item_shop_option` (`item_shop_id`,`option_id`,`param`)
SELECT `id`,252,30 FROM `item_shop` WHERE `tab_id`=64 AND `temp_id`=2002
UNION ALL
SELECT `id`,253,30 FROM `item_shop` WHERE `tab_id`=64 AND `temp_id`=2003;

COMMIT;