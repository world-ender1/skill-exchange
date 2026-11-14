# Kiểm thử tuần 5 – Thực nghiệm thuật toán, quản trị danh mục kỹ năng

Mục đích: chứng minh code tuần 5 làm đúng 64 ca kiểm thử viết trước từ tài liệu (`ca_kiem_thu.md`), bộ test **bắt được lỗi thật** ở bộ gán nhãn, chỉ số, kiểm định và API danh mục, và số trong `docs/ket_qua.md` đúng với dữ liệu thô của lượt chạy thực nghiệm.

## Các file

| File | Nội dung |
| --- | --- |
| `ca_kiem_thu.md` | 64 ca viết từ tài liệu trước khi code (TC201–TC264) |
| `backend/src/test/java/com/skillexchange/evaluation/**`, `admin/AdminSkillApiTest.java` | Test tự động: mỗi test có `@DisplayName("TCxxx ...")`, `"Dxx ..."` hoặc `"Spec x.y ..."` |
| `ket_qua.txt` | Trích kết quả lần chạy gần nhất (`Tests run`, `FAIL`, `ERROR`) |
| `anh/` | 5 ảnh chụp A01 cho TC262, TC264 |

## Cách chạy lại

```powershell
# C:\DACN, Docker đang chạy; DB test tạo một lần (D39):
docker compose up -d
cd backend
.\mvnw.cmd test | Select-String "Tests run|FAIL|ERROR" | Out-File -Encoding utf8 ..\docs\tuan_5\kiem_thu\ket_qua.txt

# Chỉ test thực nghiệm (không cần DB, vài giây):
.\mvnw.cmd -q -o test "-Dtest=com.skillexchange.evaluation.*Test"

# Hook chặn ghi đè kết quả (TC248), từ C:\DACN:
node .claude/hooks/thu-protect-results.mjs
```

Trong `ket_qua.txt` có 4 dòng `Error starting ApplicationContext`: đó là các test TC81 của tuần 3 **cố ý** khởi động ứng dụng thiếu hoặc sai `ADMIN_EMAIL`/`ADMIN_PASSWORD` để kiểm ứng dụng dừng lại. Không phải lỗi.

Hook `.claude/hooks/run-matching-tests.mjs` tự chạy test `evaluation` mỗi khi AI sửa file trong `evaluation/` (từ bước 1).

Frontend: `cd frontend; npm run build; npm run lint`.

## Thiết kế kiểm thử

| Nhóm ca | Số ca | Loại | Lớp test |
| --- | --- | --- | --- |
| A. Bộ sinh dữ liệu | 10 | Unit | `SkillCatalogTest`, `SimUserTest`, `NoiseLevelTest`, `SyntheticDataGeneratorTest` |
| B. Gán nhãn | 13 | Unit | `GroundTruthLabelerTest` |
| C. Chỉ số | 7 | Unit | `RankingMetricsTest`, `MetricSummaryTest` |
| D. Kiểm định | 3 | Unit | `PairedStatisticsTest` |
| E. Phương án | 10 | Unit | `WeightSetsTest`, `RescorerTest`, `RankersTest` |
| F. Chạy và lưu kết quả | 6 | Unit; TC247 thêm phần thủ công; TC248, TC249 thủ công | `ResultWriterTest`, `ExperimentRunnerTest`, `EvaluationMainTest` |
| G. API danh mục kỹ năng | 12 | Tích hợp | `AdminSkillApiTest` |
| H. Giao diện A01 | 3 | Thủ công | Trình duyệt, ảnh trong `anh/` |

