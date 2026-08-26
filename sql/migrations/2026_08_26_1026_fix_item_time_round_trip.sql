-- Version player item-time data and repair the shifted account 1111 snapshot.
-- Re-runnable: recovery only targets the known impossible TDLT signature and
-- the version marker is appended only to unversioned 33-field arrays.

SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `player` p
JOIN `account` a ON a.`id`=p.`account_id`
SET p.`data_item_time`=JSON_SET(
        p.`data_item_time`,
        '$[15]', CASE
            WHEN CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(p.`data_item_time`, '$[15]')), '0') AS UNSIGNED)=0
                 AND CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(p.`data_item_time`, '$[16]')), '0') AS UNSIGNED) BETWEEN 1 AND 600000
                THEN CAST(JSON_UNQUOTE(JSON_EXTRACT(p.`data_item_time`, '$[16]')) AS UNSIGNED)
            WHEN CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(p.`data_item_time`, '$[15]')), '0') AS UNSIGNED)=0
                THEN 570155
            ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(p.`data_item_time`, '$[15]')) AS UNSIGNED)
        END,
        '$[16]', CASE
            WHEN CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(p.`data_item_time`, '$[15]')), '0') AS UNSIGNED)=0
                THEN 6327
            ELSE CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(p.`data_item_time`, '$[16]')), '0') AS UNSIGNED)
        END,
        '$[17]', 0,
        '$[22]', CASE
            WHEN CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(p.`data_item_time`, '$[22]')), '0') AS UNSIGNED)=0
                THEN 1776335
            ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(p.`data_item_time`, '$[22]')) AS UNSIGNED)
        END,
        '$[23]', CASE
            WHEN CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(p.`data_item_time`, '$[22]')), '0') AS UNSIGNED)=0
                THEN 8060
            ELSE CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(p.`data_item_time`, '$[23]')), '0') AS UNSIGNED)
        END
    )
WHERE a.`username`='1111'
  AND JSON_VALID(p.`data_item_time`)
  AND JSON_TYPE(p.`data_item_time`)='ARRAY'
  AND JSON_LENGTH(p.`data_item_time`)=33
  AND CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(p.`data_item_time`, '$[17]')), '0') AS UNSIGNED)>500;

UPDATE `player`
SET `data_item_time`=JSON_SET(`data_item_time`, '$[17]', 0)
WHERE JSON_VALID(`data_item_time`)
  AND JSON_TYPE(`data_item_time`)='ARRAY'
  AND JSON_LENGTH(`data_item_time`)>=18
  AND CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`data_item_time`, '$[17]')), '0') AS UNSIGNED)>500;

UPDATE `player`
SET `data_item_time`=JSON_ARRAY_APPEND(`data_item_time`, '$', 0, '$', 2)
WHERE JSON_VALID(`data_item_time`)
  AND JSON_TYPE(`data_item_time`)='ARRAY'
  AND JSON_LENGTH(`data_item_time`)=32;

UPDATE `player`
SET `data_item_time`=JSON_ARRAY_APPEND(`data_item_time`, '$', 2)
WHERE JSON_VALID(`data_item_time`)
  AND JSON_TYPE(`data_item_time`)='ARRAY'
  AND JSON_LENGTH(`data_item_time`)=33;

COMMIT;
