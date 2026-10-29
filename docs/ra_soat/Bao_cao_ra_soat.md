# BÁO CÁO RÀ SOÁT TOÀN BỘ TÀI LIỆU

Ngày rà: 2026-10-29. Chưa sửa tài liệu nào. Báo cáo này là danh sách vấn đề để chốt trước khi sửa.

## 0. Cách rà và quy tắc áp dụng

- **Tài liệu nguồn được rà:** Proposal, Spec tuần 1, Thiết kế tuần 2, Quyết định tuần 3, `V1__init.sql`, CLAUDE.md, `To_chuc_ket_qua.md`, ca kiểm thử tuần 2 và tuần 3, các ảnh trong `docs/hinh/`.
- **Không rà:** `Noi_dung_bao_cao_*.md`, `.docx` (file dẫn xuất, sẽ sinh lại từ nguồn khi viết báo cáo), `De_xuat_chinh_sua.md`, `de_xuat_sua_spec.md` (lịch sử).
- **Thứ bậc khi mâu thuẫn:** tài liệu giai đoạn sau thắng: Quyết định tuần 3 > Thiết kế > Spec > Proposal. `V1__init.sql` là hiện thực của Thiết kế; SQL lệch Thiết kế thì SQL sai.
  - Ngoại lệ: bản mới làm mất một yêu cầu "Bắt buộc" thì người dùng quyết định.
- **Cách sửa:** sửa ngược (phương án A). Khi chốt một điểm, sửa mọi tài liệu cũ nhắc tới điểm đó trong cùng commit.
- **Phương pháp:** 5 mảng rà song song. Mỗi mảng đối chiếu chéo mọi tài liệu theo 4 trục: nhất quán, đầy đủ theo checklist, kịch bản trên giấy, truy vết. Mọi trích dẫn quan trọng đã được kiểm lại.

**Viết tắt:**

| Viết tắt | Tài liệu |
| --- | --- |
| Spec | `docs/tuan_1/Phan_tich_yeu_cau_Skill_Exchange_Tuan_1.md` |
| TK | `docs/tuan_2/Tuan_2_Thiet_ke.md` |
| QĐ | `docs/tuan_3/Quyet_dinh_tuan_3.md` |
| Prop | `docs/Skill_Exchange_Proposal_9_Tuan.md` |
| TC3 | `docs/tuan_3/kiem_thu/ca_kiem_thu.md` |
| TC2 | `docs/tuan_2/kiem_thu/test_constraints.sql` |
| TCKQ | `docs/To_chuc_ket_qua.md` |

Số sau dấu `:` là số dòng.

**Mức độ:**
- 🔴 Nghiêm trọng: sai kết quả, lỗ hổng, chặn code.
- 🟠 Cao: phải chốt trước khi code tuần đó.
- 🟡 Trung bình.
- 🟢 Thấp: câu chữ, đồng bộ.

**Cột "Chốt":**
- **Bạn**: cần bạn chọn phương án.
- **Duyệt**: chỉ cần đồng ý với đề xuất.

## 1. Tổng quan

| Nhóm | Số mục | 🔴 | 🟠 | 🟡 | 🟢 |
| --- | --- | --- | --- | --- | --- |
| Tuần 3: tài khoản, hồ sơ, kỹ năng | 17 | 1 | 4 | 6 | 6 |
| Tuần 4: matching, tìm kiếm, hồ sơ công khai | 6 | 0 | 4 | 2 | 0 |
| Trước tuần 5: thực nghiệm | 9 | 0 | 5 | 3 | 1 |
| Tuần 6: yêu cầu, phiên | 7 | 0 | 3 | 3 | 1 |
| Tuần 7: đánh giá, quản trị | 7 | 0 | 2 | 4 | 1 |
| Tuần 8–9 | 4 | 0 | 0 | 2 | 2 |
| Đồng bộ tài liệu, không cần quyết định | 6 | 0 | 0 | 0 | 6 |
| **Tổng** | **56** | **1** | **18** | **20** | **17** |

**Điểm tốt (đã kiểm, không có lỗi):**
- `V1__init.sql` khớp Thiết kế §3.3 từng cột ở cả 12 bảng, chỉ mục §3.4 khớp 7/7. Tuần 3 không cần migration sửa lược đồ.
- Ví dụ tính tay ở Phụ lục A.3 đúng hoàn toàn (D = 0.94, NDCG@5 = 0.9639, Precision@5 = 0.4). JSON mẫu `/matches` đúng (√(0.94 × 0.65) ≈ 0.78). Tổng trọng số của cả 4 cấu hình độ nhạy đều bằng 1.
- Mọi chức năng của Proposal §3 đều có FR cùng mức ưu tiên. Không bản sau nào hạ mức một chức năng Bắt buộc.
- Hai ảnh sơ đồ trạng thái, ảnh kiến trúc, ảnh luồng điều hướng khớp mermaid trong TK.
- Phiên bản công nghệ khớp giữa tài liệu và `pom.xml`/`package.json`.

---

## 2. Tuần 3: tài khoản, hồ sơ, kỹ năng (đang làm, chốt trước tiên)

### R01 🔴 Đăng ký trước bằng email người khác để chiếm tài khoản đã xác thực. Chốt: **Bạn**

- **Vấn đề:** D8 (QĐ:21) trả 409 khi đăng ký lại một email đang `PENDING_VERIFICATION` và hướng người dùng sang "gửi lại mã". D12 (QĐ:30) cho tài khoản chưa xác thực được đăng nhập. Dự án không có quên hay đổi mật khẩu (D2).
- **Kịch bản:**
  1. X đăng ký bằng `an@truong`, đặt mật khẩu của X.
  2. An thật đăng ký thì nhận 409, nên bấm "gửi lại mã" rồi bấm link.
  3. Tài khoản thành `ACTIVE` nhưng mang mật khẩu của X. An không có cách lấy lại.
- **Hệ quả:** làm yếu mục đích của FR-01 (Bắt buộc), nên thuộc diện bạn tự quyết.
- **Phương án:**
  1. *(đề xuất)* Đăng ký lại một email đang `PENDING_VERIFICATION` thì ghi đè mật khẩu và họ tên, vô hiệu token cũ, gửi link mới, trả 201 như đăng ký mới. Chỉ email đã `ACTIVE` hoặc `LOCKED` mới trả 409.
  2. Đặt mật khẩu ở bước xác thực (trang S02).
  3. Làm FR-23 (quên mật khẩu) ngay tuần 3.
- **Sửa ngược:** D8, G9 (TK:735), thêm ca kiểm thử vào TC3.

### R02 🟠 Mật khẩu "8–72 ký tự" nhưng BCrypt giới hạn 72 byte. Chốt: Duyệt