| Loại test | Số lượng (thêm ở tuần 5) | Cách làm |
| --- | --- | --- |
| Unit test thực nghiệm | 124 test trong 14 lớp | JUnit thuần, không Spring, không DB (D71); gọi lõi `matching` nhưng không sửa lõi (D68). Số mong đợi viết bằng biểu thức, ví dụ `2.5 / Math.sqrt(13.75)` cho $z$ của TC231. TC203 đọc chính file `V2__seed_skills.sql` để danh mục chép trong `evaluation` không lệch V2. TC223 gán nhãn 40 000 cặp sinh ngẫu nhiên và so Bước 1 với một bản cài đặt viết riêng trong test. TC201, TC202 kiểm tái lập theo seed (D64) |
| Test tích hợp (`@SpringBootTest` + MockMvc) | 17 test trong `AdminSkillApiTest` | Gọi 8 API của TK 4.9 trên DB `skillexchange_test`. `CatalogSnapshot` chụp `skill_categories`, `skills`, `skill_relations` trước mỗi test và trả về như V2 sau test (D39). Id của kỹ năng lấy từ DB, không ghi cứng (TC258, TC260) |
| Kiểm thủ công | 6 ca | TC247 (phần chạy thật), TC248, TC249, TC262–TC264 |

Bước 3–5 (gán nhãn, chỉ số, kiểm định) viết theo TDD hai lượt: viết test, chạy thấy đỏ đúng lý do, người học đọc từng số mong đợi, rồi mới viết code cho xanh, không sửa test.

Ngoài TC còn 66 test ghi mã quyết định tuần 5 (D53–D74) cho phần TC chưa phủ, và 1 test ghi "Spec 5.3" (chỉ tính 5 vị trí đầu).

Một test cũ đổi mong đợi: `LoginTest.adminTokenHasAdminRole` (D14, tuần 3) trước mong `GET /api/admin/skills` trả 404 vì đường dẫn chưa có; bước 9 tạo API nên đổi thành 200 (người học duyệt 2026-11-14). Mục đích của test không đổi.

## Truy vết ca → test

| Ca | Trạng thái | Ghi chú |
| --- | --- | --- |
| 59 ca tự động: TC201–TC247, TC250–TC261 | **Có test** | Mỗi ca có ít nhất một test mang mã `TCxxx` trong `@DisplayName` (74 test); không test nào ghi mã ca không tồn tại |
| TC247 | Có test, thêm phần thủ công | 2 test trong `EvaluationMainTest` giả trạng thái git; phần thủ công sửa thật một file trong `backend/` |
| TC248, TC249 | Thủ công | Kết quả ở phần "Kiểm thủ công" |
| TC262–TC264 | Thủ công | Kết quả ở phần "Kiểm thủ công" |

## Kết quả

**429/429 test đạt** (lần chạy 2026-11-14, PostgreSQL 17.11, Spring Boot 4.1.1), gồm toàn bộ 288 test của tuần 3–4. Tuần 5 thêm 141 test: 124 unit, 17 tích hợp. 124 test `evaluation` chạy tổng cộng khoảng 7 giây; `AdminSkillApiTest` khoảng 32 giây; cả bộ khoảng 2 phút, phần lớn là test tích hợp.

Frontend: `npm run build` và `npm run lint` không lỗi.

Các điều kiện của cả tuần (kế hoạch tuần 5, mục 4), kiểm 2026-11-14:

| Điều kiện | Kết quả |
| --- | --- |
| Lõi `matching/` không đổi so với cuối tuần 4 | `git diff 3afe4a8 HEAD -- backend/src/main/java/com/skillexchange/matching` rỗng |
| `evaluation/` không import `org.springframework` | `grep -r` trên cả `main` và `test`: không ra dòng nào |
| Không thêm migration | Vẫn chỉ có `V1__init.sql`, `V2__seed_skills.sql`; không chạy lại kiểm thử lược đồ tuần 2 |

## Làm hỏng code, test phải đỏ

Chạy lại 2026-11-14 bằng script: thay đúng một đoạn code, chạy lớp test liên quan, đọc test đỏ từ báo cáo surefire, rồi `git restore`. Script xóa báo cáo cũ trước mỗi lượt và có một lượt đối chứng không sửa gì (0 test đỏ), để không đọc nhầm kết quả của lần chạy trước (xem `docs/harness-log.md`, 2026-11-14). Các dòng bước 3–6 là phần "Tự kiểm" của kế hoạch; bước 9, 10 thêm cho đủ.

