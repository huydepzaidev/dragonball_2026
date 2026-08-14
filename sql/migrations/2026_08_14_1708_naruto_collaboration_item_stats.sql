-- Configure Naruto collaboration equipment metadata and make the mini pet wearable.
-- Runtime options are rolled by UseItem when opening item 2041.
-- Safe to run more than once.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `item_template`
SET `TYPE`=CASE `id` WHEN 2019 THEN 27 ELSE `TYPE` END,
    `description`=CASE `id`
        WHEN 2019 THEN 'Sức đánh, HP, KI +15%; tấn công Boss +10%; chí mạng +7%'
        WHEN 2026 THEN 'Đi cùng Pet Cửu Vĩ Hồ: +7% HP'
        WHEN 2027 THEN 'Đi cùng Đeo lưng Sharingan Vạn Hoa Đồng: +1% sát thương'
        WHEN 2030 THEN 'Sức đánh, HP, KI +20%; chuyển 10% tấn công thành HP và KI; chí mạng +15%'
        WHEN 2039 THEN 'Sức đánh, HP, KI +22%; tiềm năng, sức mạnh +55%; chí mạng +10%'
    END,
    `head`=CASE `id` WHEN 2019 THEN -1 ELSE `head` END,
    `body`=CASE `id` WHEN 2019 THEN 1990 ELSE `body` END,
    `leg`=CASE `id` WHEN 2019 THEN 1991 ELSE `leg` END
WHERE `id` IN (2019,2026,2027,2030,2039);

COMMIT;
