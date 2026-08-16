-- BEGIN MIGRATION: 2026_08_16_1110_expand_download_links_settings.sql
-- Expand download links columns in settings table to TEXT for long external URLs (Drive, Mediafire, TestFlight, etc.)
-- Re-runnable: uses safe ALTER TABLE modifications.
SET NAMES utf8mb4;
START TRANSACTION;

ALTER TABLE `settings`
  MODIFY COLUMN `Android` TEXT DEFAULT NULL,
  MODIFY COLUMN `Windows` TEXT DEFAULT NULL,
  MODIFY COLUMN `IPhone` TEXT DEFAULT NULL,
  MODIFY COLUMN `Java` TEXT DEFAULT NULL;

COMMIT;
-- END MIGRATION: 2026_08_16_1110_expand_download_links_settings.sql
