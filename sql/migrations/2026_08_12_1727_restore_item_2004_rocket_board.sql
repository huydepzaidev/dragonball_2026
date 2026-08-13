-- Restore the missing rocket board template so fresh installs keep dense item IDs.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO `item_template`
(`id`,`TYPE`,`gender`,`NAME`,`description`,`level`,`icon_id`,`part`,
 `is_up_to_up`,`power_require`,`gold`,`gem`,`head`,`body`,`leg`) VALUES
(2004,5,3,'Ván bay tên lửa','Cải trang Ván bay tên lửa',0,6755,-1,0,0,0,0,727,728,729)
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
