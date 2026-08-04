-- Central event control panel shared by the website and game server.
-- Safe to run more than once.

CREATE TABLE IF NOT EXISTS `game_event_catalog` (
  `event_key` varchar(40) NOT NULL,
  `event_name` varchar(100) NOT NULL,
  `summary` varchar(500) NOT NULL DEFAULT '',
  `enabled` tinyint(1) NOT NULL DEFAULT 0,
  `sort_order` int NOT NULL DEFAULT 0,
  `reset_version` int NOT NULL DEFAULT 0,
  `last_action` varchar(20) DEFAULT NULL,
  `last_result` varchar(500) DEFAULT NULL,
  `last_changed_at` datetime DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`event_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `game_event_item` (
  `event_key` varchar(40) NOT NULL,
  `item_id` int NOT NULL,
  `item_role` varchar(50) NOT NULL DEFAULT 'Vật phẩm sự kiện',
  `purge_on_reset` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`event_key`,`item_id`),
  KEY `idx_game_event_item_id` (`item_id`),
  CONSTRAINT `fk_game_event_item_event` FOREIGN KEY (`event_key`) REFERENCES `game_event_catalog` (`event_key`) ON DELETE CASCADE,
  CONSTRAINT `fk_game_event_item_template` FOREIGN KEY (`item_id`) REFERENCES `item_template` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `game_event_npc` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_key` varchar(40) NOT NULL,
  `npc_id` int NOT NULL,
  `map_id` int DEFAULT NULL,
  `x` int DEFAULT NULL,
  `y` int DEFAULT NULL,
  `npc_role` varchar(100) NOT NULL DEFAULT 'NPC sự kiện',
  `managed_runtime` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_game_event_npc_place` (`event_key`,`npc_id`,`map_id`,`x`,`y`),
  KEY `idx_game_event_npc_lookup` (`npc_id`,`map_id`),
  CONSTRAINT `fk_game_event_npc_event` FOREIGN KEY (`event_key`) REFERENCES `game_event_catalog` (`event_key`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `game_event_boss` (
  `event_key` varchar(40) NOT NULL,
  `boss_id` int NOT NULL,
  `quantity` int NOT NULL DEFAULT 1,
  `boss_role` varchar(100) NOT NULL DEFAULT 'Boss sự kiện',
  PRIMARY KEY (`event_key`,`boss_id`),
  KEY `idx_game_event_boss_id` (`boss_id`),
  CONSTRAINT `fk_game_event_boss_event` FOREIGN KEY (`event_key`) REFERENCES `game_event_catalog` (`event_key`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `game_event_command` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_key` varchar(40) NOT NULL,
  `target_enabled` tinyint(1) NOT NULL,
  `status` enum('PENDING','PROCESSING','DONE','FAILED') NOT NULL DEFAULT 'PENDING',
  `requested_by` varchar(50) NOT NULL,
  `result_message` varchar(500) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_game_event_command_queue` (`status`,`id`),
  KEY `idx_game_event_command_event` (`event_key`,`id`),
  CONSTRAINT `fk_game_event_command_event` FOREIGN KEY (`event_key`) REFERENCES `game_event_catalog` (`event_key`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `game_event_player_backup` (
  `command_id` bigint NOT NULL,
  `player_id` bigint NOT NULL,
  `event_key` varchar(40) NOT NULL,
  `items_body` longtext DEFAULT NULL,
  `items_bag` longtext DEFAULT NULL,
  `items_box` longtext DEFAULT NULL,
  `items_box_lucky_round` longtext DEFAULT NULL,
  `items_daban` longtext DEFAULT NULL,
  `pet` longtext DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`command_id`,`player_id`),
  KEY `idx_game_event_backup_player` (`player_id`,`created_at`),
  CONSTRAINT `fk_game_event_backup_command` FOREIGN KEY (`command_id`) REFERENCES `game_event_command` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `game_event_catalog`
(`event_key`,`event_name`,`summary`,`enabled`,`sort_order`) VALUES
('lunar_new_year','Tết Nguyên Đán','Nấu bánh, lì xì, mâm ngũ quả, Lân Con và cửa hàng Tết.',0,10),
('valentine','Valentine','Quà tình yêu, sôcôla trái tim, hoa hồng và vật phẩm Valentine.',0,20),
('womens_day','Quốc tế Phụ nữ 8/3','Hoa hồng, trồng hoa, thiệp 8/3 và phần thưởng dành cho ngày 8/3.',0,30),
('hung_vuong','Giỗ Tổ Hùng Vương','Sơn Tinh, Thủy Tinh, bánh chưng Lang Liêu và phần thưởng Hùng Vương.',1,40),
('summer','Sự kiện hè','Quần đi biển, trái dừa, nguyên liệu biển và các hoạt động mùa hè.',1,50),
('mid_autumn','Tết Trung Thu','Bánh trung thu, lồng đèn, Thỏ Ngọc, Nhật Thần và Nguyệt Thần.',0,60),
('halloween','Halloween','Bí ngô, kẹo ma, vật phẩm linh hồn và boss Halloween.',0,70),
('teachers_day','Ngày Nhà giáo 20/11','Trà, thiệp, hoa, dụng cụ học tập và cải trang thầy giáo.',0,80),
('christmas','Giáng Sinh','Tất Noel, quà, người tuyết, cây thông và Ông Già Noel.',0,90),
('top_up','Nạp thẻ','Sự kiện nạp thẻ và các mốc thưởng nạp.',1,100)
ON DUPLICATE KEY UPDATE
  `event_name`=VALUES(`event_name`),
  `summary`=VALUES(`summary`),
  `sort_order`=VALUES(`sort_order`);

-- Only exclusive event items are listed. Shared currencies and normal equipment
-- are intentionally excluded so one event cannot erase unrelated possessions.
INSERT IGNORE INTO `game_event_item` (`event_key`,`item_id`,`item_role`)
SELECT seed.event_key, seed.item_id, seed.item_role
FROM (
  SELECT 'summer' event_key, 691 item_id, 'Trang bị hè' item_role UNION ALL SELECT 'summer',692,'Trang bị hè' UNION ALL SELECT 'summer',693,'Trang bị hè' UNION ALL
  SELECT 'summer',694,'Vật phẩm hỗ trợ' UNION ALL SELECT 'summer',695,'Nguyên liệu hè' UNION ALL SELECT 'summer',696,'Nguyên liệu hè' UNION ALL SELECT 'summer',697,'Nguyên liệu hè' UNION ALL SELECT 'summer',698,'Nguyên liệu hè' UNION ALL
  SELECT 'summer',1232,'Vật phẩm hè' UNION ALL SELECT 'summer',1233,'Vật phẩm hè' UNION ALL SELECT 'summer',1234,'Cải trang hè' UNION ALL SELECT 'summer',1235,'Cải trang hè' UNION ALL SELECT 'summer',1236,'Cải trang hè' UNION ALL
  SELECT 'summer',1237,'Nguyên liệu hè' UNION ALL SELECT 'summer',1238,'Nguyên liệu hè' UNION ALL SELECT 'summer',1239,'Nguyên liệu hè' UNION ALL SELECT 'summer',1240,'Nguyên liệu hè' UNION ALL SELECT 'summer',1241,'Phần thưởng hè' UNION ALL SELECT 'summer',1242,'Phần thưởng hè' UNION ALL
  SELECT 'summer',1243,'Pet hè' UNION ALL SELECT 'summer',1244,'Pet hè' UNION ALL SELECT 'summer',1245,'Vật phẩm bắt bọ' UNION ALL SELECT 'summer',1246,'Vật phẩm bắt bọ' UNION ALL SELECT 'summer',1247,'Vật phẩm bắt bọ' UNION ALL SELECT 'summer',1248,'Vật phẩm bắt bọ' UNION ALL SELECT 'summer',1249,'Vật phẩm bắt bọ' UNION ALL SELECT 'summer',1250,'Vật phẩm bắt bọ' UNION ALL SELECT 'summer',1251,'Vật phẩm bắt bọ' UNION ALL SELECT 'summer',1252,'Thú cưỡi hè' UNION ALL SELECT 'summer',1253,'Thú cưỡi hè' UNION ALL
  SELECT 'summer',1562,'Vật phẩm hè' UNION ALL SELECT 'summer',1563,'Thú cưỡi hè' UNION ALL SELECT 'summer',1564,'Pet hè' UNION ALL SELECT 'summer',1565,'Vật phẩm hè' UNION ALL SELECT 'summer',1566,'Cải trang hè' UNION ALL SELECT 'summer',1567,'Cải trang hè' UNION ALL SELECT 'summer',1568,'Pet hè' UNION ALL SELECT 'summer',1569,'Phần thưởng hè' UNION ALL SELECT 'summer',1570,'Vật phẩm hè' UNION ALL SELECT 'summer',1571,'Phụ kiện hè' UNION ALL
  SELECT 'summer',1605,'Quà hè' UNION ALL SELECT 'summer',1606,'Quà hè' UNION ALL SELECT 'summer',1607,'Quà hè' UNION ALL SELECT 'summer',1608,'Quà hè' UNION ALL SELECT 'summer',1609,'Nguyên liệu hè' UNION ALL SELECT 'summer',1610,'Nguyên liệu hè' UNION ALL SELECT 'summer',1611,'Pet hè' UNION ALL SELECT 'summer',1612,'Nguyên liệu hè' UNION ALL SELECT 'summer',1613,'Nguyên liệu hè' UNION ALL SELECT 'summer',1614,'Vật phẩm hè' UNION ALL SELECT 'summer',1615,'Vật phẩm hè' UNION ALL SELECT 'summer',1616,'Vật phẩm hè' UNION ALL SELECT 'summer',1617,'Phụ kiện hè' UNION ALL SELECT 'summer',1618,'Phụ kiện hè' UNION ALL SELECT 'summer',1619,'Phụ kiện hè' UNION ALL SELECT 'summer',1620,'Pet hè' UNION ALL SELECT 'summer',1621,'Pet hè' UNION ALL SELECT 'summer',1622,'Pet hè' UNION ALL
  SELECT 'summer',1657,'Cải trang hè' UNION ALL SELECT 'summer',1664,'Nguyên liệu hè' UNION ALL SELECT 'summer',1665,'Nguyên liệu hè' UNION ALL SELECT 'summer',1666,'Vật phẩm hè' UNION ALL SELECT 'summer',1667,'Cải trang hè' UNION ALL SELECT 'summer',1668,'Pet hè' UNION ALL SELECT 'summer',1669,'Phụ kiện hè' UNION ALL SELECT 'summer',1670,'Phụ kiện hè' UNION ALL SELECT 'summer',1671,'Quà hè' UNION ALL SELECT 'summer',1672,'Vật phẩm hè' UNION ALL SELECT 'summer',1673,'Danh hiệu hè' UNION ALL
  SELECT 'summer',1803,'Cải trang hè' UNION ALL SELECT 'summer',1804,'Cải trang hè' UNION ALL SELECT 'summer',1805,'Vật phẩm hè' UNION ALL SELECT 'summer',1806,'Phụ kiện hè' UNION ALL

  SELECT 'halloween',585,'Nguyên liệu Halloween' UNION ALL SELECT 'halloween',642,'Cải trang Halloween' UNION ALL SELECT 'halloween',643,'Cải trang Halloween' UNION ALL SELECT 'halloween',644,'Cải trang Halloween' UNION ALL SELECT 'halloween',645,'Cải trang Halloween' UNION ALL SELECT 'halloween',646,'Cải trang Halloween' UNION ALL SELECT 'halloween',647,'Cải trang Halloween' UNION ALL
  SELECT 'halloween',702,'Ngọc bí ngô' UNION ALL SELECT 'halloween',703,'Ngọc bí ngô' UNION ALL SELECT 'halloween',704,'Ngọc bí ngô' UNION ALL SELECT 'halloween',705,'Ngọc bí ngô' UNION ALL SELECT 'halloween',706,'Ngọc bí ngô' UNION ALL SELECT 'halloween',707,'Ngọc bí ngô' UNION ALL SELECT 'halloween',708,'Ngọc bí ngô' UNION ALL
  SELECT 'halloween',739,'Cải trang Halloween' UNION ALL SELECT 'halloween',740,'Phụ kiện Halloween' UNION ALL SELECT 'halloween',741,'Phụ kiện Halloween' UNION ALL SELECT 'halloween',742,'Cải trang Halloween' UNION ALL SELECT 'halloween',743,'Thú cưỡi Halloween' UNION ALL
  SELECT 'halloween',1107,'Pet Halloween' UNION ALL SELECT 'halloween',1108,'Phụ kiện Halloween' UNION ALL SELECT 'halloween',1109,'Phụ kiện Halloween' UNION ALL SELECT 'halloween',1114,'Pet Halloween' UNION ALL SELECT 'halloween',1115,'Máy dò sự kiện' UNION ALL SELECT 'halloween',1116,'Quà Halloween' UNION ALL SELECT 'halloween',1117,'Thiệp Halloween' UNION ALL
  SELECT 'halloween',1258,'Linh hồn Halloween' UNION ALL SELECT 'halloween',1259,'Vật phẩm Halloween' UNION ALL SELECT 'halloween',1260,'Vật phẩm Halloween' UNION ALL SELECT 'halloween',1261,'Vật phẩm Halloween' UNION ALL SELECT 'halloween',1262,'Vật phẩm Halloween' UNION ALL SELECT 'halloween',1263,'Vật phẩm Halloween' UNION ALL SELECT 'halloween',1264,'Máy dò Halloween' UNION ALL
  SELECT 'halloween',1344,'Phụ kiện Halloween' UNION ALL SELECT 'halloween',1345,'Thú cưỡi Halloween' UNION ALL SELECT 'halloween',1346,'Thú cưỡi Halloween' UNION ALL SELECT 'halloween',1347,'Pet Halloween' UNION ALL SELECT 'halloween',1348,'Nguyên liệu Halloween' UNION ALL SELECT 'halloween',1349,'Nguyên liệu Halloween' UNION ALL SELECT 'halloween',1350,'Kẹo Halloween' UNION ALL SELECT 'halloween',1351,'Nguyên liệu Halloween' UNION ALL SELECT 'halloween',1352,'Kẹo Halloween' UNION ALL SELECT 'halloween',1353,'Kẹo Halloween' UNION ALL SELECT 'halloween',1354,'Nguyên liệu Halloween' UNION ALL SELECT 'halloween',1355,'Kẹo Halloween' UNION ALL SELECT 'halloween',1356,'Quà Halloween' UNION ALL SELECT 'halloween',1357,'Kẹo Halloween' UNION ALL
  SELECT 'halloween',1704,'Thú cưỡi Halloween' UNION ALL SELECT 'halloween',1725,'Quà Halloween' UNION ALL SELECT 'halloween',1726,'Kẹo Halloween' UNION ALL SELECT 'halloween',1727,'Pet Halloween' UNION ALL SELECT 'halloween',1728,'Quà Halloween' UNION ALL SELECT 'halloween',1737,'Máy dò Halloween' UNION ALL

  SELECT 'christmas',386,'Nón Noel' UNION ALL SELECT 'christmas',387,'Nón Noel' UNION ALL SELECT 'christmas',388,'Nón Noel' UNION ALL SELECT 'christmas',389,'Nón Noel' UNION ALL SELECT 'christmas',390,'Nón Noel' UNION ALL SELECT 'christmas',391,'Nón Noel' UNION ALL SELECT 'christmas',392,'Nón Noel' UNION ALL SELECT 'christmas',393,'Nón Noel' UNION ALL SELECT 'christmas',394,'Nón Noel' UNION ALL
  SELECT 'christmas',533,'Kẹo Giáng Sinh' UNION ALL SELECT 'christmas',648,'Quà Giáng Sinh' UNION ALL SELECT 'christmas',649,'Tất Giáng Sinh' UNION ALL SELECT 'christmas',745,'Phụ kiện Noel' UNION ALL SELECT 'christmas',746,'Thú cưỡi Noel' UNION ALL
  SELECT 'christmas',1155,'Cải trang Noel' UNION ALL SELECT 'christmas',1156,'Cải trang Noel' UNION ALL SELECT 'christmas',1157,'Cải trang Noel' UNION ALL SELECT 'christmas',1158,'Chú lùn Noel' UNION ALL SELECT 'christmas',1159,'Chú lùn Noel' UNION ALL SELECT 'christmas',1160,'Chú lùn Noel' UNION ALL SELECT 'christmas',1161,'Chú lùn Noel' UNION ALL SELECT 'christmas',1162,'Chú lùn Noel' UNION ALL SELECT 'christmas',1163,'Chú lùn Noel' UNION ALL SELECT 'christmas',1164,'Chú lùn Noel' UNION ALL SELECT 'christmas',1165,'Nguyên liệu Noel' UNION ALL SELECT 'christmas',1166,'Nguyên liệu Noel' UNION ALL SELECT 'christmas',1167,'Nguyên liệu Noel' UNION ALL SELECT 'christmas',1168,'Nguyên liệu Noel' UNION ALL SELECT 'christmas',1169,'Nguyên liệu Noel' UNION ALL SELECT 'christmas',1170,'Quà Noel' UNION ALL SELECT 'christmas',1171,'Quà Noel' UNION ALL SELECT 'christmas',1172,'Thú cưỡi Noel' UNION ALL SELECT 'christmas',1173,'Quà Noel' UNION ALL
  SELECT 'christmas',1424,'Cải trang Noel' UNION ALL SELECT 'christmas',1435,'Pet Noel' UNION ALL SELECT 'christmas',1436,'Cải trang Noel' UNION ALL SELECT 'christmas',1437,'Cải trang Noel' UNION ALL SELECT 'christmas',1444,'Nguyên liệu Noel' UNION ALL SELECT 'christmas',1445,'Nguyên liệu Noel' UNION ALL SELECT 'christmas',1446,'Nguyên liệu Noel' UNION ALL SELECT 'christmas',1447,'Nguyên liệu Noel' UNION ALL SELECT 'christmas',1448,'Người tuyết' UNION ALL SELECT 'christmas',1449,'Người tuyết' UNION ALL SELECT 'christmas',1451,'Nón Noel' UNION ALL SELECT 'christmas',1452,'Pet Noel' UNION ALL SELECT 'christmas',1455,'Thú cưỡi Noel' UNION ALL SELECT 'christmas',1457,'Danh hiệu Noel' UNION ALL SELECT 'christmas',1458,'Pet Noel' UNION ALL SELECT 'christmas',1459,'Nguyên liệu Noel' UNION ALL SELECT 'christmas',1460,'Nguyên liệu Noel' UNION ALL SELECT 'christmas',1461,'Nguyên liệu Noel' UNION ALL SELECT 'christmas',1462,'Nguyên liệu Noel' UNION ALL SELECT 'christmas',1463,'Nguyên liệu Noel' UNION ALL SELECT 'christmas',1464,'Kẹo Noel' UNION ALL SELECT 'christmas',1465,'Thú cưỡi Noel' UNION ALL SELECT 'christmas',1466,'Thú cưỡi Noel' UNION ALL SELECT 'christmas',1467,'Phụ kiện Noel' UNION ALL SELECT 'christmas',1573,'Vật phẩm Noel' UNION ALL SELECT 'christmas',1748,'Pet Noel' UNION ALL SELECT 'christmas',1749,'Thú cưỡi Noel' UNION ALL SELECT 'christmas',1750,'Pet Noel' UNION ALL

  SELECT 'lunar_new_year',397,'Quà Tết' UNION ALL SELECT 'lunar_new_year',398,'Quà Tết' UNION ALL SELECT 'lunar_new_year',399,'Thiệp Tết' UNION ALL SELECT 'lunar_new_year',553,'Pháo Tết' UNION ALL SELECT 'lunar_new_year',554,'Pháo Tết' UNION ALL SELECT 'lunar_new_year',568,'Vật phẩm Tết' UNION ALL SELECT 'lunar_new_year',569,'Dưa hấu Tết' UNION ALL SELECT 'lunar_new_year',668,'Quà Tết' UNION ALL SELECT 'lunar_new_year',669,'Dưa hấu Tết' UNION ALL SELECT 'lunar_new_year',717,'Lì xì' UNION ALL SELECT 'lunar_new_year',718,'Vé Tết' UNION ALL
  SELECT 'lunar_new_year',748,'Nguyên liệu Tết' UNION ALL SELECT 'lunar_new_year',749,'Nguyên liệu Tết' UNION ALL SELECT 'lunar_new_year',750,'Nguyên liệu Tết' UNION ALL SELECT 'lunar_new_year',751,'Nguyên liệu Tết' UNION ALL SELECT 'lunar_new_year',752,'Bánh Tết' UNION ALL SELECT 'lunar_new_year',753,'Bánh Tết' UNION ALL SELECT 'lunar_new_year',754,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',755,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',756,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',757,'Quà Tết' UNION ALL SELECT 'lunar_new_year',758,'Quà Tết' UNION ALL
  SELECT 'lunar_new_year',1177,'Mâm ngũ quả' UNION ALL SELECT 'lunar_new_year',1178,'Mâm ngũ quả' UNION ALL SELECT 'lunar_new_year',1179,'Mâm ngũ quả' UNION ALL SELECT 'lunar_new_year',1180,'Mâm ngũ quả' UNION ALL SELECT 'lunar_new_year',1181,'Mâm ngũ quả' UNION ALL SELECT 'lunar_new_year',1182,'Mâm ngũ quả' UNION ALL SELECT 'lunar_new_year',1183,'Lì xì Tết' UNION ALL SELECT 'lunar_new_year',1184,'Quà Tết' UNION ALL SELECT 'lunar_new_year',1185,'Phụ kiện Tết' UNION ALL SELECT 'lunar_new_year',1186,'Phụ kiện Tết' UNION ALL SELECT 'lunar_new_year',1187,'Quà Tết' UNION ALL SELECT 'lunar_new_year',1188,'Pet Tết' UNION ALL SELECT 'lunar_new_year',1191,'Thiệp Tết' UNION ALL SELECT 'lunar_new_year',1192,'Thiệp Tết' UNION ALL SELECT 'lunar_new_year',1193,'Thiệp Tết' UNION ALL SELECT 'lunar_new_year',1194,'Quà Tết' UNION ALL SELECT 'lunar_new_year',1195,'Vật phẩm Tết' UNION ALL SELECT 'lunar_new_year',1196,'Vật phẩm Tết' UNION ALL SELECT 'lunar_new_year',1197,'Phụ kiện Tết' UNION ALL SELECT 'lunar_new_year',1198,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',1199,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',1200,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',1201,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',1202,'Pet Tết' UNION ALL SELECT 'lunar_new_year',1203,'Pet Tết' UNION ALL
  SELECT 'lunar_new_year',1468,'Thú cưỡi Tết' UNION ALL SELECT 'lunar_new_year',1469,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',1470,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',1471,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',1472,'Nguyên liệu Tết' UNION ALL SELECT 'lunar_new_year',1473,'Nguyên liệu Tết' UNION ALL SELECT 'lunar_new_year',1474,'Nguyên liệu Tết' UNION ALL SELECT 'lunar_new_year',1475,'Nguyên liệu Tết' UNION ALL SELECT 'lunar_new_year',1476,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',1477,'Thú cưỡi Tết' UNION ALL SELECT 'lunar_new_year',1478,'Phụ kiện Tết' UNION ALL SELECT 'lunar_new_year',1479,'Phụ kiện Tết' UNION ALL SELECT 'lunar_new_year',1482,'Pet Tết' UNION ALL SELECT 'lunar_new_year',1483,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',1484,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',1485,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',1486,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',1487,'Thú cưỡi Tết' UNION ALL SELECT 'lunar_new_year',1493,'Lì xì Tết' UNION ALL SELECT 'lunar_new_year',1494,'Thiệp Tết' UNION ALL SELECT 'lunar_new_year',1495,'Thiệp Tết' UNION ALL SELECT 'lunar_new_year',1496,'Thiệp Tết' UNION ALL SELECT 'lunar_new_year',1497,'Pet Tết' UNION ALL SELECT 'lunar_new_year',1498,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',1499,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',1500,'Cải trang Tết' UNION ALL SELECT 'lunar_new_year',1501,'Quà Tết' UNION ALL SELECT 'lunar_new_year',1502,'Phụ kiện Tết' UNION ALL

  SELECT 'mid_autumn',465,'Bánh Trung Thu' UNION ALL SELECT 'mid_autumn',466,'Bánh Trung Thu' UNION ALL SELECT 'mid_autumn',467,'Lồng đèn' UNION ALL SELECT 'mid_autumn',468,'Lồng đèn' UNION ALL SELECT 'mid_autumn',469,'Lồng đèn' UNION ALL SELECT 'mid_autumn',470,'Lồng đèn' UNION ALL SELECT 'mid_autumn',471,'Lồng đèn' UNION ALL SELECT 'mid_autumn',472,'Bánh Trung Thu' UNION ALL SELECT 'mid_autumn',473,'Quà Trung Thu' UNION ALL
  SELECT 'mid_autumn',733,'Thú cưỡi Trung Thu' UNION ALL SELECT 'mid_autumn',734,'Thú cưỡi Trung Thu' UNION ALL SELECT 'mid_autumn',735,'Thú cưỡi Trung Thu' UNION ALL SELECT 'mid_autumn',736,'Quà Trung Thu' UNION ALL SELECT 'mid_autumn',737,'Quà Trung Thu' UNION ALL
  SELECT 'mid_autumn',800,'Lồng đèn' UNION ALL SELECT 'mid_autumn',801,'Lồng đèn' UNION ALL SELECT 'mid_autumn',802,'Lồng đèn' UNION ALL SELECT 'mid_autumn',803,'Lồng đèn' UNION ALL SELECT 'mid_autumn',804,'Lồng đèn' UNION ALL SELECT 'mid_autumn',805,'Phụ kiện Trung Thu' UNION ALL SELECT 'mid_autumn',806,'Cải trang Trung Thu' UNION ALL SELECT 'mid_autumn',1276,'Cải trang Trung Thu' UNION ALL SELECT 'mid_autumn',1277,'Cải trang Trung Thu' UNION ALL
  SELECT 'mid_autumn',1301,'Phụ kiện Trung Thu' UNION ALL SELECT 'mid_autumn',1302,'Cải trang Trung Thu' UNION ALL SELECT 'mid_autumn',1303,'Lồng đèn' UNION ALL SELECT 'mid_autumn',1304,'Nguyên liệu Trung Thu' UNION ALL SELECT 'mid_autumn',1305,'Nguyên liệu Trung Thu' UNION ALL SELECT 'mid_autumn',1306,'Bánh Trung Thu' UNION ALL SELECT 'mid_autumn',1307,'Bánh Trung Thu' UNION ALL SELECT 'mid_autumn',1308,'Bánh Trung Thu' UNION ALL SELECT 'mid_autumn',1310,'Pet Trung Thu' UNION ALL SELECT 'mid_autumn',1311,'Lồng đèn' UNION ALL SELECT 'mid_autumn',1312,'Nguyên liệu Trung Thu' UNION ALL SELECT 'mid_autumn',1313,'Bánh Trung Thu' UNION ALL SELECT 'mid_autumn',1314,'Nguyên liệu Trung Thu' UNION ALL SELECT 'mid_autumn',1315,'Nguyên liệu Trung Thu' UNION ALL SELECT 'mid_autumn',1316,'Quà Trung Thu' UNION ALL SELECT 'mid_autumn',1317,'Quà Trung Thu' UNION ALL SELECT 'mid_autumn',1318,'Pet Trung Thu' UNION ALL SELECT 'mid_autumn',1675,'Lồng đèn' UNION ALL SELECT 'mid_autumn',1700,'Cải trang Trung Thu' UNION ALL SELECT 'mid_autumn',1701,'Quà Trung Thu' UNION ALL SELECT 'mid_autumn',1702,'Phụ kiện Trung Thu' UNION ALL

  SELECT 'hung_vuong',1214,'Nguyên liệu Hùng Vương' UNION ALL SELECT 'hung_vuong',1215,'Nguyên liệu Hùng Vương' UNION ALL SELECT 'hung_vuong',1216,'Nguyên liệu Hùng Vương' UNION ALL SELECT 'hung_vuong',1217,'Nguyên liệu Hùng Vương' UNION ALL SELECT 'hung_vuong',1218,'Nguyên liệu Hùng Vương' UNION ALL SELECT 'hung_vuong',1219,'Bánh chưng Hùng Vương' UNION ALL SELECT 'hung_vuong',1220,'Nguyên liệu Hùng Vương' UNION ALL SELECT 'hung_vuong',1221,'Nguyên liệu Hùng Vương' UNION ALL SELECT 'hung_vuong',1222,'Nguyên liệu Hùng Vương' UNION ALL SELECT 'hung_vuong',1223,'Phụ kiện Hùng Vương' UNION ALL SELECT 'hung_vuong',1224,'Pet Hùng Vương' UNION ALL SELECT 'hung_vuong',1225,'Pet Hùng Vương' UNION ALL SELECT 'hung_vuong',1226,'Pet Hùng Vương' UNION ALL SELECT 'hung_vuong',1227,'Quà Hùng Vương' UNION ALL SELECT 'hung_vuong',1228,'Quà Hùng Vương' UNION ALL
  SELECT 'hung_vuong',1533,'Pet Hùng Vương' UNION ALL SELECT 'hung_vuong',1534,'Thú cưỡi Hùng Vương' UNION ALL SELECT 'hung_vuong',1539,'Phụ kiện Hùng Vương' UNION ALL SELECT 'hung_vuong',1542,'Bánh Hùng Vương' UNION ALL SELECT 'hung_vuong',1543,'Bánh Hùng Vương' UNION ALL SELECT 'hung_vuong',1544,'Nguyên liệu Hùng Vương' UNION ALL SELECT 'hung_vuong',1545,'Nguyên liệu Hùng Vương' UNION ALL SELECT 'hung_vuong',1546,'Nguyên liệu Hùng Vương' UNION ALL SELECT 'hung_vuong',1547,'Nguyên liệu Hùng Vương' UNION ALL SELECT 'hung_vuong',1548,'Nguyên liệu Hùng Vương' UNION ALL SELECT 'hung_vuong',1549,'Nguyên liệu Hùng Vương' UNION ALL SELECT 'hung_vuong',1550,'Pet Hùng Vương' UNION ALL SELECT 'hung_vuong',1551,'Pet Hùng Vương' UNION ALL SELECT 'hung_vuong',1552,'Vật phẩm Hùng Vương' UNION ALL SELECT 'hung_vuong',1556,'Bánh chưng Hùng Vương' UNION ALL SELECT 'hung_vuong',1558,'Vật phẩm Hùng Vương' UNION ALL
  SELECT 'hung_vuong',1761,'Cải trang Hùng Vương' UNION ALL SELECT 'hung_vuong',1762,'Cải trang Hùng Vương' UNION ALL SELECT 'hung_vuong',1763,'Cải trang Hùng Vương' UNION ALL SELECT 'hung_vuong',1764,'Cải trang Hùng Vương' UNION ALL SELECT 'hung_vuong',1765,'Pet Hùng Vương' UNION ALL SELECT 'hung_vuong',1766,'Pet Hùng Vương' UNION ALL SELECT 'hung_vuong',1767,'Pet Hùng Vương' UNION ALL SELECT 'hung_vuong',1768,'Pet Hùng Vương' UNION ALL SELECT 'hung_vuong',1769,'Pet Hùng Vương' UNION ALL SELECT 'hung_vuong',1770,'Pet Hùng Vương' UNION ALL SELECT 'hung_vuong',1771,'Pet Hùng Vương' UNION ALL SELECT 'hung_vuong',1772,'Phụ kiện Hùng Vương' UNION ALL SELECT 'hung_vuong',1773,'Phụ kiện Hùng Vương' UNION ALL SELECT 'hung_vuong',1776,'Quà Hùng Vương' UNION ALL SELECT 'hung_vuong',1777,'Quà Hùng Vương' UNION ALL

  SELECT 'valentine',1206,'Phụ kiện Valentine' UNION ALL SELECT 'valentine',1207,'Pet Valentine' UNION ALL SELECT 'valentine',1208,'Cải trang Valentine' UNION ALL SELECT 'valentine',1209,'Cải trang Valentine' UNION ALL SELECT 'valentine',1210,'Cải trang Valentine' UNION ALL SELECT 'valentine',1503,'Cải trang Valentine' UNION ALL SELECT 'valentine',1504,'Cải trang Valentine' UNION ALL SELECT 'valentine',1505,'Nguyên liệu Valentine' UNION ALL SELECT 'valentine',1506,'Nguyên liệu Valentine' UNION ALL SELECT 'valentine',1507,'Sôcôla Valentine' UNION ALL SELECT 'valentine',1508,'Hoa Valentine' UNION ALL SELECT 'valentine',1509,'Nguyên liệu Valentine' UNION ALL SELECT 'valentine',1510,'Quà Valentine' UNION ALL SELECT 'valentine',1511,'Quà Valentine' UNION ALL SELECT 'valentine',1512,'Cải trang Valentine' UNION ALL SELECT 'valentine',1513,'Thú cưỡi Valentine' UNION ALL SELECT 'valentine',1514,'Danh hiệu Valentine' UNION ALL SELECT 'valentine',1515,'Phụ kiện Valentine' UNION ALL SELECT 'valentine',1516,'Quà Valentine' UNION ALL SELECT 'valentine',1519,'Phụ kiện Valentine' UNION ALL SELECT 'valentine',1520,'Phụ kiện Valentine' UNION ALL SELECT 'valentine',1736,'Quà Valentine' UNION ALL SELECT 'valentine',1738,'Quà Valentine' UNION ALL

  SELECT 'womens_day',723,'Hoa 8/3' UNION ALL SELECT 'womens_day',1521,'Thiệp 8/3' UNION ALL SELECT 'womens_day',1525,'Nguyên liệu 8/3' UNION ALL SELECT 'womens_day',1526,'Nguyên liệu 8/3' UNION ALL SELECT 'womens_day',1527,'Nguyên liệu 8/3' UNION ALL SELECT 'womens_day',1528,'Nguyên liệu 8/3' UNION ALL SELECT 'womens_day',1529,'Nguyên liệu 8/3' UNION ALL SELECT 'womens_day',1530,'Hoa 8/3' UNION ALL SELECT 'womens_day',1531,'Phụ kiện 8/3' UNION ALL

  SELECT 'teachers_day',1362,'Cải trang 20/11' UNION ALL SELECT 'teachers_day',1363,'Thú cưỡi 20/11' UNION ALL SELECT 'teachers_day',1364,'Nguyên liệu 20/11' UNION ALL SELECT 'teachers_day',1365,'Nguyên liệu 20/11' UNION ALL SELECT 'teachers_day',1366,'Nguyên liệu 20/11' UNION ALL SELECT 'teachers_day',1367,'Nguyên liệu 20/11' UNION ALL SELECT 'teachers_day',1368,'Nguyên liệu 20/11' UNION ALL SELECT 'teachers_day',1369,'Quà 20/11' UNION ALL SELECT 'teachers_day',1370,'Quà 20/11' UNION ALL SELECT 'teachers_day',1371,'Cải trang 20/11' UNION ALL SELECT 'teachers_day',1372,'Nguyên liệu 20/11' UNION ALL SELECT 'teachers_day',1373,'Nguyên liệu 20/11' UNION ALL SELECT 'teachers_day',1374,'Nguyên liệu 20/11' UNION ALL SELECT 'teachers_day',1375,'Nguyên liệu 20/11' UNION ALL SELECT 'teachers_day',1376,'Thiệp 20/11' UNION ALL SELECT 'teachers_day',1377,'Thiệp 20/11' UNION ALL SELECT 'teachers_day',1378,'Cải trang 20/11' UNION ALL SELECT 'teachers_day',1379,'Cải trang 20/11' UNION ALL SELECT 'teachers_day',1380,'Cải trang 20/11' UNION ALL SELECT 'teachers_day',1381,'Cải trang 20/11' UNION ALL SELECT 'teachers_day',1382,'Vật phẩm 20/11' UNION ALL SELECT 'teachers_day',1383,'Cải trang 20/11' UNION ALL SELECT 'teachers_day',1384,'Cải trang 20/11' UNION ALL SELECT 'teachers_day',1385,'Cải trang 20/11' UNION ALL SELECT 'teachers_day',1386,'Phụ kiện 20/11' UNION ALL SELECT 'teachers_day',1387,'Vật phẩm 20/11' UNION ALL SELECT 'teachers_day',1388,'Hoa 20/11' UNION ALL SELECT 'teachers_day',1389,'Cải trang 20/11' UNION ALL SELECT 'teachers_day',1390,'Cải trang 20/11' UNION ALL SELECT 'teachers_day',1391,'Cải trang 20/11' UNION ALL SELECT 'teachers_day',1392,'Danh hiệu 20/11' UNION ALL SELECT 'teachers_day',1393,'Danh hiệu 20/11' UNION ALL SELECT 'teachers_day',1394,'Danh hiệu 20/11' UNION ALL SELECT 'teachers_day',1395,'Hoa 20/11' UNION ALL SELECT 'teachers_day',1735,'Phụ kiện 20/11' UNION ALL SELECT 'teachers_day',1739,'Quà 20/11' UNION ALL SELECT 'teachers_day',1740,'Vật phẩm 20/11'
) seed
JOIN `item_template` i ON i.id=seed.item_id;

INSERT INTO `game_event_boss` (`event_key`,`boss_id`,`quantity`,`boss_role`) VALUES
('lunar_new_year',-371,10,'Lân Con'),
('mid_autumn',-344,10,'Khỉ Đột'),
('mid_autumn',-345,10,'Nguyệt Thần'),
('mid_autumn',-346,10,'Nhật Thần'),
('halloween',-349,10,'Ma Trơi'),
('halloween',-350,10,'Dơi'),
('halloween',-351,10,'Bí Ma'),
('christmas',-353,30,'Ông Già Noel'),
('hung_vuong',-354,0,'Sơn Tinh (sinh kèm Thủy Tinh)'),
('hung_vuong',-355,10,'Thủy Tinh')
ON DUPLICATE KEY UPDATE
  `quantity`=VALUES(`quantity`),
  `boss_role`=VALUES(`boss_role`);

-- managed_runtime=1 means the server removes/recreates that exact NPC.
-- Shared NPCs are listed for information but remain available for normal game features.
INSERT IGNORE INTO `game_event_npc`
(`event_key`,`npc_id`,`map_id`,`x`,`y`,`npc_role`,`managed_runtime`) VALUES
('lunar_new_year',49,0,850,432,'NPC Tết động',1),
('hung_vuong',52,183,897,480,'Hùng Vương',1),
('hung_vuong',52,184,95,624,'Hùng Vương',1),
('hung_vuong',52,185,921,672,'Hùng Vương',1),
('mid_autumn',41,NULL,NULL,NULL,'NPC Trung Thu',0),
('halloween',107,NULL,NULL,NULL,'Bill Bí Ngô',0),
('christmas',39,5,984,408,'Santa dùng chung',0),
('christmas',78,NULL,NULL,NULL,'Ông Già Noel',0),
('christmas',79,NULL,NULL,NULL,'Cây thông Noel',0),
('summer',13,5,1068,408,'Quy Lão Kame dùng chung',0),
('summer',39,5,984,408,'Santa dùng chung',0),
('summer',84,0,950,432,'Xe nước mía',0),
('womens_day',108,NULL,NULL,NULL,'Heart',0),
('valentine',108,NULL,NULL,NULL,'Heart',0),
('teachers_day',13,5,1068,408,'Quy Lão Kame dùng chung',0);

INSERT INTO `game_event_npc`
(`event_key`,`npc_id`,`map_id`,`x`,`y`,`npc_role`,`managed_runtime`)
SELECT 'hung_vuong',51,NULL,NULL,NULL,'Dưa hấu theo người chơi',1
WHERE NOT EXISTS (
  SELECT 1 FROM `game_event_npc` WHERE `event_key`='hung_vuong' AND `npc_id`=51
);

-- Smaller events implemented outside EventManager (NPC menus, boss drops and
-- item-use handlers) are controlled independently instead of being folded into summer.
INSERT INTO `game_event_catalog`
(`event_key`,`event_name`,`summary`,`enabled`,`sort_order`) VALUES
('goku_day','Goku Day','Tích điểm, hộp quà và cải trang Goku Day.',0,42),
('childrens_day','Quốc tế Thiếu nhi','Hộp quà thiếu nhi, pet và phần thưởng đua top.',0,43),
('fruit_ice_cream','Kem trái cây','Nguyên liệu kem trái cây và bảng xếp hạng tại Chi Chi.',0,44),
('sugarcane','Nước mía','Khúc mía, nước đá, các loại nước mía và bảng xếp hạng.',0,45),
('world_cup','World Cup','Cờ đội tuyển, thiệp, capsule và vật phẩm World Cup.',0,46),
('euro','Euro','Bóng, cúp, thẻ trọng tài và vật phẩm các đội tuyển Euro.',0,47),
('black_friday','Black Friday','Phiếu giảm giá và hộp quà Black Friday.',0,48),
('vietnamese_womens_day','Ngày Phụ nữ Việt Nam 20/10','Thiệp và hộp quà sự kiện 20/10.',0,75)
ON DUPLICATE KEY UPDATE
  `event_name`=VALUES(`event_name`),
  `summary`=VALUES(`summary`),
  `sort_order`=VALUES(`sort_order`);

DELETE FROM `game_event_item`
WHERE `event_key`='summer' AND `item_id` BETWEEN 1605 AND 1622;

INSERT IGNORE INTO `game_event_item` (`event_key`,`item_id`,`item_role`)
SELECT ranges.event_key,i.id,ranges.item_role
FROM `item_template` i
JOIN (
  SELECT 'world_cup' event_key,1128 first_id,1140 last_id,'Vật phẩm World Cup' item_role UNION ALL
  SELECT 'black_friday',1141,1141,'Phiếu Black Friday' UNION ALL
  SELECT 'goku_day',1579,1597,'Vật phẩm Goku Day' UNION ALL
  SELECT 'childrens_day',1598,1608,'Quà thiếu nhi' UNION ALL
  SELECT 'fruit_ice_cream',1609,1611,'Vật phẩm kem trái cây' UNION ALL
  SELECT 'sugarcane',1612,1622,'Vật phẩm nước mía' UNION ALL
  SELECT 'euro',1623,1651,'Vật phẩm Euro' UNION ALL
  SELECT 'vietnamese_womens_day',1718,1720,'Quà 20/10' UNION ALL
  SELECT 'black_friday',1747,1747,'Quà Black Friday'
) ranges ON i.id BETWEEN ranges.first_id AND ranges.last_id;

-- These legacy boxes remain in the permanent event-point shop. Keep both the
-- boxes and their possible rewards usable after the source event is disabled.
UPDATE `game_event_item`
SET `purge_on_reset`=0
WHERE `item_id` IN (
  1592,1608,1757,1821,1840,
  1587,1588,1589,1590,1593,1595,
  1599,1600,1601,1602,1807,
  1741,1742,1743,1744,1745,1746,
  1765,1766,1767,1768,1769,1770,1771
);

-- Santa (npc_id=39) is a permanent shop. Its entire stock is not event stock,
-- even when an item id happens to overlap one of the event id ranges above.
DELETE event_item
FROM `game_event_item` event_item
INNER JOIN `item_shop` shop_item ON shop_item.temp_id=event_item.item_id
INNER JOIN `tab_shop` shop_tab ON shop_tab.id=shop_item.tab_id
INNER JOIN `shop` santa_shop ON santa_shop.id=shop_tab.shop_id
WHERE santa_shop.npc_id=39;

INSERT IGNORE INTO `game_event_npc`
(`event_key`,`npc_id`,`map_id`,`x`,`y`,`npc_role`,`managed_runtime`) VALUES
('childrens_day',81,5,240,288,'Chi Chi dùng chung',0),
('fruit_ice_cream',81,5,240,288,'Chi Chi dùng chung',0),
('sugarcane',81,5,240,288,'Chi Chi dùng chung',0),
('sugarcane',84,0,950,432,'Xe nước mía dùng chung',0),
('world_cup',55,NULL,NULL,NULL,'Bill dùng chung',0),
('euro',55,NULL,NULL,NULL,'Bill dùng chung',0);
