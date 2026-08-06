-- Rename the existing pink capsule for the Namek assassin ruby reward.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `item_template`
SET `NAME`='Capsule hồng ngọc',
    `description`='Mở ra nhận ngẫu nhiên từ 1 đến 100 hồng ngọc'
WHERE `id`=722;

COMMIT;
