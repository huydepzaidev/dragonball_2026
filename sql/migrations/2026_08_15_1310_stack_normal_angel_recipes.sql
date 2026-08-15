-- Complete recipe stacking by enabling the three normal Angel recipes.
-- Re-runnable: the canonical flag is always reset to one.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `item_template`
SET `is_up_to_up`=1
WHERE `id` BETWEEN 1071 AND 1073;

COMMIT;
