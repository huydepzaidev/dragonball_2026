# Database SQL

Database dùng chung cho game server và web là team2026.

- teamobi2026.sql: bản tổng mới nhất, chỉ dùng để import vào database trống.
- migrations/: lịch sử nâng cấp cho database đang hoạt động, chạy theo thứ tự tên file.
- archive/nro1.sql: dump cũ chỉ để đối chiếu hoặc khôi phục lịch sử.

## Quy trình cho mọi update game và web

1. Tạo một migration mới tại migrations/YYYY_MM_DD_HHMM_<game|web|shared>_<mo-ta>.sql.
2. Viết migration có thể chạy lại an toàn khi khả thi bằng DDL có điều kiện và thay đổi dữ liệu idempotent.
3. Nối nguyên nội dung migration vào cuối teamobi2026.sql, đặt giữa marker BEGIN MIGRATION và END MIGRATION.
4. Với database đang chạy, chỉ import migration mới. Với database trống, chỉ import teamobi2026.sql.
5. Kiểm tra import trên database tạm trước khi sử dụng thật.

Không import teamobi2026.sql đè lên database đang hoạt động vì phần dump chính tạo lại bảng và chèn lại dữ liệu. Không chạy lại các migration cũ sau khi cài mới bằng file tổng vì chúng đã được gộp ở cuối file.
