-- Correct Bé Rồng Cute id 1771, which was linked to the Ghost Rider icon.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE item_template
SET icon_id=15125
WHERE id=1771 AND icon_id=16187;

COMMIT;
