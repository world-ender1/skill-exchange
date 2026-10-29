# Ca kiểm thử tuần 3 – Tài khoản, hồ sơ, kỹ năng

Mục đích: liệt kê trước các ca kiểm thử cho chức năng tuần 3, viết **chỉ từ tài liệu**, không dựa vào code. Code tuần 3 phải làm các ca này pass. Ca nào thấy sai thì sửa tài liệu trước rồi mới sửa ca, không sửa ca cho khớp code.

*Cập nhật 2026-10-29:* đồng bộ với các quyết định D25–D30 và các quyết định đã sửa sau rà soát (`docs/ra_soat/Bao_cao_ra_soat.md`, R01–R17). TC34, TC35 đổi nghĩa (đăng ký lại email chưa xác thực nay là hợp lệ, xem TC77). TC48 chuyển sang nhóm A. Thêm TC77–TC90.

## Nguồn

Khi các nguồn khác nhau, nguồn mới hơn đúng: quyết định tuần 3 > thiết kế tuần 2 > spec tuần 1.

| Ký hiệu | Tài liệu |
| --- | --- |
| FR-01, FR-02, FR-03, NFR-01, NFR-02 | `docs/tuan_1/Phan_tich_yeu_cau_Skill_Exchange_Tuan_1.md`, mục 3 và 7 |
| Spec 4.6 | Spec tuần 1, mục 4.6: thang trình độ 1–4, 21 khung giờ, hình thức học. Mục này được FR-02 và FR-03 dẫn tới |
| TK 3.1, TK 3.3 | `docs/tuan_2/Tuan_2_Thiet_ke.md`: quy ước dữ liệu, mô tả bảng `users`, `user_tokens`, `skills`, `user_*` |
| TK 4.1–4.4 | Thiết kế tuần 2: quy ước API, API `auth`, `user`, `skill` |
| G5–G9 | Thiết kế tuần 2, mục 8 |
| D1–D30 | `docs/tuan_3/Quyet_dinh_tuan_3.md` |

**Loại:**
- **Tự động**: test JUnit chạy bằng `.\mvnw.cmd test`.
- **Thủ công**: kiểm tra bằng `curl`, psql, Mailpit (`http://localhost:8025`) hoặc giao diện.

**Quy ước:**
- "Có token" nghĩa là gửi `Authorization: Bearer <JWT>` hợp lệ của một tài khoản `ACTIVE`, trừ khi ca ghi khác.
- Email trường mẫu: `@student.example.edu.vn`. Đuôi thật lấy từ cấu hình (G6, D9).
- Mọi lỗi phải trả về dạng Problem Details (TK 4.1, D24).
- "Chờ hơn 60 giây" trong test tự động nghĩa là chỉnh đồng hồ giả lập, không ngủ thật.

## Tổng quan

| Nhóm | Số ca | Ý nghĩa |
| --- | --- | --- |
| A. Hợp lệ phải được nhận | 38 | Đi trọn luồng đăng ký → xác thực → đăng nhập → hồ sơ → lịch rảnh → kỹ năng, kể cả các giá trị biên. Chứng minh hệ thống không chặn nhầm. |
| B. Sai phải bị chặn | 34 | Mỗi quy tắc dữ liệu có ít nhất một ca vi phạm. Phải bị từ chối và **không đổi dữ liệu**. |
| C. Bảo mật và phân quyền | 18 | Băm mật khẩu, băm token, xác thực JWT, quyền sở hữu dữ liệu, trạng thái tài khoản, không lộ thông tin. 2 ca dành cho tuần 4. |
| **Tổng** | **90** | |

---

## A. Hợp lệ phải được nhận

