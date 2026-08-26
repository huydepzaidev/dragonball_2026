-- Restore Găng thun Pico to the normal glove equipment type.
-- Re-runnable: fresh databases already use type 2, while drifted databases converge to it.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `item_template`
SET `TYPE`=2
WHERE `id`=45
  AND `TYPE`<>2;

COMMIT;
