-- Add the Naruto collaboration chest after the Naruto resource bundle.
-- Item icon 25351 is copied from source icon 16899 into data/icon/x1..x4.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO `item_template`
(`id`,`TYPE`,`gender`,`NAME`,`description`,`level`,`icon_id`,`part`,
 `is_up_to_up`,`power_require`,`gold`,`gem`,`head`,`body`,`leg`) VALUES
(2041,27,3,'Rương hợp tác Naruto',
 'Mở ngẫu nhiên nhận vật phẩm hợp tác Naruto: pet, cải trang hoặc đeo lưng.',
 1,25351,-1,0,0,0,0,-1,-1,-1)
ON DUPLICATE KEY UPDATE
`TYPE`=VALUES(`TYPE`),`gender`=VALUES(`gender`),`NAME`=VALUES(`NAME`),
`description`=VALUES(`description`),`level`=VALUES(`level`),`icon_id`=VALUES(`icon_id`),
`part`=VALUES(`part`),`is_up_to_up`=VALUES(`is_up_to_up`),`power_require`=VALUES(`power_require`),
`gold`=VALUES(`gold`),`gem`=VALUES(`gem`),`head`=VALUES(`head`),`body`=VALUES(`body`),`leg`=VALUES(`leg`);

COMMIT;
