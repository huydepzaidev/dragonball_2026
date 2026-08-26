-- Add safe defaults for manual rankings that have never been configured.
-- Existing reward rows are preserved exactly as they are.

SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO `top_reward_config`
    (`ranking_key`, `rank_position`, `title`, `message`, `sender_name`, `rewards_json`, `updated_by`)
SELECT
    rankings.`ranking_key`,
    ranks.`rank_position`,
    CASE
        WHEN ranks.`rank_position` BETWEEN 1 AND 3
            THEN CONCAT('Quà Top ', ranks.`rank_position`)
        ELSE 'Quà Top 4–10'
    END,
    CASE
        WHEN ranks.`rank_position` BETWEEN 1 AND 3
            THEN CONCAT('Chúc mừng bạn đã đạt thứ hạng ', ranks.`rank_position`, ' trong sự kiện.')
        ELSE 'Chúc mừng bạn đã đạt thứ hạng cao trong sự kiện.'
    END,
    'Admin',
    CASE
        WHEN ranks.`rank_position` = 1
            THEN '[{"id":1538,"quantity":2,"options":[{"id":30,"param":0}]}]'
        WHEN ranks.`rank_position` = 2
            THEN '[{"id":1538,"quantity":1,"options":[{"id":30,"param":0}]}]'
        WHEN ranks.`rank_position` = 3
            THEN '[{"id":1559,"quantity":5,"options":[{"id":30,"param":0}]}]'
        ELSE '[{"id":1559,"quantity":1,"options":[{"id":30,"param":0}]}]'
    END,
    NULL
FROM (
    SELECT 'childrens_day' AS `ranking_key`
    UNION ALL SELECT 'sugarcane'
    UNION ALL SELECT 'fruit_ice_cream'
    UNION ALL SELECT 'top_up'
) AS rankings
CROSS JOIN (
    SELECT 1 AS `rank_position`
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
    UNION ALL SELECT 5
    UNION ALL SELECT 6
    UNION ALL SELECT 7
    UNION ALL SELECT 8
    UNION ALL SELECT 9
    UNION ALL SELECT 10
) AS ranks
LEFT JOIN `top_reward_config` existing
    ON existing.`ranking_key` = rankings.`ranking_key`
    AND existing.`rank_position` = ranks.`rank_position`
WHERE existing.`id` IS NULL;

COMMIT;
