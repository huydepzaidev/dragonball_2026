-- Store independent daily scores for the Daishinkan boss and summer-event rankings.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

CREATE TABLE IF NOT EXISTS daily_ranking_score (
  ranking_date date NOT NULL,
  ranking_type varchar(32) NOT NULL,
  player_id int NOT NULL,
  score bigint unsigned NOT NULL DEFAULT 0,
  updated_at timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (ranking_date,ranking_type,player_id),
  KEY idx_daily_ranking_order (ranking_date,ranking_type,score,player_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

COMMIT;
