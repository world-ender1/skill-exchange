# KẾ HOẠCH TUẦN 3

Lập ngày 2026-10-31. Áp dụng `docs/tuan_3/Quyet_dinh_tuan_3.md` (D1–D30, D39–D42) và `docs/tuan_3/kiem_thu/ca_kiem_thu.md` (93 ca). Mỗi bước: viết test trước cho các ca của bước, chạy cho FAIL, viết code cho PASS, không sửa test, rồi commit riêng. Số bước khớp với bản đồ học tập của tuần (bước 1 là đọc tài liệu và lập kế hoạch này; bước 11 là kiểm tra cả tuần và báo cáo).

Quy ước chung:
- Package theo tính năng dưới `com.skillexchange`: `skill`, `common`, `auth`, `user`.
- Test tích hợp kế thừa một lớp nền chung (tạo ở bước 2, mở rộng dần), mỗi test có `@DisplayName("TCxx ...")`.
- Sau mỗi bước: `git diff --stat -- backend/src/test` để chắc test không bị sửa sau khi duyệt; `.\mvnw.cmd test` xanh toàn bộ (hồi quy cả các bước trước).

## 1. Danh mục kỹ năng đã duyệt (D3)

| Nhóm | Kỹ năng |
| --- | --- |
| Lập trình | Java, Kotlin, Python, JavaScript, TypeScript, C++ |
| Dữ liệu | SQL, MySQL, Excel, Power BI |
| Thiết kế | Figma, Photoshop, Illustrator, Canva |
| Ngoại ngữ | Tiếng Anh, Tiếng Nhật, Tiếng Hàn, Tiếng Trung |
| Âm nhạc | Guitar, Ukulele, Piano, Thanh nhạc |
| Kỹ năng mềm | Thuyết trình, Viết CV, Quản lý thời gian, Làm việc nhóm |

| # | Cặp liên quan | Nhóm | Lý do |
| --- | --- | --- | --- |
| 1 | Java – Kotlin | Lập trình | Cùng chạy trên JVM, cú pháp gần nhau (Bài 4 dùng) |
| 2 | JavaScript – TypeScript | Lập trình | TypeScript là JavaScript có thêm kiểu dữ liệu |
| 3 | SQL – MySQL | Dữ liệu | MySQL là hệ quản trị dùng SQL (ví dụ ở spec 4.6) |
| 4 | Excel – Power BI | Dữ liệu | Cùng làm việc với bảng số liệu |
| 5 | Photoshop – Illustrator | Thiết kế | Cùng bộ Adobe, thao tác giống nhau |
| 6 | Figma – Canva | Thiết kế | Đều là công cụ thiết kế kéo thả trên trình duyệt |
| 7 | Guitar – Ukulele | Âm nhạc | Cùng là đàn dây, cách bấm hợp âm gần nhau |
| 8 | Tiếng Nhật – Tiếng Trung | Ngoại ngữ | Kanji gốc từ chữ Hán |
| 9 | Tiếng Nhật – Tiếng Hàn | Ngoại ngữ | Trật tự câu và cách dùng trợ từ gần nhau |

Số dòng mong đợi cuối `V2__seed_skills.sql`: **6** nhóm, **26** kỹ năng, **9** quan hệ.

Ràng buộc khi sửa danh sách về sau: Python, Tiếng Anh không được liên quan với kỹ năng nào, và Java chỉ liên quan với Kotlin, vì ví dụ An–Bình (tuần 4) tính điểm dựa trên điều đó. Nhóm Kỹ năng mềm cố ý không có cặp nào.

## 2. Việc làm trước (không phải code, tự làm)