- **Vấn đề:** D10 (QĐ:23) tính theo ký tự. Chữ có dấu chiếm 2–3 byte UTF-8, nên 72 ký tự có thể vượt 72 byte.
  - Spring Security bản mới ném `IllegalArgumentException` khi mật khẩu dài hơn 72 byte, dẫn tới lỗi 500. Điểm này cần một test nhỏ để xác nhận.
- **Đề xuất:** D10 thành "8–72 ký tự và không quá 72 byte UTF-8", vượt thì trả 400. Thêm ca biên vào TC37.

### R03 🟠 Body của `register` chưa đặc tả; S01 thiếu ô họ tên trong khi `full_name` là `NOT NULL`. Chốt: Duyệt

- **Bằng chứng:**
  - SQL:13 `full_name VARCHAR(100) NOT NULL`.
  - TK:609 S01 "Form email trường, mật khẩu".
  - TK:407 không có body.
  - TC3 TC01 đã tự giả định có `fullName`.
- **Đề xuất:** TK §4.2 ghi body `{email, password, fullName}`, ràng buộc theo D16/D10, email tối đa 255 ký tự. S01 thêm ô "Họ tên".

### R04 🟠 Mã trả về và response của API `auth`, `/me*` chưa chốt. Chốt: Duyệt

- **Bằng chứng:**
  - TK §4.2–4.3 không có cột mã trả về.
  - TC3 ⚠1, ⚠2 (TC3:141–142) mới chỉ là đề xuất.
  - Frontend cần biết `status` để hiện banner "chưa xác thực".
- **Đề xuất (thêm một mục D mới):**

  | API | Kết quả |
  | --- | --- |
  | `register` | 201 `{id, email, status}` |
  | `verify-email` | 204 |
  | `verify-email` với token không dùng được (không tồn tại, đã dùng, hết hạn, sai `purpose`) | 400, cùng một thông báo |
  | `resend-verification` | 204 |
  | `login` | 200 `{accessToken}` |
  | `PUT /me*` | 200, trả lại dữ liệu vừa lưu |
  | `GET /me` | có thêm `role`, `status` |

### R05 🟠 Không có API đọc lại lịch rảnh và kỹ năng của tôi. Chốt: **Bạn**

- **Bằng chứng:**
  - TK:418–422 chỉ có `GET /api/me` và 3 API `PUT`.
  - S03/S04 (TK:611–612) phải hiện dữ liệu đang có.
  - TC3 ⚠3 ghi "đã chốt khi lập kế hoạch tuần 3", nhưng chưa tài liệu nguồn nào ghi.
  - Kỹ năng bị ẩn mà người dùng vẫn giữ (D20) không có trong `GET /api/skills`, nên S04 không tra được tên kỹ năng đó.
- **Phương án:**
  1. *(đề xuất, khớp ⚠3)* 3 endpoint `GET /api/me/availability`, `/teach-skills`, `/learn-skills`, trả `{skillId, skillName, categoryId, isActive, level/targetLevel}`.
  2. Gộp tất cả vào `GET /api/me`.

### R06 🟡 Chuyển trạng thái tài khoản chưa đủ: verify, resend, mở khóa. Chốt: Duyệt

- **Vấn đề:**
  - TK:408 cho `verify-email` chuyển sang `ACTIVE` mà không xét trạng thái hiện tại. Tài khoản `LOCKED` chưa xác thực bấm link sẽ thành `ACTIVE`.
  - Khóa rồi mở khóa một tài khoản `PENDING_VERIFICATION` theo TK:526 (`ACTIVE ↔ LOCKED`) sẽ bỏ qua bước xác thực email.
- **Đề xuất:**
  - Verify chỉ đổi trạng thái khi đang `PENDING_VERIFICATION`, ngược lại trả 400 như token hỏng.
  - Resend chỉ gửi thư khi đang `PENDING_VERIFICATION`.
  - Mở khóa thì về `ACTIVE` nếu `email_verified_at` có giá trị, ngược lại về `PENDING_VERIFICATION`.

### R07 🟡 Đăng nhập tài khoản `LOCKED`: kiểm tra sai thứ tự sẽ lộ email đã tồn tại. Chốt: Duyệt

- **Vấn đề:** D13 (QĐ:31) chưa nói thứ tự kiểm tra. Nếu kiểm `LOCKED` trước mật khẩu thì người ngoài nhập mật khẩu bừa vẫn nhận 403, và biết được email đó có tồn tại.
- **Đề xuất:** kiểm tra mật khẩu trước, chỉ trả 403 khi mật khẩu đúng. Thêm ca "`LOCKED` + sai mật khẩu → 401".

### R08 🟡 `PUT /api/me`: chuỗi rỗng, thiếu khóa, trường lạ; TC19 và TC48 lệch quy tắc. Chốt: Duyệt

- **Bằng chứng:**
  - D15 (QĐ:38) dùng chữ "bỏ trống", có thể hiểu hai nghĩa.
  - TC19 (TC3:58) thiếu `fullName` mà vẫn mong 200.
  - TC48 nằm ở nhóm B ("phải bị từ chối"), nhưng Spring mặc định bỏ qua trường lạ, nên thực tế sẽ trả 200.
- **Đề xuất:**
  - Chuỗi rỗng hoặc toàn khoảng trắng thì lưu NULL; thiếu khóa coi như `null`; trường lạ (`email`) bị bỏ qua, trả 200.
  - Sửa input TC19, chuyển TC48 sang nhóm A.

### R09 🟡 Gửi mail lỗi khi đăng ký thì xử lý ra sao (TC3 ⚠4). Chốt: Duyệt

- **Đề xuất:** lưu user và token trong một transaction, gửi mail sau khi commit. Gửi lỗi thì ghi log và vẫn trả 201; người dùng dùng "gửi lại".

### R10 🟡 S02 ghi "nhập mã", trong khi D5 dùng link; chưa có cấu hình URL frontend. Chốt: Duyệt

- **Bằng chứng:**
  - D5 (QĐ:18) dùng link chứa token 256 bit, không nhập tay được.
  - TK:610 S02 "Nhập mã hoặc mở liên kết"; TK:407, G8 và ảnh h4_1 ghi "mã xác thực".
- **Đề xuất:**
  - Sửa ngược thành "link xác thực".
  - Thêm biến `FRONTEND_URL` để tạo link `…/verify-email?token=`.
  - Ghi rõ: gửi lại sớm hơn 60 giây vẫn trả 204 nhưng không gửi thư.

### R11 🟢 "Vô hiệu token cũ" (D7) làm bằng cột nào. Chốt: Duyệt

- **Vấn đề:** bảng `user_tokens` không có cột `revoked_at`. Dùng `used_at` thì sai nghĩa.
- **Đề xuất:** `DELETE` các token `VERIFY_EMAIL` chưa dùng của người đó, ghi vào D7.

### R12 🟡 Quy ước ánh xạ Entity để `ddl-auto: validate` không lỗi. Chốt: Duyệt

Không có quy ước này, ứng dụng sẽ lỗi ngay khi khởi động hoặc khi INSERT ở tuần 3. Đề xuất ghi thành một mục D mới:

- `SMALLINT` → `Short`. Dùng `Integer` thì validate báo lệch kiểu.
- `TIMESTAMPTZ` → `Instant`.
- `BIGINT` → `Long`.
- `version` → `Integer` + `@Version`.
- Cột có `DEFAULT` trong DB (`role`, `status`, `is_active`, `created_at`): Hibernate INSERT mọi cột, nên giá trị `null` sẽ vi phạm `NOT NULL`. Cần gán giá trị mặc định ở field Java, và `created_at` dùng `@CreationTimestamp`.
- Bảng khóa ghép (`user_*_skills`, `user_availability`): dùng `@IdClass` hoặc `@EmbeddedId`. Với `PUT` thay cả danh sách, xóa bằng câu `DELETE … WHERE user_id` trước rồi mới chèn. Nếu dùng `clear()` rồi thêm lại, Hibernate có thể INSERT trước DELETE và trùng khóa chính.
- Validate không kiểm tra độ dài hay CHECK. DTO phải có `@Size` khớp từng `VARCHAR(n)`, và các trường lý do dùng `@NotBlank`.

### R13 🟢 Email UNIQUE phân biệt hoa thường; admin seed thiếu `email_verified_at`. Chốt: Duyệt

- **Đề xuất:**
  - D4 cũng chuẩn hóa `ADMIN_EMAIL` (trim, lowercase) và gán `email_verified_at = now()`.
  - Kiểm tra trùng tên kỹ năng không phân biệt hoa thường ở service.
  - Index `lower(email)` là tùy chọn, không bắt buộc.

### R14 🟢 Seed V2 (D3): danh sách chưa duyệt; INSERT tra theo tên có thể âm thầm chèn 0 dòng. Chốt: Duyệt

- **Đề xuất:**
  - Duyệt danh sách 6 nhóm, khoảng 25 kỹ năng, 8–10 cặp quan hệ trước khi viết SQL.
  - Lệnh INSERT quan hệ JOIN thêm điều kiện cùng nhóm.
  - Cuối file đếm số dòng và `RAISE EXCEPTION` nếu sai số mong đợi.

### R15 🟢 Cấu hình chưa phản ánh các quyết định. Chốt: Duyệt (làm khi code)

- `.env.example` thiếu `ADMIN_EMAIL`, `ADMIN_PASSWORD` (D4) và `FRONTEND_URL`.
- `docker-compose.yml` chưa có Mailpit (D11).
- CLAUDE.md:40 ghi "409 … trùng" lệch D21 ("trùng trong payload → 400"). Đề xuất ghi rõ: trùng trong cùng payload trả 400, trùng với dữ liệu đã có (email, PENDING) trả 409.

### R16 🟢 Kết quả mong đợi trong TC3 không truy vết được về nguồn. Chốt: Duyệt

- **Ca có kết quả mong đợi không nằm trong tài liệu nguồn nào:**
  - TC72: không gửi email cho tài khoản `ACTIVE`.
  - TC15: tìm kỹ năng không phân biệt hoa thường.
  - TC70: user đã bị xóa thì 401.
  - TC11: chuẩn hóa email khi đăng nhập.
  - TC43: mốc 60 giây tính từ lúc nào.

  Ghi các điểm này vào QĐ.
- **Ca cần thêm:**
  - D4: khởi động lại không tạo trùng admin.
  - D19 chiều ngược lại (đang dạy, gửi vào danh sách học).
  - D21 với danh sách dạy 11 kỹ năng.
  - Mật khẩu có dấu vượt 72 byte (R02).
  - Verify và resend với tài khoản `LOCKED` (R06).
  - Token `RESET_PASSWORD` không dùng được để xác thực email.
  - Email dài hơn 255 ký tự.

### R17 🟢 Lỗi 401/403 từ Spring Security không tự trả Problem Details. Chốt: Duyệt (ghi chú hiện thực)

- **Vấn đề:** TK:380 ghi "được Spring hỗ trợ sẵn", nhưng điều đó chỉ đúng với lỗi trong controller. Lỗi 401/403 sinh ra ở filter của Spring Security thì không.
- **Đề xuất:** phải tự viết `AuthenticationEntryPoint` và `AccessDeniedHandler`.

---

## 3. Tuần 4: matching, tìm kiếm, hồ sơ công khai

### R18 🟠 Tài khoản ADMIN lọt vào API và danh sách gợi ý của sinh viên. Chốt: Duyệt

- **Bằng chứng:**
  - D4 tạo admin với `status = ACTIVE`. TK:401 định nghĩa quyền *Active* chỉ dựa vào `status`.
  - Vì vậy admin gọi được `/matches` và `POST /requests` (TK:467), trái với Spec:344 và TK:532 (Admin ✖).
  - Lọc cứng (Spec:92) không loại admin, và người nhận yêu cầu (TK:493) cũng có thể là admin.
- **Đề xuất:** các API của sinh viên yêu cầu `role = STUDENT`. Matching, tìm kiếm và người nhận yêu cầu chỉ gồm `STUDENT`.

### R19 🟠 Kỹ năng bị ẩn có còn dùng trong matching, tìm kiếm và tạo yêu cầu không. Chốt: **Bạn**

- **Bằng chứng:**
  - TK:221 "ẩn = không hiện trong danh sách chọn mới, vẫn giữ trong dữ liệu cũ".
  - D20 cho người dùng giữ kỹ năng ẩn.
  - Không tài liệu nào nói kỹ năng ẩn có thuộc $T_v$/$W_u$ hay không.
- **Hai mảng rà đề xuất ngược nhau:**
  1. Ẩn chỉ có nghĩa là ngừng cho chọn mới. Kỹ năng người dùng đang có vẫn tham gia matching, tìm kiếm và tạo yêu cầu. Cách này nhất quán với D20 (nếu loại khỏi matching thì việc giữ lại kỹ năng ẩn không còn ý nghĩa).
  2. Loại kỹ năng ẩn khỏi matching và khỏi kiểm tra Sim khi tạo yêu cầu. Cách này hợp với ý "admin ẩn vì kỹ năng không còn phù hợp".
- **Gợi ý:** phương án 1. Lý do: đơn giản, khớp D20, và admin vẫn có thể xóa quan hệ nếu muốn.

### R20 🟠 FR-10 (tìm kiếm, hồ sơ công khai) chưa đặc tả. Chốt: Duyệt