| Bước | Làm hỏng | Test đỏ |
| --- | --- | --- |
| 3 | Nhãn 2 đòi **mọi** kỹ năng khớp (trùng chính xác hoặc liên quan) đạt mục tiêu (đột biến của kế hoạch) | TC212, D54 |
| 3 | Nhãn 2 đòi mọi kỹ năng **trùng chính xác** đạt mục tiêu | Lượt đầu **không test nào đỏ** (lỗ hổng, xem ghi chú dưới bảng). Sau khi thêm test: D54 |
| 3 | Nhãn 2 chỉ xét kỹ năng trùng chính xác gặp đầu tiên | D54, vế (b) |
| 3 | Gán nhãn đọc trình độ tự khai thay vì thật | TC216, TC222 |
| 4 | DCG chia $\log_2(i)$ thay vì $\log_2(i+1)$ | 11 test, gồm TC224, TC225, TC226, TC227 (2 test), TC228, TC229, D55, D57, D59, Spec 5.3 |
| 4 | IDCG tính trên danh sách thay vì tập lý tưởng | TC226 |
| 5 | Wilcoxon bỏ phần hiệu chỉnh phương sai khi có hạng bằng nhau | TC232 (a), D62 (2 test) |
| 6 | `Rescorer` không xếp lại sau khi tính lại điểm | TC243 (3 test) |
| 9 | So trùng tên kỹ năng có phân biệt hoa thường (bỏ `LOWER`) | TC254, TC256 |
| 9 | Bỏ luật "đổi nhóm khi còn quan hệ → 409" (R42) | TC257 |
| 9 | `DELETE` quan hệ bỏ `LEAST/GREATEST`, chỉ nhận đúng thứ tự đã lưu | TC260, TC250 |
| 9 | `GET /api/admin/skills` bỏ kỹ năng bị ẩn | TC251, TC255 |
| 10 | Bỏ `'DELETE'` khỏi kiểu `method` của `api/client.ts` | Không có test frontend; `tsc -b` báo `TS2322` ở `AdminSkillsPage.tsx` |

**Lỗ hổng ở D54, đã bịt.** D54 ghi nhãn 2 cần "ít nhất một" kỹ năng trùng chính xác đạt mục tiêu. TC212 kiểm điều này bằng một kỹ năng trùng chính xác đạt mục tiêu (Java 3) cộng một kỹ năng **liên quan** không đạt (MySQL 1 cho SQL). Không test nào có **hai kỹ năng trùng chính xác**, một đạt một không, nên bản sai "mọi kỹ năng trùng chính xác phải đạt" qua được cả 16 test của `GroundTruthLabelerTest`. TC223 cũng không bắt: nó chỉ kiểm "mọi cặp nhãn 2 có một kỹ năng trùng chính xác", điều bản sai vẫn giữ.

Đã thêm test `d54_oneExactSkillMeetingTargetIsEnoughAmongExactMatches`: $u$ của A.3 (muốn học Java mục tiêu 3, SQL mục tiêu 2), $v$ dạy Java (3) và SQL (1), lịch và hình thức như $v_1$, phải ra nhãn 2. Vế (b) đảo thứ tự học của $u$ (SQL trước Java) để kỹ năng không đạt được xét trước, bắt thêm bản sai "chỉ xét kỹ năng trùng đầu tiên". Test xanh với code hiện tại (không sửa code), đỏ với cả ba bản sai của D54 ở bảng trên; thông báo lỗi của bản "chỉ xét kỹ năng đầu tiên" chỉ đúng vế (b).

## Phần "Tự kiểm" của từng bước