| Trước bước | Việc |
| --- | --- |
| 2 | Tạo DB test: `docker compose exec db createdb -U skillexchange skillexchange_test` (D39) |
| 3 | Thêm `JWT_SECRET` (chuỗi ngẫu nhiên ít nhất 32 byte) vào `.env` |
| 5 | Thêm `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `FRONTEND_URL` vào `.env` (chép từ `.env.example`) |
| 7 | Thêm `ADMIN_EMAIL`, `ADMIN_PASSWORD` vào `.env` |

## 3. Các bước

### Bước 2 – Danh mục kỹ năng (D3, D30, D39, D40)

- **Tạo:** `db/migration/V2__seed_skills.sql` (theo mục 1); `skill/SkillCategory`, `Skill` (Entity), `SkillCategoryRepository`, `SkillRepository`, `SkillService`, `SkillController`, DTO `SkillCategoryResponse`, `SkillResponse`.
- **Test:** `src/test/resources/application-test.yml` (đổi DB sang `skillexchange_test`); lớp nền `IntegrationTestBase` (profile test, MockMvc, `TRUNCATE users RESTART IDENTITY CASCADE` trước mỗi test); `SkillCatalogTest`.
- **Ca:** TC12, TC13, TC14, TC15, TC92. Test dùng `@WithMockUser` (chưa có JWT). TC13 ẩn kỹ năng rồi trả lại như cũ.
- **Tự kiểm:** đếm kỹ năng theo nhóm bằng psql; gõ sai tên trong seed thì backend dừng vì `RAISE EXCEPTION`.
- **Commit:** `feat(skill): seed skill catalog and add catalog APIs`

### Bước 3 – Bảo vệ cổng + lỗi chung (D14, D24)

- **Sửa:** `pom.xml` (thêm `spring-boot-starter-security-oauth2-resource-server`, tên mới của Spring Boot 4), `application.yml` (khóa JWT từ `JWT_SECRET`, không có giá trị mặc định).
- **Tạo:** `common/SecurityConfig` (stateless, tắt formLogin/httpBasic/CSRF, mở `/api/auth/**`, còn lại cần đăng nhập, `JwtDecoder` HS256, `PasswordEncoder` BCrypt); `common/ProblemAuthenticationEntryPoint` (401), `ProblemAccessDeniedHandler` (403); `common/GlobalExceptionHandler` (400 kèm `errors`, JSON hỏng 400, `DataIntegrityViolationException` không ra 500, 500 không lộ stack trace).
- **Ca:** TC62 (phần `/api/skills`, `/api/skill-categories`), TC73 (phần lỗi 500). Thêm test cho phần D24 mà ca chưa phủ ở bước này (400 kèm `errors`, JSON hỏng, 403, lỗi ràng buộc DB), dùng một controller chỉ có trong test; các ca TC36, TC59, TC69 kiểm lại ở bước sau trên API thật.
- **Chốt ở bước này** (tài liệu trước chưa nói rõ):
  - `DataIntegrityViolationException` lọt tới controller trả **409**, `detail` chung, không lộ tên ràng buộc hay câu SQL (chi tiết ghi log). Service vẫn phải kiểm trước để trả 400/409 có thông báo rõ (D24); 409 chỉ là lưới an toàn.
  - 401/403/500 đều có `detail` chung bằng tiếng Việt; 500 ghi đầy đủ stack trace vào log, không đưa vào response. 401 kèm header `WWW-Authenticate: Bearer` (RFC 6750).
  - `/api/admin/**` cần vai trò `ADMIN` ngay từ bước này (TK 4, quyền *Admin*), để quy tắc phân quyền nằm ở `SecurityConfig` trước khi có API quản trị.
  - Lỗi bảo mật ném từ trong controller/service (`AccessDeniedException`, `AuthenticationException`) cũng thành 403/401 Problem Details, không bị bộ bắt lỗi chung đổi thành 500. Bộ xử lý 401/403 của tầng filter chuyển lỗi sang `GlobalExceptionHandler`, nên mọi Problem Details được tạo ở một chỗ.
- **Tự kiểm:** gọi `/api/skills` không có vé → 401 `application/problem+json`; test bước 2 vẫn xanh.
- **Commit:** `feat(common): add security config and problem details error handling`

### Bước 4 – Đăng ký (D8 phần 409 và ghi đè, D9, D10, D25, D30)

- **Sửa:** `application.yml` (danh sách đuôi email trường, ví dụ `app.allowed-email-domains`; cấu hình test dùng `student.example.edu.vn`).
- **Tạo:** `user/User` (Entity, enum `Role`, `UserStatus`, giá trị mặc định ở field), `UserRepository`; `auth/RegisterRequest` (chuẩn hóa email trước khi validate), `RegisterResponse`, `AuthService.register`, `AuthController`; kiểm đuôi email và giới hạn 72 byte của mật khẩu.
- **Ca:** TC01, TC02, TC32–TC37, TC38 (phần đăng ký), TC60, TC85, TC86. TC34, TC35 tạo sẵn user `ACTIVE` bằng Repository.
- **Chốt ở bước này** (tài liệu trước chưa nói rõ):
  - Ghi đè khi đăng ký lại email `PENDING_VERIFICATION` (D8) làm ngay ở bước này: ghi đè `password_hash`, `full_name`, trả 201 với cùng `id`. Phần token, thư và giới hạn 60 giây làm ở bước 5 (TC77, TC78). Bước này có test riêng `D8 ...` cho phần ghi đè.
  - `ACTIVE` và `LOCKED` cùng trả 409 với cùng một thông báo (không lộ trạng thái tài khoản).
  - DTO kiểm định dạng và độ dài (`@Email`, `@Size`); Service kiểm đuôi email trường và giới hạn 72 byte. Cả hai loại lỗi đều trả 400 với `errors[{field, message}]`.
  - `fullName` bỏ khoảng trắng hai đầu trước khi kiểm và lưu (D16). `password` không bị cắt khoảng trắng, chỉ kiểm 8–72 ký tự và 72 byte (D10 không đặt thêm điều kiện).
  - Đuôi email so khớp chính xác phần sau dấu `@` cuối cùng với danh sách `app.allowed-email-domains`. `application.yml` dùng tạm `student.example.edu.vn` (giống `ADMIN_EMAIL` mẫu); đổi sang đuôi trường thật khi triển khai.
- **Tự kiểm:** `SELECT email, password_hash, status FROM users;` → băm `$2a$`/`$2b$`, email chữ thường; đăng ký `@gmail.com` → 400.
- **Commit:** `feat(auth): add registration with school email validation`

### Bước 5 – Thư có link, xác thực, gửi lại (D5–D8, D11, D18, D26, D27, D42)

Tách 2–3 commit.
- **5a. Mailpit:** sửa `docker-compose.yml` (thêm dịch vụ `mailpit`, cổng 1025/8025), `pom.xml` (thêm `spring-boot-starter-mail`), `application.yml` (`spring.mail.*`, `FRONTEND_URL`). Commit `chore: add mailpit to docker compose`.
- **5b. Token, verify, resend:** tạo `common/ClockConfig` (bean `Clock`); `auth/UserToken` (Entity, enum `TokenPurpose`), `UserTokenRepository`; `auth/TokenService` (SecureRandom 32 byte → Base64URL, SHA-256 hex, xóa token chưa dùng, giới hạn 60 giây theo `created_at`); `auth/VerificationMailer` (gửi sau commit, lỗi thì ghi log); mở rộng `AuthService` (verify, resend, đăng ký lại tài khoản `PENDING_VERIFICATION`); DTO `VerifyEmailRequest`, `ResendVerificationRequest`.
- **Test:** lớp nền thêm đồng hồ giả chỉnh tay được và mock `JavaMailSender`.
- **Ca:** TC03, TC05, TC06, TC07, TC39–TC43, TC61, TC72, TC77, TC78, TC80, TC87, TC88, TC89, TC91.
- **Chốt ở bước này** (tài liệu trước chưa nói rõ):
  - Chia 3 commit: 5a Mailpit; 5b đăng ký gửi thư + verify (TC03, TC05, TC06, TC39–TC41, TC61, TC80, TC87, TC88); 5c resend + giới hạn 60 giây + đăng ký lại (TC07, TC42, TC43, TC72, TC77, TC78, TC89, TC91).
  - Mailpit ghim `axllent/mailpit:v1.31.6`. `spring.mail.properties` đặt timeout 5 giây để máy chủ thư tắt không làm treo request. `app.frontend-url: ${FRONTEND_URL}` không có mặc định; `app.mail.from: no-reply@skillexchange.local`.
  - Biên thời gian: được gửi thư mới khi đã qua **≥ 60 giây** kể từ `created_at` của token mới nhất; token còn dùng được khi giờ hiện tại **< `expires_at`** (đúng 24 giờ là hết hạn).
  - `verify-email` thất bại trả 400 với một `detail` chung "Link xác thực không hợp lệ hoặc đã hết hạn" (D26). Body thiếu hoặc rỗng `token` là lỗi validation, trả 400 kèm `errors[].field = token` (chỉ phụ thuộc chuỗi gửi lên, như D42).
  - Thư dạng văn bản thường, tiêu đề "Xác thực email Skill Exchange", gửi bằng `@TransactionalEventListener(phase = AFTER_COMMIT)`; lỗi gửi thì ghi log mức ERROR, API vẫn trả 201/204 (D27).
  - TC77 phần "đăng nhập bằng mật khẩu cũ/mới" kiểm ở bước 6 (chưa có login); bước 5 kiểm bằng `PasswordEncoder.matches` trên `password_hash`.
- **Tự kiểm:** thư hiện trong Mailpit có link `http://localhost:5173/verify-email?token=...`; gọi verify bằng `.http` lần 1 → 204, lần 2 → 400; `token_hash` khác token; tắt Mailpit vẫn đăng ký được.
- **Commit:** `feat(auth): add email verification links with hashed tokens` (có thể tách verify và resend + đăng ký lại).

### Bước 6 – Đăng nhập JWT (D12, D13, D14)

- **Tạo:** `auth/JwtTokenService` (`JwtEncoder` HS256, claim `sub`, `role`, hạn 24 giờ tính từ `Clock`); `auth/LoginRequest`, `LoginResponse`, `AuthService.login` (email → mật khẩu → trạng thái); bộ lọc đọc lại user mỗi request (không tồn tại → 401, `LOCKED` → 403); `JwtAuthenticationConverter` đọc claim `role`.
- **Test:** tiện ích tạo vé trong test (hợp lệ, sai chữ ký, hết hạn).
- **Ca:** TC08, TC09, TC10, TC11, TC44, TC63, TC64, TC65, TC68, TC69, TC70, TC71, TC90; phần `login` của TC73; phần "đăng nhập bằng mật khẩu cũ/mới" của TC77 (dời từ bước 5).
- **Chốt ở bước này** (tài liệu trước chưa nói rõ):
  - TC63, TC64, TC69, TC70 ghi `GET /api/me`, nhưng API này bước 8 mới có. Bước này kiểm trên `GET /api/skills` (cũng cần đăng nhập); bộ lọc vé chạy giống nhau cho mọi đường dẫn cần đăng nhập. Bước 8 chỉ thêm phần `/api/me` của TC62, TC73.
  - Thông báo: email không tồn tại và sai mật khẩu (kể cả tài khoản `LOCKED` sai mật khẩu, TC90) cùng 401 `detail` "Email hoặc mật khẩu không đúng"; `LOCKED` đúng mật khẩu → 403 "Tài khoản đã bị khóa". Khi email không tồn tại vẫn chạy một lần so BCrypt với bản băm giả, để thời gian trả lời không lộ email nào đã đăng ký.
  - Body `login`: `email` thiếu/rỗng hoặc `password` thiếu/rỗng → 400 kèm `errors` (D24). Không kiểm định dạng email hay độ dài mật khẩu: sai thì chỉ là không khớp → 401. Mật khẩu quá 72 byte cũng trả 401 (không so BCrypt, vì BCrypt ném lỗi với chuỗi quá 72 byte), không được thành 500.
  - Hạn vé: `iat` lấy từ bean `Clock`, `exp = iat + 24 giờ`. Bộ giải mã vé kiểm `exp` theo **cùng** bean `Clock` (để test vặn giờ được) và không cho lệch giờ (clock skew = 0) vì một máy chủ vừa phát vừa kiểm vé. Vé dùng được tới hết thời điểm `exp`, quá `exp` là 401.
  - Vai trò lấy từ claim `role` thành quyền `ROLE_STUDENT`/`ROLE_ADMIN`. Trạng thái tài khoản đọc lại từ DB mỗi request (D13); vai trò không có API đổi nên đọc từ vé.
  - Đường dẫn `/api/auth/**` bỏ qua header `Authorization`: vé cũ hết hạn còn nằm trong `localStorage` không chặn được việc đăng nhập lại (bước 10).
  - Quyền *Active* (`status = ACTIVE`, `role = STUDENT`) chưa có API nào dùng trong tuần 3, làm cùng API tuần 4.
- **Tự kiểm:** dán vé vào jwt.io (chỉ có `sub`, `role`, `iat`, `exp`); sửa 1 ký tự → 401; sai mật khẩu và sai email cùng thông báo.
- **Commit:** `feat(auth): add JWT login`

### Bước 7 – Admin đầu tiên (D4)

- **Tạo:** `auth/AdminInitializer` (`ApplicationRunner`; chuẩn hóa email; tạo nếu chưa có; `email_verified_at` từ `Clock`). Hành vi khi thiếu `ADMIN_EMAIL`/`ADMIN_PASSWORD` chốt ở bước này và ghi bổ sung vào file này.
- **Ca:** TC81 (gọi runner hai lần trong test).
- **Chốt ở bước này** (tài liệu trước chưa nói rõ; cách xử lý khi thiếu cấu hình do người dùng chọn ngày 2026-11-01):
  - Cấu hình `app.admin.email: ${ADMIN_EMAIL:}`, `app.admin.password: ${ADMIN_PASSWORD:}`. Thiếu hoặc rỗng một trong hai, email sai định dạng hoặc dài hơn 255 ký tự, mật khẩu không đạt D10 (8–72 ký tự, ≤ 72 byte) → **ứng dụng dừng lúc khởi động**, thông báo lỗi nói rõ biến nào sai nhưng **không** in giá trị mật khẩu. Giống cách làm với `JWT_SECRET`, `FRONTEND_URL`: không có hệ thống nào chạy mà không có người quản trị danh mục kỹ năng.
  - Email đã có tài khoản (bất kể vai trò, trạng thái) → không tạo, không sửa gì. Nếu tài khoản đó không phải `ADMIN` thì ghi log WARN; **không** tự nâng quyền (ai đó đăng ký trước bằng email này thì không thành admin).
  - Không kiểm đuôi email trường cho admin (admin không phải sinh viên, D9 chỉ áp dụng cho đăng ký). `full_name` của admin: "Quản trị viên".
  - Đổi `ADMIN_PASSWORD` sau khi admin đã được tạo không đổi mật khẩu trong DB.
  - Log khi tạo chỉ ghi email, không ghi mật khẩu.
- **Tự kiểm:** khởi động 2 lần vẫn 1 admin; vé admin có `role = ADMIN`; log không có mật khẩu.
- **Commit:** `feat(auth): create initial admin account on startup`

### Bước 8 – Hồ sơ của tôi (D15, D16, D18, D29)

- **Tạo:** `user/MeController`, `UserProfileService`, DTO `MyProfileResponse`, `UpdateProfileRequest` (không có `email`/`role`/`status`; rỗng → NULL); `updated_at` bằng `@UpdateTimestamp`.
- **Ca:** TC16–TC20, TC38 (phần `PUT /api/me`), TC45, TC46, TC47, TC48, TC59, TC66; phần `/api/me` của TC62, TC73.
- **Chốt ở bước này** (tài liệu trước chưa nói rõ):
  - "Tôi là ai" chỉ lấy từ claim `sub` của vé đã kiểm chữ ký; body không có trường `id`, gửi kèm `id`, `email`, `role`, `status` đều bị bỏ qua (D15, TC48, TC66).
  - `bio`, `phone`, `contactInfo` bỏ khoảng trắng hai đầu trước khi kiểm và lưu (như `fullName`, D16); còn rỗng thì lưu NULL (D15). Độ dài kiểm sau khi bỏ khoảng trắng.
  - `learningMode` nhận chuỗi `ONLINE`/`OFFLINE`/`ANY` (đúng chữ hoa); giá trị khác → 400 kèm `errors[].field = learningMode` (không để lỗi đọc JSON chung, để frontend hiện lỗi cạnh ô).
  - `phone` chỉ gồm chữ số, khoảng trắng, `+`, `-` (D16), không bắt buộc phải có chữ số.
  - Admin gọi được `GET`/`PUT /api/me` (quyền *User*, TK 4.3); frontend cần `role` từ `GET /api/me`.
  - `PUT /api/me` không có body hoặc body là `null` → 400 (lỗi đọc JSON, như TC59).
- **Tự kiểm:** sửa hồ sơ bằng `.http` rồi xem lại; tấn công thử A sửa hồ sơ B, ghi các cách đã thử vào `.http`.
- **Commit:** `feat(user): add my profile APIs`

### Bước 9 – Giờ rảnh, kỹ năng dạy và học (D19, D20, D21, D28, D30, D41)

- **Tạo:** Entity khóa ghép `UserAvailability`, `UserTeachSkill`, `UserLearnSkill` (lớp khóa, enum `Period`, kiểu `Short`) và Repository; service thay danh sách (xóa rồi chèn trong một `@Transactional`; kiểm trùng trong request, tối đa 10 kỹ năng / 21 khung, kỹ năng ẩn, dạy–học chồng nhau); mở rộng `MeController` với 6 API `GET`/`PUT`; DTO tương ứng.
- **Ca:** TC21–TC30, TC49–TC58, TC67, TC79, TC82, TC83, TC84, TC93; phần `PUT /api/me/availability` của TC62. Ca cần ẩn kỹ năng đổi thẳng `is_active` trong test rồi trả lại.
- **Chốt ở bước này** (tài liệu trước chưa nói rõ):
  - Body của 3 `PUT` là mảng JSON (TK 4.3). Lỗi 400 vẫn có `errors` (D24): lỗi của phần tử thứ `i` (đếm từ 0) có `field = "[i].tenTruong"` (ví dụ `[0].dayOfWeek`, `[2].skillId`); lỗi của cả danh sách (quá 10 kỹ năng / 21 khung) có `field` là tên danh sách: `availability`, `teachSkills`, `learnSkills`. Phần tử `null` trong mảng → 400. Không gửi body → 400.
  - `period` nhận chuỗi `MORNING`/`AFTERNOON`/`EVENING` (đúng chữ hoa), giá trị khác → 400 kèm `errors[].field = [i].period` (như `learningMode` ở bước 8). `dayOfWeek`, `level`, `targetLevel` là số nguyên; `level` bắt buộc.
  - Thứ tự kiểm khi `PUT` kỹ năng: dữ liệu từng phần tử và số lượng → trùng `skillId` trong request (báo ở lần xuất hiện thứ hai) → kỹ năng không tồn tại → kỹ năng bị ẩn mà chưa có → chồng với danh sách kia (D19). Gặp lỗi đầu tiên thì dừng, không thay đổi gì trong DB.
  - "Đã có từ trước" ở D20 nghĩa là đã nằm trong **chính** danh sách đang `PUT` (đang dạy Java bị ẩn thì `PUT` dạy vẫn giữ được Java; nhưng chuyển Java bị ẩn sang danh sách học là thêm mới → 400).
  - Lỗi trùng, không tồn tại, bị ẩn, chồng dạy–học đều trả 400 với `field = "[i].skillId"` (D21 dành 409 cho xung đột với dữ liệu đã có kiểu email; ở đây là dữ liệu gửi lên sai).
  - Admin cũng gọi được 6 API này (quyền *User*); không có gì đặc biệt.
- **Tự kiểm:** lưu 2 khung rồi 1 khung → còn 1; gửi trùng kỹ năng → 400, không phải 500.
- **Commit:** `feat(user): add availability and skill profile APIs`

### Bước 10 – Frontend S01–S04 (D5, D22, D23)

- **Sửa:** `frontend/package.json` (thêm `react-router`), `vite.config.ts` (proxy `/api` → `http://localhost:8080`).
- **Tạo:** `src/api/` (hàm gọi API chung: gắn vé, đọc Problem Details, 401 → về đăng nhập), `src/auth/` (lưu vé trong `localStorage`), `src/pages/` (đăng nhập/đăng ký S01, xác thực S02 tại `/verify-email`, hồ sơ S03, kỹ năng S04), router trong `App.tsx`.
- **Ca:** TC04, TC31, TC74 (kiểm thử thủ công; frontend chưa có test tự động).
- **Tự kiểm:** đi trọn S01 → S04 rồi F5; email Gmail báo lỗi; tắt backend không trắng trang; đăng xuất rồi gõ `/profile` → về đăng nhập; `npm run build`, `npm run lint` không lỗi.
- **Commit:** mỗi màn hình một commit, ví dụ `feat(frontend): add login and register pages`.

## 4. Đối chiếu ca kiểm thử

Đã đối chiếu TC01–TC93: mọi ca tự động đều thuộc một bước.
- Ca thủ công: TC04, TC31, TC74 → bước 10.
- Ca tuần 4: TC75, TC76 → không làm tuần này.
- Ca trải trên nhiều bước (mỗi phần làm khi API tương ứng đã có): TC38 (bước 4, 8), TC62 (bước 3, 8, 9), TC73 (bước 3, 6, 8).

## 5. Bước 11 – Kiểm tra cả tuần

Truy vết ca → test, làm hỏng code để chắc test đỏ, chạy lại kiểm thử lược đồ tuần 2 (vẫn 73/73), đối chiếu code với D1–D30, D39–D42, viết `docs/tuan_3/kiem_thu/README.md` và nội dung báo cáo tuần 3.
