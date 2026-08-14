-- Rename the seven baby dragons by the Dragon Ball shown in each icon and
-- update the shared full-permanent-set bonus description.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE item_template
SET NAME = CASE id
        WHEN 1765 THEN 'Rồng nhí 7 sao'
        WHEN 1766 THEN 'Rồng nhí 6 sao'
        WHEN 1767 THEN 'Rồng nhí 5 sao'
        WHEN 1768 THEN 'Rồng nhí 4 sao'
        WHEN 1769 THEN 'Rồng nhí 3 sao'
        WHEN 1770 THEN 'Rồng nhí 2 sao'
        WHEN 1771 THEN 'Rồng nhí 1 sao'
        ELSE NAME
    END,
    description = 'Sở hữu đủ 7 Rồng Nhí vĩnh viễn: +1% HP, KI, sức đánh và né đòn',
    icon_id = CASE WHEN id = 1771 THEN 15125 ELSE icon_id END
WHERE id BETWEEN 1765 AND 1771;

COMMIT;