| Bước | Tự kiểm của kế hoạch | Kết quả (2026-11-14) |
| --- | --- | --- |
| 1 | `evaluation` không import Spring | Đạt (xem "Kết quả") |
| 2 | Chạy `SyntheticDataGeneratorTest` bằng hai lệnh `mvnw` riêng, so dấu vân tay bộ N = 100, seed 1 | Đạt: hai JVM cùng in `d1b2ab366891fd06` |
| 3–6 | Làm hỏng code | Xem bảng trên; bước 3 tìm ra lỗ hổng D54, đã thêm test |
| 6 | Lõi `matching` không đổi | Đạt |
| 7 | Chạy `--giai-doan chon --out <thư mục tạm>`; chạy lại cùng `--out` thì bị từ chối | Đạt: lần hai thoát mã 1, "Thư mục kết quả đã tồn tại, không ghi đè (D72)", thư mục không đổi. Chưa mở `metrics.csv` bằng Excel |
| 8 | Số trong `docs/ket_qua.md` khớp `metrics.csv`; có tag `v1.0-thuc-nghiem` | Đạt: script tính lại **476/476 ô** của 7 bảng từ `metrics.csv` và `kiem_dinh.csv` (trung bình, độ lệch chuẩn mẫu, làm tròn như `ket_qua.md`); lượt đối chứng sửa một số trong bản sao thì bắt đúng ô đó. Tag có |
| 9 | Admin gọi `GET /api/admin/skills` thấy 26 kỹ năng; sinh viên nhận 403 | Đạt, trong lượt kiểm A01 bên dưới |
| 10 | `npm run build`, `npm run lint`; màn hình 360 px | Đạt |

**Tái lập ở quy mô thật (NFR-06, D64).** Chạy lại giai đoạn báo cáo (seed 3–5, cấu hình đã chọn) ở commit `911c6a8`, vào thư mục tạm, trong JVM khác lượt chính thức: `metrics.csv`, `per_user.csv`, `kiem_dinh.csv` giống hệt `thuc_nghiem/ket_qua/2026-11-14_bao-cao/`, trừ 8 dòng latency (D61 cho phép khác). Mã `evaluation` và `matching` không đổi giữa `3a01bfc` và `911c6a8`.

## Đối chiếu code với quyết định (D52–D77)

Ba lượt review độc lập (phiên AI riêng, chỉ đọc, không được đưa kết luận của người chạy) đối chiếu code với từng câu của quyết định và phần "Chốt ở bước này" của kế hoạch, chia theo: (1) sinh dữ liệu, gán nhãn, chỉ số, kiểm định (D53–D55, D57–D60, D62–D64); (2) phương án, cách chạy, kết quả (D56, D61, D65–D72, phần D62, D64 ở `ExperimentRunner`); (3) phạm vi, API, giao diện (D52, D73–D76). D77 không có code. Mọi chỗ lệch được mở code hoặc chạy thử lại trước khi xử lý.

