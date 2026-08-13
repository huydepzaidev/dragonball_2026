-- Repair two malformed rows that shift the implicit part IDs in update_data/part.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO `part` (`id`,`TYPE`,`DATA`)
SELECT 1919, source_part.`TYPE`, source_part.`DATA`
FROM `part` source_part
WHERE source_part.`id`=1949
  AND source_part.`DATA` LIKE '[[21326,%'
  AND NOT EXISTS (SELECT 1 FROM `part` target_part WHERE target_part.`id`=1919);

DELETE FROM `part`
WHERE `id`=1949
  AND `DATA` LIKE '[[21326,%';

UPDATE `part`
SET `DATA`='[[15288,0,10],[2955,0,0],[2955,0,0]]'
WHERE `id`=1999;

COMMIT;
