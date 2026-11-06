# Ca kiểm thử tuần 4 – Matching, hồ sơ công khai, tìm kiếm

Mục đích: liệt kê trước các ca kiểm thử cho chức năng tuần 4 (FR-04, FR-10), viết **chỉ từ tài liệu**, trước khi có code. Code tuần 4 phải làm các ca này pass. Ca nào thấy sai thì sửa tài liệu trước rồi mới sửa ca, không sửa ca cho khớp code.

Viết ngày 2026-11-04, sau khi chốt D43–D48. Mã ca bắt đầu từ TC101 để không trùng tuần 3 (TC01–TC93). TC75, TC76 của tuần 3 được hẹn làm ở tuần 4 và nằm trong nhóm E.

*Cập nhật 2026-11-04:* thêm TC185 cho D49; TC179 ghi nguồn D50 cho cách hiển thị phần trăm.

## Nguồn

Khi các nguồn khác nhau, nguồn mới hơn đúng: quyết định tuần 4 > quyết định tuần 3 > thiết kế tuần 2 > spec tuần 1.

| Ký hiệu | Tài liệu |
| --- | --- |
| Spec 4.1–4.6, A.3 | `docs/tuan_1/Phan_tich_yeu_cau_Skill_Exchange_Tuan_1.md`: Sim, lọc cứng, công thức điểm, $D$, phân loại, quy ước dữ liệu, ví dụ tính tay |
| Spec 7.1 | Spec tuần 1, danh mục unit test nhóm 1–3 |
| FR-04, FR-10, NFR-02, NFR-04, NFR-09 | Spec tuần 1, mục 3 và 7 |
| TK 3.5 | `docs/tuan_2/Tuan_2_Thiet_ke.md`: dữ liệu dùng cho matching |
| TK 4.1, 4.3, 4.5 | Thiết kế tuần 2: quy ước API, quyền *Active*, `/users/{id}`, `/users/search`, `/matches` |
| TK 6.1, 6.3 | Thiết kế tuần 2: màn hình S05, S06, S10 |
| D12, D13, D40, D41 | `docs/tuan_3/Quyet_dinh_tuan_3.md` |
| D31–D38, D43–D50 | `docs/tuan_4/Quyet_dinh_tuan_4.md` |

**Loại:**
- **Unit**: JUnit thuần, không Spring, không DB, chạy trên lõi `matching` (D34, D43).
- **Tích hợp**: `@SpringBootTest` trên DB test `skillexchange_test` (D39), kế thừa `IntegrationTestBase`.
- **Thủ công**: kiểm tra bằng giao diện, `curl` hoặc log.

**Quy ước:**
- Số thực so bằng dung sai $10^{-9}$ (D36). "≈" trong cột mong đợi chỉ để đọc; giá trị dùng trong test là biểu thức ghi kèm.
- Điểm trong response API làm tròn 3 chữ số kiểu HALF_UP (TK 4.5, kế hoạch tuần 4 bước 8). Các ca API chỉ so những điểm không rơi vào ranh giới làm tròn.
- Trọng số mặc định (0.55, 0.20, 0.15, 0.05, 0.05), trừ khi ca ghi khác.
- "Java ← Kotlin" nghĩa là kỹ năng muốn học Java được đáp ứng bởi kỹ năng dạy Kotlin (cặp `want`, `teach` trong `youLearn`, `theyLearn`).
- Khung giờ ghi tắt: "Tối T4" = `{dayOfWeek: 3, period: EVENING}` (1 = Thứ Hai, 7 = Chủ nhật).
- "Có token" nghĩa là token hợp lệ của một sinh viên `ACTIVE`, trừ khi ca ghi khác.
- Dữ liệu cho `exchange_requests`, `exchange_sessions`, `reviews` được chèn thẳng bằng SQL trong test, vì API tạo chúng làm ở tuần 6–7.

## Dữ liệu mẫu dùng chung

