-- Restore the existing combat capsule and add a dedicated ruby reward capsule.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `item_template`
SET `NAME`='Capsule hồng',
    `description`='VPSK'
WHERE `id`=722;

INSERT INTO `item_template`
    (`id`, `TYPE`, `gender`, `NAME`, `description`, `level`, `icon_id`, `part`,
     `is_up_to_up`, `power_require`, `gold`, `gem`, `head`, `body`, `leg`)
VALUES
    (2005, 27, 3, 'Capsule hồng ngọc',
     'Mở ra nhận ngẫu nhiên từ 1 đến 100 hồng ngọc',
     1, 6782, -1, 1, 0, 0, 0, -1, -1, -1)
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
