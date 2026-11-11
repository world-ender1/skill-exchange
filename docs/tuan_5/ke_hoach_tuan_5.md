# KẾ HOẠCH TUẦN 5

Lập ngày 2026-11-11. Áp dụng `docs/tuan_5/Quyet_dinh_tuan_5.md` (D52–D77) và `docs/tuan_5/kiem_thu/ca_kiem_thu.md` (64 ca, TC201–TC264).

Quy ước chung:
- Module thực nghiệm ở `backend/src/main/java/com/skillexchange/evaluation/`, test ở `backend/src/test/java/com/skillexchange/evaluation/`.
- **Bước 1–7 là Java thuần:** JUnit thuần, không `@SpringBootTest`, không import `org.springframework`, không dùng DB (D71). Chạy nhanh bằng `.\mvnw.cmd -q -o test "-Dtest=com.skillexchange.evaluation.*Test"`.
- **Không sửa lõi `matching/`** (D68). Sau mỗi bước 1–8: `git diff --stat -- backend/src/main/java/com/skillexchange/matching` phải rỗng.
- **Bước 3–5 làm TDD hai lượt** như lõi tuần 4: lượt TEST viết test, chạy thấy đỏ, người học đọc từng số mong đợi; lượt CODE viết code cho xanh, không sửa test. Gọi `/buoc-tuan` cho các bước này thì skill dừng sau lượt TEST.
- Test tích hợp (bước 9) kế thừa `IntegrationTestBase`. Mỗi test có `@DisplayName("TCxxx ...")`.
- Sau mỗi bước: `git diff --stat -- backend/src/test` để chắc test đã duyệt không bị sửa; `.\mvnw.cmd test` xanh toàn bộ, kể cả test tuần 3–4.

## 1. Việc làm trước (không phải code, tự làm)

| Trước bước | Việc |
| --- | --- |
| 1 | Đọc `Quyet_dinh_tuan_5.md` và spec 5.1–5.4. Tự gán nhãn trên giấy cho $v_1$–$v_9$ ở spec A.3, rồi mới xem cột "Nhãn" |
| 4 | Tự tính tay TC224, TC226, TC229 (DCG, IDCG, NDCG) |
| 8 | Commit hết thay đổi trong `backend/` (giai đoạn báo cáo dừng nếu còn thay đổi, D71). Ghi lại CPU, RAM của máy |
| 9 | Docker đang chạy, DB `skillexchange_test` đã có (D39). Đọc lại `skill_categories`, `skills`, `skill_relations` trong `V1__init.sql` và `V2__seed_skills.sql` |

## 2. Các bước

### Bước 1 – Khung `evaluation` và danh mục (D53, D63, D71)

- **Tạo** trong `evaluation/`:
  - `SkillCatalog`: 6 nhóm, 26 kỹ năng, 9 cặp liên quan chép từ `V2__seed_skills.sql`; trả `List<SkillRelation>` để dựng `SkillSimilarity` của lõi.
  - Record `SimTeach(skillId, trueLevel, declaredLevel)`, `SimLearn(skillId, Integer target)`, `SimUser(id, teach, learn, trueSlots, declaredSlots, mode)`; `SimUser.toMatchProfile(catalog)` dựng `MatchProfile` từ giá trị **tự khai**, `ACTIVE`, `STUDENT`, Reputation 0.5.
  - Enum `NoiseLevel` (`KHONG`, `MAC_DINH`, `CAO`) mang ba xác suất của spec 5.4.