Kỹ năng và quan hệ lấy từ `V2__seed_skills.sql`: Java–Kotlin, SQL–MySQL, Tiếng Nhật–Tiếng Trung, Tiếng Nhật–Tiếng Hàn là cặp liên quan. Java và Python cùng nhóm Lập trình nhưng **không** liên quan.

| Hồ sơ | Muốn học (mục tiêu) | Dạy (trình độ) | Lịch rảnh | Hình thức | Lịch sử |
| --- | --- | --- | --- | --- | --- |
| **An** | Java (3), Tiếng Anh (không đặt) | Python (4) | Tối T2, Tối T4, Tối T6 | `ONLINE` | Chưa có |
| **Bình** | Python (2) | Kotlin (2), Tiếng Anh (3) | Tối T4, Tối T6, Sáng T7, Sáng CN | `ANY` | Chưa có |

Kết quả tính tay (đáp án ở bài học tuần 4, mục C2):

| Thành phần | An học từ Bình | Bình học từ An |
| --- | --- | --- |
| SkillFit | (0.5 + 1.0) / 2 = 0.75 | 1.0 |
| LevelFit | (0.5 + 0.5) / 2 = 0.5 | 1.0 |
| ScheduleFit | 2 / min(3, 4) = 2/3 | 2/3 |
| ModeFit | 1.0 | 1.0 |
| Reputation | 0.5 | 0.5 |
| $D$ | **0.6875** | **0.925** |

Hai chiều; `MutualScore` = `Math.sqrt(0.6875 * 0.925)` ≈ 0.797457.

## Tổng quan

| Nhóm | Số ca | Ý nghĩa |
| --- | --- | --- |
| A. Lõi tính điểm | 26 | Sim, SkillFit, LevelFit, ScheduleFit, ModeFit, Reputation, trọng số. Spec 7.1 nhóm 2. |
| B. Lọc cứng, phân loại, xếp hạng | 18 | `HardFilter`, `MatchingEngine`, ví dụ tính tay, bằng điểm. Spec 7.1 nhóm 1 và 3. |
| C. API `/api/matches` | 14 | Tham số, dữ liệu thật từ DB, riêng tư, Reputation từ dữ liệu thô (D43). |
| D. Hồ sơ công khai và tìm kiếm | 15 | `/api/users/{id}`, `/api/users/search` (TK 4.3, D37, D38, D45, D46). |
| E. Phân quyền và riêng tư | 8 | Quyền *Active* (D44), thứ tự 403 trước 400, TC75, TC76. |
| F. Giao diện | 6 | S05, S06, S10, thủ công. |
| **Tổng** | **87** | |

---

## A. Lõi tính điểm (unit)

