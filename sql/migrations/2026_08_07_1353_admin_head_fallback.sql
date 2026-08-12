-- Replace the admin character's legacy head 31 with the built-in Earth fallback head 64.
-- The current Unity client cannot resolve head 31 in Char.getAvatar, leaving the panel
-- header blank even after the incompatible costume has been unequipped.
-- Safe to run more than once and scoped to the known account/player pair.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `player` p
INNER JOIN `account` a ON a.id=p.account_id
SET p.head=64
WHERE a.username='1'
  AND p.name='admin'
  AND p.gender=0
  AND p.head=31;

COMMIT;