- **Bằng chứng:** TK:423–425 chỉ có đường dẫn, chưa có quy tắc ai xuất hiện, tìm theo gì, trả trường nào.
- **Đề xuất (thêm vào TK §4.3):**
  - **Ai xuất hiện:** chỉ `ACTIVE` + `STUDENT`, khác người gọi. Người không hiển thị được thì `/users/{id}` trả 404.
  - **Tham số tìm:** `skillId` khớp kỹ năng **dạy**, trùng chính xác. `q` là chuỗi con trong họ tên, không phân biệt hoa thường. Có cả hai tham số thì kết hợp AND. Giới hạn 20 kết quả.
  - **Hồ sơ công khai:** `id`, `fullName`, `bio`, `learningMode`, kỹ năng dạy/học kèm trình độ (ghi rõ "tự khai", Prop:107), lịch rảnh. Không có email, phone, contactInfo.
  - **Điểm đánh giá:** thêm ở tuần 7.

### R21 🟠 Response của `/matches` thiếu trường mà màn hình và NFR-09 cần. Chốt: Duyệt

- **Bằng chứng:**
  - Wireframe S05 (TK:652–659) hiện "★ 4.2", "Mới", hình thức học và tổng số kết quả. JSON ở TK:448–458 không có các trường này.
  - Spec:97: khi $W_u$ rỗng thì giao diện chuyển hướng, nhưng API chưa nói trả gì.
- **Đề xuất:**
  - Thêm `learningMode`, `ratingAverage`, `reviewCount`, `total`.
  - Khi $W_u$ rỗng: trả 200 kèm `needsLearnSkills: true`.
  - Chốt giá trị tối đa của `limit` và giá trị mặc định của `type`.
  - Tùy chọn: thêm `hasPendingRequest` để nút "Gửi yêu cầu" không dẫn tới 409.

### R22 🟡 Chưa thiết kế cách module `evaluation` dùng chung lõi matching. Chốt: **Bạn**

- **Rủi ro:** nếu tuần 4 viết matching gắn chặt với JPA thì tuần 5 phải viết lại cho thực nghiệm.
- **Đề xuất:**
  - Lõi `MatchingEngine` nhận một danh sách hồ sơ trong bộ nhớ và bộ trọng số, không biết gì về Repository.
  - `MatchingService` (đọc từ DB) và `EvaluationRunner` (dùng dữ liệu sinh) cùng gọi lõi này.
  - Chốt cách chạy thực nghiệm (`CommandLineRunner` với Spring profile `evaluation`, hoặc test) và các cột của `metrics.csv` (TCKQ:68–74).

### R23 🟡 Spec giao unit test nhóm 3 ở tuần 4–5, nhưng nhóm này có test cho chức năng tuần 6–7. Chốt: Duyệt

- **Bằng chứng:** Spec:384 đặt nhóm 3 ở tuần 4–5. Hai ca Spec:403–404 (đánh giá, xác nhận hoàn thành) lại thuộc FR-06/07, làm ở tuần 6–7.
- **Đề xuất:** chuyển hai ca đó sang nhóm 5.

---

## 4. Trước tuần 5: thiết kế thực nghiệm

Nhóm này quyết định thực nghiệm có chứng minh được điều gì hay không. Kết quả mô phỏng nhanh với dữ liệu phân bố đều, theo đúng tham số của Spec, cho thấy nhiều chỉ số **không phân biệt được** các phương án.

### R24 🟠 Nhãn suy biến: mọi ứng viên cùng nhãn 1 thì NDCG@5 = 1 với mọi phương án. Chốt: **Bạn**

- **Vấn đề:** người học không đặt mục tiêu thì LevelFit luôn bằng 0.5 (Spec:106, 168). Khi đó không ứng viên nào đạt nhãn 2 hay nhãn 0, nên mọi thứ tự xếp hạng đều "lý tưởng".
- **Số liệu:** với 70% kỹ năng có đặt mục tiêu, khoảng 17% người dùng rơi vào trường hợp này. Các ca đó kéo mọi phương án về gần 1.
- **Đề xuất:**
  - Chỉ tính NDCG trên người dùng có ít nhất 2 mức nhãn khác nhau, và báo cáo số người bị loại.
  - Bộ sinh dữ liệu dùng tỷ lệ mục tiêu NULL cố định.

### R25 🟠 Nhãn 2 và nhãn 0 mơ hồ khi người học muốn học nhiều kỹ năng. Chốt: **Bạn**

- **Vấn đề:** nhãn 2 đòi "LevelFit = 1.0" (Spec:196), nhưng LevelFit là trung bình trên mọi kỹ năng.
  - Ví dụ chính của Spec:170: Java trùng chính xác và đạt mục tiêu, MySQL chỉ liên quan, LevelFit = 0.75. Cặp này chỉ được nhãn 1, dù phần Java hoàn hảo.
  - Nhãn 0 (Spec:200) chưa rõ áp dụng cho từng kỹ năng hay cho toàn bộ.
- **Đề xuất:**
  - Nhãn 2: tồn tại một kỹ năng trùng chính xác và đạt mục tiêu.
  - Nhãn 0: Sim lớn nhất là 0.5, và mọi cặp kỹ năng đều thấp hơn mục tiêu từ 2 bậc.
  - Thêm một ví dụ tính tay vào Phụ lục A.3.

### R26 🟠 Precision@5 thiên vị B0; ứng viên bị loại ở Bước 1 chưa có nhãn. Chốt: **Bạn**

- **Vấn đề:** B0 chỉ lấy ứng viên trùng chính xác (Spec:217), mà nhãn 0 đòi "không trùng chính xác". Vì vậy B0 không bao giờ có ứng viên nhãn 0, Precision@5 của B0 luôn tối đa, và P không thể vượt B0 trên chỉ số này.
- **Liên quan:** nếu chốt C4 là B0/B1 không lọc lịch rảnh, danh sách của chúng sẽ chứa ứng viên chưa có nhãn.
- **Đề xuất:**
  - Dùng Precision@5 nghiêm ngặt (chỉ tính nhãn 2) làm chỉ số chính.
  - Ứng viên vi phạm Bước 1 có rel = 0.
  - Mọi phương án dùng chung bước lọc lịch rảnh và hình thức học.

### R27 🟠 Coverage của B0 và B1 gần như chắc chắn bằng 1.0. Prop:49 hứa "chứng minh" cải thiện độ bao phủ. Chốt: **Bạn**

- **Vấn đề:** khoảng 25 kỹ năng với 100 người dùng thì mỗi kỹ năng gần như chắc chắn có người dạy. Mô phỏng cho Coverage = 1.0 ở cả N = 100 và N = 500.
- **Đề xuất:**
  - Sinh dữ liệu có độ phổ biến kỹ năng lệch (một số kỹ năng rất ít người dạy), thêm kịch bản dữ liệu thưa (N = 30–50).
  - Thêm chỉ số "tỷ lệ người dùng có ít nhất 5 ứng viên" hoặc "có ít nhất 1 ứng viên nhãn 2".
  - Định nghĩa "người dùng đủ điều kiện tìm kiếm" là `ACTIVE` và có ít nhất một kỹ năng muốn học.
  - Sửa Prop:49 từ "chứng minh" thành "kiểm tra giả thuyết".

### R28 🟠 Cấu hình "Ưu tiên uy tín" vô nghĩa vì dữ liệu giả lập không có lịch sử đánh giá. Chốt: **Bạn**