| Mã | Nguồn | Đầu vào | Kết quả mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC101 | Spec 4.1 | `sim(Java, Java)` | 1.0 | Unit |
| TC102 | Spec 4.1, TK 3.3 | `sim(Java, Kotlin)` và `sim(Kotlin, Java)` (quan hệ lưu một chiều, `a < b`) | Cả hai 0.5 | Unit |
| TC103 | Spec 4.1, TK 3.3 | `sim(Java, Python)` (cùng nhóm, không đánh dấu liên quan); `sim(Tiếng Trung, Tiếng Hàn)` (cùng liên quan với Tiếng Nhật) | Cả hai 0.0: cùng nhóm chưa đủ, quan hệ không bắc cầu | Unit |
| TC104 | Spec 4.1 | $W_u$ = {Java, Figma}, $T_v$ = {Java} | SkillFit = 0.5 | Unit |
| TC105 | Spec 4.1 | $W_u$ = {Java}, $T_v$ = {Kotlin, Java} | SkillFit = 1.0 (lấy max, không lấy trung bình trên $T_v$) | Unit |
| TC106 | Spec 4.1 | An học từ Bình | SkillFit = 0.75 | Unit |
| TC107 | Spec 4.2, NFR-04 | $W_u$ rỗng, $T_v$ = {Java} | SkillFit = 0.0, không ném lỗi, không `NaN` | Unit |
| TC108 | Spec 4.3, 4.6 | LevelFit một kỹ năng, mục tiêu 3, trình độ dạy lần lượt 4, 3, 2, 1 (`@ParameterizedTest`) | 1.0, 1.0, 0.5, 0.0 | Unit |
| TC109 | Spec 4.3, 4.6 | Mục tiêu 4, trình độ 1; mục tiêu bỏ trống, trình độ 1 | 0.0; 0.5 | Unit |
| TC110 | Spec 4.6 | Ví dụ spec: $u$ muốn Java (3), SQL (2), Figma; $v$ dạy Java (3), MySQL (1) | LevelFit = (1.0 + 0.5) / 2 = 0.75. Figma không vào mẫu số | Unit |
| TC111 | Spec 4.3 | An học từ Bình (Java khớp Kotlin mức 2, mục tiêu 3; Tiếng Anh không mục tiêu) | LevelFit = 0.5 | Unit |
| TC112 | Spec 4.3 | $u$ muốn Java (2); $v$ chỉ dạy Kotlin (2) | LevelFit = 1.0: kỹ năng liên quan so trình độ bình thường, không giảm thêm | Unit |
| TC113 | Spec 4.3 | $u$ muốn Java (3); $v$ dạy Java (1) và Kotlin (4) | LevelFit = 0.0: chọn Java vì Sim 1.0 cao hơn, dù Kotlin trình độ cao hơn | Unit |
| TC114 | Spec 4.3 | $u$ muốn Tiếng Nhật (3); $v$ dạy Tiếng Trung (1) và Tiếng Hàn (3), cùng Sim 0.5 | LevelFit = 1.0: bằng Sim thì chọn trình độ cao hơn | Unit |
| TC115 | D46 | $u$ muốn Tiếng Nhật (2); $v$ dạy hai kỹ năng cùng Sim 0.5, cùng trình độ 3, `skillId` 9 và 7 | LevelFit = 1.0; kỹ năng được chọn (ghi trong `youLearn`) là `skillId` 7 | Unit |
| TC116 | Spec 4.3, 4.6 | ScheduleFit: $u$ 6 khung, $v$ 3 khung, trùng 2; $u$ 4 khung, $v$ 3 khung, trùng 2 | 2/3; 2/3 | Unit |
| TC117 | Spec 4.3 | ScheduleFit: $u$ 1 khung, $v$ 5 khung, trùng 1 | 1.0 (chia cho min, nhược điểm đã biết) | Unit |
| TC118 | Spec 4.3 | ScheduleFit: $u$ chưa khai báo, $v$ có 3 khung; cả hai chưa khai báo | 0.5; 0.5 | Unit |
| TC119 | Spec 4.3 | ModeFit 9 cặp không thứ tự (bỏ `ONLINE`+`OFFLINE`, bị loại ở lọc cứng, TC132): `ONLINE`+`ONLINE`, `OFFLINE`+`OFFLINE`, `ANY`+`ANY`, `ONLINE`+`ANY`, `OFFLINE`+`ANY`, `ONLINE`+chưa khai báo, `OFFLINE`+chưa khai báo, `ANY`+chưa khai báo, chưa khai báo+chưa khai báo. Mỗi cặp chạy cả hai chiều | 1.0, 1.0, 1.0, 1.0, 1.0, 0.5, 0.5, 0.5, 0.5; đổi chiều cho cùng kết quả | Unit |
| TC120 | Spec 4.3, D43 | `ReputationCalculator`: không có đánh giá, không có phiên | 0.5 | Unit |
| TC121 | Spec 4.3, 7.1 | Chỉ một đánh giá 5 sao, không có phiên | RatingNorm 1.0, CompletionRate 0.5, Reputation = 0.7 × 1.0 + 0.3 × 0.5 = 0.85 | Unit |
| TC122 | Spec 4.3, 7.1 | Không có đánh giá; phiên: 3 `COMPLETED`, 1 `CANCELLED` đã bắt đầu | CompletionRate 0.75, Reputation = 0.35 + 0.225 = 0.575 | Unit |
| TC123 | Spec 4.3, 7.1, D43 | Đánh giá 5 và 4; phiên: `COMPLETED`, `CANCELLED` đã bắt đầu, `IN_PROGRESS`, `WAITING_CONFIRMATION`, `SCHEDULED`, `CANCELLED` chưa bắt đầu | Mẫu số = 2, CompletionRate 0.5, Reputation = 0.7 × 0.875 + 0.3 × 0.5 = 0.7625 | Unit |
| TC124 | Spec 4.3, NFR-04 | Không có đánh giá; phiên chỉ gồm `SCHEDULED`, `IN_PROGRESS`, `WAITING_CONFIRMATION`, `CANCELLED` chưa bắt đầu | Mẫu số = 0, CompletionRate 0.5, Reputation 0.5, không `NaN` | Unit |
| TC125 | Spec A.3, D36 | Đánh giá 5, 5, 4, 4, 3 (trung bình 4.2); 4 `COMPLETED`, 1 `CANCELLED` đã bắt đầu | Reputation = 0.8 (code ra 0.7999999999999999, so bằng dung sai) | Unit |
| TC126 | D35, D47 | `MatchingWeights` mặc định; bộ (0.5, 0.2, 0.15, 0.05, 0.05) tổng 0.95; bộ (0.6, 0.2, 0.15, 0.1, −0.05) tổng 1 nhưng có trọng số âm | Mặc định tạo được, tổng 1; hai bộ sau ném `IllegalArgumentException` | Unit |

