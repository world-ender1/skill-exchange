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
  - Record `NoiseLevel(ten, pLevel, pMiss, pExtra)` với ba hằng `KHONG`, `MAC_DINH`, `CAO` mang ba bộ xác suất của spec 5.4.
- **Sửa:** `.claude/hooks/run-matching-tests.mjs` chạy thêm test `evaluation` khi sửa file trong `evaluation/`.
- **Test:** `SkillCatalogTest`, `SimUserTest`, `NoiseLevelTest`.
- **Ca:** TC203, TC210.
- **Chốt ở bước này:**
  - `NoiseLevel` là record có ba hằng, không phải enum: TC206, TC207 cần mức (1, 0, 0) và (0, 1, 0) ngoài ba mức chuẩn. `ten` của ba hằng là giá trị cột `muc_nhieu` ở D72 (`khong`, `mac_dinh`, `cao`). Xác suất ngoài [0, 1] bị từ chối.
  - `SkillCatalog.V2` là danh mục duy nhất, có `categories()`, `skills()` (mã, tên, nhóm), `relations()`, `nameOf(id)`, `idOf(tên)`; mã hoặc tên không có thì báo lỗi.
  - TC203 so tên nhóm, tên kỹ năng và các cặp với chính file `V2__seed_skills.sql` (test đọc file từ classpath), không chỉ với danh sách chép tay, để bản chép trong `evaluation` không lệch V2 mà không ai biết.
  - `SimUser` từ chối người có lịch ở một lớp mà lớp kia rỗng (D53: việc có khai lịch không có nhiễu); `SimTeach`, `SimLearn` từ chối trình độ, mục tiêu ngoài 1–4.
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
  - Cách rút từng loại trong luồng dữ liệu thật:
    - Hoán vị Zipf: `Collections.shuffle(mã 1..26, random)`, hạng $r$ = vị trí + 1.
    - Kỹ năng dạy: mỗi lần chọn rút một `nextDouble()` nhân tổng $1/r$ của các kỹ năng còn lại, duyệt các kỹ năng còn lại theo thứ tự hạng, lấy kỹ năng đầu tiên mà tổng cộng dồn vượt số đó.
    - Chọn đều không lặp (kỹ năng học, khung giờ): `nextInt(số còn lại)` trên danh sách còn lại, xếp theo mã (kỹ năng) hoặc theo ngày rồi buổi (khung), rồi bỏ phần tử đã chọn.
    - Số lượng: kỹ năng dạy `2 + nextInt(4)`, kỹ năng học `1 + nextInt(3)`, khung `3 + nextInt(7)`. Trình độ thật và mục tiêu: `1 + nextInt(4)`.
    - Mục tiêu: `nextDouble() < 0.3` thì bỏ trống; chỉ rút trình độ mục tiêu khi không bỏ trống.
    - Khai lịch, khai hình thức: `nextDouble() < 0.85`. Hình thức: `nextDouble()` < 0.35 là `ONLINE`, < 0.60 là `OFFLINE`, còn lại `ANY`. Người không khai thì không rút số khung, khung, hình thức.
    - Xáo cuối: `Collections.shuffle(danh sách người, random)`, mã = vị trí + 1.
  - Thứ tự trong danh sách của `SimUser`: kỹ năng dạy và học theo thứ tự đã rút; khung thật và khung tự khai xếp theo ngày rồi buổi.
  - Chi tiết luồng nhiễu, với từng người:
    - Từng kỹ năng dạy theo thứ tự trong danh sách: số thứ nhất `nextDouble() < pLevel` thì lệch; số thứ hai `nextDouble() < 0.5` thì lệch lên.
    - Sau đó 21 số cho 21 khung. Số này rút cho **mọi** người; người không khai lịch bỏ qua kết quả (D53). Khung thật bị bỏ sót khi số < `pMiss`; khung không thật được thêm khi số < `pExtra`.
    - Cuối cùng, người có khai lịch rút `nextInt(số khung thật)` để chọn khung giữ lại. Số này chỉ dùng khi lịch tự khai rỗng.
  - So sánh dùng dấu `<`. Nhờ vậy mức nhiễu cao hơn chỉ thêm thay đổi: lệch hoặc sót ở mức mặc định thì cũng lệch hoặc sót ở mức cao (TC202).
  - `n < 1` bị từ chối (`IllegalArgumentException`).
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
  - `GroundTruthLabeler` nhận `SkillSimilarity` qua constructor (dựng từ `SkillCatalog.V2.relations()`), giống `MatchingEngine`; `label(u, v)` là phương thức của đối tượng.
  - Bước 1:
    - "$v = u$" so theo mã.
    - Không xét "chưa xác thực / bị khóa", vì mọi người giả lập là `ACTIVE`, `STUDENT` (spec 5.4).
    - Chỉ ghép $W_u$ với $T_v$ (hướng $u$ học từ $v$), giống lọc cứng (3) của lõi.
    - "Đã khai lịch" là lịch thật khác rỗng (theo D53, hai lớp cùng rỗng hoặc cùng có khung). "Đã khai hình thức" là `mode` khác `null`.
  - `Relevance.LOAI.rel()` ném `IllegalStateException`: ứng viên bị loại không vào tập lý tưởng. Bước 4 tự đổi thành rel = 0 khi ứng viên có trong danh sách (D55).
  - Thêm ca biên: Kotlin (2) với mục tiêu Java 3 (thấp hơn 1 bậc) là nhãn 1, không phải nhãn 0; $u$ chưa khai lịch thì không bị loại, nhãn tối đa 1.
