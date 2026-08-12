-- Item 568 is the normal Mabu egg, not Lunar New Year event stock.
-- Rerunnable: delete every accidental event classification for this item.
SET NAMES utf8mb4;
START TRANSACTION;

DELETE FROM `game_event_item`
WHERE `item_id`=568;

COMMIT;
