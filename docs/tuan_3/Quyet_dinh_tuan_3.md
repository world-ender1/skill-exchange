# QUYẾT ĐỊNH TRIỂN KHAI – TUẦN 3

Chốt ngày 2026-10-24, cập nhật 2026-10-29 sau rà soát (`docs/ra_soat/Bao_cao_ra_soat.md`, mục R01–R17); D18 bổ sung ngoại lệ đồng hồ `Clock` cùng ngày, phát hiện khi chuẩn bị cách test; thêm D39–D42 ngày 2026-10-31 sau bước đọc tài liệu đầu tuần. Áp dụng khi làm FR-01, FR-02, FR-03.

File này là tài liệu mới nhất cho phạm vi tuần 3. Nếu khác spec tuần 1 hoặc thiết kế tuần 2 (`docs/tuan_2/Tuan_2_Thiet_ke.md`) thì file này đúng. Khi thêm hoặc đổi một quyết định ở đây, phải sửa luôn các tài liệu cũ nhắc tới điểm đó trong cùng commit.

## Cách đọc file này

**Quyết định từ đâu ra.** Trước khi code tuần 3, spec (FR-01–FR-03) và thiết kế tuần 2 (mục 3.3, 4.1–4.4) được đọc lại để tìm những chỗ **chưa nói rõ**, tức là những chỗ khi code sẽ phải tự đoán. Mỗi chỗ như vậy được chốt thành một quyết định. Cột **Câu hỏi** ghi chỗ chưa rõ đó; cột **Nguồn** ghi yêu cầu hoặc mục thiết kế mà quyết định bổ sung, và mục rà soát (R…) đã dẫn tới quyết định nếu có.

**Vì sao số D không liền nhau trong từng mục.** Mã D là số cố định, cấp theo **thứ tự chốt**, không theo vị trí trong file:
- D1–D24 chốt ngày 2026-10-24, khi đọc tài liệu lần đầu.
- D25–D30 thêm ngày 2026-10-29 sau rà soát, được xếp vào mục phù hợp nhưng giữ số mới.
- D31–D38 thuộc tuần 4 (`docs/tuan_4/Quyet_dinh_tuan_4.md`).
- D39–D42 thêm ngày 2026-10-31, khi cho AI đọc tài liệu trước lúc code và phát hiện còn 4 chỗ chưa rõ. Vì D31–D38 đã dùng cho tuần 4 nên số nhảy lên D39.

Không đánh số lại, vì các mã này đã được trích ở ca kiểm thử, thiết kế tuần 2, `CLAUDE.md` và báo cáo rà soát. Quyết định mới lấy số tiếp theo; quyết định bị bỏ thì ghi "bỏ" chứ không xóa số.

**Các mục chia theo luồng người dùng:** (1) tuần này làm gì; (2) đăng ký và xác thực email (S01, S02); (3) đăng nhập và phân quyền; (4) hồ sơ, lịch rảnh, kỹ năng (S03, S04); (5) quy ước chung cho API và frontend; (6) cách chạy test.

**Ký hiệu nguồn:** FR/NFR = spec tuần 1, mục 3; Spec 4.6, 6.3 = mục của spec tuần 1; TK = thiết kế tuần 2; G5–G9 = giả định ở TK mục 8; S01–S04 = màn hình ở TK mục 6.1; R = mục trong báo cáo rà soát.

## 1. Phạm vi tuần 3