- **Tự kiểm:** đổi điều kiện nhãn 2 thành "mọi kỹ năng khớp đều đạt mục tiêu" thì TC212 phải đỏ; đọc trình độ tự khai thay vì thật thì TC216, TC222 phải đỏ.
- **Commit:** `feat(evaluation): add ground-truth labeler`

### Bước 4 – Chỉ số (TDD hai lượt; D57–D60; spec 5.3)

- **Tạo:** `RankingMetrics` (hàm thuần trên danh sách rel và tập rel lý tưởng: `relsInList`, `precisionAt5`, `strictPrecisionAt5`, `dcgAt5`, `idcgAt5`, `ndcgAt5`); `UserMetrics` (chỉ số của một người, một dòng `per_user.csv`); `MetricSummary` gộp theo người dùng: hai bản trung bình NDCG@5 (D59), số ca IDCG = 0, số người bị loại, Coverage và hai chỉ số bao phủ (D60).
- **Test:** `RankingMetricsTest`, `MetricSummaryTest`.
- **Ca:** TC224–TC230.
- **Chốt ở bước này:**
  - Hàm chỉ số nhận số nguyên rel (0, 1, 2), không nhận `Relevance`, để test ghi thẳng ví dụ A.3. Ứng viên `LOAI` trong danh sách được đổi thành rel 0 trước khi gọi (D55) bằng `RankingMetrics.relsInList(List<Relevance>)`; TC228 kiểm qua hàm này.
  - Đầu vào là `List<Integer>`. Danh sách: rel của **cả** danh sách ứng viên, theo thứ tự xếp hạng; Precision@5 và DCG@5 chỉ đọc 5 phần tử đầu. Tập lý tưởng: rel của mọi ứng viên trong tập lý tưởng, thứ tự không quan trọng; IDCG@5 xếp giảm dần rồi lấy 5 đầu. rel ngoài 0–2 bị từ chối (`IllegalArgumentException`).
  - `ndcgAt5` từ chối (`IllegalArgumentException`) khi danh sách có nhiều ứng viên rel = $r$ hơn tập lý tưởng, với $r$ = 1 hoặc 2: ứng viên có rel > 0 đã qua Bước 1 thật nên phải có trong tập lý tưởng. Nhờ vậy NDCG@5 ≤ 1, và bước 7 ghép nhầm danh sách với tập lý tưởng thì báo lỗi ngay.
  - Record `UserMetrics.of(userId, listRels, idealRels)` tính chỉ số của một người, có đủ cột của `per_user.csv` (D72): `so_ung_vien` = số phần tử của danh sách; `so_muc_nhan_ly_tuong` = số giá trị rel khác nhau trong tập lý tưởng (0–3). `MetricSummary.of(List<UserMetrics>)` gộp lại. Danh sách truyền vào chỉ gồm người dùng đủ điều kiện (bước 7 lọc); danh sách rỗng bị từ chối.
  - Precision@5 (cả hai bản), Coverage và hai chỉ số bao phủ lấy trung bình trên mọi người dùng đủ điều kiện; số ca IDCG@5 = 0 cũng đếm trên mọi người. Chỉ NDCG@5 có hai bản, vì R24 chỉ nói về NDCG và D72 chỉ có `ndcg_at_5_tat_ca`, `ndcg_at_5_loai_suy_bien`. Q8 của `Can_chot_truoc_tuan_5.md` hỏi cả Precision lẫn NDCG; theo thứ bậc, quyết định D72 đúng, và D59, spec 5.3 đã ghi rõ phạm vi. Bản (2) gồm người có `so_muc_nhan_ly_tuong` ≥ 2; người bị loại gồm cả người có tập lý tưởng rỗng.
  - Không còn ai thuộc bản (2) thì NDCG@5 bản (2) là `NaN`, số người tính = 0: giá trị "không xác định", không bị đọc nhầm thành điểm thấp. Code trả `NaN` bằng một nhánh riêng, không để phép chia 0/0 tự sinh. NFR-04 (không `NaN`) nói về điểm của lõi matching, không áp dụng cho số gộp này.
  - Cộng theo đúng thứ tự danh sách người dùng truyền vào (xếp theo mã), để số thực giống nhau giữa các lần chạy (D64).
