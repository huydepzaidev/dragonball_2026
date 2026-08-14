-- Give the Naruto collaboration pet a valid transparent head part.
-- NewPet is rendered as a normal three-part character and cannot use head -1.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

DELETE FROM `part` WHERE `id`=2102;

INSERT INTO `part` (`id`,`TYPE`,`DATA`) VALUES
(2102,0,'[[2955,0,0],[2955,0,0],[2955,0,0]]');

UPDATE `item_template`
SET `head`=2102,`body`=1990,`leg`=1991
WHERE `id`=2019;

COMMIT;
