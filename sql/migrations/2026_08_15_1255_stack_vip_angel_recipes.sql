-- Make all three VIP Angel-equipment recipes stackable in client and server data.
-- Re-runnable: the canonical flag is always reset to one.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `item_template`
SET `is_up_to_up`=1
WHERE `id` BETWEEN 1084 AND 1086;

COMMIT;
