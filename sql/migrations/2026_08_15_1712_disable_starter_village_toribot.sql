-- Remove Tori-Bot from all three starter villages (maps 0, 7, and 14).
-- Safe to run more than once and preserves every other NPC in each map.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `map_template`
SET `npcs`=CASE `id`
        WHEN 0 THEN REPLACE(
            REPLACE(JSON_COMPACT(`npcs`), ',[74,426,432]', ''),
            '[74,426,432],', '')
        WHEN 7 THEN REPLACE(
            REPLACE(JSON_COMPACT(`npcs`), ',[74,607,432]', ''),
            '[74,607,432],', '')
        WHEN 14 THEN REPLACE(
            REPLACE(JSON_COMPACT(`npcs`), ',[74,286,409]', ''),
            '[74,286,409],', '')
    END
WHERE (`id`=0 AND JSON_COMPACT(`npcs`) LIKE '%[74,426,432]%')
   OR (`id`=7 AND JSON_COMPACT(`npcs`) LIKE '%[74,607,432]%')
   OR (`id`=14 AND JSON_COMPACT(`npcs`) LIKE '%[74,286,409]%');

COMMIT;