| Mã | Câu hỏi | Quyết định | Nguồn |
| --- | --- | --- | --- |
| D1 | Tuần này làm những API và màn hình nào? | Chỉ làm `/api/auth/*`, `/api/me*`, `/api/skill-categories`, `/api/skills` và màn hình S01–S04. `/api/users/{id}`, `/api/users/search` làm ở tuần 4; `/api/users/{id}/reviews` làm ở tuần 7. | Proposal (kế hoạch tuần 3), TK 4.2–4.4 |
| D2 | Có làm quên mật khẩu không? | Không làm quên mật khẩu (FR-23, mức "Nên có"). Phần xử lý token viết đủ chung để dùng lại cho `RESET_PASSWORD` sau này. | FR-23 |
| D3 | Danh mục kỹ năng ban đầu lấy từ đâu, nạp thế nào cho an toàn? | `V2__seed_skills.sql` nạp 6 nhóm (Lập trình, Dữ liệu, Thiết kế, Ngoại ngữ, Âm nhạc, Kỹ năng mềm), khoảng 25 kỹ năng và 8–10 cặp kỹ năng liên quan cùng nhóm. Danh sách cụ thể được duyệt trước khi viết SQL. Lệnh INSERT quan hệ tra id theo tên, JOIN thêm điều kiện hai kỹ năng cùng `category_id`, và dùng `LEAST`/`GREATEST` để thỏa `CHECK skill_a_id < skill_b_id`. Cuối file đếm số dòng của `skill_categories`, `skills`, `skill_relations`; sai số mong đợi thì `RAISE EXCEPTION` để Flyway dừng (tránh gõ sai tên làm INSERT âm thầm chèn 0 dòng). | FR-03, TK 3.3; R14 |
| D4 | Tài khoản admin đầu tiên được tạo bằng cách nào? | Admin đầu tiên được tạo lúc khởi động: đọc `ADMIN_EMAIL`, `ADMIN_PASSWORD` từ `.env`; email được chuẩn hóa như D9. Nếu chưa có tài khoản với email đó thì tạo với `role = ADMIN`, `status = ACTIVE`, `email_verified_at = now()`. Khởi động lại không tạo trùng. Không đưa mật khẩu vào migration. | Spec 6.3; R13, R16 |

## 2. Đăng ký và xác thực email

