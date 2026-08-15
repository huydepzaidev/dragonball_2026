-- Roll back the Angel crafting quality option because the legacy client protocol
-- serializes the option-template count in one byte. A 256th template wraps the
-- count to zero and desynchronizes the client data packet.
-- Re-runnable: deleting a missing row is a no-op.
SET NAMES utf8mb4;
START TRANSACTION;

DELETE FROM `item_option_template`
WHERE `id`=255;

COMMIT;
