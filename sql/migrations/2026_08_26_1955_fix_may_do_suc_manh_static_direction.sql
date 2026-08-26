-- BEGIN MIGRATION: 2026_08_26_1955_fix_may_do_suc_manh_static_direction.sql
-- Configure May do suc manh (Mob Template 117) as static dummy (type = 0, range_move = 0, speed = 1)
-- to prevent continuous random idle direction flipping and walking while retaining hit direction update.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `mob_template`
SET `TYPE` = 0,
    `range_move` = 0,
    `speed` = 1
WHERE `id` = 117;

COMMIT;
-- END MIGRATION: 2026_08_26_1955_fix_may_do_suc_manh_static_direction.sql