| Kết quả | Quyết định | Xử lý |
| --- | --- | --- |
| Khớp | D52, D53, D54, D55, D56, D57, D58, D59, D60, D63, D65, D66, D67 (phần code), D68, D69, D70, D71, D73, D75, D76; D62, D64 ở đường chạy chính | – |
| Tài liệu chưa theo kịp code (không đổi số liệu) | D64: "mức nhiễu cao hơn chỉ thêm thay đổi" sai với khung giữ lại khi nhiễu xóa hết lịch (đo lại: 4 trên 4224 người có khai lịch, seed 1–5, N = 1000); "kỹ năng theo thứ tự của V2" khác cách chọn theo hạng Zipf của kế hoạch bước 2 | Sửa D64, kế hoạch bước 2; comment `SyntheticDataGenerator` – `7e625a5` |
| Tài liệu chưa theo kịp code (không đổi số liệu) | D61: "cho từng phương án" trong khi kế hoạch (e) và code đo B0, B1, R, P; "B0, B1 … gần P" sai với B0 (0.671 so với 1.258 ms); ô latency cũng là ô chỉ số nên có thêm một lượt chạy trước lượt làm nóng | Sửa D61, kế hoạch bước 7; câu ghi chú latency trong `README.md` do `ResultWriter` sinh – `7e625a5` |
| Tài liệu chưa theo kịp code (không đổi số liệu) | D62, trường hợp biên chưa xảy ra: kế hoạch ghi "các cột số để trống" khi không có người nào, code và test ghi `so_cap` = 0, `n_wilcoxon` = 0 | Sửa kế hoạch bước 7 |
| Tài liệu chưa theo kịp code | D74: "hai mã bằng nhau 400" chưa nói chỉ áp dụng cho lệnh thêm (`DELETE` trả 404 theo kế hoạch bước 9); trỏ D15 thay vì D16. Kế hoạch bước 9 vừa ghi sửa vừa ghi không sửa `GlobalExceptionHandler`. TK 6.1, §7 ghi thiếu đường dẫn API của A01 | Sửa D74, kế hoạch bước 9, TK 6.1, §7 |
| Code lệch ở trường hợp biên | D72: hook `protect-results` cho lọt 7 lệnh ghi (`Export-Csv`, `[IO.File]::`, `curl -o`, `wget -O`, `unzip -d`, `tar -C`, `Expand-Archive`); đã thử bằng cách đưa JSON vào hook | Bịt, thêm 8 ca vào bảng thử, 55/55 đúng – `fafa779`; sửa kế hoạch bước 8 |
| Code lệch ở trường hợp biên | D72: `README.md` của lượt chạy chỉ ghi khi đóng file; tiến trình bị dừng ngang (Ctrl+C) thì không có `README.md` | Giữ code đã gắn tag `v1.0-thuc-nghiem`; sửa D72 và `docs/To_chuc_ket_qua.md`: thiếu `README.md` là dấu hiệu lượt chạy dở |
| Sai trong chính README này | Bản đầu ghi vi phạm `UNIQUE` đồng thời "chỉ kiểm bằng đọc code", trong khi đã có test `d74_concurrentCreateGivesConflictNotServerError` | Sửa README và `ca_kiem_thu.md` |

Ghi chú, không phải lệch: spec 5.4 ghi seed 3–5 chỉ dùng cho cấu hình mặc định và cấu hình đã chọn, còn D70 (d) chạy thêm `P_sf_jaccard` ở seed 3–5 (chỉ chỉ số `_it_khung`); theo thứ bậc tài liệu thì D70 đúng, kế hoạch bước 7 đã giải thích. Phần "báo cáo ghi" của D56, D66, D67 kiểm khi viết báo cáo tuần 5. Các tình huống đồng thời và trường hợp biên khác review tìm ra ghi ở "Giới hạn".

### Truy vết quyết định → ca, test