- **Vấn đề:** dữ liệu giả lập (Spec:238) không có rating hay phiên, nên mọi người có Reputation = 0.5. Nhãn cũng không dùng uy tín. Cấu hình 4 (Spec:244) thực chất chỉ là giảm trọng số kỹ năng.
- **Đề xuất:** bỏ cấu hình 4, hoặc ghi rõ nó đo gì. Ghi vào phần hạn chế: "Reputation không đánh giá được bằng nhãn offline".

### R29 🟡 Thiếu bảng tham số sinh dữ liệu giả lập. Chốt: Duyệt

- **Đề xuất:** thêm bảng tham số vào Spec §5.4, gộp luôn T6. Bảng gồm:
  - danh mục kỹ năng dùng V2 hay danh mục riêng; mật độ quan hệ liên quan;
  - phân bố số khung rảnh, hình thức học, trình độ và mục tiêu; tỷ lệ mục tiêu NULL;
  - bộ sinh có tuân thủ D19/D21 không;
  - cách gán id (ảnh hưởng quy tắc xếp khi bằng điểm).

  Kết quả của R24, R26 và R27 phụ thuộc trực tiếp vào các tham số này.

### R30 🟡 Latency trong thực nghiệm và NFR-03 đo hai thứ khác nhau. Chốt: Duyệt

- **Đề xuất:** báo cáo 2 số:
  - thời gian thuật toán chạy trong bộ nhớ, cho từng phương án (FR-11);
  - thời gian end-to-end của `GET /api/matches` trên 1.000 hồ sơ nạp vào DB (NFR-03, tuần 8, cần script seed).

### R31 🟡 TCKQ:29 đòi p-value, Spec:239 chỉ có 5 seed. Chốt: **Bạn**

- **Vấn đề:** tính p-value trên 5 seed gần như vô nghĩa.
- **Phương án:**
  - kiểm định cặp theo từng người dùng (Wilcoxon trên NDCG@5 giữa B1 và P) hoặc bootstrap để có khoảng tin cậy;
  - hoặc sửa TCKQ:29 chỉ yêu cầu trung bình ± độ lệch chuẩn.

### R32 🟢 So sánh số thực khi bằng điểm. Chốt: Duyệt

- **Vấn đề:** D của ví dụ A.3 tính bằng code ra 0.9400000000000001.
- **Đề xuất:**
  - Unit test dùng dung sai.
  - Quy tắc bằng điểm (Spec:135) so sánh theo epsilon 1e-9.
  - IDCG ở A.3 ngầm giả định tập ứng viên chỉ có 5 người; ghi rõ giả định này.

---

## 5. Tuần 6: yêu cầu và phiên

### R33 🟠 Báo cáo sự cố được gửi ở trạng thái phiên nào; báo cáo có thể bị "treo" mãi ở `OPEN`. Chốt: **Bạn**

- **Bằng chứng:**
  - TK:513 không có điều kiện trạng thái khi gửi báo cáo.
  - TK:592 và Spec:318: admin chỉ chốt được khi phiên đang `WAITING_CONFIRMATION`.
  - TC2 A19–A23 lại báo cáo và hủy phiên đang `IN_PROGRESS` (A23 còn báo cáo trên phiên đã `CANCELLED`), trái với TK:592 và Spec:324.
- **Kịch bản treo:** báo cáo gửi khi phiên đang `SCHEDULED`/`IN_PROGRESS`, hoặc bên kia xác nhận hoàn thành sau khi báo cáo đã mở. Admin không chốt được, mà CHECK ở SQL:160 cũng không cho đóng báo cáo nếu không chốt phiên.
- **Phương án:**
  1. *(đề xuất)* Chỉ nhận báo cáo khi phiên `WAITING_CONFIRMATION`, khác thì trả 409. Khi đang có báo cáo `OPEN` thì `complete` trả 409, chỉ admin chốt được. Mỗi phiên một báo cáo mở; người gửi sau nhận 409.
  2. Cho báo cáo ở mọi trạng thái chưa kết thúc và cho admin chốt cả từ `IN_PROGRESS`. Phải sửa Spec §6 và TK §5.2.
- **Sửa ngược:** TC2 A19–A23 và mô tả kịch bản TK:366.

### R34 🟠 Các mốc thời gian đã trôi qua: chấp nhận muộn, hủy sau giờ hẹn, bắt đầu muộn. Chốt: **Bạn**

- **Bằng chứng:**
  - Spec:282 và Spec:314 cho hủy phiên "**trước** giờ hẹn". TK:575/588 không đặt điều kiện thời gian; theo thứ bậc thì TK thắng.
  - Không có quy tắc nào cho việc chấp nhận một yêu cầu đã quá giờ đề xuất (sinh ra phiên có giờ bắt đầu trong quá khứ).
  - Nút "Bắt đầu" có cận dưới nhưng không có cận trên.
  - Phiên `SCHEDULED` không ai bắt đầu thì sao: chưa có quy tắc.
- **Đề xuất:**
  - `accept` khi đã quá `proposed_start_at` trả 409.
  - Hủy phiên `SCHEDULED` được ở mọi thời điểm, không tính vào CompletionRate.
  - "Bắt đầu" không có cận trên.
  - Ghi "phiên `SCHEDULED` không tự hết hạn" vào Spec:322 và G2.
  - Sửa ngược Spec:282, Spec:314.

### R35 🟠 Ai thấy địa điểm/link (`location_or_link`), qua endpoint nào; CLAUDE.md ghi sai. Chốt: **Bạn**

- **Vấn đề:**
  - Spec:365 chỉ cho xem địa điểm/link sau khi chấp nhận. Người nhận vì thế phải đồng ý một địa điểm OFFLINE mà mình chưa được xem.
  - Spec:347 cấm cả người gửi xem lại link do chính mình nhập.
  - Spec:418 ngụ ý `GET /requests/{id}` trả liên hệ sau `ACCEPTED`, trong khi TK:717 chỉ trả qua `/me` và `/sessions/{id}`.
  - CLAUDE.md:45 ghi "phiên đã `ACCEPTED`": phiên không có trạng thái này, và câu này thiếu quyền xem của admin.
  - Spec:447 xếp "quyền xem liên hệ" ở tuần 7, nhưng `/sessions/{id}` (tuần 6) đã trả liên hệ.
- **Phương án:**
  1. Giữ nguyên quy tắc hiện tại. Người nhận không hợp địa điểm thì hủy phiên `SCHEDULED` (không bị phạt).
  2. *(đề xuất)* Người gửi và người nhận thấy `location_or_link` ngay khi yêu cầu còn `PENDING`. `phone`/`contact_info` vẫn chỉ mở qua `/sessions/{id}`.
