-- Display disciple TNSM bonuses as explicit x3/x4 multipliers.
-- Runtime strength is unchanged: legacy +100%/+200% already produced x3/x4.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO `item_option_template` (`id`,`NAME`) VALUES
(254,'x# TN, SM cho đệ tử khi sư phụ mặc')
ON DUPLICATE KEY UPDATE `NAME`=VALUES(`NAME`);

DELETE FROM `item_shop_option` WHERE `item_shop_id` IN (1003,1009);

INSERT INTO `item_shop_option` (`item_shop_id`,`option_id`,`param`) VALUES
-- Quy Lão Kame: HP/KI/SĐ 22%, x3 TNSM đệ tử, x3 chưởng/phút.
(1003,50,22),(1003,77,22),(1003,103,22),(1003,254,3),(1003,159,3),(1003,30,0),
-- Jacky-Chun: HP/KI/SĐ 23%, x4 TNSM đệ tử, x4 chưởng/phút.
(1009,50,23),(1009,77,23),(1009,103,23),(1009,254,4),(1009,159,4),(1009,30,0);

COMMIT;