- **Sửa:** `.claude/hooks/run-matching-tests.mjs` chạy thêm test `evaluation` khi sửa file trong `evaluation/`.
- **Test:** `SkillCatalogTest`, `SimUserTest`.
- **Ca:** TC203, TC210.
- **Chốt ở bước này:**
  - Mã kỹ năng trong dữ liệu giả lập là 1..26 theo thứ tự trong V2. Không cần khớp `id` của DB, vì `evaluation` không đọc DB.
  - Hình thức học chỉ có một trường (`null` = chưa khai), vì không có nhiễu và giống nhau ở hai lớp (D53). Chưa khai lịch thì `trueSlots`, `declaredSlots` đều rỗng; người có khai luôn có ít nhất 1 khung thật.
  - Các danh sách trong record là `List` có thứ tự cố định (D64); chỉ `toMatchProfile` mới đổi lịch sang `Set` cho lõi.
- **Tự kiểm:** `grep -r "org.springframework" backend/src/main/java/com/skillexchange/evaluation` không ra gì.
- **Commit:** `feat(evaluation): add simulation model and skill catalog`

### Bước 2 – Bộ sinh dữ liệu (D53, D63, D64; spec 5.4)

- **Tạo:** `SyntheticDataGenerator.generate(int n, long seed, NoiseLevel noise)` trả `List<SimUser>` xếp theo mã.
- **Test:** `SyntheticDataGeneratorTest`.
- **Ca:** TC201, TC202, TC204–TC209.
- **Chốt ở bước này:**
  - Thứ tự rút số của luồng dữ liệu thật (`new Random(seed)`): hoán vị Zipf của 26 kỹ năng trước; sau đó từng người theo thứ tự sinh: số kỹ năng dạy, các kỹ năng dạy, trình độ thật từng kỹ năng, số kỹ năng học, các kỹ năng học, mục tiêu từng kỹ năng, có khai lịch không, số khung, các khung, có khai hình thức không, hình thức; cuối cùng xáo thứ tự để gán mã.
  - Luồng nhiễu (`new Random(seed + 1_000_000)`) duyệt người theo mã tăng dần; mỗi kỹ năng dạy rút **hai** số (có lệch không, lệch lên hay xuống), mỗi khung trong 21 khung theo thứ tự ngày rồi buổi rút **một** số, và mỗi người có khai lịch rút thêm **một** số để chọn khung giữ lại. Số được rút kể cả khi xác suất bằng 0 hoặc khi không cần dùng (giải quyết ⚠1 của bộ ca), để ba mức nhiễu dùng chung một dãy số.
  - Ở biên (trình độ 1 hoặc 4), số "lên hay xuống" vẫn được rút nhưng bị bỏ qua, hướng lệch là hướng duy nhất có thể.
  - Không rút số ngẫu nhiên trong lúc duyệt `Set` (D64).
- **Tự kiểm:** chạy `SyntheticDataGeneratorTest` hai lần bằng hai lệnh `mvnw` riêng (hai lần khởi động JVM), so dấu vân tay của bộ N = 100, seed 1 mà test in ra log: phải giống nhau. TC201 chạy trong một JVM nên không tự bắt được lỗi thứ tự của `Set.copyOf`.
- **Commit:** `feat(evaluation): add synthetic data generator`

### Bước 3 – Gán nhãn (TDD hai lượt; D53–D55; spec 5.1)

- **Tạo:** enum `Relevance` (`LOAI`, `NHAN_0`, `NHAN_1`, `NHAN_2`, có `rel()` trả 0, 1, 2; `LOAI` không có rel); `GroundTruthLabeler.label(SimUser u, SimUser v)`.
- **Test:** `GroundTruthLabelerTest`.
- **Ca:** TC211–TC223.
- **Chốt ở bước này:**
  - Labeler chỉ đọc giá trị thật (trình độ thật, lịch thật) cùng kỹ năng, mục tiêu, hình thức. Không gọi `ScoreCalculator`, `HardFilter`, LevelFit hay bất kỳ hàm điểm nào (spec 5.1). Được dùng `SkillSimilarity` của lõi, vì đây là tra danh mục, không phải hàm điểm.
  - TC223 kiểm Bước 1 bằng code độc lập viết trong test, như TC141 của tuần 4.