- **Sửa ngược (dù chọn phương án nào):**
  - `GET /requests/{id}` không bao giờ trả `phone`/`contactInfo`; sau `ACCEPTED` thì trả `sessionId`.
  - Sửa CLAUDE.md:45 và Spec:418.
  - Dời "quyền xem liên hệ" sang tuần 6.

### R36 🟡 Kiểm tra khi tạo yêu cầu còn thiếu luật và chưa chốt mã lỗi. Chốt: Duyệt

- **Vấn đề:** TK:493–496 không kiểm tra `senderLearnSkillId` có thuộc danh sách muốn học của người gửi, hay `receiverLearnSkillId` có thuộc danh sách muốn học của người nhận. Thiếu kiểm tra này thì người gửi có thể "gán" cho người nhận một kỹ năng họ không hề muốn học.
- **Đề xuất:**

  | Trường hợp | Mã |
  | --- | --- |
  | Kỹ năng không thuộc danh sách muốn học tương ứng | 400 |
  | Người nhận không tồn tại, không `ACTIVE` hoặc không phải `STUDENT` (không lộ trạng thái tài khoản) | 404 |
  | Gửi cho chính mình, Sim = 0, giờ đề xuất ở quá khứ | 400 |
  | Trùng `PENDING` cùng chiều, kể cả lỗi unique từ DB | 409 |
  | Thứ tự kiểm tra | quyền (403) trước, trạng thái (409) sau |

### R37 🟡 Khóa tài khoản khi người đó đang có yêu cầu hoặc phiên dở dang. Chốt: **Bạn**

- **Vấn đề:** D13 làm người bị khóa nhận 403 với mọi thao tác. Tài liệu chưa nói người nhận còn chấp nhận được yêu cầu của người bị khóa không, và phiên dở dang xử lý ra sao.
- **Phương án:**
  1. *(đề xuất)* Khóa tài khoản không tự đổi dữ liệu nào. `accept` trả 409 nếu bên kia không còn `ACTIVE`. Phiên dở dang do bên còn lại hủy hoặc báo cáo. Ghi vào phần hạn chế.
  2. Khi khóa, tự hủy các yêu cầu `PENDING` và phiên `SCHEDULED` của người đó. Cách này phá quy tắc "chỉ người gửi được hủy yêu cầu".

### R38 🟡 API §4.6–4.9 chưa có response. Chốt: Duyệt

- **Đề xuất:** viết bảng DTO trong Quyết định tuần 6:
  - `accept` trả `{requestId, sessionId}` để chuyển từ S08 sang S09.
  - `SessionDetail` gồm `status`, giờ, `mode`, `locationOrLink`, `partner{…, phone, contactInfo}`, `myCompleted`, `myReviewed`, `openReport`, `resolution`.
  - Danh sách sắp theo `created_at DESC`, chưa phân trang.

### R39 🟢 `exchange_requests.responded_at` chưa ràng buộc với `status`. Chốt: Duyệt

- **Đề xuất:**
  - Ghi vào TK §3.3: `responded_at` bắt buộc khi `ACCEPTED`/`REJECTED`, và cũng được ghi khi `CANCELLED`. Service tự bảo đảm.
  - Sửa ca TC2 A19, đang `ACCEPTED` mà không có `responded_at`.
  - CHECK trong DB qua migration mới là tùy chọn.

---

## 6. Tuần 7: đánh giá, quản trị

### R40 🟠 Admin chốt phiên: Thiết kế làm mất chữ "kèm lý do" của FR-09 và có thể gây lỗi 500. Chốt: **Bạn** (thuộc diện ngoại lệ thứ bậc)

- **Bằng chứng:**
  - FR-09 (Spec:49), Spec:330 và Prop:71 đều ghi "kèm lý do".
  - TK:525 có `note` nhưng không ghi bắt buộc; `admin_note` cho phép NULL.
  - Admin chọn hủy mà không có `note` thì vi phạm CHECK `cancel_reason` (SQL:128), thành lỗi 500.
- **Đề xuất:**
  - `note` bắt buộc, thiếu thì 400.
  - Khi chốt hủy: `cancel_reason = note`, `cancelled_by = adminId`.
  - Khi chốt hoàn thành: `completed_at = now()`.
  - CHECK `admin_note` trong DB là tùy chọn.

### R41 🟠 Quản trị (FR-08) thiếu API danh sách, màn hình khóa tài khoản và quy tắc khóa. Chốt: Duyệt

- **Bằng chứng:**
  - Admin không có cách tìm người cần khóa (không có `GET /api/admin/users`).
  - Admin không thấy kỹ năng đã ẩn để hiện lại (`/api/skills` chỉ trả kỹ năng đang hoạt động).
  - Không có GET quan hệ kỹ năng, không sửa được tên nhóm.
  - TK:711 truy vết khóa tài khoản sang màn hình A01, nhưng A01 là màn hình quản trị kỹ năng.
  - Ảnh use case h3_1 thiếu "Khóa/mở khóa".
  - A01 không có trong luồng điều hướng (§6.2, ảnh h4_3).
- **Đề xuất:**
  - Thêm `GET /api/admin/users?q=&status=`, `GET /api/admin/skills` (kể cả kỹ năng ẩn), `GET /api/admin/skill-relations`, `PUT /api/admin/skill-categories/{id}`.
  - Thêm màn hình A03 "Quản lý tài khoản".
  - Body của API khóa: `{status}`.
  - Không cho khóa tài khoản ADMIN (409). Mở khóa theo R06.
  - Cập nhật h3_1, §6.1, §6.2/h4_3, §7.

### R42 🟡 Đổi nhóm kỹ năng làm hỏng điều kiện "cùng nhóm" của quan hệ liên quan. Chốt: Duyệt

- **Vấn đề:** điều kiện "cùng nhóm" chỉ được kiểm tra lúc tạo quan hệ (TK:219), trong khi `PUT /admin/skills/{id}` cho đổi nhóm (TK:521).
- **Đề xuất:** đổi nhóm khi kỹ năng còn quan hệ thì trả 409; admin phải gỡ quan hệ trước.

### R43 🟡 Mẫu số CompletionRate tính cả phiên chưa kết thúc. Chốt: **Bạn**

- **Vấn đề:** người vừa bấm "Bắt đầu" bị giảm điểm ngay. Phiên kẹt ở `WAITING_CONFIRMATION` làm giảm điểm vĩnh viễn.
- **Phương án:**
  1. Mẫu số chỉ gồm phiên đã kết thúc: `COMPLETED` cộng `CANCELLED` có `started_at`. Sửa Spec:115 và TK:352.
  2. Giữ nguyên và ghi vào phần hạn chế.

### R44 🟡 Đánh giá trong mentoring một chiều vẫn cộng vào uy tín "dạy" của người học. Chốt: **Bạn**

- **Vấn đề:** trong phiên một chiều, mentor chấm điểm người học, nhưng điểm đó vẫn tính vào Reputation dạy của người học.
- **Gợi ý:** giữ nguyên và ghi rõ ở Spec §6.3: "rating là đánh giá đối tác nói chung", đồng thời ghi vào phần hạn chế.