| Mã | Câu hỏi | Quyết định | Nguồn |
| --- | --- | --- | --- |
| D5 | Xác thực email bằng mã nhập tay hay bằng link? | Xác thực bằng **link chứa token** ngẫu nhiên 256 bit (mã hóa Base64URL), không có mã để nhập tay. Link có dạng `${FRONTEND_URL}/verify-email?token=...`, với `FRONTEND_URL` đọc từ `.env` (dev: `http://localhost:5173`). Trang S02 đọc `token` từ URL rồi gọi `POST /api/auth/verify-email`. | FR-01, TK 4.2, S02; R10 |
| D6 | Token lưu trong DB dạng gì? | `token_hash` = SHA-256 của token, lưu dạng hex 64 ký tự. Không dùng BCrypt vì cần tra cứu theo hash. | NFR-01, TK 3.3 |
| D7 | Token hết hạn khi nào? Gửi lại link thì token cũ ra sao? Có giới hạn số lần gửi không? | Token hết hạn sau 24 giờ. Mỗi lần tạo token mới thì **xóa** (`DELETE`) các token `VERIFY_EMAIL` chưa dùng của tài khoản đó. Không dùng `used_at` cho việc này vì `used_at` nghĩa là "đã dùng". Giới hạn gửi thư: cách thư gần nhất ít nhất 60 giây, tính theo `created_at` của token mới nhất (token tạo lúc đăng ký cũng tính). `resend-verification` chỉ gửi thư khi tài khoản đang `PENDING_VERIFICATION` và đã qua 60 giây; mọi trường hợp khác (email không tồn tại, `ACTIVE`, `LOCKED`, chưa đủ 60 giây) vẫn trả 204 nhưng không gửi thư. Email thiếu hoặc sai định dạng: xem D42. | FR-01, TK 4.2, G8; R06, R11, R16 |
| D8 | Đăng ký bằng email đã có trong hệ thống thì sao? | Đăng ký với email đã thuộc tài khoản `ACTIVE` hoặc `LOCKED` trả 409. Đăng ký với email đang `PENDING_VERIFICATION` được coi là **đăng ký lại**: ghi đè `password_hash` và `full_name`, tạo token mới và gửi thư theo D7, trả 201 giống đăng ký mới. Nếu chưa đủ 60 giây từ thư trước thì vẫn ghi đè nhưng không tạo token mới và không gửi thư. Lý do: nếu trả 409, người khác có thể đăng ký trước bằng email của chủ thật, rồi khi chủ thật xác thực, tài khoản mang mật khẩu của kẻ kia (rà soát R01). Hạn chế còn lại: ai đăng ký sau cùng trước khi link được bấm thì mật khẩu của người đó có hiệu lực; chưa có quên mật khẩu (D2) nên chủ email không tự đặt lại được. Ghi vào phần hạn chế của báo cáo. | FR-01, TK 4.1; R01 |
| D9 | Email viết hoa, có khoảng trắng thì xử lý thế nào? Đuôi email trường lấy ở đâu? | Email được bỏ khoảng trắng thừa và đổi về chữ thường trước khi kiểm tra và lưu, ở **mọi** chỗ nhận email: đăng ký, đăng nhập, gửi lại thư, `ADMIN_EMAIL` (D4). Email dài tối đa 255 ký tự (cột `VARCHAR(255)`). Danh sách đuôi email trường cấu hình trong `application.yml` (không ghi cứng trong code); chỉ nhận đúng đuôi đã liệt kê. | FR-01, G6; R13, R16 |
| D10 | Mật khẩu dài bao nhiêu, có yêu cầu ký tự đặc biệt không? | Mật khẩu dài 8–72 ký tự **và** không quá 72 byte khi mã hóa UTF-8 (`password.getBytes(UTF_8).length <= 72`); vượt thì 400. Lý do: BCrypt chỉ dùng 72 byte đầu, và chữ tiếng Việt có dấu chiếm 2–3 byte, nên 72 ký tự có dấu có thể vượt giới hạn. Không bắt buộc ký tự đặc biệt. | NFR-01; R02 |
| D11 | Khi phát triển trên máy, thư được gửi đi đâu? | Ở máy dev, email được gửi tới **Mailpit** chạy trong `docker-compose` (SMTP `localhost:1025`, xem thư ở `http://localhost:8025`). | FR-01; R15 |
| D25 | Đăng ký cần gửi những trường nào? | Body của `POST /api/auth/register`: `{email, password, fullName}`, cả ba bắt buộc. `email` theo D9, `password` theo D10, `fullName` theo D16. Màn hình S01 có ô "Họ tên". | TK 4.2, TK 3.3 (`full_name NOT NULL`), S01; R03 |
| D26 | Khi nào bấm link thì xác thực thành công? Thất bại thì báo gì? | `verify-email` chỉ đổi trạng thái khi token tồn tại, đúng `purpose = VERIFY_EMAIL`, chưa dùng, chưa hết hạn, **và** tài khoản đang `PENDING_VERIFICATION`. Khi đó: `status = ACTIVE`, `email_verified_at = now()`, token có `used_at = now()`. Mọi trường hợp khác (không tồn tại, đã dùng, hết hạn, sai `purpose`, tài khoản `LOCKED` hoặc đã `ACTIVE`) trả 400 với cùng một `detail`, để người ngoài không phân biệt được. | TK 4.2; R06, R16 |
| D27 | Gửi thư lỗi thì đăng ký có thất bại không? | Đăng ký: lưu user và token trong một transaction, gửi thư **sau khi** commit. Gửi thư lỗi thì ghi log và vẫn trả 201; người dùng dùng "gửi lại link". | FR-01; R09 |
| D42 | `resend-verification` nhận email thiếu hoặc sai định dạng thì trả gì? (Thiết kế ghi "luôn trả 204", còn D24 nói dữ liệu sai trả 400) | Body thiếu `email`, `email` rỗng, sai định dạng hoặc dài hơn 255 ký tự trả **400** (lỗi validation theo D24, có `errors`), không gửi thư. Mọi email **đúng định dạng** thì theo D7: luôn trả 204, chỉ gửi thư khi đủ điều kiện. Không lộ thông tin, vì việc trả 400 chỉ phụ thuộc chuỗi gửi lên, không phụ thuộc email đó có trong DB hay không. | TK 4.2, D7, D24 |

