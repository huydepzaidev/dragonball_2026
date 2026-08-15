-- Remove the Collection Chest NPC from Future Bulma's house (map 102).
-- Safe to run more than once and preserves every other NPC in the map.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `map_template`
SET `npcs`=CASE
        WHEN JSON_COMPACT(`npcs`)='[[82,218,360]]' THEN '[]'
        ELSE REPLACE(
            REPLACE(JSON_COMPACT(`npcs`), ',[82,218,360]', ''),
            '[82,218,360],', '')
    END
WHERE `id`=102
  AND JSON_COMPACT(`npcs`) LIKE '%[82,218,360]%';

COMMIT;