## B. Lọc cứng, phân loại, xếp hạng (unit)

Mỗi ca lọc cứng có ứng viên đối chứng hợp lệ trong cùng danh sách, để hàm "loại tất cả" không pass.

| Mã | Nguồn | Đầu vào | Kết quả mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC127 | Spec 4.2 (1) | Danh sách ứng viên có chính $u$ | $u$ không có trong kết quả | Unit |
| TC128 | Spec 4.2 (2), D43 | Ứng viên `PENDING_VERIFICATION`, khớp kỹ năng | Bị loại | Unit |
| TC129 | Spec 4.2 (2), D43 | Ứng viên `LOCKED`, khớp kỹ năng | Bị loại | Unit |
| TC130 | Spec 4.2 (2), 6.3, D43 | Ứng viên `ACTIVE` có `role = ADMIN`, khớp kỹ năng | Bị loại | Unit |
| TC131 | Spec 4.2 (3) | Ứng viên dạy Python, $u$ chỉ muốn Java | Bị loại (SkillFit = 0) | Unit |
| TC132 | Spec 4.2 (4), (5) | Ứng viên khai Sáng T2 khi $u$ khai Tối T2 (không trùng); ứng viên `OFFLINE` khi $u$ `ONLINE` | Cả hai bị loại | Unit |
| TC133 | Spec 4.2, 7.1 | Ứng viên chưa khai lịch; ứng viên chưa khai hình thức; ứng viên `ANY` khi $u$ `OFFLINE` | Cả ba còn trong kết quả, lần lượt ScheduleFit 0.5, ModeFit 0.5, ModeFit 1.0 | Unit |
| TC134 | Spec 4.4, 4.5 | An gọi với ứng viên Bình | $D$(An, Bình) = 0.6875; $D$(Bình, An) = 0.925; loại `TWO_WAY`; `MutualScore` = `Math.sqrt(0.6875 * 0.925)` | Unit |
| TC135 | Spec A.3, D36 | $u$ muốn Java (3), 4 khung, `ONLINE`; $v$ dạy Java (3), 3 khung trùng 2, `ONLINE`, Reputation 0.8 | $D(u,v)$ = 0.94 (code ra 0.9400000000000001, so bằng dung sai) | Unit |
| TC136 | Spec 4.5, 7.1 | Ứng viên dạy Java cho $u$ nhưng không dạy được gì $v$ muốn học; ứng viên có $W_v$ rỗng | Cả hai loại `ONE_WAY`, không có trong nhóm `TWO_WAY`; điểm xếp hạng = $D(u,v)$ | Unit |
| TC137 | Spec 4.5 | $u$ muốn Java, dạy Python; $v$ muốn Python, chỉ dạy Kotlin (liên quan Java, Sim 0.5) | `TWO_WAY`: cặp chỉ dựa trên kỹ năng liên quan vẫn là hai chiều; không dùng ngưỡng MutualScore | Unit |
| TC138 | Spec 4.4, D36 | `Comparator` với hai kết quả điểm 0.5 và 0.5 + 1e-12, `userId` 9 và 4 | Thứ tự 4, 9 | Unit |
| TC139 | D36 | Hai kết quả điểm 0.5 (`userId` 4) và 0.5 + 1e-6 (`userId` 9) | Thứ tự 9, 4: chênh trên ngưỡng thì điểm cao trước | Unit |
| TC140 | Spec 4.5 | 3 ứng viên hai chiều và 2 ứng viên một chiều có điểm khác nhau; một ứng viên một chiều có $D$ = 0.9 cao hơn mọi MutualScore | Nhóm `TWO_WAY` xếp theo MutualScore giảm dần, nhóm `ONE_WAY` theo $D$ giảm dần; hai nhóm tách riêng | Unit |
| TC141 | Spec 4.4, NFR-04, 7.1 | 50 hồ sơ ngẫu nhiên (seed cố định), chạy matching cho từng người | Mọi thành phần, $D$ và MutualScore thuộc [0, 1], không `NaN`; không ứng viên nào vi phạm 5 điều kiện lọc cứng | Unit |
| TC142 | D35 | An gọi với ứng viên Bình, trọng số (1, 0, 0, 0, 0) | $D$(An, Bình) = SkillFit = 0.75 | Unit |
| TC143 | Spec 4.2 | $u$ có $W_u$ rỗng | Cả hai nhóm rỗng, không ném lỗi | Unit |
| TC144 | D46, NFR-09 | An gọi với ứng viên Bình | `youLearn` = [Java ← Kotlin, liên quan, trình độ 2; Tiếng Anh ← Tiếng Anh, trùng, trình độ 3] đúng thứ tự này; `theyLearn` = [Python ← Python, trùng, trình độ 4]; `commonSlots` = [Tối T4, Tối T6] | Unit |