## 3. Đăng nhập và phân quyền

| Mã | Câu hỏi | Quyết định | Nguồn |
| --- | --- | --- | --- |
| D12 | Tài khoản chưa xác thực email có đăng nhập được không? | Tài khoản `PENDING_VERIFICATION` **được đăng nhập** và dùng các API quyền *User* (`/api/me*`, danh mục kỹ năng). Các API quyền *Active* trả 403. | TK 4.1, 4.3, G9 |
| D13 | Đăng nhập kiểm tra theo thứ tự nào? Khóa tài khoản thì token đã cấp còn dùng được không? | Đăng nhập kiểm tra theo thứ tự: tìm email → so mật khẩu → xét trạng thái. Email không tồn tại hoặc sai mật khẩu đều trả 401 với cùng một thông báo. Chỉ khi mật khẩu **đúng** mà tài khoản `LOCKED` mới trả 403 (kiểm tra trạng thái trước mật khẩu sẽ cho người ngoài biết email đó tồn tại). Mỗi request có token đều đọc lại user từ DB: user không còn tồn tại thì 401; `LOCKED` thì 403. Nhờ vậy khóa tài khoản có hiệu lực ngay, không chờ token hết hạn. | FR-01, NFR-01, G9; R07, R16 |
| D14 | Làm JWT bằng thư viện nào, token chứa gì? | Dùng `spring-boot-starter-security-oauth2-resource-server` (Nimbus; Spring Boot 4 đổi tên, tên cũ `spring-boot-starter-oauth2-resource-server` đã deprecated). Token ký HS256 bằng `JWT_SECRET`, hạn 24 giờ, chưa có refresh token. Claim `sub` = userId, claim `role` = `STUDENT`/`ADMIN`. | TK 4.1, G5 |

## 4. Hồ sơ, lịch rảnh, kỹ năng

