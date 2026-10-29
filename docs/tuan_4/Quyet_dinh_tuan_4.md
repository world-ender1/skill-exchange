# QUYẾT ĐỊNH TRIỂN KHAI – TUẦN 4

Chốt ngày 2026-10-29, từ đợt 2 của rà soát (`docs/ra_soat/Bao_cao_ra_soat.md`, R18–R23, R32, R43). Áp dụng khi làm FR-04 (gợi ý ghép cặp) và FR-10 (hồ sơ công khai, tìm kiếm).

File này là tài liệu mới nhất cho phạm vi tuần 4. Nếu khác spec tuần 1, thiết kế tuần 2 hoặc quyết định tuần 3 thì file này đúng. Khi thêm hoặc đổi một quyết định ở đây, phải sửa luôn các tài liệu cũ nhắc tới điểm đó trong cùng commit. Mã quyết định đánh số tiếp theo tuần 3 (D31 trở đi) để không trùng.

**Nơi ghi các quy tắc của tuần 4** (file này không chép lại, chỉ trỏ tới):

| Nội dung | Ở đâu |
| --- | --- |
| Công thức điểm, lọc cứng, phân loại, bằng điểm | Spec 4.1–4.5 |
| Admin không tham gia ghép cặp; kỹ năng bị ẩn vẫn dùng | Spec 4.1, 4.2, 6.3 |
| Mẫu số CompletionRate | Spec 4.3; truy vấn ở TK 3.5 |
| Hợp đồng `/api/matches` (tham số, response, `needsLearnSkills`, `hasPendingRequest`) | TK 4.5 |
| Hợp đồng `/api/users/{id}`, `/api/users/search` (ai hiển thị, trường trả về, cách tìm) | TK 4.3 |
| Quyền *Active* = `ACTIVE` + `STUDENT` | TK 4.1 |
| Unit test nhóm 1–3 | Spec 7.1 |

## 1. Phạm vi tuần 4

| Mã | Quyết định |
| --- | --- |
| D31 | Làm: lõi matching (lọc cứng, chấm điểm, phân loại, xếp hạng), `GET /api/matches`, `GET /api/users/{id}`, `GET /api/users/search`, màn hình S05, S06, S10, unit test nhóm 1–3 (spec 7.1). **Không** làm: gửi yêu cầu (S07, tuần 6), đánh giá (`/api/users/{id}/reviews`, phần đánh giá của S06, tuần 7). Nút "Gửi yêu cầu" ở S05, S06 chưa hiển thị cho tới tuần 6. |
| D32 | Reputation được code đầy đủ theo spec 4.3 ngay tuần 4 (cần cho unit test nhóm 2) và đọc dữ liệu thật từ `reviews`, `exchange_sessions`. Các bảng này chưa có dữ liệu cho tới tuần 6–7, nên khi chạy thật mọi người đều có Reputation = 0.5, `ratingAverage = null`, `reviewCount = 0`. Đây là hành vi đúng, không phải lỗi. `hasPendingRequest` cũng truy vấn thật bảng `exchange_requests`. |

## 2. Kiến trúc và cài đặt

| Mã | Quyết định |
| --- | --- |
| D33 | Quyền *Active* (TK 4.1) được kiểm tra ở một chỗ dùng chung cho mọi API loại này (ví dụ một phương thức kiểm tra hoặc cấu hình trong `SecurityConfig`): tài khoản phải `ACTIVE` **và** `role = STUDENT`, ngược lại 403. Trạng thái đọc từ DB ở mỗi request như D13; vai trò lấy từ claim `role` của JWT (D14). Không viết lại điều kiện này trong từng controller. |
| D34 | **Lõi matching không phụ thuộc DB.** Package `matching` chia ba phần: (1) các record dữ liệu đầu vào, ví dụ `MatchProfile` (id, kỹ năng muốn học kèm mục tiêu, kỹ năng dạy kèm trình độ, các khung rảnh, hình thức học, Reputation) và `SkillSimilarity` (tra $Sim(s,t)$ từ danh mục và quan hệ); (2) lõi thuần Java: `HardFilter`, `ScoreCalculator` và `MatchingEngine` (gọi hai lớp trên, phân loại, xếp hạng), nhận `MatchProfile` của người gọi, danh sách ứng viên và bộ trọng số `MatchingWeights`, trả danh sách kết quả; lõi không gọi Repository và không dùng Spring; (3) `MatchingService` (`@Service`) nạp dữ liệu bằng vài truy vấn gộp (TK 3.5), dựng `MatchProfile`, gọi lõi, rồi đổi sang DTO và thêm `ratingAverage`, `reviewCount`, `hasPendingRequest`. Lý do: unit test nhóm 1–3 chạy thẳng trên lõi, không cần DB; tuần 5 module `evaluation` dựng `MatchProfile` từ dữ liệu giả lập và gọi đúng lõi này (FR-11, NFR-05). |
| D35 | Trọng số nằm ở **một** record `MatchingWeights` với hằng số mặc định (0.55, 0.20, 0.15, 0.05, 0.05) theo spec 4.4. Hàm khởi tạo kiểm tra tổng bằng 1 (sai số $10^{-9}$). Thực nghiệm độ nhạy ở tuần 5 truyền bộ trọng số khác vào cùng lõi. |
| D36 | So sánh điểm khi xếp hạng: nếu $|a - b| < 10^{-9}$ thì coi là bằng và xếp theo `userId` tăng dần, ngược lại điểm cao đứng trước. Viết thành một `Comparator` dùng chung cho cả hai nhóm và cho B0, B1 ở tuần 5. Unit test so số thực bằng dung sai (`assertEquals(expected, actual, 1e-9)`), không so bằng tuyệt đối: ví dụ ở phụ lục A.3 tính bằng code ra `0.9400000000000001`. |
| D37 | Tìm kiếm theo tên dùng `ILIKE` với chuỗi đã escape các ký tự `%` và `_` của người dùng. Không phân biệt hoa thường nhưng **phân biệt dấu** ("an" không khớp "Ân"); bỏ dấu khi tìm là việc nên có, chưa làm. |
| D38 | Đường dẫn có `id` không phải số (ví dụ `/api/users/abc`) trả 400 dạng Problem Details, không trả 500. |

## 3. Việc cần xong trước khi code tuần 4

- Code tuần 3 chạy được (đăng ký, hồ sơ, kỹ năng), vì matching đọc chính các dữ liệu này.
- `V2__seed_skills.sql` đã duyệt danh sách và đã chạy (D3), vì $Sim$ cần danh mục và quan hệ liên quan.
- Ca kiểm thử tuần 4 viết từ tài liệu (`docs/tuan_4/kiem_thu/ca_kiem_thu.md`), theo quy tắc trong CLAUDE.md.
