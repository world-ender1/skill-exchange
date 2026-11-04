# QUYẾT ĐỊNH TRIỂN KHAI – TUẦN 4

Chốt ngày 2026-10-29, từ đợt 2 của rà soát (`docs/ra_soat/Bao_cao_ra_soat.md`, R18–R23, R32, R43). Áp dụng khi làm FR-04 (gợi ý ghép cặp) và FR-10 (hồ sơ công khai, tìm kiếm).

File này là tài liệu mới nhất cho phạm vi tuần 4. Nếu khác spec tuần 1, thiết kế tuần 2 hoặc quyết định tuần 3 thì file này đúng. Khi thêm hoặc đổi một quyết định ở đây, phải sửa luôn các tài liệu cũ nhắc tới điểm đó trong cùng commit. Mã quyết định đánh số tiếp theo tuần 3 (D31 trở đi) để không trùng. D39–D42 đã dùng cho tuần 3, nên quyết định tuần 4 thêm sau ngày chốt bắt đầu từ D43.

*Cập nhật 2026-11-04:* thêm D43–D48 sau khi rà lại tài liệu tuần 4 trước lúc viết ca kiểm thử; sửa D31, D33, D34, D36, D37, D38 cho khớp.

**Nơi ghi các quy tắc của tuần 4** (file này không chép lại, chỉ trỏ tới):

| Nội dung | Ở đâu |
| --- | --- |
| Công thức điểm, lọc cứng, phân loại, bằng điểm | Spec 4.1–4.5; thang trình độ và khung giờ cho LevelFit, ScheduleFit ở Spec 4.6 |
| Ví dụ tính tay cho unit test | Spec phụ lục A.3 |
| Admin không tham gia ghép cặp; kỹ năng bị ẩn vẫn dùng | Spec 4.1, 4.2, 6.3 |
| Mẫu số CompletionRate | Spec 4.3; truy vấn ở TK 3.5 |
| Hợp đồng `/api/matches` (tham số, response, `needsLearnSkills`, `hasPendingRequest`) | TK 4.5 |
| Hợp đồng `/api/users/{id}`, `/api/users/search` (ai hiển thị, trường trả về, cách tìm) | TK 4.3 |
| Quyền *Active* = `ACTIVE` + `STUDENT` | TK 4.1 |
| Unit test nhóm 1–3 | Spec 7.1 |

## 1. Phạm vi tuần 4

| Mã | Quyết định |
| --- | --- |
| D31 | Làm: lõi matching (lọc cứng, chấm điểm, phân loại, xếp hạng), `GET /api/matches`, `GET /api/users/{id}`, `GET /api/users/search`, màn hình S05, S06, S10, unit test nhóm 1–3 (spec 7.1). **Không** làm: gửi yêu cầu (S07, tuần 6), đánh giá (`/api/users/{id}/reviews`, phần đánh giá của S06, tuần 7). Nút "Gửi yêu cầu" ở S05, S06 chưa hiển thị cho tới tuần 6. Lọc theo hình thức học ở S05 (FR-12, "Nên có", rà soát R48) cũng **không** làm ở tuần 4, giữ lịch "tuần 8 nếu kịp" của TK mục 8. Ca kiểm thử tuần 4 gồm cả các ca riêng tư (NFR-02) của `/api/users/{id}`, `/api/users/search`, `/api/matches`, vì các API này làm ở tuần 4 dù spec 7.1 xếp ca riêng tư vào nhóm 5. |
| D32 | Reputation được code đầy đủ theo spec 4.3 ngay tuần 4 (cần cho unit test nhóm 2) và đọc dữ liệu thật từ `reviews`, `exchange_sessions`. Các bảng này chưa có dữ liệu cho tới tuần 6–7, nên khi chạy thật mọi người đều có Reputation = 0.5, `ratingAverage = null`, `reviewCount = 0`. Đây là hành vi đúng, không phải lỗi. `hasPendingRequest` cũng truy vấn thật bảng `exchange_requests`. |

## 2. Kiến trúc và cài đặt

