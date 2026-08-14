-- Keep Naruto collaboration names and descriptions aligned with the event specification.
-- Naruto's absolute HP uses the existing HP+#K option so values up to 35,000 fit the client protocol.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `item_template`
SET `NAME`=CASE `id`
        WHEN 2019 THEN 'Pet Cửu Vĩ Hồ'
        WHEN 2026 THEN 'Cải trang Naruto'
        WHEN 2027 THEN 'Cải trang Sasuke'
        WHEN 2030 THEN 'Cờ Sharingan Vạn Hoa Đồng'
        WHEN 2039 THEN 'Cải trang Akatsuki'
    END,
    `description`=CASE `id`
        WHEN 2019 THEN 'Sức đánh, HP, KI +15%; tấn công Boss +10%; chí mạng +7%'
        WHEN 2026 THEN 'Đi cùng Pet Cửu Vĩ Hồ: +7% HP'
        WHEN 2027 THEN 'Đi cùng Cờ Sharingan Vạn Hoa Đồng: +1% sát thương'
        WHEN 2030 THEN 'Sức đánh, HP, KI +20%; chuyển 10% tấn công thành HP và KI; chí mạng +15%'
        WHEN 2039 THEN 'Sức đánh, HP, KI +22%; tiềm năng, sức mạnh +55%; chí mạng +10%'
    END
WHERE `id` IN (2019,2026,2027,2030,2039);

UPDATE `flag_bag`
SET NAME='Cờ Sharingan Vạn Hoa Đồng'
WHERE `id`=180;

COMMIT;