- **Tự kiểm:** đổi điều kiện nhãn 2 thành "mọi kỹ năng khớp đều đạt mục tiêu" thì TC212 phải đỏ; đọc trình độ tự khai thay vì thật thì TC216, TC222 phải đỏ.
- **Commit:** `feat(evaluation): add ground-truth labeler`

### Bước 4 – Chỉ số (TDD hai lượt; D57–D60; spec 5.3)

- **Tạo:** `RankingMetrics` (hàm thuần trên danh sách rel và tập rel lý tưởng: `precisionAt5`, `strictPrecisionAt5`, `dcgAt5`, `idcgAt5`, `ndcgAt5`); `MetricSummary` gộp theo người dùng: hai bản trung bình (D59), số ca IDCG = 0, số người bị loại, Coverage và hai chỉ số bao phủ (D60).
- **Test:** `RankingMetricsTest`, `MetricSummaryTest`.
- **Ca:** TC224–TC230.
- **Chốt ở bước này:** hàm chỉ số nhận số nguyên rel (0, 1, 2), không nhận `Relevance`, để test ghi thẳng ví dụ A.3. Ứng viên `LOAI` trong danh sách được đổi thành rel 0 trước khi gọi (D55).
- **Tự kiểm:** dùng $\log_2(i)$ thay vì $\log_2(i+1)$ thì TC224 phải đỏ; IDCG tính trên danh sách thay vì tập lý tưởng thì TC226 phải đỏ.
- **Commit:** `feat(evaluation): add ranking metrics`

### Bước 5 – Kiểm định (TDD hai lượt; D62)

- **Tạo:** `PairedStatistics`: `wilcoxon(double[] diffs)` trả `n`, $W^+$, `z`, `p` hoặc kết quả "không có cặp khác nhau"; `bootstrapMeanCi(double[] diffs, int times, Random random)`.
- **Test:** `PairedStatisticsTest`.
- **Ca:** TC231–TC233.
- **Chốt ở bước này:** $\Phi$ tính bằng xấp xỉ hàm `erf` của Abramowitz–Stegun 7.1.26 (sai số dưới $1.5 \times 10^{-7}$), đủ cho dung sai $10^{-3}$ của TC231–TC232; không thêm thư viện thống kê.
- **Tự kiểm:** quên hiệu chỉnh hạng bằng nhau thì TC232 (a) phải đỏ.
- **Commit:** `feat(evaluation): add paired statistics`

### Bước 6 – Các phương án (D56, D65–D68; spec 5.2)

- **Tạo:**
  - `WeightSets`: 3 bộ bỏ thành phần (D66), 4 bộ độ nhạy (spec 5.4), có tên khớp `phuong_an` của D72.
  - Enum `ScheduleFitFormula` (`MIN`, `JACCARD`, `TRAN_3`) và `Rescorer`: tính lại ScheduleFit, $D$, MutualScore từ `ScoreBreakdown` của lõi và lịch tự khai của hai bên, dựng `MatchResult` mới, xếp lại từng nhóm bằng `RankingComparator` (D68).
  - `Rankers`: `b0`, `b1`, `random`, `p(weights, formula)`, mỗi hàm nhận người gọi và danh sách hồ sơ, trả danh sách ứng viên đã xếp (D56, D65).
- **Test:** `WeightSetsTest`, `RescorerTest`, `RankersTest`.
- **Ca:** TC234–TC243.
- **Chốt ở bước này:** B0, B1 tạo `MatchingEngine` riêng với bộ trọng số (1, 0, 0, 0, 0); B0 dùng `new SkillSimilarity(List.of())`. Một `MatchingEngine` cho mỗi cấu hình, tạo một lần và dùng lại cho mọi người dùng.
- **Tự kiểm:** `git diff --stat -- backend/src/main/java/com/skillexchange/matching` rỗng; bỏ bước xếp lại sau khi tính lại điểm thì TC243 phải đỏ.
- **Commit:** `feat(evaluation): add baselines and ranker variants`