| Mã | Câu hỏi | Quyết định | Nguồn |
| --- | --- | --- | --- |
| D15 | Sửa hồ sơ mà bỏ trống hoặc không gửi một trường thì xóa hay giữ? Gửi trường lạ thì sao? | `PUT /api/me` thay toàn bộ hồ sơ. Với `bio`, `learningMode`, `phone`, `contactInfo`: không gửi khóa, gửi `null`, chuỗi rỗng hoặc chỉ có khoảng trắng đều lưu NULL (ví dụ `learningMode = null` là "chưa khai báo"). `fullName` bắt buộc (D16). Trường không có trong API (ví dụ `email`) bị bỏ qua, không báo lỗi: không đổi email được qua API này. | FR-02, TK 4.3; R08 |
| D16 | Mỗi trường hồ sơ được dài bao nhiêu, nhận ký tự gì? | `fullName` bắt buộc, 1–100 ký tự sau khi bỏ khoảng trắng hai đầu; `bio` tối đa 1000; `contactInfo` tối đa 500; `phone` tối đa 20 ký tự, chỉ gồm số, khoảng trắng, `+`, `-`. | FR-02, TK 3.3 |
| D17 | Email có hiện trong hồ sơ công khai không? | Email trường không xuất hiện trong hồ sơ công khai (NFR-02). | NFR-02 |
| D18 | Các cột thời gian được ghi bằng gì? | `created_at` do Hibernate ghi bằng `@CreationTimestamp`, `updated_at` bằng `@UpdateTimestamp`. **Ngoại lệ:** các mốc thời gian dùng cho quy tắc thời gian của token và xác thực (`user_tokens.created_at`, `expires_at`, `used_at`, `users.email_verified_at`, kể cả khi tạo admin ở D4) do service gán từ **một** bean `java.time.Clock` dùng chung; không dùng `@CreationTimestamp` hay `Instant.now()` cho các cột này. Lý do: D7 so `created_at` của token với giờ hiện tại, và test (TC06, TC41, TC43, TC77, TC78, TC89) thay `Clock` bằng đồng hồ giả lập; nếu `created_at` lấy giờ thật của máy còn code so với giờ giả thì hai đồng hồ lệch nhau và test sai. | TK 3.3; R12 |
| D19 | Một kỹ năng vừa dạy vừa muốn học được không? | Một kỹ năng không được vừa nằm trong danh sách dạy vừa nằm trong danh sách muốn học (400), xét cả hai chiều. Muốn chuyển một kỹ năng từ "muốn học" sang "dạy", S04 lưu danh sách học (đã bỏ kỹ năng đó) trước, rồi mới lưu danh sách dạy. | FR-03; R16 |
| D20 | Kỹ năng bị admin ẩn thì người đang có nó ra sao? | Kỹ năng bị ẩn (`is_active = false`) không thêm mới được (400), nhưng nếu người dùng đã có từ trước thì được giữ khi `PUT` lại danh sách. | TK 3.3, TK 4.4 |
| D21 | Mỗi danh sách được bao nhiêu phần tử? Gửi trùng thì sao? | Tối đa 10 kỹ năng mỗi danh sách (dạy và học); lịch rảnh tối đa 21 khung. Gửi trùng `skillId` hoặc trùng khung giờ **trong cùng một request** trả 400 (409 dành cho xung đột với dữ liệu đã có, ví dụ email đã đăng ký). | Spec 4.6, TK 4.1; R16 |
| D28 | Làm sao đọc lại lịch rảnh và kỹ năng đã lưu? | Đọc lại dữ liệu của tôi: `GET /api/me/availability` trả `[{dayOfWeek, period}]`; `GET /api/me/teach-skills` trả `[{skillId, skillName, categoryId, categoryName, isActive, level}]`; `GET /api/me/learn-skills` tương tự với `targetLevel` (có thể `null`). Trả cả tên và `isActive` vì kỹ năng đã bị ẩn (D20) không có trong `GET /api/skills`. Ba API này quyền *User*, như các `PUT` tương ứng. | TK 4.3, S03, S04; R05 |
| D40 | API danh mục kỹ năng trả về những trường nào, theo thứ tự nào? | `GET /api/skill-categories` trả `[{id, name}]`. `GET /api/skills` trả `[{id, name, categoryId, categoryName}]`, chỉ gồm kỹ năng `is_active = true`. Cả hai xếp theo `name` (thứ tự `ORDER BY name` của PostgreSQL), rồi theo `id`. `categoryId` không tồn tại thì trả danh sách rỗng (200); `q` rỗng thì không lọc theo tên. | TK 4.4 |
| D41 | Các danh sách "của tôi" trả về theo thứ tự nào? | `GET /api/me/availability` xếp theo `dayOfWeek` tăng dần, rồi `period` theo thứ tự trong ngày `MORNING` → `AFTERNOON` → `EVENING` (không theo chữ cái). `GET /api/me/teach-skills` và `/learn-skills` xếp theo `skillName`, rồi `skillId`. Các `PUT` tương ứng trả cùng thứ tự. Lý do: test so sánh danh sách cần thứ tự cố định. Tên xếp theo thứ tự của PostgreSQL, nên ca kiểm thử chỉ so thứ tự giữa các tên không dấu. | TK 4.3, D28, D29 |

## 5. Quy ước API và frontend

