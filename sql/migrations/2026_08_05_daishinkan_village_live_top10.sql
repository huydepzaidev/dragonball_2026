-- Configure Daishinkan and place him in all three starter villages.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO `npc_template` (`id`,`NAME`,`head`,`body`,`leg`,`avatar`) VALUES
(64,'Daishinkan',703,704,705,6578)
ON DUPLICATE KEY UPDATE
`NAME`=VALUES(`NAME`),`head`=VALUES(`head`),`body`=VALUES(`body`),
`leg`=VALUES(`leg`),`avatar`=VALUES(`avatar`);

UPDATE `map_template`
SET `npcs`=IF(TRIM(`npcs`)='[]','[[64,760,432]]',
    CONCAT(LEFT(TRIM(`npcs`),CHAR_LENGTH(TRIM(`npcs`))-1),',[64,760,432]]'))
WHERE `id`=0 AND `npcs` NOT LIKE '%[64,%';

UPDATE `map_template`
SET `npcs`=IF(TRIM(`npcs`)='[]','[[64,1030,432]]',
    CONCAT(LEFT(TRIM(`npcs`),CHAR_LENGTH(TRIM(`npcs`))-1),',[64,1030,432]]'))
WHERE `id`=7 AND `npcs` NOT LIKE '%[64,%';

UPDATE `map_template`
SET `npcs`=IF(TRIM(`npcs`)='[]','[[64,700,408]]',
    CONCAT(LEFT(TRIM(`npcs`),CHAR_LENGTH(TRIM(`npcs`))-1),',[64,700,408]]'))
WHERE `id`=14 AND `npcs` NOT LIKE '%[64,%';

COMMIT;
