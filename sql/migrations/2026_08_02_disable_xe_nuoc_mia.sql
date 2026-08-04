-- Tắt NPC Xe nước mía (template ID 84) tại ba làng khởi đầu.
START TRANSACTION;

UPDATE map_template
SET npcs = REPLACE(npcs, ',[84,950,432]', '')
WHERE id = 0 AND npcs LIKE '%[84,950,432]%';

UPDATE map_template
SET npcs = REPLACE(npcs, ',[84,853,432]', '')
WHERE id = 7 AND npcs LIKE '%[84,853,432]%';

UPDATE map_template
SET npcs = REPLACE(npcs, ',[84,953,408]', '')
WHERE id = 14 AND npcs LIKE '%[84,953,408]%';

COMMIT;

-- Rollback nếu cần:
-- UPDATE map_template SET npcs = '[[7,228,432],[67,590,432],[74,426,432],[84,950,432]]' WHERE id = 0;
-- UPDATE map_template SET npcs = '[[6,564,432],[8,300,432],[74,607,432],[84,853,432]]' WHERE id = 7;
-- UPDATE map_template SET npcs = '[[9,396,408],[6,252,408],[74,286,409],[84,953,408]]' WHERE id = 14;
