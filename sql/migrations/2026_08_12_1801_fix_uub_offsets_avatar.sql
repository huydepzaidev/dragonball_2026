-- Restore the original Uub frame offsets and map its head to the matching avatar.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `part`
SET `DATA`=CASE `id`
    WHEN 946 THEN '[[8586,4,7],[8587,2,5],[2955,0,0]]'
    WHEN 947 THEN '[[8588,2,6],[8589,1,4],[8590,-1,2],[8591,1,2],[8592,1,1],[8593,2,2],[8594,1,2],[8595,3,4],[8596,3,7],[8597,1,8],[8598,1,3],[8599,1,3],[8600,2,6],[8601,2,3],[8602,2,5],[8603,0,2],[2955,0,0]]'
    WHEN 948 THEN '[[8604,9,9],[8605,1,4],[8606,0,3],[8607,1,2],[8608,0,2],[8609,2,2],[8610,0,4],[8611,3,9],[8612,0,7],[8613,1,3],[8614,1,5],[8615,0,8],[8616,0,7],[2955,0,0]]'
END
WHERE `id` IN (946,947,948);

INSERT INTO `head_avatar` (`head_id`,`avatar_id`) VALUES (946,8617)
ON DUPLICATE KEY UPDATE `avatar_id`=VALUES(`avatar_id`);

COMMIT;