### Bước 7 – Chạy thực nghiệm và ghi kết quả (D61, D69–D72)

- **Tạo:** `ExperimentRunner` (các tổ hợp của D70, đo latency theo D61, gọi kiểm định D62), `ResultWriter` (`README.md`, `metrics.csv`, `per_user.csv`, `kiem_dinh.csv`, `log.txt`; dừng nếu thư mục đã có; gọi `git` lấy commit và trạng thái), `EvaluationMain` (tham số `--giai-doan`, `--ten`, `--trong-so`, `--schedule`, `--out` của D71).
- **Test:** `ResultWriterTest`, `ExperimentRunnerTest` (chạy N = 50, một seed vào `@TempDir`).
- **Ca:** TC244–TC246.
- **Chốt ở bước này:**
  - `ExperimentRunner` nhận danh sách quy mô và seed qua tham số, để test chạy bản nhỏ; `EvaluationMain` truyền đúng ma trận của D70.
  - CPU lấy từ biến môi trường `PROCESSOR_IDENTIFIER` (Windows), RAM từ `com.sun.management.OperatingSystemMXBean#getTotalMemorySize`, phiên bản Java từ `Runtime.version()`.
  - Không tìm thấy `git` thì ghi "không rõ" cho commit; giai đoạn báo cáo khi đó cũng dừng (không truy được commit).
- **Tự kiểm:** chạy `--giai-doan chon --ten thu --out <thư mục tạm>` bằng tay, mở `metrics.csv` bằng Excel; chạy lại cùng `--out` thì bị từ chối.
- **Commit:** `feat(evaluation): add experiment runner`

### Bước 8 – Chạy thật và lưu kết quả (D69, D72; `docs/To_chuc_ket_qua.md`)

- **Tạo:** `.claude/hooks/protect-results.mjs` (PreToolUse cho Write, Edit, Bash): chặn ghi vào file đã có trong `thuc_nghiem/ket_qua/` và lệnh xóa, di chuyển thư mục này; đăng ký trong `.claude/settings.json`. Thư mục `thuc_nghiem/ket_qua/`.
- **Chạy:**
  1. Giai đoạn chọn: `--giai-doan chon --ten chon-cau-hinh`.
  2. Đọc NDCG@5 bản (2), ghi cấu hình đã chọn vào D69 (bổ sung), commit.
  3. Gắn tag `v1.0-thuc-nghiem` cho commit đó.
  4. Giai đoạn báo cáo: `--giai-doan bao-cao --ten bao-cao --trong-so <…> --schedule <…>`.
  5. Tạo `docs/ket_qua.md` theo mẫu `docs/To_chuc_ket_qua.md` §3.1: mỗi con số kèm file nguồn, commit, ngày.
- **Ca:** TC247–TC249.
- **Tự kiểm:** con số trong `docs/ket_qua.md` khớp `metrics.csv`; `git tag` có `v1.0-thuc-nghiem`.
- **Commit:** `chore: add hook protecting experiment results`; `docs: record chosen configuration (D69)`; `docs: add experiment results`. Thư mục kết quả thô commit cùng `docs/ket_qua.md`.

### Bước 9 – API danh mục kỹ năng (D73–D75; TK 4.9)

- **Tạo** trong package `admin`:
  - `AdminSkillController` (8 API của TK 4.9), `AdminSkillService`.
  - DTO: `AdminSkillResponse`, `CategoryRequest`, `CreateSkillRequest`, `UpdateSkillRequest`, `SkillRelationRequest`, `SkillRelationResponse`.
  - `SkillRelationRepository` (`@Repository`, `JdbcTemplate`, SQL thuần).