### R45 🟢 Đánh giá có được sửa, xóa không; có thời hạn không; comment dài tối đa bao nhiêu. Chốt: Duyệt

- **Đề xuất:** không sửa, không xóa, không thời hạn; comment tối đa 1000 ký tự. Ghi vào Spec §6.3.

### R46 🟡 Tuần 7 quá tải. Chốt: **Bạn**

- **Vấn đề:** tuần 7 gồm FR-07, Reputation thật, quyền liên hệ, FR-08 (A01 và khóa), xử lý báo cáo (A02), FR-14 nếu kịp, test nhóm 5, cộng thêm các API ở R41. Tổng cộng khoảng 10 API và 3 màn hình.
- **Đề xuất:**
  - Dời CRUD danh mục kỹ năng (A01) lên tuần 4–5, vì phần này độc lập.
  - Dời quyền xem liên hệ về tuần 6 (R35).
  - Ghi rõ FR-09 chia đôi: gửi báo cáo ở tuần 6, xử lý ở tuần 7.

---

## 7. Tuần 8–9

### R47 🟡 Có deploy lên cloud không. Chốt: **Bạn**

- **Bằng chứng:** Prop:145 và Prop:159 ghi "Deploy cloud". Spec:449 chỉ ghi "Triển khai demo", dù Spec:437 nói "thống nhất với proposal".
- **Nếu có deploy:** thêm vào tuần 8: Dockerfile, biến môi trường, SMTP thật (Mailpit chỉ dùng khi dev), `FRONTEND_URL`.

### R48 🟢 Các chức năng "Nên có" (FR-12, 13, 15, 23) không có tuần làm; người dùng không có cách lấy lại mật khẩu. Chốt: Duyệt

- **Đề xuất:**
  - Ghi vào Spec §9: "Nên có làm ở tuần 8 nếu dư thời gian".
  - FR-12 lọc phía client, gắn vào tuần 4 vì chi phí thấp.
  - FR-23 nếu không làm thì ghi vào phần hạn chế.
  - Bổ sung FR-12…16 vào bảng truy vết.

### R49 🟡 NFR và Prop §8 chưa có cách kiểm tra. Chốt: Duyệt

- **Đề xuất:**
  - Thêm cột "cách kiểm tra" cho các NFR.
  - Kịch bản E2E ở tuần 8.
  - NFR-08: checklist 3 trình duyệt ở độ rộng 360 px.
  - NFR-07: clone repo mới và dựng lại theo README ở tuần 9.

### R50 🟢 Đăng ký trả 409 làm lộ email đã tồn tại, trái với tinh thần của D8/D13. Chốt: Duyệt

- **Đề xuất:** chấp nhận đánh đổi vì trải nghiệm đăng ký, và ghi vào phần hạn chế. R01 phương án 1 cũng làm giảm vấn đề này.

---

## 8. Đồng bộ tài liệu (không cần quyết định, sửa cùng lúc)

### R51 🟢 TK §8 và các tham chiếu "giả định Gx" đã lỗi thời

- **Vấn đề:** cả G1–G9 thực tế đã được chốt ở Spec hoặc QĐ:
  - G5 → D14; G6 → D9; G8 → D7; G9 → D12, D13.

  Nhưng các tham chiếu "giả định G…" vẫn còn ở TK:70, 263, 377, 407, 496, 504, 526, 587. Spec:7 chỉ nhắc G1–G7. Cột "Liên quan" dùng mã T7/L4 của file lịch sử.
- **Sửa:** đổi §8 thành bảng "đã chốt ở đâu", thay các tham chiếu bằng mục Spec hoặc mã D tương ứng.

### R52 🟢 Câu chữ lệch nhỏ

- Spec:338 "`has_rated` lưu riêng", trong khi TK:307 suy ra từ việc có dòng đánh giá hay không.
- TK §4.10 thiếu dòng "khóa/mở khóa" và "xem chi tiết phiên". Admin xem mọi phiên hay chỉ phiên có báo cáo: đề xuất chỉ phiên có báo cáo, khớp Spec:347 "tra cứu vi phạm".
- Truy vết FR-07 thiếu màn hình S06.

### R53 🟢 Bộ kiểm thử ràng buộc tuần 2 không phủ hết như tài liệu tuyên bố

- **Vấn đề:** TK:367 và README tuyên bố "mỗi ràng buộc ít nhất một câu vi phạm", nhưng còn khoảng 10 ràng buộc chưa có ca nào:
  - CHECK `completed_at` khi phiên `COMPLETED`;
  - nửa `cancelled_at` của CHECK hủy phiên;
  - CHECK miền giá trị của `type`, `status`, `mode`, `duration_minutes`, `resolution`;
  - `skill_categories.name UNIQUE`;
  - cận dưới 0 của `target_level`, `day_of_week`;
  - đường CASCADE theo chiều dương;
  - DEFAULT của `role`, `status`.
- **Sửa:** thêm các ca này, hoặc sửa câu thành "các ràng buộc chính".

### R54 🟢 ERD và §3.3 chưa đủ chi tiết

- ERD thiếu FK `first_completed_by`, `cancelled_by`, `resolved_by`.
- §3.3 các bảng kỹ năng thiếu kiểu dữ liệu và DEFAULT.
- TK:70 ghi "mặc định `RESTRICT`", nhưng thực tế là `NO ACTION`, và nó chỉ chặn xóa bảng cha.

### R55 🟢 Bảng truy vết TK §7 thiếu

- **Còn thiếu:** NFR-03…09, cột "test", cột "tuần", `/skills` và `/skill-categories` cho FR-03, FR-12…16.
- **Sửa:** mở rộng bảng sau khi chốt các mục trên.

### R56 🟢 Quy tắc thứ bậc và cách sửa ngược

- QĐ:3 ghi "spec là chuẩn" và CLAUDE.md:9 gọi spec là "nguồn sự thật". Cả hai ngược với thứ bậc đã chốt (giai đoạn sau thắng).
- **Sửa:**
  - Ghi thứ bậc và quy tắc "sửa ngược" vào CLAUDE.md.
  - Sửa QĐ:3.
  - Thêm cổng "viết ca kiểm thử từ tài liệu trước khi code" cho mỗi tuần.

---

## 9. Danh sách cần bạn quyết định (19 mục)

