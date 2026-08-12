-- Grant +50% total KI only while all five pieces of Set Picolo are equipped.
-- Server-side stat logic lives in NPoint; this migration keeps the option text canonical.
-- It also removes the per-item KI bonus from the superseded implementation.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

SET @namek_bonus_json := COALESCE((
  SELECT NULLIF(`bonus_options_json`,'')
  FROM `activation_reward_config`
  WHERE `planet`=1
  LIMIT 1
),'[]');

SET @namek_ki_path := JSON_UNQUOTE(JSON_SEARCH(
  @namek_bonus_json,'one','103',NULL,'$[*].id'
));

UPDATE `activation_reward_config`
SET `bonus_options_json`=IF(
  @namek_ki_path IS NULL,
  @namek_bonus_json,
  JSON_REMOVE(@namek_bonus_json,REPLACE(@namek_ki_path,'.id',''))
)
WHERE `planet`=1;

UPDATE `item_option_template`
SET `NAME`='$(5 món +100% sát thương Masenkosappo, +50% KI)'
WHERE `id`=142;

COMMIT;