## C. API `/api/matches` (tích hợp)

Các ca dùng An và Bình tạo qua API tuần 3 (đăng ký, xác thực, khai hồ sơ, lịch, kỹ năng), trừ khi ca ghi khác.

| Mã | Nguồn | Đầu vào | Kết quả mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC145 | TK 4.5, D38 | Thiếu `type`; `type=FOO`; `limit=0`; `limit=51`; `limit=abc` | Cả 5 trả 400 Problem Details; `limit=abc` có `detail` tiếng Việt nêu tên tham số | Tích hợp |
| TC146 | TK 4.5 | `limit=1`; `limit=50` | 200 | Tích hợp |
| TC147 | TK 4.5, spec 4.4 | 12 ứng viên hai chiều hợp lệ, không truyền `limit` | `total = 12`, `items` có 10 phần tử | Tích hợp |
| TC148 | TK 4.5, spec 4.2 | Người gọi chưa khai kỹ năng muốn học | 200, `needsLearnSkills = true`, `total = 0`, `items = []` | Tích hợp |
| TC149 | TK 4.5, spec 4.4, 4.5 | An gọi `type=TWO_WAY` | Có Bình: `type = TWO_WAY`, `score = 0.797`, `forward.d = 0.688`, `backward.d = 0.925`, `forward.scheduleFit = 0.667`, `reputation = 0.5` | Tích hợp |
| TC150 | TK 4.5 | An gọi `type=ONE_WAY` | Không có Bình (Bình thuộc nhóm hai chiều) | Tích hợp |
| TC151 | TK 4.5, D32 | An xem kết quả về Bình khi chưa có đánh giá | `ratingAverage = null`, `reviewCount = 0` | Tích hợp |
| TC152 | TK 4.5, D32 | Chèn `exchange_requests` `PENDING` từ An tới Bình; rồi xét chiều ngược lại (yêu cầu `PENDING` từ Bình tới An, không có từ An) | Lần 1: Bình vẫn trong danh sách của An, `hasPendingRequest = true`. Lần 2: `hasPendingRequest = false` (chỉ tính yêu cầu người gọi gửi đi) | Tích hợp |
| TC153 | Spec 4.1, R19 | Ẩn Kotlin (`skills.is_active = false`), An gọi lại | Bình vẫn có, cùng `score = 0.797` | Tích hợp |
| TC154 | Spec 4.2 (2), D43 | Tạo thêm ba người khớp kỹ năng với An: một `PENDING_VERIFICATION`, một `LOCKED`, một admin | Không ai trong ba người xuất hiện ở cả hai nhóm | Tích hợp |
| TC155 | D43 (c), TK 3.5 | Ứng viên $v$ khớp với An. Chèn: phiên `COMPLETED` từ yêu cầu $v$ gửi, phiên `CANCELLED` đã bắt đầu từ yêu cầu $v$ nhận, phiên `CANCELLED` chưa bắt đầu và phiên `IN_PROGRESS` cũng của $v$, một phiên `COMPLETED` của hai người khác; đánh giá 5 sao cho $v$ trên phiên `COMPLETED` | `reputation` của $v$ = 0.85 (RatingNorm 1.0, CompletionRate 1/2); `ratingAverage = 5`, `reviewCount = 1`. Phiên của người khác không bị tính | Tích hợp |
| TC156 | NFR-02, TK 4.5 | Bình có `phone`, `contactInfo`; An gọi `/matches` | Không có key `email`, `phone`, `contactInfo` ở mọi mức của JSON | Tích hợp |
| TC185 | D49, TK 4.5 | An gọi `type=ONE_WAY` có ít nhất một kết quả; Bình chưa có đánh giá, An gọi `type=TWO_WAY` | `ONE_WAY`: mỗi phần tử **không có key** `backward`, `theyLearn` (không phải `null`). `TWO_WAY`: có key `ratingAverage` với giá trị `null` | Tích hợp |
| TC157 | TK 3.5 | Bật log SQL, gọi `/matches` với 20 rồi 40 ứng viên | Số câu SQL như nhau (không N+1) | Thủ công |