| Mã | Câu hỏi | Gợi ý |
| --- | --- | --- |
| R01 | Chặn chiếm tài khoản bằng cách nào | Đăng ký lại email `PENDING` thì ghi đè mật khẩu |
| R05 | Đọc lịch rảnh và kỹ năng của tôi: 3 GET riêng hay gộp vào `/me` | 3 GET riêng |
| R19 | Kỹ năng ẩn còn dùng trong matching không | Còn dùng (ẩn = chỉ ngừng cho chọn mới) |
| R22 | Kiến trúc lõi matching dùng chung cho thực nghiệm | Lõi thuần Java + 2 lớp gọi |
| R24 | Xử lý nhãn suy biến | Loại người dùng chỉ có 1 mức nhãn, báo cáo số bị loại |
| R25 | Nhãn với nhiều kỹ năng | Xét theo từng kỹ năng |
| R26 | Precision thiên vị B0 | Precision nghiêm ngặt + lọc chung |
| R27 | Coverage = 1 | Dữ liệu lệch + kịch bản thưa + chỉ số mới; sửa Prop:49 |
| R28 | Cấu hình "Ưu tiên uy tín" | Bỏ, ghi vào hạn chế |
| R31 | Thống kê | Kiểm định cặp theo người dùng, hoặc chỉ trung bình ± độ lệch chuẩn |
| R33 | Báo cáo sự cố ở trạng thái nào | Chỉ `WAITING_CONFIRMATION`; khóa `complete` khi có báo cáo mở |
| R34 | Các mốc thời gian | `accept` muộn → 409; hủy `SCHEDULED` bất kỳ lúc nào; "Bắt đầu" không cận trên |
| R35 | Địa điểm/link trước khi chấp nhận | Cho người gửi và người nhận thấy ngay |
| R37 | Khóa người đang có yêu cầu hoặc phiên | Không tự đổi dữ liệu; `accept` kiểm tra hai bên còn `ACTIVE` |
| R40 | `note` khi admin chốt phiên | Bắt buộc |
| R43 | Mẫu số CompletionRate | Chỉ phiên đã kết thúc |
| R44 | Đánh giá trong mentoring | Giữ nguyên, ghi vào hạn chế |
| R46 | Dàn lại tuần 7 | A01 lên tuần 4–5, liên hệ về tuần 6 |
| R47 | Deploy cloud | Bạn chọn |

Các mục còn lại (cột "Chốt: Duyệt") sẽ làm theo đề xuất nếu bạn không phản đối.

## 10. Thứ tự sửa đề xuất

1. **Đợt 1, ngay (chặn tuần 3):** R01–R17, R56. Sửa QĐ tuần 3 (thêm D25 trở đi), TK §4.2–4.3, §6.1, TC3, CLAUDE.md.
2. **Đợt 2, trước tuần 4:** R18–R23, R46, R51, R55.
3. **Đợt 3, trước tuần 5:** R24–R32. Đóng toàn bộ Spec §5.5.
4. **Đợt 4, trước tuần 6:** R33–R45, R52–R54. Có thể gom vào "Quyết định tuần 6".
5. **Đợt 5, trước tuần 8:** R47–R50.

Mỗi mục được chốt: sửa tài liệu mới nhất, grep và sửa ngược mọi tài liệu cũ nhắc tới nó trong cùng commit.

**Trạng thái** (người dùng chốt ngày 2026-10-29: mọi mục theo gợi ý):

| Đợt | Trạng thái | Ghi chú |
| --- | --- | --- |
| 1 | Đã sửa 2026-10-29 | Quyết định tuần 3 (sửa D3, D4, D5, D7–D10, D13, D15, D16, D18, D19, D21, D24; thêm D25–D30), TK §4.1–4.4, §6.1 (S01, S02), §8 (G8, G9), ca kiểm thử tuần 3 (90 ca), CLAUDE.md, `.env.example`. **Còn mở:** danh sách seed kỹ năng của D3 chưa duyệt; thêm Mailpit vào `docker-compose.yml` làm khi code (R15). Phần R13 về tên kỹ năng trùng hoa thường dời sang tuần 7 (admin) |
| 2 | Đã sửa 2026-10-29 | R18–R23, R46, R51, R55, cộng thêm R32 và R43 (unit test tuần 4 cần). Spec 4.1–4.4, 6.3, 7.1, 9 và phụ lục A.1; TK §2, 3.3, 3.5, 4.1, 4.3, 4.5, 4.6, 5.2, 6.1, 7, 8; tạo `docs/tuan_4/Quyet_dinh_tuan_4.md` (D31–D38); TC2 truy vấn C05/C06 (chạy lại 2026-10-29: 73/73 PASS); CLAUDE.md. **Không sửa** Proposal §7: đây là tài liệu đã nộp, phần điều chỉnh kế hoạch được ghi ở spec mục 9 |
| 3–5 | Chưa sửa | R47 (deploy cloud) chưa có gợi ý, cần người dùng chọn |

---

## Phụ lục: Bảng trạng thái × hành động (cơ sở viết test tuần 6–7)

**Ký hiệu:**
- **P** = người tham gia; **3rd** = người dùng khác; **Adm** = admin.
- Tài khoản `LOCKED` hoặc `PENDING_VERIFICATION` nhận 403 với mọi API trong bảng.
- **[Rxx]** = ô chưa xác định, chờ chốt mục tương ứng.

### Yêu cầu

| Trạng thái | accept | reject | cancel | GET /requests/{id} |
| --- | --- | --- | --- | --- |
| PENDING | Người nhận → ACCEPTED + phiên SCHEDULED trong cùng transaction. Người khác: 403. Quá giờ đề xuất [R34]. Bên kia bị khóa [R37] | Người nhận → REJECTED. Người khác: 403 | Người gửi → CANCELLED. Người khác: 403 | P: 200, địa điểm/link [R35]. 3rd/Adm: 403 |
| ACCEPTED / REJECTED / CANCELLED | 409 | 409 | 409 | P: 200 |

Tạo yêu cầu: chỉ `STUDENT` + `ACTIVE` [R18]. Mã lỗi theo [R36]. Trùng `PENDING` cùng chiều: 409. Chiều ngược lại: được phép.

### Phiên

| Trạng thái | start | complete | cancel (P, kèm lý do) | report (P) | review (P) | admin resolve |
| --- | --- | --- | --- | --- | --- | --- |
| SCHEDULED | P, từ 15 phút trước giờ hẹn → IN_PROGRESS; sớm hơn: 409 | 409 | → CANCELLED, không tính CR [R34] | [R33] | 409 | [R33] |
| IN_PROGRESS | 409 | P → WAITING_CONFIRMATION | → CANCELLED, tính CR của cả hai bên | [R33] | 409 | 409 theo TK [R33] |
| WAITING_CONFIRMATION | 409 | Bên còn lại → COMPLETED; người đã bấm: 409; khi có báo cáo mở [R33] | 409 | P → báo cáo OPEN; đã có báo cáo mở: 409 | 409 | Adm + báo cáo OPEN → COMPLETED/CANCELLED [R40] |
| COMPLETED | 409 | 409 | 409 | 409 | P → 201, mỗi người 1 lần; lần 2: 409 | – |
| CANCELLED | 409 | 409 | 409 | 409 | 409 | – |

3rd gọi bất kỳ hành động nào: 403. Admin gọi start, complete, cancel, review: 403.
