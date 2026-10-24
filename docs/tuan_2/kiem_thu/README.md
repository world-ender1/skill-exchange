# Kiểm thử thiết kế cơ sở dữ liệu (tuần 2)

Mục đích: chứng minh lược đồ ở mục 3 của `Tuan_2_Thiet_ke.md` **chạy được thật** và các ràng buộc **chặn đúng dữ liệu sai, nhận đúng dữ liệu hợp lệ**.

## Các file

| File | Nội dung |
| --- | --- |
| `backend/src/main/resources/db/migration/V1__init.sql` | Lược đồ viết từ mục 3, cũng là migration Flyway đầu tiên của tuần 3 |
| `test_constraints.sql` | 73 ca kiểm thử, mỗi ca tự ghi PASS/FAIL |
| `ket_qua.txt` | Kết quả lần chạy gần nhất |

## Cách chạy lại

Chạy trên database **tạm** `design_test`, không đụng database `skillexchange` của ứng dụng.

```bash
# ở thư mục gốc C:\DACN, Docker đang chạy
docker compose up -d
docker compose exec -T db psql -U skillexchange -d postgres -c "DROP DATABASE IF EXISTS design_test" -c "CREATE DATABASE design_test"
docker compose exec -T db psql -U skillexchange -d design_test -v ON_ERROR_STOP=1 < backend/src/main/resources/db/migration/V1__init.sql
docker compose exec -T db psql -U skillexchange -d design_test < docs/tuan_2/kiem_thu/test_constraints.sql
docker compose exec -T db psql -U skillexchange -d postgres -c "DROP DATABASE design_test"
```

## Thiết kế ca kiểm thử

| Nhóm | Số ca | Ý nghĩa |
| --- | --- | --- |
| A. Dữ liệu hợp lệ | 23 | Đi trọn kịch bản: tạo kỹ năng, hồ sơ → gửi yêu cầu → chấp nhận → bắt đầu → hoàn thành → đánh giá; thêm một phiên bị báo cáo sự cố và admin hủy. **Phải được nhận**, chứng minh thiết kế không chặn nhầm. |
| B. Dữ liệu sai | 44 | Mỗi ràng buộc ít nhất một câu vi phạm: giá trị ngoài miền, trùng lặp, thiếu cột bắt buộc, sai nhất quán loại yêu cầu, gửi trùng `PENDING` (G4), hai báo cáo đang mở, xóa dữ liệu đang được dùng... **Phải bị chặn.** |
| C. Truy vấn công thức | 6 | Tính $Sim$, số khung rảnh trùng, $RatingNorm$, $CompletionRate$ (kể cả trường hợp mẫu số bằng 0) trên dữ liệu nhóm A, so với giá trị tính tay. |

## Kết quả

**73/73 PASS** (lần chạy 2026-10-24, PostgreSQL 17).

Lần chạy đầu có **1 ca FAIL (B42)**: báo cáo sự cố đang `OPEN` nhưng vẫn ghi được `resolution`. Nguyên nhân là ràng buộc viết dạng `(status = 'RESOLVED') = (đủ 3 cột)`: khi chỉ điền 1 trong 3 cột thì hai vế cùng sai nên vẫn "bằng nhau". Đã sửa thành hai ràng buộc riêng: `RESOLVED` ⇒ đủ 3 cột; `OPEN` ⇒ cả 3 cột trống. Đây là ví dụ kiểm thử bắt được lỗi mà đọc bằng mắt khó thấy.

## Những điểm tài liệu thiết kế còn thiếu (phát hiện khi viết SQL)

Các điểm dưới đây đã được bổ sung vào `V1__init.sql` (đánh dấu `[BỔ SUNG]`) và **đã cập nhật ngược vào `Tuan_2_Thiet_ke.md`** (mục 3.1, 3.3, 3.5, 3.6, 5.1):

| # | Chỗ thiếu | Đã chọn trong SQL | Nên ghi vào |
| --- | --- | --- | --- |
| K1 | Nhiều cột enum, số không ghi NOT NULL (`type`, `mode`, `status`, `duration_minutes`, `purpose`) | NOT NULL, `status` có giá trị mặc định | Mục 3.3 |
| K2 | "Lý do bắt buộc khi hủy" và các quy tắc trạng thái phiên mới chỉ là câu chữ | CHECK: `CANCELLED` ⇒ có lý do và thời điểm; `COMPLETED` ⇒ có `completed_at`; đã bắt đầu ⇒ có `started_at`; `WAITING_CONFIRMATION` ⇒ có `first_completed_by` | Mục 3.3, bảng `exchange_sessions` |
| K3 | Không nói báo cáo đã xử lý cần những cột nào | CHECK nhất quán `status` với `resolution`, `resolved_by`, `resolved_at` | Mục 3.3, bảng `incident_reports` |
| K4 | Không nói xóa người dùng / kỹ năng thì sao | Kỹ năng, lịch rảnh, token của người dùng xóa theo (CASCADE); yêu cầu, phiên, đánh giá **cấm xóa** (giữ lịch sử). Kỹ năng đang dùng không xóa được, chỉ ẩn | Mục 3.1 |
| K5 | `exchange_sessions`, `user_tokens` thiếu `created_at` | Thêm | Mục 3.3 |
| K6 | Mục 3.5 không nhắc giá trị mặc định khi chưa có phiên (truy vấn trả về NULL) | Ca C06 xác nhận DB trả NULL, service phải đổi thành **0.5** theo spec mục 4.3 | Mục 3.5 |
| K7 | Bảng `exchange_requests` **không có** cột `version`: người gửi hủy đúng lúc người nhận chấp nhận thì có thể ghi đè nhau | Thêm `version INTEGER NOT NULL DEFAULT 0` | Mục 3.3 và 5.1 |

## Giới hạn

Database chỉ kiểm được những gì nằm trong **một dòng** hoặc **một bảng**. Các quy tắc sau phải kiểm ở tầng service (unit test tuần 3–6):

- Người bấm hoàn thành, người hủy, người đánh giá phải là người tham gia phiên.
- Thứ tự chuyển trạng thái (ví dụ không được đi thẳng `SCHEDULED` → `COMPLETED`). DB chỉ kiểm từng trạng thái có đủ dữ liệu, không kiểm đường đi.
- Hai kỹ năng liên quan phải cùng nhóm.
- Chỉ người `ACTIVE` mới được gửi yêu cầu.
