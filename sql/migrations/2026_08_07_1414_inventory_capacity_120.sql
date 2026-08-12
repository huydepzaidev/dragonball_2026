-- Normalize the repaired admin inventory to the shared 120-slot server limit.
-- Service.player serializes these lengths as signed bytes, so the limit must stay at or
-- below 127. Only known trailing empty slots are removed; every item is preserved.
-- Safe to run more than once and scoped to the known account/player pair.
SET NAMES utf8mb4;
START TRANSACTION;

SET @inventory_120_player_id := (
  SELECT p.id
  FROM `player` p
  INNER JOIN `account` a ON a.id=p.account_id
  WHERE a.username='1'
    AND p.name='admin'
    AND JSON_LENGTH(p.items_bag)=127
    AND JSON_LENGTH(p.items_box)=127
    AND NOT EXISTS (
      SELECT 1
      FROM (
        SELECT 120 n UNION ALL SELECT 121 UNION ALL SELECT 122 UNION ALL
        SELECT 123 UNION ALL SELECT 124 UNION ALL SELECT 125 UNION ALL SELECT 126
      ) tail
      WHERE CAST(JSON_UNQUOTE(JSON_EXTRACT(
              JSON_UNQUOTE(JSON_EXTRACT(p.items_bag,CONCAT('$[',tail.n,']'))),
              '$[0]'
            )) AS SIGNED)<>-1
    )
    AND NOT EXISTS (
      SELECT 1
      FROM (
        SELECT 120 n UNION ALL SELECT 121 UNION ALL SELECT 122 UNION ALL
        SELECT 123 UNION ALL SELECT 124 UNION ALL SELECT 125 UNION ALL SELECT 126
      ) tail
      WHERE CAST(JSON_UNQUOTE(JSON_EXTRACT(
              JSON_UNQUOTE(JSON_EXTRACT(p.items_box,CONCAT('$[',tail.n,']'))),
              '$[0]'
            )) AS SIGNED)<>-1
    )
  LIMIT 1
);

UPDATE `player`
SET items_bag=JSON_REMOVE(
      items_bag,'$[126]','$[125]','$[124]','$[123]','$[122]','$[121]','$[120]'
    ),
    items_box=JSON_REMOVE(
      items_box,'$[126]','$[125]','$[124]','$[123]','$[122]','$[121]','$[120]'
    )
WHERE id=@inventory_120_player_id;

COMMIT;
