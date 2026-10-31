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

- **Sửa:** `pom.xml` (thêm `spring-boot-starter-oauth2-resource-server`), `application.yml` (khóa JWT từ `JWT_SECRET`, không có giá trị mặc định).
- **Tạo:** `common/SecurityConfig` (stateless, tắt formLogin/httpBasic/CSRF, mở `/api/auth/**`, còn lại cần đăng nhập, `JwtDecoder` HS256, `PasswordEncoder` BCrypt); `common/ProblemAuthenticationEntryPoint` (401), `ProblemAccessDeniedHandler` (403); `common/GlobalExceptionHandler` (400 kèm `errors`, JSON hỏng 400, `DataIntegrityViolationException` không ra 500, 500 không lộ stack trace).
- **Ca:** TC62 (phần `/api/skills`, `/api/skill-categories`), TC73 (phần lỗi 500).
- **Tự kiểm:** gọi `/api/skills` không có vé → 401 `application/problem+json`; test bước 2 vẫn xanh.
- **Commit:** `feat(common): add security config and problem details error handling`

### Bước 4 – Đăng ký (D8 phần 409 và ghi đè, D9, D10, D25, D30)

- **Sửa:** `application.yml` (danh sách đuôi email trường, ví dụ `app.allowed-email-domains`; cấu hình test dùng `student.example.edu.vn`).
- **Tạo:** `user/User` (Entity, enum `Role`, `UserStatus`, giá trị mặc định ở field), `UserRepository`; `auth/RegisterRequest` (chuẩn hóa email trước khi validate), `RegisterResponse`, `AuthService.register`, `AuthController`; kiểm đuôi email và giới hạn 72 byte của mật khẩu.
- **Ca:** TC01, TC02, TC32–TC37, TC38 (phần đăng ký), TC60, TC85, TC86. TC34, TC35 tạo sẵn user `ACTIVE` bằng Repository.
- **Tự kiểm:** `SELECT email, password_hash, status FROM users;` → băm `$2a$`/`$2b$`, email chữ thường; đăng ký `@gmail.com` → 400.
- **Commit:** `feat(auth): add registration with school email validation`

### Bước 5 – Thư có link, xác thực, gửi lại (D5–D8, D11, D18, D26, D27, D42)

Tách 2–3 commit.
- **5a. Mailpit:** sửa `docker-compose.yml` (thêm dịch vụ `mailpit`, cổng 1025/8025), `pom.xml` (thêm `spring-boot-starter-mail`), `application.yml` (`spring.mail.*`, `FRONTEND_URL`). Commit `chore: add mailpit to docker compose`.
- **5b. Token, verify, resend:** tạo `common/ClockConfig` (bean `Clock`); `auth/UserToken` (Entity, enum `TokenPurpose`), `UserTokenRepository`; `auth/TokenService` (SecureRandom 32 byte → Base64URL, SHA-256 hex, xóa token chưa dùng, giới hạn 60 giây theo `created_at`); `auth/VerificationMailer` (gửi sau commit, lỗi thì ghi log); mở rộng `AuthService` (verify, resend, đăng ký lại tài khoản `PENDING_VERIFICATION`); DTO `VerifyEmailRequest`, `ResendVerificationRequest`.
- **Test:** lớp nền thêm đồng hồ giả chỉnh tay được và mock `JavaMailSender`.
- **Ca:** TC03, TC05, TC06, TC07, TC39–TC43, TC61, TC72, TC77, TC78, TC80, TC87, TC88, TC89, TC91.
- **Tự kiểm:** thư hiện trong Mailpit có link `http://localhost:5173/verify-email?token=...`; gọi verify bằng `.http` lần 1 → 204, lần 2 → 400; `token_hash` khác token; tắt Mailpit vẫn đăng ký được.
- **Commit:** `feat(auth): add email verification links with hashed tokens` (có thể tách verify và resend + đăng ký lại).

### Bước 6 – Đăng nhập JWT (D12, D13, D14)

- **Tạo:** `auth/JwtTokenService` (`JwtEncoder` HS256, claim `sub`, `role`, hạn 24 giờ tính từ `Clock`); `auth/LoginRequest`, `LoginResponse`, `AuthService.login` (email → mật khẩu → trạng thái); bộ lọc đọc lại user mỗi request (không tồn tại → 401, `LOCKED` → 403); `JwtAuthenticationConverter` đọc claim `role`.
- **Test:** tiện ích tạo vé trong test (hợp lệ, sai chữ ký, hết hạn).
- **Ca:** TC08, TC09, TC10, TC11, TC44, TC63, TC64, TC65, TC68, TC69, TC70, TC71, TC90; phần `login` của TC73.
- **Tự kiểm:** dán vé vào jwt.io (chỉ có `sub`, `role`, `iat`, `exp`); sửa 1 ký tự → 401; sai mật khẩu và sai email cùng thông báo.
- **Commit:** `feat(auth): add JWT login`

### Bước 7 – Admin đầu tiên (D4)

- **Tạo:** `auth/AdminInitializer` (`ApplicationRunner`; chuẩn hóa email; tạo nếu chưa có; `email_verified_at` từ `Clock`). Hành vi khi thiếu `ADMIN_EMAIL`/`ADMIN_PASSWORD` chốt ở bước này và ghi bổ sung vào file này.
- **Ca:** TC81 (gọi runner hai lần trong test).
- **Tự kiểm:** khởi động 2 lần vẫn 1 admin; vé admin có `role = ADMIN`; log không có mật khẩu.
- **Commit:** `feat(auth): create initial admin account on startup`

### Bước 8 – Hồ sơ của tôi (D15, D16, D18, D29)

- **Tạo:** `user/MeController`, `UserProfileService`, DTO `MyProfileResponse`, `UpdateProfileRequest` (không có `email`/`role`/`status`; rỗng → NULL); `updated_at` bằng `@UpdateTimestamp`.
- **Ca:** TC16–TC20, TC38 (phần `PUT /api/me`), TC45, TC46, TC47, TC48, TC59, TC66; phần `/api/me` của TC62, TC73.
- **Tự kiểm:** sửa hồ sơ bằng `.http` rồi xem lại; tấn công thử A sửa hồ sơ B, ghi các cách đã thử vào `.http`.
- **Commit:** `feat(user): add my profile APIs`

### Bước 9 – Giờ rảnh, kỹ năng dạy và học (D19, D20, D21, D28, D30, D41)

- **Tạo:** Entity khóa ghép `UserAvailability`, `UserTeachSkill`, `UserLearnSkill` (lớp khóa, enum `Period`, kiểu `Short`) và Repository; service thay danh sách (xóa rồi chèn trong một `@Transactional`; kiểm trùng trong request, tối đa 10 kỹ năng / 21 khung, kỹ năng ẩn, dạy–học chồng nhau); mở rộng `MeController` với 6 API `GET`/`PUT`; DTO tương ứng.
- **Ca:** TC21–TC30, TC49–TC58, TC67, TC79, TC82, TC83, TC84, TC93; phần `PUT /api/me/availability` của TC62. Ca cần ẩn kỹ năng đổi thẳng `is_active` trong test rồi trả lại.
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
