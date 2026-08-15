-- Give Set Gohan a 90% rate for both the five-item Set box and single-item Capsule.
-- Re-runnable: existing rows only replace weights; missing rows receive the canonical pool.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO `activation_reward_config`
  (`planet`,`planet_name`,`activation_options_json`,`activation_weights_json`,
   `bonus_options_json`,`enabled`)
VALUES
  (0,'Trái Đất','[127,128,129,233,245]',
   '{"127":25,"128":25,"129":25,"233":900,"245":25}','[]',1),
  (1,'Namek','[130,131,132,233,237]',
   '{"130":25,"131":25,"132":25,"233":900,"237":25}','[]',1),
  (2,'Xayda','[133,135,134,233,241]',
   '{"133":25,"135":25,"134":25,"233":900,"241":25}','[]',1)
ON DUPLICATE KEY UPDATE
  `activation_weights_json`=VALUES(`activation_weights_json`);

COMMIT;