| Mã | Quyết định |
| --- | --- |
| D33 | Quyền *Active* (TK 4.1) được kiểm tra ở một chỗ dùng chung cho mọi API loại này (ví dụ một phương thức kiểm tra hoặc cấu hình trong `SecurityConfig`): tài khoản phải `ACTIVE` **và** `role = STUDENT`, ngược lại 403. Trạng thái đọc từ DB ở mỗi request như D13; vai trò lấy từ claim `role` của JWT (D14). Không viết lại điều kiện này trong từng controller. Cách làm cụ thể: D44. |
| D34 | **Lõi matching không phụ thuộc DB.** Package `matching` chia ba phần: (1) các record dữ liệu đầu vào, ví dụ `MatchProfile` (id, trạng thái tài khoản, vai trò, kỹ năng muốn học kèm mục tiêu, kỹ năng dạy kèm trình độ, các khung rảnh, hình thức học, Reputation; xem D43) và `SkillSimilarity` (tra $Sim(s,t)$ từ danh mục và quan hệ); (2) lõi thuần Java: `HardFilter`, `ScoreCalculator`, `ReputationCalculator` (D43) và `MatchingEngine` (gọi `HardFilter`, `ScoreCalculator`, phân loại, xếp hạng), nhận `MatchProfile` của người gọi, danh sách ứng viên và bộ trọng số `MatchingWeights`, trả danh sách kết quả; lõi không gọi Repository và không dùng Spring; (3) `MatchingService` (`@Service`) nạp dữ liệu thô bằng vài truy vấn gộp (TK 3.5), tính Reputation bằng `ReputationCalculator`, dựng `MatchProfile`, gọi lõi, rồi đổi sang DTO và thêm `ratingAverage`, `reviewCount`, `hasPendingRequest`. Lý do: unit test nhóm 1–3 chạy thẳng trên lõi, không cần DB; tuần 5 module `evaluation` dựng `MatchProfile` từ dữ liệu giả lập và gọi đúng lõi này (FR-11, NFR-05). |
| D35 | Trọng số nằm ở **một** record `MatchingWeights` với hằng số mặc định (0.55, 0.20, 0.15, 0.05, 0.05) theo spec 4.4. Hàm khởi tạo kiểm tra tổng bằng 1 (sai số $10^{-9}$). Thực nghiệm độ nhạy ở tuần 5 truyền bộ trọng số khác vào cùng lõi. |
| D36 | So sánh điểm khi xếp hạng: nếu $|a - b| < 10^{-9}$ thì coi là bằng và xếp theo `userId` tăng dần, ngược lại điểm cao đứng trước. Viết thành một `Comparator` dùng chung cho cả hai nhóm và cho B0, B1 ở tuần 5. Unit test so số thực bằng dung sai (`assertEquals(expected, actual, 1e-9)`), không so bằng tuyệt đối: ví dụ ở phụ lục A.3 tính bằng code ra `0.9400000000000001`. Hạn chế của cách so này: xem D48. |
| D37 | Tìm kiếm theo tên dùng `ILIKE ... ESCAPE '\'` với chuỗi đã escape các ký tự `\`, `%` và `_` của người dùng (PostgreSQL mặc định coi `\` là ký tự escape trong `LIKE`, nên phải escape nó trước). Không phân biệt hoa thường nhưng **phân biệt dấu** ("an" không khớp "Ân"); bỏ dấu khi tìm là việc nên có, chưa làm. |
| D38 | Đường dẫn có `id` không phải số (ví dụ `/api/users/abc`) trả 400 dạng Problem Details, không trả 500. Áp dụng cho cả tham số truy vấn sai kiểu (`skillId=abc`, `type=FOO`). `ResponseEntityExceptionHandler` đã trả 400 sẵn nhưng `detail` là câu tiếng Anh của Spring, nên `GlobalExceptionHandler` override `handleTypeMismatch` để trả câu tiếng Việt kèm tên tham số, như đã làm với `handleHttpMessageNotReadable`. |
| D43 | **Mọi quy tắc spec 7.1 nhóm 1–2 nằm trong lõi để unit test được.** (a) `MatchProfile` có `status` và `role`; `HardFilter` tự loại ứng viên không phải `ACTIVE` + `STUDENT` (điều kiện 2 của lọc cứng, spec 4.2). `MatchingService` vẫn được lọc sẵn trong truy vấn cho nhẹ, nhưng quy tắc chính thức và unit test ở `HardFilter`. (b) `ReputationCalculator` là lớp thuần, nhận dữ liệu thô của một người: danh sách điểm đánh giá nhận được và danh sách phiên đã tham gia dạng `SessionOutcome(status, started)` (`started` = `started_at IS NOT NULL`). Lớp tự lọc mẫu số CompletionRate theo spec 4.3, đổi trường hợp chưa có dữ liệu thành 0.5 và trả $Reputation = 0.7 \cdot RatingNorm + 0.3 \cdot CompletionRate$. Truy vấn chỉ nạp dữ liệu thô: phiên lấy qua JOIN `exchange_requests` theo `sender_id` hoặc `receiver_id`, mỗi phiên một dòng cho mỗi người tham gia, không đếm trùng. (c) Có một test tích hợp kiểm truy vấn nạp đúng dữ liệu thô. Lý do: D34 yêu cầu unit test nhóm 1–3 chạy trên lõi; nếu hai quy tắc này nằm trong SQL thì chỉ test được bằng DB. |
| D44 | **Quyền *Active* kiểm ở tầng filter, 403 đứng trước mọi lỗi 400/404.** `AccountStatusFilter` (đã đọc tài khoản từ DB mỗi request, D13) gắn thêm quyền `ACTIVE_STUDENT` khi `status = ACTIVE` và claim `role = STUDENT`. `SecurityConfig` thêm luật `hasAuthority("ACTIVE_STUDENT")` cho các đường dẫn quyền *Active* ở TK 4.1: tuần 4 là `/api/matches/**` và `/api/users/**`; tuần 6 thêm đường dẫn của mình vào cùng luật. Spring kiểm luật trước khi vào controller, nên admin hoặc tài khoản `PENDING_VERIFICATION` gọi `/api/users/abc` hay `/api/matches` thiếu `type` đều nhận 403, không nhận 400. Không kiểm lại điều kiện này trong service. |
| D45 | **Chi tiết `GET /api/users/search`.** (a) `q` được cắt khoảng trắng hai đầu; còn rỗng thì coi như không truyền, nên `q=` (hoặc chỉ có khoảng trắng) mà không có `skillId` trả 400 như thiếu cả hai tham số. (b) `q` dài tối đa 100 ký tự (bằng `full_name`), dài hơn trả 400. (c) `skillId` không tồn tại trả 200 với danh sách rỗng; `skillId` của kỹ năng bị ẩn vẫn khớp (spec 4.1). Giao diện S10 chỉ cho chọn kỹ năng đang hoạt động (lấy từ `/api/skills`, D40). (d) Response là mảng JSON, không bọc `{items}`, không báo "còn nữa" khi vượt 20 người. Mỗi phần tử: `id`, `fullName`, `learningMode`, `teachSkills[{skillId, skillName, categoryName, level}]`. |
| D46 | **Chọn kỹ năng và thứ tự các danh sách trong kết quả.** (a) LevelFit chọn $t \in T_v$ theo $Sim(s,t)$ cao nhất, bằng thì trình độ cao hơn, bằng nữa thì `skillId` nhỏ hơn. Điểm không đổi nhưng tên kỹ năng trong `youLearn`, `theyLearn` cố định. (b) `youLearn`, `theyLearn` có một dòng cho mỗi kỹ năng muốn học có $Sim > 0$, kèm kỹ năng dạy đã chọn ở (a), xếp theo tên kỹ năng muốn học rồi `skillId` như D41. `commonSlots` xếp như lịch rảnh ở D41. (c) Hồ sơ công khai `/api/users/{id}` và `teachSkills` của kết quả tìm kiếm dùng thứ tự D41; kỹ năng bị ẩn vẫn hiện. |
| D47 | `MatchingWeights` kiểm **mỗi** trọng số nằm trong $[0, 1]$ **và** tổng bằng 1 (sai số $10^{-9}$), sai thì ném `IllegalArgumentException`. Lý do: trọng số âm vẫn có thể cho tổng bằng 1 nhưng làm $D$ ra ngoài $[0, 1]$ (NFR-04); thực nghiệm độ nhạy tuần 5 truyền nhiều bộ trọng số nên cần chặn ở đây. |
| D48 | **Hạn chế đã biết của D36.** So "bằng nhau khi chênh dưới $10^{-9}$" không có tính bắc cầu ($a \approx b$, $b \approx c$ nhưng có thể $a \not\approx c$), trái hợp đồng của `Comparator`; về lý thuyết `List.sort` có thể ném `IllegalArgumentException: Comparison method violates its general contract`. Vẫn giữ D36 vì đúng với câu chữ của spec 4.4 và chỉ xảy ra khi ba điểm trở lên cách nhau dưới $10^{-9}$, rất hiếm với điểm thật. Không lượng tử hóa điểm (`Math.round(x * 1e9)`), vì sẽ coi hai điểm chênh $10^{-10}$ nằm hai bên ranh giới làm tròn là khác nhau. Ghi vào phần hạn chế của báo cáo tuần 4. |

## 3. Việc cần xong trước khi code tuần 4

- Code tuần 3 chạy được (đăng ký, hồ sơ, kỹ năng), vì matching đọc chính các dữ liệu này.
- `V2__seed_skills.sql` đã duyệt danh sách và đã chạy (D3), vì $Sim$ cần danh mục và quan hệ liên quan.
- Ca kiểm thử tuần 4 viết từ tài liệu (`docs/tuan_4/kiem_thu/ca_kiem_thu.md`), theo quy tắc trong CLAUDE.md.
