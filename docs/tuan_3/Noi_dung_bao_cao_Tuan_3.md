# Nội dung điền vào mẫu báo cáo – Tuần 3 (Tài khoản, hồ sơ, kỹ năng)

Nguồn: `git log` tuần 3, `Quyet_dinh_tuan_3.md`, `ke_hoach_tuan_3.md`, `kiem_thu/README.md`. Phiên bản lấy từ `backend/pom.xml`, `frontend/package.json`, `docker-compose.yml`. Chỗ chưa có dữ liệu ghi **[CẦN BỔ SUNG]**. Số bảng/hình nối tiếp báo cáo tuần 2 (Bảng 5.1, Bảng 6.1, Hình 6.1).

## Bảng đối chiếu: phần nào của tuần 3 điền vào mục nào của mẫu

| Phần của tuần 3 | Mục trong mẫu | Ghi chú |
| --- | --- | --- |
| Thư viện mới (oauth2 resource server, mail, react-router, Mailpit) | 5.1 Môi trường và công cụ | Bổ sung vào Bảng 5.1 |
| Package `auth`, `user`, `skill`, `common`; thư mục frontend | 5.2 Tổ chức mã nguồn | Thay đoạn tuần 2, có link GitHub |
| Đăng ký, xác thực email, JWT, hồ sơ, kỹ năng, S01–S04 | 5.3 Cài đặt các chức năng chính | Mục mới |
| Biến môi trường `.env` | 5.4 Triển khai | Mới có môi trường dev, chưa deploy |
| `kiem_thu/README.md` | 6.1, 6.2 | Thêm kiểm thử tích hợp backend |
| D8, D2, giới hạn kiểm thử | 7.2 Hạn chế | Ghi dần, viết lại ở bản cuối kỳ |
| Lệnh chạy | Phụ lục | Bản nháp |

---

# CHƯƠNG 5 – MỤC 5.1 đến 5.4

## 5.1. Môi trường và công cụ phát triển

Bổ sung vào Bảng 5.1 các dòng sau:

| Hạng mục | Công cụ / phiên bản |
| --- | --- |
| Xác thực | Spring Security 7, `spring-boot-starter-security-oauth2-resource-server` (ký và kiểm tra JWT HS256) |
| Gửi thư | `spring-boot-starter-mail`; khi phát triển dùng Mailpit v1.31.6 (Docker) để bắt thư, không gửi ra ngoài |
| Điều hướng frontend | React Router 8.5 |
| Kiểm thử | JUnit 5, Spring Boot Test, MockMvc, Spring Security Test; CSDL kiểm thử riêng `skillexchange_test` |

## 5.2. Tổ chức mã nguồn

Mã nguồn: https://github.com/world-ender1/skill-exchange

*Bảng 5.2: Tổ chức mã nguồn sau tuần 3*

| Thư mục / package | Vai trò |
| --- | --- |
| `backend/.../auth` | Đăng ký, xác thực email, gửi lại link, đăng nhập, tạo admin đầu tiên, bộ lọc trạng thái tài khoản |
| `backend/.../user` | Hồ sơ của tôi, lịch rảnh, kỹ năng dạy / muốn học |
| `backend/.../skill` | Danh mục kỹ năng (chỉ đọc) |
| `backend/.../common` | Cấu hình bảo mật, xử lý lỗi Problem Details, bean `Clock` |
| `backend/src/main/resources/db/migration` | `V1__init.sql` (lược đồ), `V2__seed_skills.sql` (6 nhóm, 26 kỹ năng, 9 cặp liên quan) |
| `frontend/src/api`, `auth`, `components`, `pages` | Gọi API, lưu vé, thành phần dùng chung, 4 màn hình S01–S04 |

Mỗi package backend chia tầng Controller → Service → Repository → Entity; Controller chỉ nhận và trả DTO.

## 5.3. Cài đặt các chức năng chính

