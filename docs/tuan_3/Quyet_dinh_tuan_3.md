# QUYẾT ĐỊNH TRIỂN KHAI – TUẦN 3

Chốt ngày 2026-10-24, cập nhật 2026-10-29 sau rà soát (`docs/ra_soat/Bao_cao_ra_soat.md`, mục R01–R17); D18 bổ sung ngoại lệ đồng hồ `Clock` cùng ngày, phát hiện khi chuẩn bị cách test. Áp dụng khi làm FR-01, FR-02, FR-03.

File này là tài liệu mới nhất cho phạm vi tuần 3. Nếu khác spec tuần 1 hoặc thiết kế tuần 2 (`docs/tuan_2/Tuan_2_Thiet_ke.md`) thì file này đúng. Khi thêm hoặc đổi một quyết định ở đây, phải sửa luôn các tài liệu cũ nhắc tới điểm đó trong cùng commit.

## 1. Phạm vi tuần 3

| Mã | Quyết định |
| --- | --- |
| D1 | Chỉ làm `/api/auth/*`, `/api/me*`, `/api/skill-categories`, `/api/skills` và màn hình S01–S04. `/api/users/{id}`, `/api/users/search` làm ở tuần 4; `/api/users/{id}/reviews` làm ở tuần 7. |
| D2 | Không làm quên mật khẩu (FR-23, mức "Nên có"). Phần xử lý token viết đủ chung để dùng lại cho `RESET_PASSWORD` sau này. |
| D3 | `V2__seed_skills.sql` nạp 6 nhóm (Lập trình, Dữ liệu, Thiết kế, Ngoại ngữ, Âm nhạc, Kỹ năng mềm), khoảng 25 kỹ năng và 8–10 cặp kỹ năng liên quan cùng nhóm. Danh sách cụ thể được duyệt trước khi viết SQL. Lệnh INSERT quan hệ tra id theo tên, JOIN thêm điều kiện hai kỹ năng cùng `category_id`, và dùng `LEAST`/`GREATEST` để thỏa `CHECK skill_a_id < skill_b_id`. Cuối file đếm số dòng của `skill_categories`, `skills`, `skill_relations`; sai số mong đợi thì `RAISE EXCEPTION` để Flyway dừng (tránh gõ sai tên làm INSERT âm thầm chèn 0 dòng). |
| D4 | Admin đầu tiên được tạo lúc khởi động: đọc `ADMIN_EMAIL`, `ADMIN_PASSWORD` từ `.env`; email được chuẩn hóa như D9. Nếu chưa có tài khoản với email đó thì tạo với `role = ADMIN`, `status = ACTIVE`, `email_verified_at = now()`. Khởi động lại không tạo trùng. Không đưa mật khẩu vào migration. |

## 2. Đăng ký và xác thực email

