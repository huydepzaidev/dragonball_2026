-- Seed the canonical activation-set pools and weights for both container IDs 1538 and 1559.
-- Existing Admin configuration is preserved.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO `activation_reward_config`
  (`planet`,`planet_name`,`activation_options_json`,`activation_weights_json`,
   `bonus_options_json`,`enabled`)
VALUES
  (0,'Trái Đất','[127,128,129,233,245]',
   '{"127":20,"128":120,"129":20,"233":120,"245":20}','[]',1),
  (1,'Namek','[130,131,132,233,237]',
   '{"130":120,"131":20,"132":20,"233":120,"237":20}','[]',1),
  (2,'Xayda','[133,135,134,233,241]',
   '{"133":20,"135":20,"134":120,"233":120,"241":20}','[]',1)
ON DUPLICATE KEY UPDATE `planet`=VALUES(`planet`);

COMMIT;