**Đăng ký và xác thực email (FR-01).** Email được chuẩn hóa (bỏ khoảng trắng, chữ thường) rồi so khớp chính xác với danh sách đuôi email trường trong cấu hình. Mật khẩu băm BCrypt. Tài khoản mới ở trạng thái `PENDING_VERIFICATION`; hệ thống sinh mã ngẫu nhiên 32 byte, **chỉ lưu bản băm SHA-256** trong `user_tokens`, và gửi link `/verify-email?token=...` sau khi transaction commit (để không gửi thư cho tài khoản bị rollback). Link hạn 24 giờ, dùng một lần; gửi lại cách nhau ít nhất 60 giây và luôn trả cùng một kết quả để không lộ email nào đã đăng ký.

**Đăng nhập và phân quyền (FR-01, NFR-01).** Đăng nhập đúng trả JWT HS256 hạn 24 giờ chứa `sub` (id) và `role`. Mỗi request, sau khi kiểm chữ ký và hạn của vé, một bộ lọc đọc lại trạng thái tài khoản trong DB: tài khoản bị xóa trả 401, bị khóa trả 403 **ngay**, không phải chờ vé hết hạn. Mọi lỗi, kể cả 401/403 từ Spring Security, trả dạng Problem Details (RFC 9457).

**Thời gian kiểm thử được.** Mọi mốc thời gian (hạn link, giới hạn 60 giây, `iat`/`exp` của JWT) lấy từ một bean `Clock` dùng chung. Khi test, đồng hồ giả được tua tới để kiểm hạn 24 giờ mà không phải chờ.

**Hồ sơ, lịch rảnh, kỹ năng (FR-02, FR-03).** `GET/PUT /api/me` sửa họ tên, giới thiệu, hình thức học, liên hệ riêng tư; không sửa được email, vai trò, trạng thái. Lịch rảnh (7 ngày × 3 buổi) và hai danh sách kỹ năng dùng `PUT` thay cả danh sách: trong một transaction xóa danh sách cũ rồi chèn mới. Một kỹ năng không được vừa dạy vừa muốn học; trùng lặp trong cùng request trả 400 kèm vị trí phần tử lỗi (`[i].skillId`). Người dùng chỉ thao tác được dữ liệu của chính mình vì id luôn lấy từ vé, không nhận từ request.

**Giao diện S01–S04.** React gọi API qua proxy của Vite (không cần CORS), lưu vé trong `localStorage`, tự gắn `Authorization` và về trang đăng nhập khi nhận 401. Lỗi từng ô hiện ngay dưới ô nhập, lấy từ `errors[]` của Problem Details.

**[CẦN BỔ SUNG – Hình 5.1–5.4: ảnh chụp S01–S04, không chứa dữ liệu cá nhân thật]**

## 5.4. Triển khai

Hiện chạy trên máy phát triển; triển khai cloud ở tuần 9. Biến môi trường bắt buộc (file `.env` ở thư mục gốc, mẫu `.env.example`, không ghi giá trị thật vào báo cáo):

*Bảng 5.3: Biến môi trường*