| Mã | Câu hỏi | Quyết định | Nguồn |
| --- | --- | --- | --- |
| D22 | Frontend gọi backend khác cổng thế nào khi dev? | Khi dev, Vite proxy chuyển `/api` sang `http://localhost:8080`, chưa cấu hình CORS. | TK 2 (kiến trúc) |
| D23 | Lưu token ở đâu, dùng thư viện gì cho điều hướng và form? | Token lưu trong `localStorage`. Điều hướng bằng `react-router`. Form viết tay, chưa dùng thư viện form. | TK 2 (kiến trúc), TK 6 |
| D24 | Lỗi trả về dạng gì, kể cả lỗi từ Spring Security và từ DB? | Lỗi validation (400) trả Problem Details kèm thuộc tính `errors: [{ "field": "...", "message": "..." }]`. Lỗi 401/403 do Spring Security sinh ra ở tầng filter **không** tự thành Problem Details; phải tự viết `AuthenticationEntryPoint` (401) và `AccessDeniedHandler` (403) trả `application/problem+json`. Lỗi vi phạm ràng buộc DB (ví dụ trùng khóa) không được lọt ra thành 500: service kiểm tra trước, hoặc bắt `DataIntegrityViolationException` và đổi thành 400/409. | TK 4.1; R17 |
| D29 | Mỗi API thành công trả mã gì và nội dung gì? | Mã trả về khi thành công: `register` 201 kèm `{id, email, status}`; `verify-email` 204; `resend-verification` 204; `login` 200 kèm `{accessToken}`; các `PUT /api/me*` 200 kèm dữ liệu vừa lưu (cùng dạng với `GET` tương ứng). `GET /api/me` trả `{id, email, fullName, role, status, bio, learningMode, phone, contactInfo}`; frontend dùng `status` để hiện thông báo "chưa xác thực email". | TK 4.2, 4.3; R04 |
| D30 | Entity Java phải viết thế nào để khớp bảng khi `ddl-auto: validate`? | Quy ước ánh xạ Entity (vì `ddl-auto: validate` so kiểu Java với kiểu cột): `BIGINT` → `Long`; `SMALLINT` → `Short` (dùng `Integer` sẽ lỗi validate); `TIMESTAMPTZ` → `Instant`; `version` → `Integer` + `@Version`; cột chuỗi có `CHECK IN (...)` → enum + `@Enumerated(EnumType.STRING)`. Cột có `DEFAULT` trong DB (`role`, `status`, `is_active`) phải gán giá trị mặc định ngay ở field Java, vì Hibernate luôn ghi đủ mọi cột khi INSERT nên `DEFAULT` của DB không được dùng. Bảng khóa ghép (`user_teach_skills`, `user_learn_skills`, `user_availability`) dùng `@IdClass` hoặc `@EmbeddedId`; khi `PUT` thay danh sách thì xóa bằng câu `DELETE ... WHERE user_id = ?` trước rồi mới chèn. Validate không kiểm tra độ dài hay `CHECK`, nên DTO phải có `@Size` khớp từng `VARCHAR(n)`. | `V1__init.sql`, TK 3.3; R12 |

## 6. Kiểm thử

| Mã | Câu hỏi | Quyết định | Nguồn |
| --- | --- | --- | --- |
| D39 | Test tích hợp chạy trên database nào, dọn dữ liệu giữa các test thế nào? | Test `@SpringBootTest` chạy trên database riêng `skillexchange_test` trong cùng container PostgreSQL, không đụng database `skillexchange` của ứng dụng. Tạo một lần: `docker compose exec db createdb -U skillexchange skillexchange_test`; Flyway tự chạy migration khi test khởi động. Cấu hình test (ví dụ profile `test`) chỉ đổi tên database, vẫn đọc tài khoản và mật khẩu DB từ `.env`. Trước mỗi test xóa dữ liệu người dùng (`TRUNCATE users RESTART IDENTITY CASCADE`, kéo theo mọi bảng tham chiếu tới `users`); giữ nguyên danh mục kỹ năng do `V2` nạp, test nào đổi danh mục (ví dụ ẩn kỹ năng) phải tự trả lại như cũ. **Không** dùng `@Transactional` tự rollback cho test có gửi thư, vì D27 chỉ gửi thư sau khi transaction commit; rollback thì thư không bao giờ được gửi và TC03, TC80 không kiểm được. Unit test thuần Java không cần DB. | D27, CLAUDE.md (lệnh test) |
