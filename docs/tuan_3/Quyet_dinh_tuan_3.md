# QUYẾT ĐỊNH TRIỂN KHAI – TUẦN 3

Chốt ngày 2026-10-24. Bổ sung cho thiết kế tuần 2 (`docs/tuan_2/Tuan_2_Thiet_ke.md`), áp dụng khi làm FR-01, FR-02, FR-03. Nếu mâu thuẫn với spec tuần 1 thì spec là chuẩn, và cần sửa lại file này.

## 1. Phạm vi tuần 3

| Mã | Quyết định |
| --- | --- |
| D1 | Chỉ làm `/api/auth/*`, `/api/me*`, `/api/skill-categories`, `/api/skills` và màn hình S01–S04. `/api/users/{id}`, `/api/users/search` làm ở tuần 4; `/api/users/{id}/reviews` làm ở tuần 7. |
| D2 | Không làm quên mật khẩu (FR-23, mức "Nên có"). Phần xử lý token viết đủ chung để dùng lại cho `RESET_PASSWORD` sau này. |
| D3 | `V2__seed_skills.sql` nạp 6 nhóm (Lập trình, Dữ liệu, Thiết kế, Ngoại ngữ, Âm nhạc, Kỹ năng mềm), khoảng 25 kỹ năng và 8–10 cặp kỹ năng liên quan cùng nhóm. Danh sách cụ thể được duyệt trước khi viết SQL. Lệnh INSERT quan hệ tra id theo tên và dùng `LEAST`/`GREATEST` để thỏa `CHECK skill_a_id < skill_b_id`. |
| D4 | Admin đầu tiên được tạo lúc khởi động: đọc `ADMIN_EMAIL`, `ADMIN_PASSWORD` từ `.env`; nếu chưa có tài khoản với email đó thì tạo với `role = ADMIN`, `status = ACTIVE`. Không đưa mật khẩu vào migration. |

## 2. Đăng ký và xác thực email

| Mã | Quyết định |
| --- | --- |
| D5 | Xác thực bằng **link chứa token** ngẫu nhiên 256 bit (mã hóa Base64URL). Trang S02 đọc `token` từ URL rồi gọi `POST /api/auth/verify-email`. |
| D6 | `token_hash` = SHA-256 của token, lưu dạng hex 64 ký tự. Không dùng BCrypt vì cần tra cứu theo hash. |
| D7 | Token hết hạn sau 24 giờ. Gửi lại thì token `VERIFY_EMAIL` cũ chưa dùng bị vô hiệu. Mỗi tài khoản gửi lại cách nhau ít nhất 60 giây. |
| D8 | Đăng ký trùng email trả 409, kể cả khi tài khoản cũ chưa xác thực (dùng "gửi lại mã"). `resend-verification` luôn trả 204, dù email có tồn tại hay không. |
| D9 | Email được bỏ khoảng trắng thừa và đổi về chữ thường trước khi kiểm tra và lưu. Danh sách đuôi email trường cấu hình trong `application.yml` (không ghi cứng trong code); chỉ nhận đúng đuôi đã liệt kê. |
| D10 | Mật khẩu dài 8–72 ký tự (BCrypt chỉ dùng 72 byte đầu), không bắt buộc ký tự đặc biệt. |
| D11 | Ở máy dev, email được gửi tới **Mailpit** chạy trong `docker-compose` (xem thư ở `http://localhost:8025`). |

## 3. Đăng nhập và phân quyền

| Mã | Quyết định |
| --- | --- |
| D12 | Tài khoản `PENDING_VERIFICATION` **được đăng nhập** và dùng các API quyền *User* (`/api/me*`, danh mục kỹ năng). Các API quyền *Active* trả 403. |
| D13 | Sai email hoặc sai mật khẩu đều trả 401 với cùng một thông báo. Tài khoản `LOCKED` đăng nhập thì trả 403. Mỗi request có token đều đọc lại `status` từ DB, nên khóa tài khoản có hiệu lực ngay, không chờ token hết hạn. |
| D14 | Dùng `spring-boot-starter-oauth2-resource-server` (Nimbus). Token ký HS256 bằng `JWT_SECRET`, hạn 24 giờ (G5). Claim `sub` = userId, claim `role` = `STUDENT`/`ADMIN`. |

## 4. Hồ sơ, lịch rảnh, kỹ năng

| Mã | Quyết định |
| --- | --- |
| D15 | `PUT /api/me` thay toàn bộ hồ sơ: trường nào `null` hoặc bỏ trống thì lưu NULL (ví dụ `learningMode = null` là "chưa khai báo"). Không cho đổi email. |
| D16 | `fullName` bắt buộc, 1–100 ký tự; `bio` tối đa 1000; `contactInfo` tối đa 500; `phone` tối đa 20 ký tự, chỉ gồm số, khoảng trắng, `+`, `-`. |
| D17 | Email trường không xuất hiện trong hồ sơ công khai (NFR-02). |
| D18 | `updated_at` do Hibernate ghi bằng `@UpdateTimestamp`. |
| D19 | Một kỹ năng không được vừa nằm trong danh sách dạy vừa nằm trong danh sách muốn học (400). |
| D20 | Kỹ năng bị ẩn (`is_active = false`) không thêm mới được (400), nhưng nếu người dùng đã có từ trước thì được giữ khi `PUT` lại danh sách. |
| D21 | Tối đa 10 kỹ năng mỗi danh sách; lịch rảnh tối đa 21 khung. Gửi trùng `skillId` hoặc trùng khung giờ trả 400. |

## 5. Quy ước API và frontend

| Mã | Quyết định |
| --- | --- |
| D22 | Khi dev, Vite proxy chuyển `/api` sang `http://localhost:8080`, chưa cấu hình CORS. |
| D23 | Token lưu trong `localStorage`. Điều hướng bằng `react-router`. Form viết tay, chưa dùng thư viện form. |
| D24 | Lỗi validation (400) trả Problem Details kèm thuộc tính `errors: [{ "field": "...", "message": "..." }]`. |