| Mã | Nguồn | Đầu vào | Kết quả mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC01 | FR-01, TK 4.2, D25, D29 | `POST /api/auth/register` với `{email: "an@student.example.edu.vn", password: "matkhau123", fullName: "Nguyễn An"}` | 201, body có `id`, `email`, `status = PENDING_VERIFICATION`. DB có 1 dòng `users`: `role = STUDENT`, `status = PENDING_VERIFICATION`, `email_verified_at` NULL | Tự động |
| TC02 | G6, D9 | Đăng ký với email `"  An@Student.Example.EDU.vn "` | 201. Email lưu trong DB là `an@student.example.edu.vn` (đã bỏ khoảng trắng, đổi về chữ thường) | Tự động |
| TC03 | FR-01, TK 4.2, D5 | Đăng ký như TC01 | Gửi đúng 1 email tới địa chỉ đã đăng ký; nội dung chứa link dạng `${FRONTEND_URL}/verify-email?token=...` | Tự động |
| TC04 | FR-01, TK 4.2, D11 | Đăng ký trên máy dev, mở Mailpit | Thấy thư xác thực. Bấm link thì mở trang S02 và báo xác thực thành công | Thủ công |
| TC05 | FR-01, TK 4.2, D26, D29 | `POST /api/auth/verify-email` với token lấy từ email | 204. User chuyển `ACTIVE`, `email_verified_at` khác NULL; dòng `user_tokens` có `used_at` khác NULL | Tự động |
| TC06 | G8, D7 | Verify khi đồng hồ ở 23 giờ 59 phút sau lúc tạo token | 204, user chuyển `ACTIVE` | Tự động |
| TC07 | TK 4.2, G8, D7 | Tài khoản `PENDING_VERIFICATION`, gọi `POST /api/auth/resend-verification` với `{email}` sau hơn 60 giây kể từ token trước | 204. Có email mới; token mới verify được | Tự động |
| TC08 | FR-01, TK 4.2, D29 | `POST /api/auth/login` với email và mật khẩu đúng của tài khoản `ACTIVE` | 200, body có `accessToken` | Tự động |
| TC09 | G5, D14 | Giải mã `accessToken` của TC08 | Có `sub` = id người dùng, `role = STUDENT`; `exp - iat` = 86 400 giây (24 giờ) | Tự động |
| TC10 | G9, D12 | Đăng nhập bằng tài khoản `PENDING_VERIFICATION`, mật khẩu đúng | 200, có `accessToken` | Tự động |
| TC11 | D9 | Đăng nhập với email viết hoa khác lúc đăng ký (`AN@student...`) | 200 | Tự động |
| TC12 | TK 4.4 | `GET /api/skill-categories`, có token | 200, trả về danh sách nhóm kỹ năng của danh mục mẫu | Tự động |
| TC13 | TK 4.4, TK 3.3 | Ẩn một kỹ năng (`is_active = false`) rồi gọi `GET /api/skills` | 200. Kỹ năng bị ẩn không có trong danh sách; các kỹ năng còn hoạt động đều có | Tự động |
| TC14 | TK 4.4 | `GET /api/skills?categoryId=<id nhóm Lập trình>` | Chỉ trả kỹ năng thuộc nhóm đó | Tự động |
| TC15 | TK 4.4 | `GET /api/skills?q=java` | Có "Java", tìm không phân biệt hoa thường | Tự động |
| TC16 | TK 4.3, NFR-02, D29 | `GET /api/me`, có token | 200. Trả hồ sơ của chính người gọi, **có** `email`, `phone`, `contactInfo`, `role`, `status` | Tự động |
| TC17 | FR-02, TK 4.3, D29 | `PUT /api/me` với `fullName`, `bio`, `learningMode: "ONLINE"`, `phone: "0901 234 567"`, `contactInfo: "zalo: an"` | 200, body là hồ sơ vừa lưu. Gọi `GET /api/me` thấy đúng các giá trị vừa gửi | Tự động |
| TC18 | FR-02, Spec 4.6 | `PUT /api/me` lần lượt với `learningMode` = `ONLINE`, `OFFLINE`, `ANY` | Cả 3 lần đều 200 và được lưu đúng | Tự động |
| TC19 | Spec 4.6, TK 3.1, D15 | Đang có `learningMode = ONLINE`, `bio = "abc"`. Gửi `PUT /api/me` với `fullName: "Nguyễn An"`, `learningMode: null`, `phone: ""`, không gửi khóa `bio` | 200. DB: `learning_mode`, `phone`, `bio` đều NULL (nghĩa là "chưa khai báo") | Tự động |
| TC20 | G9, D12 | Tài khoản `PENDING_VERIFICATION` gọi `PUT /api/me` hợp lệ | 200, dữ liệu được lưu | Tự động |
| TC21 | FR-02, TK 4.3 | `PUT /api/me/availability` với `[{dayOfWeek: 2, period: "EVENING"}, {dayOfWeek: 5, period: "EVENING"}]` | 200. `GET /api/me/availability` và DB đều có đúng 2 khung của người gọi | Tự động |
| TC22 | TK 4.3 | Sau TC21, gửi `PUT /api/me/availability` với `[{dayOfWeek: 1, period: "MORNING"}]` | 200. Chỉ còn 1 khung: danh sách được **thay** chứ không cộng thêm | Tự động |
| TC23 | TK 4.3, TK 3.1 | `PUT /api/me/availability` với `[]` | 200. Không còn khung nào của người gọi (nghĩa là "chưa khai báo lịch") | Tự động |
| TC24 | Spec 4.6, D21 | `PUT /api/me/availability` đủ 21 khung (7 ngày × 3 buổi) | 200, DB có 21 dòng | Tự động |
| TC25 | FR-03, Spec 4.6 | `PUT /api/me/teach-skills` với `[{skillId: Java, level: 1}, {skillId: Python, level: 4}]` | 200. Lưu đúng hai trình độ biên 1 và 4 | Tự động |
| TC26 | FR-03, Spec 4.6 | `PUT /api/me/learn-skills` với `[{skillId: Tiếng Anh}]`, không có `targetLevel` | 200. `target_level` là NULL | Tự động |
| TC27 | FR-03, Spec 4.6 | `PUT /api/me/learn-skills` với `targetLevel` = 1 và 4 | 200, lưu đúng | Tự động |
| TC28 | TK 4.3, D30 | Đang dạy Java, gửi `PUT /api/me/teach-skills` với `[{skillId: Python, level: 3}]` | 200. Chỉ còn Python; Java bị bỏ khỏi danh sách dạy | Tự động |
| TC29 | TK 4.3 | `PUT /api/me/teach-skills` và `PUT /api/me/learn-skills` với `[]` | 200. Cả hai danh sách của người gọi đều rỗng | Tự động |
| TC30 | TK 3.3, D20 | Người dùng đang dạy Java, admin ẩn Java, người dùng `PUT` lại danh sách dạy vẫn có Java | 200. Java vẫn được giữ | Tự động |
| TC31 | FR-01, FR-02, FR-03 | Trên giao diện: đăng ký (S01, có ô họ tên) → bấm link trong Mailpit (S02) → đăng nhập → điền hồ sơ và tick lịch (S03) → thêm kỹ năng dạy và học (S04) → tải lại trang (F5) | Mọi bước thành công. Sau khi tải lại vẫn còn đăng nhập, dữ liệu vẫn còn và hiện đúng trên S03, S04 | Thủ công |
| TC48 | TK 4.3, D15 | `PUT /api/me` hợp lệ nhưng có thêm `"email": "khac@student.example.edu.vn"` | 200. Các trường hợp lệ được lưu; email trong DB không đổi (trường lạ bị bỏ qua) | Tự động |
| TC77 | D8 | Đăng ký `an@...` với mật khẩu `matkhau111`. Chờ hơn 60 giây. Đăng ký lại cùng email (chưa xác thực) với mật khẩu `matkhau222`, `fullName: "An Thật"` | 201. Vẫn chỉ 1 user, `full_name = "An Thật"`. Đăng nhập bằng `matkhau111` → 401, bằng `matkhau222` → 200. Token của thư đầu verify → 400; token của thư mới verify → 204 | Tự động |
| TC78 | D7, D8 | Đăng ký, rồi đăng ký lại cùng email sau 30 giây với mật khẩu khác | 201. Mật khẩu được ghi đè; **không** có email mới; token của thư đầu vẫn verify được | Tự động |
| TC79 | TK 4.3, D28 | Sau TC21 và TC25, gọi `GET /api/me/availability`, `GET /api/me/teach-skills`, `GET /api/me/learn-skills`. Sau đó admin ẩn Java (người dùng đang dạy Java) và gọi lại `GET /api/me/teach-skills` | 200, đúng dữ liệu đã lưu, có `skillName`, `categoryName`. Java vẫn có trong danh sách với `isActive = false` | Tự động |
| TC80 | D27 | Giả lập máy chủ mail lỗi khi đăng ký | 201, user được tạo ở `PENDING_VERIFICATION`, có log lỗi. Sau khi mail hoạt động lại, `resend-verification` (sau 60 giây) gửi được thư | Tự động |
| TC81 | D4 | Khởi động ứng dụng với `ADMIN_EMAIL = " Admin@Student.Example.edu.vn "`; khởi động lại lần hai | Có đúng 1 tài khoản `admin@student.example.edu.vn` với `role = ADMIN`, `status = ACTIVE`, `email_verified_at` khác NULL; lần hai không tạo thêm | Tự động |
| TC82 | D19 | Đang muốn học Java. Gửi `PUT /api/me/learn-skills` không có Java, rồi `PUT /api/me/teach-skills` có Java | Cả hai đều 200; Java chỉ nằm trong danh sách dạy | Tự động |

