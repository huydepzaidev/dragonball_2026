-- Move the admin character's incompatible costume from body slot 5 to an empty bag slot.
-- The equipped costume resolves to head part 1264, whose small-image IDs are absent from
-- the current data/update_data/image bundle and crash the Unity client avatar renderer.
-- Safe to run more than once; the guarded update only applies to the known broken state.
SET NAMES utf8mb4;
START TRANSACTION;

SET @admin_costume_repair_player_id := (
  SELECT p.id
  FROM `player` p
  INNER JOIN `account` a ON a.id=p.account_id
  WHERE a.username='1'
    AND p.name='admin'
    AND CAST(JSON_UNQUOTE(JSON_EXTRACT(
          JSON_UNQUOTE(JSON_EXTRACT(p.items_body,'$[5]')),'$[0]'
        )) AS SIGNED)=1255
    AND CAST(JSON_UNQUOTE(JSON_EXTRACT(
          JSON_UNQUOTE(JSON_EXTRACT(p.items_bag,'$[64]')),'$[0]'
        )) AS SIGNED)=-1
  LIMIT 1
);

SET @admin_costume_repair_item := (
  SELECT JSON_EXTRACT(items_body,'$[5]')
  FROM `player`
  WHERE id=@admin_costume_repair_player_id
);

SET @admin_costume_repair_empty_slot := (
  SELECT JSON_EXTRACT(items_bag,'$[64]')
  FROM `player`
  WHERE id=@admin_costume_repair_player_id
);

UPDATE `player`
SET items_bag=JSON_SET(
      items_bag,'$[64]',JSON_EXTRACT(@admin_costume_repair_item,'$')
    ),
    items_body=JSON_SET(
      items_body,'$[5]',JSON_EXTRACT(@admin_costume_repair_empty_slot,'$')
    )
WHERE id=@admin_costume_repair_player_id;

COMMIT;
