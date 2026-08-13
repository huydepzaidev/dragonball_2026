-- Restore the missing name-change card so item template IDs remain dense through 2007.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO `item_template`
(`id`,`TYPE`,`gender`,`NAME`,`description`,`level`,`icon_id`,`part`,
 `is_up_to_up`,`power_require`,`gold`,`gem`,`head`,`body`,`leg`) VALUES
(2006,27,3,'Thẻ đổi tên','Dùng để đổi tên nhân vật',1,2989,-1,1,0,0,0,-1,-1,-1)
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

COMMIT;