---

## B. Sai phải bị chặn

Mọi ca trong nhóm này còn phải kiểm thêm: **dữ liệu trong DB không thay đổi**.

| Mã | Nguồn | Đầu vào | Kết quả mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC32 | FR-01, G6 | Đăng ký với `an@gmail.com` | 400. Không tạo user | Tự động |
| TC33 | G6, D9 | Đăng ký với đuôi là tên miền con chưa có trong cấu hình (`an@abc.student.example.edu.vn`) | 400 | Tự động |
| TC34 | TK 4.1, D8 | Đăng ký lại email của tài khoản đã `ACTIVE` (sau TC05) | 409. Vẫn chỉ có 1 user, mật khẩu cũ không đổi | Tự động |
| TC35 | TK 4.1, D8, D9 | Đăng ký lại email của TC34 nhưng viết hoa khác (`An@Student...`) | 409 | Tự động |
| TC36 | TK 4.1, D24, D25 | Đăng ký thiếu `fullName`, và `email` sai định dạng (`"an"`) | 400. Problem Details có `errors` chứa hai trường `fullName` và `email` | Tự động |
| TC37 | NFR-01, D10 | Mật khẩu dài 7 ký tự; mật khẩu dài 73 ký tự | Cả hai trả 400, `errors[].field = password`. Ở biên: 8 và 72 ký tự ASCII được nhận | Tự động |
| TC38 | TK 3.3, D16, D25 | `fullName` dài 101 ký tự (khi đăng ký và khi `PUT /api/me`) | 400 | Tự động |
| TC39 | TK 4.2, D26 | `verify-email` với một token ngẫu nhiên không tồn tại | 400. Không user nào đổi trạng thái | Tự động |
| TC40 | TK 3.3, D26 | Gọi `verify-email` lần hai với token đã dùng ở TC05 | 400, cùng `detail` với TC39 | Tự động |
| TC41 | G8, D7, D26 | Verify khi đồng hồ ở 24 giờ 1 giây sau lúc tạo token | 400. User vẫn `PENDING_VERIFICATION` | Tự động |
| TC42 | G8, D7 | Gửi lại link (như TC07), sau đó verify bằng **token cũ** | 400. Token mới vẫn dùng được | Tự động |
| TC43 | D7 | Đăng ký, chờ hơn 60 giây, rồi gọi `resend-verification` hai lần cách nhau 30 giây | Cả hai lần đều trả 204, nhưng chỉ lần đầu có email mới | Tự động |
| TC44 | FR-01, TK 4.1, D13 | Đăng nhập đúng email, sai mật khẩu | 401, không có `accessToken` | Tự động |
| TC45 | TK 3.3, D16 | `PUT /api/me` với `fullName: ""`, rồi `fullName: "   "` | 400, `errors[].field = fullName` | Tự động |
| TC46 | Spec 4.6, TK 3.3 | `PUT /api/me` với `learningMode: "BOTH"` | 400 | Tự động |
| TC47 | D16 | `PUT /api/me` với `bio` dài 1001 ký tự; `contactInfo` dài 501 ký tự; `phone: "abc123"`; `phone` dài 21 ký tự | Mỗi trường hợp đều 400, có `errors[].field` tương ứng | Tự động |
| TC49 | Spec 4.6, TK 3.3 | `PUT /api/me/availability` với `dayOfWeek` = 0, rồi `dayOfWeek` = 8 | 400. Lịch cũ còn nguyên | Tự động |
| TC50 | Spec 4.6, TK 3.3 | `PUT /api/me/availability` với `period: "NIGHT"` | 400 | Tự động |
| TC51 | D21 | `PUT /api/me/availability` có hai lần cùng `{dayOfWeek: 2, period: "EVENING"}` | 400 | Tự động |
| TC52 | FR-03, Spec 4.6 | `PUT /api/me/teach-skills` với `level` = 0; `level` = 5; không có `level` | Cả ba trả 400 (kỹ năng dạy bắt buộc có trình độ) | Tự động |
| TC53 | FR-03, Spec 4.6 | `PUT /api/me/learn-skills` với `targetLevel` = 0, rồi `targetLevel` = 5 | 400 | Tự động |
| TC54 | FR-03 | `PUT /api/me/teach-skills` với `skillId` không có trong danh mục | 400 | Tự động |
| TC55 | FR-03, TK 4.4, D20 | Kỹ năng đang ẩn, người dùng **chưa có** nó, gửi `PUT /api/me/learn-skills` chứa kỹ năng đó | 400 | Tự động |
| TC56 | D21 | `PUT /api/me/teach-skills` có cùng `skillId` hai lần | 400 | Tự động |
| TC57 | D21 | `PUT /api/me/learn-skills` với 11 kỹ năng | 400. Ở biên: 10 kỹ năng được nhận | Tự động |
| TC58 | D19 | Đang muốn học Java, gửi `PUT /api/me/teach-skills` có Java | 400 | Tự động |
| TC59 | TK 4.1, D24 | Gửi body JSON hỏng (`{"fullName":`) tới `PUT /api/me` | 400 dạng Problem Details, không trả 500 | Tự động |
| TC83 | D19 | Đang dạy Java, gửi `PUT /api/me/learn-skills` có Java | 400 | Tự động |
| TC84 | D21 | `PUT /api/me/teach-skills` với 11 kỹ năng | 400. Ở biên: 10 kỹ năng được nhận | Tự động |
| TC85 | D10 | Đăng ký với mật khẩu gồm 25 chữ "ệ" (25 ký tự, 75 byte UTF-8) | 400, `errors[].field = password`, không trả 500. Ở biên: 24 chữ "ệ" (72 byte) được nhận | Tự động |
| TC86 | D9 | Đăng ký với email dài 256 ký tự (đúng đuôi trường) | 400 | Tự động |
| TC87 | D26 | Tạo một token `purpose = RESET_PASSWORD` cho tài khoản `PENDING_VERIFICATION`, dùng nó gọi `verify-email` | 400. User vẫn `PENDING_VERIFICATION` | Tự động |
| TC88 | D26 | Tài khoản `PENDING_VERIFICATION` có token hợp lệ, admin chuyển sang `LOCKED`, rồi verify bằng token đó | 400. User vẫn `LOCKED` | Tự động |
| TC89 | D7 | Đăng ký, rồi gọi `resend-verification` sau 30 giây | 204, không có email mới (thư đăng ký cũng tính vào giới hạn 60 giây) | Tự động |

