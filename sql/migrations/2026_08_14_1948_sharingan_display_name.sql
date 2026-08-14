-- Remove the equipment-category prefix from the Sharingan display text.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `item_template`
SET `NAME`='Sharingan Vạn Hoa Đồng'
WHERE `id`=2030;

UPDATE `item_template`
SET `description`='Đi cùng Sharingan Vạn Hoa Đồng: +1% sát thương'
WHERE `id`=2027;

COMMIT;