- **Tự kiểm:** dùng $\log_2(i)$ thay vì $\log_2(i+1)$ thì TC224 phải đỏ; IDCG tính trên danh sách thay vì tập lý tưởng thì TC226 phải đỏ.
- **Commit:** `feat(evaluation): add ranking metrics`

### Bước 5 – Kiểm định (TDD hai lượt; D62)

- **Tạo:** `PairedStatistics`: `wilcoxon(double[] diffs)` trả `n`, $W^+$, `z`, `p` hoặc kết quả "không có cặp khác nhau"; `bootstrapMeanCi(double[] diffs, int times, Random random)`.
- **Test:** `PairedStatisticsTest`.
- **Ca:** TC231–TC233.
- **Chốt ở bước này:**
  - $\Phi$ tính bằng xấp xỉ hàm `erf` của Abramowitz–Stegun 7.1.26 (sai số dưới $1.5 \times 10^{-7}$), đủ cho dung sai $10^{-3}$ của TC231–TC232; không thêm thư viện thống kê. $p = 2(1 - \Phi(|z|))$ nên sai số tuyệt đối của $p$ cũng dưới $1.5 \times 10^{-7}$; báo cáo ghi $p < 10^{-6}$ thay vì số nhỏ hơn. `PairedStatistics.standardNormalCdf` để mức package cho test kiểm vài giá trị đã biết của $\Phi$.
  - `wilcoxon` trả `Optional<WilcoxonResult>` (record `n`, `wPlus`, `z`, `p`); `Optional.empty()` là "không có cặp khác nhau", nên bước 7 buộc phải xử lý trường hợp này, không có `NaN`. `z` giữ dấu: dương khi $W^+$ lớn hơn kỳ vọng, tức phần tử thứ nhất của hiệu (P trong "P − B1") tốt hơn.
  - **Hạng bằng nhau dùng dung sai $10^{-9}$**, cùng ngưỡng bỏ hiệu 0: xếp giá trị tuyệt đối tăng dần; một nhóm bằng nhau gồm phần tử đầu nhóm và mọi phần tử kế tiếp lệch nó dưới $10^{-9}$ (so với phần tử đầu nhóm, không so nối tiếp). Lý do: trong `double`, `0.6 - 0.4` = 0.19999999999999996 ≠ 0.2, mà hiệu Precision@5 luôn là bội của 0.2; so bằng `==` sẽ tách các hạng bằng nhau và làm sai $W^+$, hiệu chỉnh hạng bằng nhau.
  - Mẫu số $\sqrt{n(n+1)(2n+1)/24 - \sum (t^3 - t)/48}$ luôn dương khi $n \ge 1$ (trường hợp xấu nhất, mọi hiệu bằng nhau, còn $3n(n+1)^2/48$), nên không cần nhánh riêng.
  - Bootstrap dùng **mọi** hiệu, kể cả hiệu bằng 0 (D62 chỉ bỏ hiệu 0 ở Wilcoxon), vì nó lấy mẫu lại người dùng. Thứ tự rút (D64): lần lặp 1..`times`, mỗi lần rút $n$ chỉ số `random.nextInt(n)` liên tiếp, cộng theo thứ tự rút rồi chia $n$. Xếp `times` giá trị trung bình tăng dần; cận dưới là phần tử thứ $\lceil 0.025 \cdot times \rceil$, cận trên thứ $\lceil 0.975 \cdot times \rceil$ (đếm từ 1), tính bằng số nguyên để không lệch vì làm tròn số thực. Trả record `ConfidenceInterval(low, high)`.
  - Hiệu chứa `NaN` hoặc vô cực: cả hai hàm từ chối (`IllegalArgumentException`). Bootstrap với mảng rỗng hoặc `times < 1`: từ chối. Wilcoxon với mảng rỗng: "không có cặp khác nhau".
