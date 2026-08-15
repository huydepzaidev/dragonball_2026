-- Display the deterministic 0/5/10/15% quality granted by Angel crafting.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO `item_option_template` (`id`,`NAME`) VALUES
(255,'Chế tạo Thiên Sứ (+#% chỉ số)')
ON DUPLICATE KEY UPDATE `NAME`=VALUES(`NAME`);

COMMIT;