| Quyết định | Kiểm bằng |
| --- | --- |
| D52 phạm vi | Không phải hành vi code. Lõi `matching` không đổi; 8 API của TC250 không gồm khóa tài khoản |
| D53 hai lớp dữ liệu | TC208, TC210, TC215, TC218, TC219; 3 test D53 |
| D54 nhãn 2, nhãn 0 | TC212, TC213, TC221; 2 test D54 (một test thêm 2026-11-14 cho ca hai kỹ năng trùng chính xác) |
| D55 trượt Bước 1 | TC228; 3 test D55 |
| D56 danh sách của P | TC240; 1 test D56 |
| D57 IDCG trên tập lý tưởng | TC226; 3 test D57 |
| D58 Precision@5 hai bản | TC225, TC226; 1 test D58 |
| D59 hai bản trung bình NDCG@5 | TC229; 3 test D59 |
| D60 bao phủ | TC230; 2 test D60 |
| D61 latency | TC249 (thủ công); 1 test D61 |
| D62 kiểm định | TC231–TC233; 11 test D62 |
| D63 tham số sinh dữ liệu | TC203, TC204–TC209 (spec 5.4); 5 test D63 |
| D64 tái lập | TC201, TC202, TC239; 3 test D64; dấu vân tay hai JVM; chạy lại giai đoạn báo cáo ra cùng số |
| D65 B0, B1, R | TC234–TC237, TC239; 2 test D65 |
| D66, D67 bộ trọng số | TC241; 1 test D66 |
| D68 không sửa lõi | TC242, TC243; `git diff` của `matching` rỗng |
| D69 chọn cấu hình | Không phải code; bảng 1 của `docs/ket_qua.md` đã đối chiếu với `metrics.csv` |
| D70 tổ hợp chạy | 6 test D70 |
| D71 cách chạy | TC247 (2 test + thủ công); 6 test D71; không import Spring |
| D72 định dạng kết quả | TC244–TC246, TC248; 8 test D72 |
| D73 API | TC250–TC255, TC258–TC260; 1 test D73 |
| D74 kiểm dữ liệu, mã lỗi | TC252–TC258, TC261; 4 test D74, trong đó `d74_concurrentCreateGivesConflictNotServerError` cho năm request cùng lúc thêm "Rust" (một 201, bốn 409, không 500; không ép được lần nào cũng tranh chấp). Đổi tên cùng lúc chỉ kiểm bằng đọc code |
| D75 kỹ năng bị ẩn | TC251, TC255, TC258 |
| D76 giao diện A01 | TC262–TC264 (thủ công) |
| D77 không sửa báo cáo cũ | Không phải code; nằm ở báo cáo tuần 5 |

## Kiểm thủ công

TC262–TC264 chạy bằng Chrome headless điều khiển qua DevTools Protocol (script viết riêng, 38 lượt kiểm), trên một DB tạm tạo bằng `createdb` và xóa sau khi xong, để kỹ năng và nhóm thêm thử không vào DB dev (API không có lệnh xóa). Chưa bấm thử bằng tay trên trình duyệt thật.

| Ca | Kết quả |
| --- | --- |
| TC247 – sửa tạm một file trong `backend/`, chạy `--giai-doan bao-cao` | Đạt (14/11/2026). Sửa tạm `backend/.gitattributes`: giai đoạn báo cáo thoát mã 1, "Dừng: backend/ có thay đổi chưa commit; commit hết rồi chạy lại (D71)", không tạo thư mục (`--out` trỏ vào thư mục tạm). Giai đoạn chọn vẫn chạy, `README.md` ghi "có" thay đổi chưa commit. Trả file về, `backend/` sạch |
| TC248 – hook chặn sửa, xóa thư mục kết quả | Đạt bằng `.claude/hooks/thu-protect-results.mjs`: lượt đầu (14/11/2026) 47/47 ca đúng; review độc lập tìm ra 7 lệnh ghi lọt qua, bịt ở `fafa779`, bảng thử nay **55/55 ca** đúng (Write, Edit, Bash, PowerShell; chặn ghi, xóa, di chuyển, `git clean`, `git stash -u`; cho qua đọc, commit và lệnh chạy thực nghiệm). Script đưa JSON giả vào hook, không chạy lệnh xóa thật. Thư mục mới vẫn tạo được: hai lượt chạy của bước 8 tạo sau commit thêm hook (`4d4bc65`) |
| TC249 – latency ở lượt chạy báo cáo | Đạt: N = 1000 có `latency_tb_ms`, `latency_p95_ms` > 0 cho `B0`, `B1`, `R`, `P` (D70 e); `P` trung bình 1.258 ms ≤ 2000 ms. `README.md` của lượt chạy ghi CPU, RAM. Số ở `docs/ket_qua.md`, bảng 7 |
| TC262 – admin, sinh viên đăng nhập | Đạt (14/11/2026). Admin vào `/admin/skills`, menu chỉ có "Danh mục kỹ năng" và "Đăng xuất", trang hiện 26 kỹ năng, 9 cặp. Sinh viên vào `/profile`, menu 4 mục như cũ. Ảnh `tc262_admin_a01_1280.png` |
| TC263 – gõ đường dẫn của vai trò khác | Đạt (14/11/2026). Admin gõ `/matches`, `/profile`, `/skills`, `/search`, `/users/1`, `/`, `/abc`, `/login`, `/admin/khong-co` đều về `/admin/skills`. Sinh viên gõ `/admin/skills`, `/admin/khong-co`, `/`, `/login` đều về `/profile`. Sinh viên tự gọi `GET /api/admin/skills` nhận 403; chưa đăng nhập gõ `/admin/skills` về `/login` |
| TC264 – các thao tác trên A01, 360 px | Đạt (14/11/2026). Thêm kỹ năng, đổi tên, ẩn rồi hiện, thêm nhóm, đổi tên nhóm, thêm cặp, xóa cặp, đổi nhóm sau khi gỡ quan hệ: mỗi thao tác hiện đúng sau khi tải lại trang. Lỗi hiện `detail` của backend: "Tên kỹ năng đã tồn tại" (409), "Hai kỹ năng phải cùng nhóm" (400), "Kỹ năng đang có quan hệ liên quan, hãy gỡ quan hệ trước khi đổi nhóm" (409); tên toàn khoảng trắng hiện "Dữ liệu không hợp lệ" và "Tên không được để trống" dưới ô. Ở 360 px không cuộn ngang (`scrollWidth` = 360), kể cả khi mở form sửa. Ảnh `tc264_a01_360.png`, `tc264_a01_360_dang_sua.png`, `tc264_an_ky_nang_360.png`, `tc264_loi_doi_nhom_360.png` |