- **Tự kiểm:** quên hiệu chỉnh hạng bằng nhau thì TC232 (a) phải đỏ.
- **Commit:** `feat(evaluation): add paired statistics`

### Bước 6 – Các phương án (D56, D65–D68; spec 5.2)

- **Tạo:**
  - `WeightSets`: 3 bộ bỏ thành phần (D66), 4 bộ độ nhạy (spec 5.4), có tên khớp `phuong_an` của D72.
  - Enum `ScheduleFitFormula` (`MIN`, `JACCARD`, `TRAN_3`) và `Rescorer`: tính lại ScheduleFit, $D$, MutualScore từ `ScoreBreakdown` của lõi và lịch tự khai của hai bên, dựng `MatchResult` mới, xếp lại từng nhóm bằng `RankingComparator` (D68).
  - `Rankers`: `b0`, `b1`, `random`, `p(weights, formula)`, mỗi hàm trả một `Ranker` (interface: nhận người gọi và danh sách hồ sơ, trả danh sách ứng viên đã xếp) (D56, D65).
- **Test:** `WeightSetsTest`, `RescorerTest`, `RankersTest`.
- **Ca:** TC234–TC243.
- **Chốt ở bước này:**
  - B0, B1 tạo `MatchingEngine` riêng với bộ trọng số (1, 0, 0, 0, 0); B0 dùng `new SkillSimilarity(List.of())`. Một `MatchingEngine` cho mỗi cấu hình, tạo một lần và dùng lại cho mọi người dùng.
  - Thêm interface `Ranker`: `rank(caller, profiles)` trả `List<Long>` mã ứng viên đã xếp, **đủ cả danh sách** (không cắt 5), vì `so_ung_vien` và tỷ lệ có nhãn 2 (D60) đọc cả danh sách. `new Rankers(relations)`; mỗi hàm `b0()`, `b1()`, `random(seed)`, `p(weights, formula)` tạo engine một lần và trả một `Ranker`. Bước 7 gọi mỗi hàm một lần cho mỗi cấu hình, đo latency (D61) quanh `rank`.
  - `p(weights, MIN)` trả đúng kết quả lõi, không qua `Rescorer`: khớp chữ "đúng như lõi trả về" của D56, và latency của P đo đúng lõi (D61). `JACCARD`, `TRAN_3` mới qua `Rescorer`; TC242 (a) vẫn kiểm `Rescorer` với `MIN`.
  - R: danh sách của B1 xếp theo `userId` tăng dần, rồi `Collections.shuffle(ids, new Random(seed + 2_000_000 + caller.userId()))` (D64); `seed` là seed của bộ dữ liệu, Random tạo mới cho từng người gọi. TC239 dùng cùng bộ dữ liệu seed 1, nhiễu mặc định cho cả ba lượt, chỉ đổi seed của R, để "khác thứ tự" không đến từ dữ liệu khác.
  - `Rescorer` tính ScheduleFit cho từng chiều theo đúng vai trò người học, người dạy (ba công thức đều đối xứng nên hai chiều bằng nhau), rồi tính $D$ theo đúng thứ tự cộng của `ScoreCalculator.breakdown`. Không tìm thấy hồ sơ của một ứng viên trong danh sách: lỗi (`IllegalArgumentException`).
  - `WeightSets`: record `Named(phuongAn, weights)`. Bộ mặc định dùng chính `MatchingWeights.DEFAULT`; ba bộ bỏ thành phần ghi số như D66, có test đối chiếu với quy tắc chia đều từ bộ mặc định.