- **Sửa:** `skill/SkillRepository`, `skill/SkillCategoryRepository` (tìm theo tên không phân biệt hoa thường); `skill/Skill`, `skill/SkillCategory` nếu cần setter; `common/GlobalExceptionHandler` (vi phạm `UNIQUE` của DB → 409, D74).
- **Test:** `admin/AdminSkillApiTest`; mỗi test trả danh mục về như V2 sau khi chạy.
- **Ca:** TC250–TC261.
- **Chốt ở bước này:**
  - `skill_relations` đọc ghi bằng `JdbcTemplate` thay vì Entity: bảng chỉ có hai cột khóa, và `MatchingDataRepository` đã đọc bảng này bằng SQL thuần, nên không có hai cách đọc cùng một bảng. Lệnh thêm dùng `LEAST/GREATEST` như V2.
  - Câu `detail` (giải quyết ⚠2 của bộ ca): "Tên kỹ năng đã tồn tại", "Tên nhóm kỹ năng đã tồn tại", "Không tìm thấy kỹ năng", "Không tìm thấy nhóm kỹ năng", "Không tìm thấy quan hệ liên quan", "Hai kỹ năng phải khác nhau", "Hai kỹ năng phải cùng nhóm", "Hai kỹ năng đã được đánh dấu liên quan", "Kỹ năng đang có quan hệ liên quan, hãy gỡ quan hệ trước khi đổi nhóm".
  - So tên không phân biệt hoa thường bằng `lower(name)` trong SQL; `name` mới cắt bằng `strip()` (D15).
- **Tự kiểm:** ghi lệnh thử vào `hoc_vibe_coding/http/tuan_5.http`; đăng nhập admin, gọi `GET /api/admin/skills` thấy 26 kỹ năng; sinh viên gọi nhận 403.
- **Commit:** `feat(admin): add skill catalog management API`

### Bước 10 – Giao diện A01 (D76; TK 6.1, 6.2)

- **Sửa:** `src/api/types.ts` (kiểu của D73), `src/App.tsx` (route `/admin/skills`), `src/pages/LoginPage.tsx` (sau đăng nhập đọc `/api/me`, `ADMIN` vào `/admin/skills`), `src/components/Layout.tsx` (menu theo vai trò).
- **Tạo:** `src/pages/AdminSkillsPage.tsx`, `src/auth/RequireAdmin.tsx` (sinh viên bị chuyển về `/profile`).
- **Ca:** TC262–TC264 (thủ công).
- **Tự kiểm:** `npm run build`, `npm run lint` không lỗi; màn hình rộng 360 px.
- **Commit:** `feat(frontend): add admin skill catalog page`

## 3. Đối chiếu ca kiểm thử

Đã đối chiếu 64 ca: mọi ca đều thuộc một bước.

| Bước | Ca |
| --- | --- |
| 1 | TC203, TC210 |
| 2 | TC201, TC202, TC204–TC209 |
| 3 | TC211–TC223 |
| 4 | TC224–TC230 |
| 5 | TC231–TC233 |
| 6 | TC234–TC243 |
| 7 | TC244–TC246 |
| 8 | TC247–TC249 (thủ công) |
| 9 | TC250–TC261 |
| 10 | TC262–TC264 (thủ công) |

## 4. Sau bước 10 – Kiểm tra cả tuần

- `.\mvnw.cmd test` xanh toàn bộ; `npm run build`, `npm run lint` không lỗi.
- Lõi `matching/` không đổi so với cuối tuần 4; `evaluation/` không import `org.springframework`.
- Truy vết ca → test; làm hỏng code để chắc test đỏ (các mục "Tự kiểm").
- Đối chiếu code với D52–D77; viết `docs/tuan_5/kiem_thu/README.md`.
- Nội dung báo cáo tuần 5 có mục "Điều chỉnh so với báo cáo trước" (D77): định nghĩa nhãn 2 khác Bảng 4.11 của báo cáo tuần 3, câu "chứng minh" của proposal, hạn chế của cách đo P (D56) và cách chia trọng số (D66).