| Mã | Quyết định |
| --- | --- |
| D5 | Xác thực bằng **link chứa token** ngẫu nhiên 256 bit (mã hóa Base64URL), không có mã để nhập tay. Link có dạng `${FRONTEND_URL}/verify-email?token=...`, với `FRONTEND_URL` đọc từ `.env` (dev: `http://localhost:5173`). Trang S02 đọc `token` từ URL rồi gọi `POST /api/auth/verify-email`. |
| D6 | `token_hash` = SHA-256 của token, lưu dạng hex 64 ký tự. Không dùng BCrypt vì cần tra cứu theo hash. |
| D7 | Token hết hạn sau 24 giờ. Mỗi lần tạo token mới thì **xóa** (`DELETE`) các token `VERIFY_EMAIL` chưa dùng của tài khoản đó. Không dùng `used_at` cho việc này vì `used_at` nghĩa là "đã dùng". Giới hạn gửi thư: cách thư gần nhất ít nhất 60 giây, tính theo `created_at` của token mới nhất (token tạo lúc đăng ký cũng tính). `resend-verification` chỉ gửi thư khi tài khoản đang `PENDING_VERIFICATION` và đã qua 60 giây; mọi trường hợp khác (email không tồn tại, `ACTIVE`, `LOCKED`, chưa đủ 60 giây) vẫn trả 204 nhưng không gửi thư. |
| D8 | Đăng ký với email đã thuộc tài khoản `ACTIVE` hoặc `LOCKED` trả 409. Đăng ký với email đang `PENDING_VERIFICATION` được coi là **đăng ký lại**: ghi đè `password_hash` và `full_name`, tạo token mới và gửi thư theo D7, trả 201 giống đăng ký mới. Nếu chưa đủ 60 giây từ thư trước thì vẫn ghi đè nhưng không tạo token mới và không gửi thư. Lý do: nếu trả 409, người khác có thể đăng ký trước bằng email của chủ thật, rồi khi chủ thật xác thực, tài khoản mang mật khẩu của kẻ kia (rà soát R01). Hạn chế còn lại: ai đăng ký sau cùng trước khi link được bấm thì mật khẩu của người đó có hiệu lực; chưa có quên mật khẩu (D2) nên chủ email không tự đặt lại được. Ghi vào phần hạn chế của báo cáo. |
| D9 | Email được bỏ khoảng trắng thừa và đổi về chữ thường trước khi kiểm tra và lưu, ở **mọi** chỗ nhận email: đăng ký, đăng nhập, gửi lại thư, `ADMIN_EMAIL` (D4). Email dài tối đa 255 ký tự (cột `VARCHAR(255)`). Danh sách đuôi email trường cấu hình trong `application.yml` (không ghi cứng trong code); chỉ nhận đúng đuôi đã liệt kê. |
| D10 | Mật khẩu dài 8–72 ký tự **và** không quá 72 byte khi mã hóa UTF-8 (`password.getBytes(UTF_8).length <= 72`); vượt thì 400. Lý do: BCrypt chỉ dùng 72 byte đầu, và chữ tiếng Việt có dấu chiếm 2–3 byte, nên 72 ký tự có dấu có thể vượt giới hạn. Không bắt buộc ký tự đặc biệt. |
| D11 | Ở máy dev, email được gửi tới **Mailpit** chạy trong `docker-compose` (SMTP `localhost:1025`, xem thư ở `http://localhost:8025`). |
| D25 | Body của `POST /api/auth/register`: `{email, password, fullName}`, cả ba bắt buộc. `email` theo D9, `password` theo D10, `fullName` theo D16. Màn hình S01 có ô "Họ tên". |
| D26 | `verify-email` chỉ đổi trạng thái khi token tồn tại, đúng `purpose = VERIFY_EMAIL`, chưa dùng, chưa hết hạn, **và** tài khoản đang `PENDING_VERIFICATION`. Khi đó: `status = ACTIVE`, `email_verified_at = now()`, token có `used_at = now()`. Mọi trường hợp khác (không tồn tại, đã dùng, hết hạn, sai `purpose`, tài khoản `LOCKED` hoặc đã `ACTIVE`) trả 400 với cùng một `detail`, để người ngoài không phân biệt được. |
| D27 | Đăng ký: lưu user và token trong một transaction, gửi thư **sau khi** commit. Gửi thư lỗi thì ghi log và vẫn trả 201; người dùng dùng "gửi lại link". |

## 3. Đăng nhập và phân quyền

| Mã | Quyết định |
| --- | --- |
| D12 | Tài khoản `PENDING_VERIFICATION` **được đăng nhập** và dùng các API quyền *User* (`/api/me*`, danh mục kỹ năng). Các API quyền *Active* trả 403. |
| D13 | Đăng nhập kiểm tra theo thứ tự: tìm email → so mật khẩu → xét trạng thái. Email không tồn tại hoặc sai mật khẩu đều trả 401 với cùng một thông báo. Chỉ khi mật khẩu **đúng** mà tài khoản `LOCKED` mới trả 403 (kiểm tra trạng thái trước mật khẩu sẽ cho người ngoài biết email đó tồn tại). Mỗi request có token đều đọc lại user từ DB: user không còn tồn tại thì 401; `LOCKED` thì 403. Nhờ vậy khóa tài khoản có hiệu lực ngay, không chờ token hết hạn. |
| D14 | Dùng `spring-boot-starter-oauth2-resource-server` (Nimbus). Token ký HS256 bằng `JWT_SECRET`, hạn 24 giờ, chưa có refresh token. Claim `sub` = userId, claim `role` = `STUDENT`/`ADMIN`. |

## 4. Hồ sơ, lịch rảnh, kỹ năng

