-- Use the dedicated inventory icons instead of costume animation frames.
-- The head avatars remain 6915, 6947, and 6979.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `item_template`
SET `icon_id`=CASE `id`
    WHEN 2001 THEN 6914
    WHEN 2002 THEN 6917
    WHEN 2003 THEN 6949
END
WHERE `id` IN (2001,2002,2003);

COMMIT;