## D. Hồ sơ công khai và tìm kiếm (tích hợp)

| Mã | Nguồn | Đầu vào | Kết quả mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC158 | TK 4.3, D46 | An gọi `GET /api/users/{id của Bình}` | 200, đúng các trường `id`, `fullName`, `bio`, `learningMode`, `teachSkills[{skillId, skillName, categoryName, level}]`, `learnSkills[{skillId, skillName, categoryName, targetLevel}]`, `availability[{dayOfWeek, period}]`; danh sách theo thứ tự D41 | Tích hợp |
| TC159 | TK 4.3 | An xem hồ sơ của chính mình qua `/users/{id}` | 200 | Tích hợp |
| TC160 | TK 4.3 | `/users/{id}` với id không tồn tại, id của tài khoản `PENDING_VERIFICATION`, `LOCKED`, admin | Cả 4 trả 404 với cùng `detail` | Tích hợp |
| TC161 | D38 | `/users/abc`; `/users/99999999999999999999` (vượt `Long`) | Cả hai 400, `detail` tiếng Việt, không 500 | Tích hợp |
| TC162 | D46 (c), spec 4.1 | Ẩn Kotlin, xem hồ sơ Bình | Kotlin vẫn có trong `teachSkills` | Tích hợp |
| TC163 | TK 4.3, D45 (a) | `/users/search` không tham số; `q=` ; `q=%20%20` (chỉ khoảng trắng), không `skillId` | Cả 3 trả 400 | Tích hợp |
| TC164 | D45 (b) | `q` dài 100 ký tự; `q` dài 101 ký tự | 200; 400 | Tích hợp |
| TC165 | TK 4.3, D37 | Có "Nguyễn An", "Lê Văn Anh", "Trần Ân". Tìm `q=AN`; tìm `q=an` | `q=AN`: Nguyễn An và Lê Văn Anh, không có Trần Ân. `q=an`: cùng kết quả (không phân biệt hoa thường, phân biệt dấu) | Tích hợp |
| TC166 | D37 | Có "Lê_Test" và "Le Test". Tìm `q=%`; `q=_`; `q=\` | `%`: rỗng. `_`: chỉ Lê_Test. `\`: rỗng. Không câu nào trả về tất cả | Tích hợp |
| TC167 | TK 4.3 | `skillId` = Java. Có: X dạy Java, Y chỉ muốn học Java, Z dạy Kotlin | Chỉ X: chỉ khớp kỹ năng **dạy**, trùng chính xác | Tích hợp |
| TC168 | D45 (c), spec 4.1 | `skillId` của kỹ năng bị ẩn mà X đang dạy; `skillId=999999` (không tồn tại) | X có trong kết quả; 200 với `[]` | Tích hợp |
| TC169 | TK 4.3 | `skillId` = Java và `q=Nguyen` (có X dạy Java tên khác, có "Nguyen B" không dạy Java, có "Nguyen C" dạy Java) | Chỉ Nguyen C: phải thỏa cả hai | Tích hợp |
| TC170 | TK 4.3 | Người gọi dạy Java; có thêm một admin, một `PENDING`, một `LOCKED` dạy Java; tìm `skillId` = Java | Không có người gọi và ba người kia | Tích hợp |
| TC171 | TK 4.3 | 22 người dạy Java, tên không theo thứ tự tạo, có hai người trùng tên | Trả 20 người, xếp theo `fullName` rồi `id`; người trùng tên thì `id` nhỏ trước | Tích hợp |
| TC172 | D45 (d), NFR-02 | Kết quả tìm kiếm có người có `phone`, `contactInfo` | Response là mảng JSON; mỗi phần tử chỉ có `id`, `fullName`, `learningMode`, `teachSkills`; không có `email`, `phone`, `contactInfo` | Tích hợp |

## E. Phân quyền và riêng tư (tích hợp)

| Mã | Nguồn | Đầu vào | Kết quả mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC75 | D12, D44, TK 4.1 | Tài khoản `PENDING_VERIFICATION` gọi `/users/{id}`, `/users/search?q=a`, `/matches?type=TWO_WAY` | Cả 3 trả 403 | Tích hợp |
| TC76 | NFR-02, TK 4.3 | `GET /api/users/{id}` của người có `phone`, `contactInfo` | Không có key `email`, `phone`, `contactInfo` | Tích hợp |
| TC173 | Spec 6.3, D44 | Admin gọi `/users/{id}`, `/users/search?q=a`, `/matches?type=TWO_WAY` | Cả 3 trả 403 | Tích hợp |
| TC174 | D44 | Admin gọi `/users/abc`; tài khoản `PENDING` gọi `/matches?type=FOO`; admin gọi `/users/search` không tham số | Cả 3 trả 403, không phải 400 | Tích hợp |
| TC175 | TK 4.1 | Không có token gọi ba API trên | 401 | Tích hợp |
| TC176 | D13 | Token của tài khoản vừa bị `LOCKED` gọi `/matches?type=TWO_WAY` | 403 | Tích hợp |
| TC177 | D12 | Tài khoản `PENDING_VERIFICATION` gọi `GET /api/me`, `GET /api/skills` | Vẫn 200 (luật *Active* không chặn nhầm API quyền *User*) | Tích hợp |
| TC178 | D44 | Sinh viên `ACTIVE` gọi ba API | 200, không bị chặn | Tích hợp |

## F. Giao diện (thủ công)

| Mã | Nguồn | Đầu vào | Kết quả mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC179 | TK 6.1, 6.3, NFR-09, D50 | An mở S05 | Hai tab "Trao đổi hai chiều" và "Tìm người hướng dẫn", có `total` trên tab. Thẻ Bình: mức phù hợp 79.7%, nhãn "Mới", "Java ← Kotlin (kỹ năng liên quan)", lịch trùng Tối T4, Tối T6, hình thức. "Xem chi tiết điểm" hiện 5 thành phần khớp bảng tính tay | Thủ công |
| TC180 | D31 | S05 và S06 | Không có nút "Gửi yêu cầu"; S06 không có phần đánh giá | Thủ công |
| TC181 | TK 4.5 | Tài khoản chưa khai kỹ năng muốn học mở S05 | Lời nhắc và nút sang S04, không trang trắng | Thủ công |
| TC182 | TK 4.3, NFR-02 | Mở S06 hồ sơ Bình | Có kỹ năng, lịch rảnh, ghi "trình độ tự khai báo"; không có email, số điện thoại, liên hệ | Thủ công |
| TC183 | TK 6.1, D45 | S10: tìm "bình"; tìm theo kỹ năng Python; ô chọn kỹ năng | Ra đúng người; ô chọn chỉ có kỹ năng đang hoạt động | Thủ công |
| TC184 | NFR-08 | S05, S06, S10 ở màn hình rộng 360 px | Đọc được, không cuộn ngang | Thủ công |

---

## Không kiểm ở tuần 4

- Hiệu năng ≤ 2 giây trên 1.000 hồ sơ (NFR-03): đo ở tuần 5 cùng thực nghiệm (Latency, spec A.2).
- Lọc theo hình thức học ở S05 (FR-12): không làm ở tuần 4 (D31).
- `/api/users/{id}/reviews` và phần đánh giá của S06: tuần 7 (D31).
- `ModeFit(ONLINE, OFFLINE)` ở mức hàm: cặp này đã bị loại ở lọc cứng (TC132) nên không đi tới hàm điểm. Cách xử lý trong hàm (ném lỗi hay trả 0) để lúc code chọn, ghi lý do trong commit và có một test, nhưng không phải quy tắc nghiệp vụ.

## Những điểm tài liệu chưa quy định rõ (phát hiện khi viết ca)

| # | Chỗ chưa rõ | Ca liên quan | Cách xử lý trong bộ ca |
| --- | --- | --- | --- |
| ⚠1 | Chế độ làm tròn 3 chữ số của điểm trong API (HALF_UP hay HALF_EVEN) chưa quy định. Hai chế độ khác nhau ở các số như 0.7625 | TC149, TC155 | Các ca API chỉ so số không rơi vào ranh giới: 0.6875 → 0.688 và 0.797457 → 0.797 ở mọi chế độ. *Đã chốt:* HALF_UP cho mọi số thực trong response, kể cả `ratingAverage` (kế hoạch tuần 4, bước 8) |
| ⚠2 | Tên kỹ năng tiếng Việt có dấu xếp theo collation của PostgreSQL (D41) | TC144, TC171 | TC144 so thứ tự "Java" < "Tiếng Anh"; TC171 dùng tên không dấu để so thứ tự, như D41 đã ghi |