| Mã | Quyết định |
| --- | --- |
| D15 | `PUT /api/me` thay toàn bộ hồ sơ. Với `bio`, `learningMode`, `phone`, `contactInfo`: không gửi khóa, gửi `null`, chuỗi rỗng hoặc chỉ có khoảng trắng đều lưu NULL (ví dụ `learningMode = null` là "chưa khai báo"). `fullName` bắt buộc (D16). Trường không có trong API (ví dụ `email`) bị bỏ qua, không báo lỗi: không đổi email được qua API này. |
| D16 | `fullName` bắt buộc, 1–100 ký tự sau khi bỏ khoảng trắng hai đầu; `bio` tối đa 1000; `contactInfo` tối đa 500; `phone` tối đa 20 ký tự, chỉ gồm số, khoảng trắng, `+`, `-`. |
| D17 | Email trường không xuất hiện trong hồ sơ công khai (NFR-02). |
| D18 | `created_at` do Hibernate ghi bằng `@CreationTimestamp`, `updated_at` bằng `@UpdateTimestamp`. **Ngoại lệ:** các mốc thời gian dùng cho quy tắc thời gian của token và xác thực (`user_tokens.created_at`, `expires_at`, `used_at`, `users.email_verified_at`, kể cả khi tạo admin ở D4) do service gán từ **một** bean `java.time.Clock` dùng chung; không dùng `@CreationTimestamp` hay `Instant.now()` cho các cột này. Lý do: D7 so `created_at` của token với giờ hiện tại, và test (TC06, TC41, TC43, TC77, TC78, TC89) thay `Clock` bằng đồng hồ giả lập; nếu `created_at` lấy giờ thật của máy còn code so với giờ giả thì hai đồng hồ lệch nhau và test sai. |
| D19 | Một kỹ năng không được vừa nằm trong danh sách dạy vừa nằm trong danh sách muốn học (400), xét cả hai chiều. Muốn chuyển một kỹ năng từ "muốn học" sang "dạy", S04 lưu danh sách học (đã bỏ kỹ năng đó) trước, rồi mới lưu danh sách dạy. |
| D20 | Kỹ năng bị ẩn (`is_active = false`) không thêm mới được (400), nhưng nếu người dùng đã có từ trước thì được giữ khi `PUT` lại danh sách. |
| D21 | Tối đa 10 kỹ năng mỗi danh sách (dạy và học); lịch rảnh tối đa 21 khung. Gửi trùng `skillId` hoặc trùng khung giờ **trong cùng một request** trả 400 (409 dành cho xung đột với dữ liệu đã có, ví dụ email đã đăng ký). |
| D28 | Đọc lại dữ liệu của tôi: `GET /api/me/availability` trả `[{dayOfWeek, period}]`; `GET /api/me/teach-skills` trả `[{skillId, skillName, categoryId, categoryName, isActive, level}]`; `GET /api/me/learn-skills` tương tự với `targetLevel` (có thể `null`). Trả cả tên và `isActive` vì kỹ năng đã bị ẩn (D20) không có trong `GET /api/skills`. Ba API này quyền *User*, như các `PUT` tương ứng. |

## 5. Quy ước API và frontend

| Mã | Quyết định |
| --- | --- |
| D22 | Khi dev, Vite proxy chuyển `/api` sang `http://localhost:8080`, chưa cấu hình CORS. |
| D23 | Token lưu trong `localStorage`. Điều hướng bằng `react-router`. Form viết tay, chưa dùng thư viện form. |
| D24 | Lỗi validation (400) trả Problem Details kèm thuộc tính `errors: [{ "field": "...", "message": "..." }]`. Lỗi 401/403 do Spring Security sinh ra ở tầng filter **không** tự thành Problem Details; phải tự viết `AuthenticationEntryPoint` (401) và `AccessDeniedHandler` (403) trả `application/problem+json`. Lỗi vi phạm ràng buộc DB (ví dụ trùng khóa) không được lọt ra thành 500: service kiểm tra trước, hoặc bắt `DataIntegrityViolationException` và đổi thành 400/409. |
| D29 | Mã trả về khi thành công: `register` 201 kèm `{id, email, status}`; `verify-email` 204; `resend-verification` 204; `login` 200 kèm `{accessToken}`; các `PUT /api/me*` 200 kèm dữ liệu vừa lưu (cùng dạng với `GET` tương ứng). `GET /api/me` trả `{id, email, fullName, role, status, bio, learningMode, phone, contactInfo}`; frontend dùng `status` để hiện thông báo "chưa xác thực email". |
| D30 | Quy ước ánh xạ Entity (vì `ddl-auto: validate` so kiểu Java với kiểu cột): `BIGINT` → `Long`; `SMALLINT` → `Short` (dùng `Integer` sẽ lỗi validate); `TIMESTAMPTZ` → `Instant`; `version` → `Integer` + `@Version`; cột chuỗi có `CHECK IN (...)` → enum + `@Enumerated(EnumType.STRING)`. Cột có `DEFAULT` trong DB (`role`, `status`, `is_active`) phải gán giá trị mặc định ngay ở field Java, vì Hibernate luôn ghi đủ mọi cột khi INSERT nên `DEFAULT` của DB không được dùng. Bảng khóa ghép (`user_teach_skills`, `user_learn_skills`, `user_availability`) dùng `@IdClass` hoặc `@EmbeddedId`; khi `PUT` thay danh sách thì xóa bằng câu `DELETE ... WHERE user_id = ?` trước rồi mới chèn. Validate không kiểm tra độ dài hay `CHECK`, nên DTO phải có `@Size` khớp từng `VARCHAR(n)`. |
