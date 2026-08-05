-- Permanent Ruby shop at Santa.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO `item_template`
(`id`,`TYPE`,`gender`,`NAME`,`description`,`level`,`icon_id`,`part`,`is_up_to_up`,`power_require`,`gold`,`gem`,`head`,`body`,`leg`) VALUES
(2000,29,3,'Đổi Skill 2-3 đệ tử','Yêu cầu đệ tử đã có đủ skill 2 và skill 3',0,5223,0,1,1500000,0,0,-1,-1,-1),
(2001,5,3,'Cải trang Sói Đỏ','Cải trang Sói Vô Tinh màu đỏ',0,6884,-1,0,0,0,0,745,746,747),
(2002,5,3,'Cải trang Sói Vàng','Cải trang Sói Vô Tinh màu vàng',0,6916,-1,0,0,0,0,748,749,750),
(2003,5,3,'Cải trang Sói Xanh Xám','Cải trang Sói Vô Tinh màu xanh xám',0,6948,-1,0,0,0,0,751,752,753)
ON DUPLICATE KEY UPDATE
`TYPE`=VALUES(`TYPE`),`gender`=VALUES(`gender`),`NAME`=VALUES(`NAME`),
`description`=VALUES(`description`),`level`=VALUES(`level`),`icon_id`=VALUES(`icon_id`),
`part`=VALUES(`part`),`is_up_to_up`=VALUES(`is_up_to_up`),`power_require`=VALUES(`power_require`),
`gold`=VALUES(`gold`),`gem`=VALUES(`gem`),`head`=VALUES(`head`),`body`=VALUES(`body`),`leg`=VALUES(`leg`);

UPDATE `item_template`
SET `NAME`='Cải trang Quy Lão Kame',
    `description`='HP, KI, sức đánh +22%; x3 TN, SM đệ tử; x3 chưởng mỗi phút'
WHERE `id`=710;

UPDATE `item_template`
SET `NAME`='Cải trang Jacky-Chun',
    `description`='HP, KI, sức đánh +23%; x4 TN, SM đệ tử; x4 chưởng mỗi phút'
WHERE `id`=711;

INSERT INTO `shop` (`id`,`npc_id`,`tag_name`,`type_shop`) VALUES
(37,39,'SATAN_RUBY',0)
ON DUPLICATE KEY UPDATE
`npc_id`=VALUES(`npc_id`),`tag_name`=VALUES(`tag_name`),`type_shop`=VALUES(`type_shop`);

INSERT INTO `tab_shop` (`id`,`shop_id`,`NAME`) VALUES
(64,37,'Cửa hàng<>Hồng Ngọc')
ON DUPLICATE KEY UPDATE
`shop_id`=VALUES(`shop_id`),`NAME`=VALUES(`NAME`);

INSERT INTO `item_shop`
(`id`,`tab_id`,`temp_id`,`is_new`,`is_sell`,`type_sell`,`cost`,`icon_spec`,`create_time`) VALUES
(1003,64,710,1,1,3,2500,0,'2026-08-05 00:00:07'),
(1004,64,2000,1,1,3,500,0,'2026-08-05 00:00:06'),
(1005,64,2001,1,1,3,3000,0,'2026-08-05 00:00:05'),
(1006,64,2002,1,1,3,3000,0,'2026-08-05 00:00:04'),
(1007,64,2003,1,1,3,3000,0,'2026-08-05 00:00:03'),
(1008,64,860,1,1,3,3000,0,'2026-08-05 00:00:02'),
(1009,64,711,1,1,3,2500,0,'2026-08-05 00:00:01')
ON DUPLICATE KEY UPDATE
`tab_id`=VALUES(`tab_id`),`temp_id`=VALUES(`temp_id`),`is_new`=VALUES(`is_new`),
`is_sell`=VALUES(`is_sell`),`type_sell`=VALUES(`type_sell`),`cost`=VALUES(`cost`),
`icon_spec`=VALUES(`icon_spec`),`create_time`=VALUES(`create_time`);

DELETE FROM `item_shop_option` WHERE `item_shop_id` BETWEEN 1003 AND 1009;

INSERT INTO `item_shop_option` (`item_shop_id`,`option_id`,`param`) VALUES
-- Quy Lão Kame: HP/KI/SĐ 22%, x3 TNSM đệ tử, x3 chưởng/phút.
(1003,50,22),(1003,77,22),(1003,103,22),(1003,160,100),(1003,159,3),(1003,30,0),
-- Đổi đồng thời skill 2 và 3 đệ tử.
(1004,30,0),
-- Ba Sói: HP/KI/SĐ 35%, thêm 25% sát thương chí mạng.
(1005,50,35),(1005,77,35),(1005,103,35),(1005,5,25),(1005,30,0),
(1006,50,35),(1006,77,35),(1006,103,35),(1006,5,25),(1006,30,0),
(1007,50,35),(1007,77,35),(1007,103,35),(1007,5,25),(1007,30,0),
-- Mị Nương: HP/SĐ 22%, đẹp +22% SĐ xung quanh, may mắn 25%.
(1008,50,22),(1008,77,22),(1008,117,22),(1008,236,25),(1008,30,0),
-- Jacky-Chun: HP/KI/SĐ 23%, x4 TNSM đệ tử, x4 chưởng/phút.
(1009,50,23),(1009,77,23),(1009,103,23),(1009,160,200),(1009,159,4),(1009,30,0);

COMMIT;