## Giới hạn

- Frontend chưa có test tự động. A01 kiểm bằng một script chạy một lần, script không lưu trong repo. Ở frontend, kiểu TypeScript là lưới an toàn duy nhất.
- D71 xét `git status` của `backend/`, nên file bị `.gitignore` không được tính, kể cả `target/classes`. Sửa code, biên dịch, rồi trả mã nguồn về mà không biên dịch lại thì giai đoạn báo cáo vẫn chạy với lớp đã biên dịch từ code đã sửa. Cách chạy của D71 luôn `.\mvnw.cmd -q compile` trước nên tránh được, nhưng chương trình không tự kiểm.
- Vi phạm `UNIQUE` khi thêm cùng tên đúng một lúc có test nhưng không ép được tranh chấp xảy ra mỗi lần; đổi tên cùng lúc chỉ kiểm bằng đọc code.
- Hai tình huống đồng thời D74 không bảo đảm (review độc lập; chấp nhận vì chỉ có một admin, xem lại ở tuần 8): "Rust" và "rust" thêm cùng lúc có thể cùng nhận 201, vì `UNIQUE` của DB phân biệt hoa thường và D74 chọn không thêm migration; đổi nhóm một kỹ năng và thêm quan hệ cho nó cùng lúc có thể tạo quan hệ khác nhóm, DB không có ràng buộc chặn.
- Độ dài tên đếm bằng `String.length()` (đơn vị UTF-16): ký tự ngoài BMP như emoji tính là 2, nên 51 emoji đã bị coi là quá 100 ký tự. Giống `fullName` (D16).
- Tên có ký tự NUL: suy từ cách các thư viện đổi lỗi thì ra 409 "Dữ liệu xung đột" thay vì 400; chưa chạy thử.
- So tên không phân biệt hoa thường với chữ có dấu dựa vào `LC_CTYPE` của DB (`en_US.utf8` trên máy dev); cần kiểm lại ở môi trường deploy tuần 8 (kế hoạch bước 9).
- Hook `protect-results` so chuỗi trên cả lệnh, nên chặn nhầm cả commit message hay heredoc có tên thư mục kết quả cùng một tên lệnh ghi (gặp hai lần khi review). Cách tránh ghi ở kế hoạch bước 8.
- Latency đo trong bộ nhớ, một seed, một máy; chưa đo end-to-end `GET /api/matches` trên 1.000 hồ sơ trong DB (tuần 8, D61).
- Chưa đo độ phủ mã (coverage).
