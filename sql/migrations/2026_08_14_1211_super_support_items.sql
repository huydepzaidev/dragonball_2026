-- Rename and document the existing super support items.
-- Each use adds 10 minutes, capped at 120 minutes by the server.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `item_template`
SET `NAME` = CASE `id`
        WHEN 1150 THEN 'Cuồng nộ siêu cấp'
        WHEN 1151 THEN 'Bổ khí siêu cấp'
        WHEN 1152 THEN 'Bổ huyết siêu cấp'
        WHEN 1153 THEN 'Giáp Xên siêu cấp'
        WHEN 1154 THEN 'Ẩn danh siêu cấp'
        ELSE `NAME`
    END,
    `description` = CASE `id`
        WHEN 1150 THEN 'Mỗi lần dùng +10 phút, tối đa 120 phút, tăng 120% sức đánh gốc'
        WHEN 1151 THEN 'Mỗi lần dùng +10 phút, tối đa 120 phút, tăng 120% KI'
        WHEN 1152 THEN 'Mỗi lần dùng +10 phút, tối đa 120 phút, tăng 120% HP'
        WHEN 1153 THEN 'Mỗi lần dùng +10 phút, tối đa 120 phút, giảm 60% sát thương'
        WHEN 1154 THEN 'Mỗi lần dùng +10 phút ẩn danh, cộng dồn tối đa 120 phút'
        ELSE `description`
    END,
    `icon_id` = CASE `id`
        WHEN 1150 THEN 10716
        WHEN 1151 THEN 10715
        WHEN 1152 THEN 10714
        WHEN 1153 THEN 10712
        WHEN 1154 THEN 10717
        ELSE `icon_id`
    END,
    `is_up_to_up` = 1
WHERE `id` BETWEEN 1150 AND 1154;

COMMIT;
