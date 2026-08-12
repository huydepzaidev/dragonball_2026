-- Keep the admin inventory counts within the signed-byte range used by the Unity client.
-- Service.player writes bag and box counts before the head-avatar table. Counts above 127
-- make the client packet parser overflow, so Panel.paintTopInfo never receives its avatars.
-- Only trailing empty slots are removed; all occupied slots and item payloads are preserved.
-- Safe to run more than once and scoped to the known account/player pair.
SET NAMES utf8mb4;
START TRANSACTION;

SET @admin_packet_repair_player_id := (
  SELECT p.id
  FROM `player` p
  INNER JOIN `account` a ON a.id=p.account_id
  WHERE a.username='1'
    AND p.name='admin'
    AND JSON_LENGTH(p.items_bag)=140
    AND JSON_LENGTH(p.items_box)=130
    AND NOT EXISTS (
      SELECT 1
      FROM (
        SELECT 127 n UNION ALL SELECT 128 UNION ALL SELECT 129 UNION ALL
        SELECT 130 UNION ALL SELECT 131 UNION ALL SELECT 132 UNION ALL
        SELECT 133 UNION ALL SELECT 134 UNION ALL SELECT 135 UNION ALL
        SELECT 136 UNION ALL SELECT 137 UNION ALL SELECT 138 UNION ALL SELECT 139
      ) tail
      WHERE CAST(JSON_UNQUOTE(JSON_EXTRACT(
              JSON_UNQUOTE(JSON_EXTRACT(p.items_bag,CONCAT('$[',tail.n,']'))),
              '$[0]'
            )) AS SIGNED)<>-1
    )
    AND NOT EXISTS (
      SELECT 1
      FROM (SELECT 127 n UNION ALL SELECT 128 UNION ALL SELECT 129) tail
      WHERE CAST(JSON_UNQUOTE(JSON_EXTRACT(
              JSON_UNQUOTE(JSON_EXTRACT(p.items_box,CONCAT('$[',tail.n,']'))),
              '$[0]'
            )) AS SIGNED)<>-1
    )
  LIMIT 1
);

UPDATE `player`
SET items_bag=JSON_REMOVE(
      items_bag,
      '$[139]','$[138]','$[137]','$[136]','$[135]','$[134]','$[133]',
      '$[132]','$[131]','$[130]','$[129]','$[128]','$[127]'
    ),
    items_box=JSON_REMOVE(items_box,'$[129]','$[128]','$[127]')
WHERE id=@admin_packet_repair_player_id;

-- Head 31 and its avatar mapping are valid. Restore the original appearance now that the
-- packet can reach the head-avatar table instead of keeping the diagnostic fallback head.
UPDATE `player` p
INNER JOIN `account` a ON a.id=p.account_id
SET p.head=31
WHERE a.username='1'
  AND p.name='admin'
  AND p.gender=0
  AND p.head=64;

COMMIT;
