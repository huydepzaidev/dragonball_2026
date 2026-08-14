-- Add the remapped Dragon Ball flag resources without overwriting icon IDs 13055-13175.
-- Source icon IDs 13055-13175 are stored at 25003-25123 (offset +11948).
-- Type-11 item_template.part points to flag_bag.id, not to the part table.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO `flag_bag`
(`id`,`icon_data`,`NAME`,`gold`,`gem`,`icon_id`) VALUES
(157,'25003,25004,25005,25006,25007,25008,25009,25010','Cờ ngọc rồng 1 sao',-1,-1,25011),
(158,'25012,25013,25014,25015,25016,25017,25018,25019','Cờ ngọc rồng 2 sao',-1,-1,25066),
(159,'25020,25021,25022,25023,25024,25025,25026,25027','Cờ ngọc rồng 3 sao',-1,-1,25028),
(160,'25029,25030,25031,25032,25033,25034,25035,25036,25037','Cờ ngọc rồng 4 sao',-1,-1,25038),
(161,'25039,25040,25041,25042,25043,25044,25045,25046','Cờ ngọc rồng 5 sao',-1,-1,25047),
(162,'25048,25049,25050,25051,25052,25053,25054,25055','Cờ ngọc rồng 6 sao',-1,-1,25056),
(163,'25057,25058,25059,25060,25061,25062,25063,25064','Cờ ngọc rồng 7 sao',-1,-1,25065),
(164,'25067,25068,25069,25070,25071,25072,25073,25074','Cờ ngọc rồng Super',-1,-1,25123)
ON DUPLICATE KEY UPDATE
`icon_data`=VALUES(`icon_data`),`NAME`=VALUES(`NAME`),`gold`=VALUES(`gold`),
`gem`=VALUES(`gem`),`icon_id`=VALUES(`icon_id`);

INSERT INTO `item_template`
(`id`,`TYPE`,`gender`,`NAME`,`description`,`level`,`icon_id`,`part`,
 `is_up_to_up`,`power_require`,`gold`,`gem`,`head`,`body`,`leg`) VALUES
(2008,11,3,'Cờ ngọc rồng 1 sao','Vật phẩm sự kiện',0,25011,157,0,0,0,0,-1,-1,-1),
(2009,11,3,'Cờ ngọc rồng 2 sao','Vật phẩm sự kiện',0,25066,158,0,0,0,0,-1,-1,-1),
(2010,11,3,'Cờ ngọc rồng 3 sao','Vật phẩm sự kiện',0,25028,159,0,0,0,0,-1,-1,-1),
(2011,11,3,'Cờ ngọc rồng 4 sao','Vật phẩm sự kiện',0,25038,160,0,0,0,0,-1,-1,-1),
(2012,11,3,'Cờ ngọc rồng 5 sao','Vật phẩm sự kiện',0,25047,161,0,0,0,0,-1,-1,-1),
(2013,11,3,'Cờ ngọc rồng 6 sao','Vật phẩm sự kiện',0,25056,162,0,0,0,0,-1,-1,-1),
(2014,11,3,'Cờ ngọc rồng 7 sao','Vật phẩm sự kiện',0,25065,163,0,0,0,0,-1,-1,-1),
(2015,11,3,'Cờ ngọc rồng Super','Vật phẩm sự kiện',0,25123,164,0,0,0,0,-1,-1,-1)
ON DUPLICATE KEY UPDATE
`TYPE`=VALUES(`TYPE`),`gender`=VALUES(`gender`),`NAME`=VALUES(`NAME`),
`description`=VALUES(`description`),`level`=VALUES(`level`),`icon_id`=VALUES(`icon_id`),
`part`=VALUES(`part`),`is_up_to_up`=VALUES(`is_up_to_up`),`power_require`=VALUES(`power_require`),
`gold`=VALUES(`gold`),`gem`=VALUES(`gem`),`head`=VALUES(`head`),`body`=VALUES(`body`),`leg`=VALUES(`leg`);

COMMIT;