---

## C. Bảo mật và phân quyền

| Mã | Nguồn | Đầu vào | Kết quả mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC60 | NFR-01, TK 3.3 | Sau TC01, đọc `users.password_hash` | Khác mật khẩu gốc, có dạng BCrypt (`$2a$`/`$2b$`), so khớp với mật khẩu gốc thì đúng | Tự động |
| TC61 | NFR-01, TK 3.3, D6 | Sau TC03, so token trong email với `user_tokens.token_hash` | DB không chứa token gốc; `token_hash` bằng SHA-256 của token | Tự động |
| TC62 | TK 4.1, D24 | Gọi `GET /api/me`, `PUT /api/me/availability`, `GET /api/skills`, `GET /api/skill-categories` **không có token** | Tất cả trả 401, `Content-Type: application/problem+json` | Tự động |
| TC63 | TK 4.1, NFR-01 | Gọi `GET /api/me` với token sai chữ ký, trong đó có token tự sửa `role` thành `ADMIN` | 401 | Tự động |
| TC64 | G5, D14 | Gọi `GET /api/me` với token đã quá 24 giờ | 401 | Tự động |
| TC65 | TK 4.2 | Gọi 4 API `/api/auth/*` không có token | Không trả 401. Mỗi API trả mã theo nghiệp vụ của nó | Tự động |
| TC66 | NFR-01 | User A gọi `GET /api/me` và `PUT /api/me` bằng token của A | Chỉ đọc và sửa hồ sơ của A; hồ sơ của B không đổi | Tự động |
| TC67 | NFR-01 | User A `PUT` và `GET` lịch rảnh và hai danh sách kỹ năng | Chỉ thấy và đổi dữ liệu của A; lịch và kỹ năng của B không đổi | Tự động |
| TC68 | G9, D13 | Tài khoản `LOCKED` đăng nhập với mật khẩu đúng | 403, không có `accessToken` | Tự động |
| TC69 | G9, D13 | Đăng nhập lấy token → chuyển tài khoản sang `LOCKED` → gọi `GET /api/me` bằng token đó | 403 ngay, không chờ token hết hạn, `Content-Type: application/problem+json`. Mở khóa lại thì 200 | Tự động |
| TC70 | NFR-01, D13 | Token hợp lệ của một user đã bị xóa khỏi DB | 401 | Tự động |
| TC71 | NFR-01, D13 | So sánh response của "sai mật khẩu" (TC44) và "email không tồn tại" | Cùng là 401 và cùng `detail`, không đoán được email nào đã đăng ký | Tự động |
| TC72 | TK 4.2, D7 | `resend-verification` với email chưa đăng ký, với email của tài khoản đã `ACTIVE`, và với email của tài khoản `LOCKED` | Cả ba trả 204, không gửi email | Tự động |
| TC73 | NFR-01, D24 | Xem response của `GET /api/me`, `POST /api/auth/login` và một lỗi 500 giả lập | Không có `password_hash` hay `token_hash`; không lộ stack trace | Tự động |
| TC74 | FR-01, TK 4.1 | Trên giao diện bấm "Đăng xuất", rồi gõ thẳng `/profile` | Token bị xóa khỏi `localStorage`; bị chuyển về trang đăng nhập | Thủ công |
| TC75 | FR-01, G9, TK 4.3 | **Tuần 4.** Tài khoản `PENDING_VERIFICATION` gọi một API quyền *Active* (ví dụ `GET /api/users/{id}`) | 403 | Tự động (tuần 4) |
| TC76 | NFR-02, TK 4.3 | **Tuần 4.** `GET /api/users/{id}` | Không có `email`, `phone`, `contactInfo` | Tự động (tuần 4) |
| TC90 | D13 | Tài khoản `LOCKED` đăng nhập với mật khẩu **sai** | 401, cùng `detail` với TC44 (không lộ việc email tồn tại và bị khóa) | Tự động |

---

## Những điểm tài liệu chưa quy định rõ (phát hiện khi viết ca)

Cả 6 điểm phát hiện khi viết ca lần đầu đã được chốt vào tài liệu nguồn ngày 2026-10-29.

| # | Chỗ chưa rõ | Ca liên quan | Đã chốt ở |
| --- | --- | --- | --- |
| ⚠1 | Dùng lại token đã dùng trả 400 hay 409 | TC40 | D26: mọi token không dùng được trả 400, cùng `detail` |
| ⚠2 | Mã thành công và nội dung trả về của `register`, `verify-email`, `resend-verification` | TC01, TC05, TC07 | D29, TK 4.2 |
| ⚠3 | Không có API đọc lại lịch rảnh và kỹ năng | TC21–TC30, TC79 | D28, TK 4.3 |
| ⚠4 | Gửi mail lỗi khi đăng ký | TC80 | D27 |
| ⚠5 | Kiểm tra kỹ năng bị ẩn khi nhận `PUT` | TC30, TC55 | D20, TK 4.3 |
| ⚠6 | Token cấp trước khi bị khóa | TC69 | D13, G9 |