- **Tự kiểm:** `git diff --stat -- backend/src/main/java/com/skillexchange/matching` rỗng; bỏ bước xếp lại sau khi tính lại điểm thì TC243 phải đỏ.
- **Commit:** `feat(evaluation): add baselines and ranker variants`

### Bước 7 – Chạy thực nghiệm và ghi kết quả (D61, D69–D72)

- **Tạo:** `ExperimentConfig` (ma trận chạy), `ExperimentRunner` (các tổ hợp của D70, đo latency theo D61, gọi kiểm định D62), `ResultWriter` (`README.md`, `metrics.csv`, `per_user.csv`, `kiem_dinh.csv`, `log.txt`; dừng nếu thư mục đã có; gọi `git` lấy commit và trạng thái), `EvaluationMain` (tham số `--giai-doan`, `--ten`, `--trong-so`, `--schedule`, `--out` của D71).
- **Test:** `ResultWriterTest`, `ExperimentRunnerTest` (chạy N = 50, một seed vào `@TempDir`).
- **Ca:** TC244–TC246.
- **Chốt ở bước này:**
  - `ExperimentRunner` nhận danh sách quy mô và seed qua tham số, để test chạy bản nhỏ; `EvaluationMain` truyền đúng ma trận của D70.
  - CPU lấy từ biến môi trường `PROCESSOR_IDENTIFIER` (Windows), RAM từ `com.sun.management.OperatingSystemMXBean#getTotalMemorySize`, phiên bản Java từ `Runtime.version()`.
  - Không tìm thấy `git` thì ghi "không rõ" cho commit; giai đoạn báo cáo khi đó cũng dừng (không truy được commit).
  - **Ma trận** là record `ExperimentConfig` (thêm file): giai đoạn, danh sách seed, `mainN` (500), danh sách quy mô (50, 100, 500, 1000), `latencyN` (1000), `latencySeed` (3), số lần bootstrap (2000), bộ trọng số và công thức đã chọn. `ExperimentConfig.chon()` và `baoCao(...)` ghi đúng số của D70; test dựng bản nhỏ (N = 50, một seed).
  - **Ô** = (seed, N, mức nhiễu, phương án). Nhiều mục của D70 cần cùng một ô thì ô tính một lần, mang các phần cần ghi: chỉ số đầy đủ ((a), (b), (c)), chỉ số `_it_khung` ((d)), latency ((e)); `metrics.csv` không có dòng lặp. Thứ tự ghi: seed tăng dần → N tăng dần → mức nhiễu `khong`, `mac_dinh`, `cao` → phương án theo thứ tự thêm vào ở (a), (b), (c), (d), (e). Trong một ô: chỉ số theo thứ tự `chi_so` của D72, rồi latency, rồi `_it_khung`.
  - **Giai đoạn chọn:** N = 500, nhiễu mặc định: `P`, `P_w_ky_nang`, `P_w_lich`, `P_w_uy_tin`, `P_sf_jaccard`, `P_sf_tran3`, chỉ số đầy đủ. Không latency, không `_it_khung`, không kiểm định (`kiem_dinh.csv` chỉ có dòng tên cột).
  - **Giai đoạn báo cáo:** (a) `B0`, `B1`, `R`, `P`, 3 bộ bỏ thành phần; **cấu hình đã chọn chạy thành hai phương án riêng** (người dùng chốt 2026-11-13): bộ trọng số khác mặc định thì thêm `P_w_<x>` (với `MIN`), công thức khác `MIN` thì thêm `P_sf_<y>` (với bộ mặc định); không chạy tổ hợp, vì D69 chọn riêng từng chiều và tổ hợp chưa được so ở seed chọn. (b) `B0`, `B1`, `R`, `P` ở từng N. (c) `B1`, `P`, 3 bộ bỏ thành phần ở 3 mức nhiễu. (d) `P`, `P_sf_jaccard`, `P_sf_tran3`: chỉ ghi chỉ số `_it_khung`, vì seed báo cáo chỉ dùng cho cấu hình mặc định và cấu hình đã chọn (spec 5.4); phương án nào là cấu hình đã chọn thì có thêm chỉ số đầy đủ từ (a). (e) `B0`, `B1`, `R`, `P` (các phương án có ở N = 1000, như TC249), N = `latencyN`, seed `latencySeed`.
  - `per_user.csv` chỉ có dòng của ô có chỉ số đầy đủ. Người dùng đủ điều kiện = có ít nhất một kỹ năng muốn học (D60).
  - **Chỉ số `_it_khung`:** nhóm $u$ có lịch tự khai 1–3 khung: `precision_at_5_it_khung`, `precision_at_5_nghiem_ngat_it_khung`, `ndcg_at_5_tat_ca_it_khung`, `ndcg_at_5_loai_suy_bien_it_khung` (hai bản như D59). Nhóm rỗng thì `NaN`, `so_nguoi_tinh` = 0 (nhánh riêng như D59). Chỉ số phụ **gộp mọi vị trí của mọi người dùng** (người dùng chốt 2026-11-13): `ty_le_top5_it_khung` = số vị trí trong top 5 do ứng viên khai 1–3 khung chiếm / số vị trí có người trong top 5; `ty_le_danh_sach_it_khung` = như vậy trên cả danh sách; không có vị trí nào thì `NaN`; `so_nguoi_tinh` = số người dùng đủ điều kiện.
  - **Latency (D61):** mỗi phương án một lượt làm nóng trên mọi người dùng, rồi một lượt đo `System.nanoTime()` quanh `Ranker.rank`. p95 là phần tử thứ $\lceil 95n/100 \rceil$ (đếm từ 1) sau khi xếp tăng dần, tính bằng số nguyên như bootstrap. Đơn vị ms; `so_nguoi_tinh` = số người được đo. Tổng số ứng viên của lượt đo ghi vào `log.txt` để JIT không bỏ được lời gọi.
  - **Kiểm định (D62):** người dùng của N = `mainN`, nhiễu mặc định, gộp mọi seed báo cáo theo thứ tự seed rồi mã, chỉ người thuộc bản (2). Hiệu = `P` − phương án so sánh. **Mỗi phép so sánh một `new Random(3_000_000)` mới**, để kết quả không phụ thuộc thứ tự các phép. Cột `kiem_dinh.csv`: `n, muc_nhieu, seeds, phuong_an, so_voi, chi_so, so_cap, hieu_trung_binh, ci_thap, ci_cao, n_wilcoxon, w_cong, z, p, ghi_chu`; `seeds` ghi `3;4;5`; `chi_so` ∈ {`ndcg_at_5`, `precision_at_5_nghiem_ngat`} như cột của `per_user.csv`. Thứ tự dòng: `so_voi` lần lượt `B1`, `P_bo_level`, `P_bo_schedule`, `P_bo_mode`; trong mỗi phép, `ndcg_at_5` rồi `precision_at_5_nghiem_ngat`. Không có cặp khác nhau: `n_wilcoxon` = 0, `w_cong`, `z`, `p` để trống, `ghi_chu` = `khong_co_cap_khac_nhau`. Không có người nào: các cột số để trống, `ghi_chu` = `khong_co_nguoi_tinh`.
  - **CSV:** UTF-8, dấu phẩy, dòng đầu là tên cột; mọi giá trị là ASCII không chứa dấu phẩy nên không cần BOM hay dấu ngoặc. Số thực ghi bằng `Double.toString` (dấu chấm, đủ chữ số để đọc lại đúng giá trị, có thể dạng `1.0E-7`), không làm tròn; `NaN` ghi `NaN`; số đếm (`so_ca_idcg_0`, `so_nguoi_bi_loai`) ghi số nguyên. Excel cài tiếng Việt dùng dấu phẩy thập phân, nên mở bằng Data → From Text/CSV, dấu tách là dấu phẩy.
  - **Thư mục:** `--out` là chính thư mục của lượt chạy (thay `thuc_nghiem/ket_qua/<ngày>_<tên>/`, đúng chữ "thư mục kết quả" của TC244). Mặc định tính từ `backend/`: `../thuc_nghiem/ket_qua/<yyyy-MM-dd>_<tên>/`, ngày của máy. Thư mục cha chưa có thì tạo; thư mục lượt chạy tạo bằng `Files.createDirectory`, đã có thì báo lỗi và không ghi gì (TC244).
  - **Tham số:** `--giai-doan` (`chon` | `bao-cao`) và `--ten` bắt buộc; `--ten` chỉ gồm chữ cái không dấu, số, `-`, `_`. `--trong-so` (`mac_dinh` | `ky_nang` | `lich` | `uy_tin`) và `--schedule` (`min` | `jaccard` | `tran3`) bắt buộc ở `bao-cao`, không được dùng ở `chon`. Tham số lạ, thiếu giá trị hoặc lặp: báo lỗi, mã thoát 1, không tạo thư mục.
  - **Git:** record `GitInfo(commit, known, dirty)`, đọc bằng `git rev-parse --short HEAD` và `git status --porcelain -- .` trong `backend/`; lệnh lỗi hoặc không có `git` thì commit "không rõ". Giai đoạn báo cáo kiểm **trước khi tạo thư mục**: có thay đổi hoặc không rõ commit thì dừng, mã thoát 1 (TC247). Test truyền `GitInfo` giả, không phụ thuộc trạng thái repo.
  - **Thứ tự:** kiểm tham số → kiểm git (giai đoạn báo cáo) → tạo thư mục → chạy. CSV và `log.txt` ghi dần trong lúc chạy; `README.md` ghi lúc đóng, kể cả khi lượt chạy hỏng (trạng thái "hỏng" kèm lý do, D72). `log.txt` có thời gian nên TC246 không so file này.
  - `README.md`: tên, trạng thái, lệnh, giai đoạn, seed, cấu hình đã chọn (giai đoạn báo cáo), commit, thay đổi chưa commit trong `backend/` (có / không / không rõ), CPU, RAM, Java, thời điểm bắt đầu, thời gian chạy; giai đoạn báo cáo thêm ghi chú latency của D61 (B0, B1, R chạy qua cùng lõi với P).
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