| Biến | Ý nghĩa |
| --- | --- |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT` | Kết nối PostgreSQL |
| `JWT_SECRET` | Khóa ký JWT, tối thiểu 32 byte |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Máy chủ gửi thư (dev: Mailpit) |
| `FRONTEND_URL` | Địa chỉ frontend dùng trong link xác thực |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Tài khoản quản trị tạo lúc khởi động lần đầu; thiếu hoặc sai thì ứng dụng dừng |

---

# CHƯƠNG 6 – MỤC 6.1, 6.2

## 6.1. Kế hoạch kiểm thử (bổ sung)

- **Kiểm thử tích hợp backend (tuần 3, đã làm):** 93 ca viết trước từ tài liệu (`docs/tuan_3/kiem_thu/ca_kiem_thu.md`), chia ba nhóm: hợp lệ phải được nhận (40), sai phải bị chặn (35), bảo mật và phân quyền (18). Test gọi API thật trên CSDL kiểm thử riêng; thư và đồng hồ được giả lập. Code viết theo TDD: test chạy đỏ trước, rồi mới code.
- **Kiểm tra chất lượng bộ test:** cố ý làm hỏng 6 quy tắc quan trọng, xác nhận test đỏ.
- **Giao diện:** kiểm thủ công 3 ca (TC04, TC31, TC74).

## 6.2. Kết quả kiểm thử (bổ sung)

Backend đạt **139/139 test** (ngày 01/11/2026); 88/88 ca tự động tuần 3 đều có test, 3 ca thủ công, 2 ca thuộc tuần 4. Kiểm thử lược đồ tuần 2 chạy lại vẫn đạt 73/73.

*Bảng 6.2: Một số ca kiểm thử tiêu biểu tuần 3*

| Mã TC | Yêu cầu | Mô tả / đầu vào | Kết quả mong đợi | Kết quả |
| --- | --- | --- | --- | --- |
| TC32 | FR-01 | Đăng ký bằng email Gmail | 400, lỗi ở trường `email` | Đạt |
| TC05 | FR-01 | Bấm link xác thực trong thư | 204, tài khoản `ACTIVE` | Đạt |
| TC60 | NFR-01 | Kiểm mật khẩu lưu trong DB | Là bản băm BCrypt, không phải mật khẩu gốc | Đạt |
| TC64 | D14 | Gọi API bằng vé đã hết hạn | 401 Problem Details | Đạt |
| TC69 | D13 | Tài khoản bị khóa sau khi đã đăng nhập | 403 ngay ở request kế tiếp | Đạt |
| TC67 | NFR-01 | A sửa danh sách kỹ năng | Chỉ dữ liệu của A thay đổi | Đạt |
| TC31 | FR-01–03 | Trọn luồng S01 → S04 trên trình duyệt, tải lại trang | Vẫn đăng nhập, dữ liệu còn | **[CẦN BỔ SUNG]** |

*Bảng 6.3: Làm hỏng code để kiểm tra bộ test*

| Quy tắc bị làm hỏng | Test bắt được |
| --- | --- |
| Nhận mọi đuôi email | TC32, TC33 |
| Lưu mật khẩu không băm | TC60, TC77, TC85 |
| Bỏ chặn tài khoản bị khóa | TC69 |
| Link xác thực dùng lại được | Lần đầu **không** test nào đỏ; đã thêm unit test `UserTokenTest` |
| Bỏ kiểm hạn JWT | TC64 |
| `/api/me` luôn dùng một id cố định | TC67 |

Phát hiện đáng chú ý: điều kiện "link chưa dùng" không kiểm được qua API vì sau lần bấm đầu tài khoản đã `ACTIVE` và bị chặn ở lớp thứ hai. Test tích hợp vẫn xanh dù điều kiện bị xóa, nên đã bổ sung unit test cho riêng điều kiện này, vì nó sẽ cần cho chức năng đặt lại mật khẩu.

**[CẦN BỔ SUNG – Hình 6.2: ảnh chụp kết quả `mvnw test`]**

---

# CHƯƠNG 7 – MỤC 7.2. Hạn chế (ghi dần)

- Đăng ký lại email chưa xác thực ghi đè mật khẩu (D8): ai đăng ký sau cùng trước khi link được bấm thì mật khẩu của người đó có hiệu lực. Chưa có quên mật khẩu (D2, FR-23 "Nên có") nên chủ email không tự đặt lại được.
- JWT chưa có refresh token và chưa thu hồi được; đăng xuất chỉ xóa vé ở trình duyệt. Tài khoản bị khóa vẫn bị chặn ngay nhờ kiểm trạng thái mỗi request.
- Vé lưu trong `localStorage` nên có rủi ro nếu trang bị XSS.
- Frontend chưa có test tự động.

---

# PHỤ LỤC – Hướng dẫn chạy (bản nháp)

```powershell
# Yêu cầu: Java 21, Node.js 24 (bản dùng khi phát triển), Docker Desktop
copy .env.example .env          # điền giá trị
docker compose up -d            # PostgreSQL + Mailpit (http://localhost:8025)
cd backend; .\mvnw.cmd spring-boot:run      # http://localhost:8080
cd frontend; npm install; npm run dev       # http://localhost:5173
```
