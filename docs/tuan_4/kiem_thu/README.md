# Kiểm thử tuần 4 – Matching, hồ sơ công khai, tìm kiếm

Mục đích: chứng minh code tuần 4 làm đúng 87 ca kiểm thử viết trước từ tài liệu (`ca_kiem_thu.md`), bộ test **bắt được lỗi thật** trong công thức điểm, và code khớp các quyết định D31–D38, D43–D51.

## Các file

| File | Nội dung |
| --- | --- |
| `ca_kiem_thu.md` | 87 ca viết từ tài liệu trước khi code (TC101–TC185, cộng TC75, TC76 hẹn từ tuần 3) |
| `backend/src/test/java/com/skillexchange/**` | Test tự động: mỗi test có `@DisplayName("TCxxx ...")` hoặc `"Dxx ..."` |
| `ket_qua.txt` | Trích kết quả lần chạy gần nhất (`Tests run`, `FAIL`, `ERROR`) |
| `anh/` | 16 ảnh chụp các ca thủ công TC179–TC184 |

## Cách chạy lại

```powershell
# C:\DACN, Docker đang chạy; DB test tạo một lần (D39):
docker compose up -d
docker compose exec db createdb -U skillexchange skillexchange_test
cd backend
.\mvnw.cmd test | Select-String "Tests run|FAIL|ERROR" | Out-File -Encoding utf8 ..\docs\tuan_4\kiem_thu\ket_qua.txt

# Chỉ test lõi matching (không cần DB, khoảng 20 giây):
.\mvnw.cmd -q -o test "-Dtest=com.skillexchange.matching.*Test,!MatchesApiTest"
```

Trong `ket_qua.txt` có 4 dòng `Error starting ApplicationContext`: đó là các test TC81 của tuần 3 **cố ý** khởi động ứng dụng thiếu hoặc sai `ADMIN_EMAIL`/`ADMIN_PASSWORD` để kiểm ứng dụng dừng lại. Không phải lỗi.

Hook `.claude/hooks/run-matching-tests.mjs` tự chạy test lõi mỗi khi AI sửa file trong `matching/` và báo đỏ nếu có test hỏng.

Frontend: `cd frontend; npm run build; npm run lint`.

## Thiết kế kiểm thử

| Nhóm ca | Số ca | Loại | Lớp test |
| --- | --- | --- | --- |
| A. Lõi tính điểm | 26 | Unit | `SkillSimilarityTest`, `SkillFitTest`, `LevelFitTest`, `ScheduleAndModeFitTest`, `ReputationCalculatorTest`, `MatchingWeightsTest` |
| B. Lọc cứng, phân loại, xếp hạng | 18 | Unit | `HardFilterTest`, `MatchingEngineTest`, `RankingComparatorTest` |
| C. API `/api/matches` | 14 | Tích hợp; TC157 thủ công | `MatchesApiTest` |
| D. Hồ sơ công khai, tìm kiếm | 15 | Tích hợp | `PublicProfileTest`, `UserSearchTest` |
| E. Phân quyền, riêng tư | 8 | Tích hợp | `ActiveAccessTest`, `PublicProfileTest` (TC76) |
| F. Giao diện | 6 | Thủ công | Trình duyệt, ảnh trong `anh/` |

| Loại test | Số lượng (thêm ở tuần 4) | Cách làm |
| --- | --- | --- |
| Unit test lõi matching | 104 test trong 9 lớp | JUnit thuần, không Spring, không DB (D34). Số thực so bằng dung sai $10^{-9}$ (D36); số mong đợi viết bằng biểu thức, ví dụ `Math.sqrt(0.6875 * 0.925)`, không chép số đã làm tròn. TC141 là kiểm thử thuộc tính: dữ liệu ngẫu nhiên seed 42 (có cả tài khoản không `ACTIVE`, admin, hồ sơ thiếu dữ liệu), kiểm 5 điều kiện lọc cứng bằng code viết riêng trong test, cả "không thừa" lẫn "không thiếu" |
| Unit test khác | 3 test (`LikePatternsTest`) | Escape `\`, `%`, `_` cho `ILIKE` (D37) |
| Test tích hợp (`@SpringBootTest` + MockMvc) | 42 test trong 4 lớp: `MatchesApiTest` 14, `UserSearchTest` 14, `ActiveAccessTest` 8, `PublicProfileTest` 6 | Gọi API thật trên DB `skillexchange_test`. Helper `TestAccounts` tạo tài khoản thẳng trong DB với trạng thái, vai trò cho trước, rồi khai hồ sơ, lịch, kỹ năng qua API tuần 3. TC155 chèn phiên và đánh giá vào DB để kiểm truy vấn dữ liệu thô của Reputation (D43c) |
| Kiểm thủ công | 7 ca | TC157 (đếm câu SQL trong log), TC179–TC184 (trình duyệt) |

Code lõi viết theo TDD hai lượt: viết test, chạy thấy đỏ đúng lý do, người học đọc từng số mong đợi, rồi mới viết code cho xanh, không sửa test.

Ngoài TC còn 21 test ghi mã quyết định tuần 4 (D36, D37, D38, D43–D47) cho phần TC chưa phủ, ví dụ thứ tự `youLearn`, `commonSlots` (D46), thứ tự escape (D37), trọng số `NaN` (D47).

## Truy vết ca → test

| Ca | Trạng thái | Ghi chú |
| --- | --- | --- |
| 80 ca tự động (nhóm A–E trừ TC157) | **Có test** | Mỗi ca có ít nhất một test mang mã `TCxxx` trong `@DisplayName`; không test nào ghi mã ca không tồn tại |
| TC75, TC76 (hẹn từ tuần 3) | **Có test** | `ActiveAccessTest`, `PublicProfileTest` |
| TC157 | Thủ công | Đếm câu SQL trong log |
| TC179–TC184 | Thủ công | Kết quả ở phần "Kiểm thủ công" |

TC75, TC173, TC174, TC175, TC178 kiểm trên cả `/api/matches` lẫn `/api/users` (phần `/matches` viết ở bước 8, phần `/users` thêm ở bước 9).

## Kết quả

**288/288 test đạt** (lần chạy 2026-11-08, PostgreSQL 17.11, Spring Boot 4.1.1), gồm toàn bộ 139 test của tuần 3. Tuần 4 thêm 149 test: 107 unit, 42 tích hợp. 104 test lõi tự chạy chưa tới 1 giây; phần lớn thời gian của cả bộ (khoảng 1 phút 49 giây) là test tích hợp.

Frontend: `npm run build` và `npm run lint` không lỗi.

Kiểm thử lược đồ tuần 2 chạy lại 2026-11-08: vẫn **73/73**, kết quả giống hệt từng dòng lần trước (tuần 4 không thêm migration).

**Lần đỏ sai lý do:** `LikePatternsTest` lần đầu đỏ vì file tạo bằng heredoc của Git Bash làm mất một nửa số dấu `\`, không phải vì code thiếu. Tạo lại bằng công cụ Write thì đỏ đúng lý do (ghi ở `docs/harness-log.md`).

## Làm hỏng code, test phải đỏ

Sửa tạm một chỗ, chạy test, ghi test nào đỏ, rồi `git restore`. Đã làm 47 lần qua các bước. Mỗi lần đều có ít nhất một test đỏ, trừ hai trường hợp: một lần đúng thiết kế (bước 8, dòng cuối) và một lần ở frontend (trình biên dịch TypeScript bắt thay test).

| Bước | Làm hỏng | Test đỏ |
| --- | --- | --- |
| 2 | Bỏ "trùng chính xác → 1.0" | TC101, TC104, TC105, TC106 |
| 3 | LevelFit chia cả kỹ năng Sim = 0 | TC110 (0.5 thay vì 0.75) |
| 3 | Lấy kỹ năng dạy đầu tiên thay vì tốt nhất | TC113, TC114, TC115 |
| 4 | ScheduleFit chia cho `max` thay vì `min` | TC116 (3 test), TC117 |
| 4 | Bỏ nhánh "chưa khai báo lịch → 0.5" | TC118 (2 test, ra `NaN` hoặc 0) |
| 4 | Kiểm `ANY` trước `null` trong ModeFit | TC119 (4 dòng) |
| 5 | Bỏ nhánh "mẫu số 0 → 0.5" của CompletionRate | TC120, TC121, TC124, spec 4.3 (1 sao), D43 (ra `NaN`) |
| 5 | Tính `IN_PROGRESS` vào mẫu số (quy tắc cũ) | TC123 (ra 0.7125), TC124, D43 |
| 5 | `CANCELLED` không xét `started` | TC123, TC124, D43 |
| 5 | RatingNorm = trung bình / 5 | TC123, TC125, spec 4.3 (1 sao). TC121 không bắt (5 sao ra 1.0 ở cả hai công thức) |
| 6 | `filter` trả danh sách rỗng | 10 test, gồm TC133 |
| 6 | Quên `role == STUDENT` | TC130 |
| 6 | Bỏ "cả hai đã khai lịch" | TC133, spec 4.2 (4) |
| 6 | Quên `v ≠ u` | TC127 |
| 6 | Đòi khớp kỹ năng hai chiều | 12 test |
| 7 | Trung bình cộng thay trung bình nhân | TC134, TC140 |
| 7 | So điểm tuyệt đối, bỏ `EPSILON` | TC138 |
| 7 | Gộp hai nhóm làm một | TC135, TC136, TC140, D46 (2 test) |
| 7 | Ghi cứng 0.55 thay vì `weights.skillFit()` | TC142 |
| 7 | Đổi trọng số mặc định thành (0.50, 0.20, 0.15, 0.05, 0.10), tổng vẫn bằng 1 | TC126, TC134 (An–Bình), TC135 (A.3) |
| 7 | Bằng điểm thì `userId` giảm dần | TC138, D36 (bằng điểm tuyệt đối) |
| 7 | Bỏ sắp `youLearn` theo tên | D46. TC144 không bắt (dữ liệu An nhập sẵn đúng thứ tự) |
| 7 | Lọc cứng quên điều kiện hình thức | TC141 (`modeFit` ném lỗi, nhờ lựa chọn ở bước 4) |
| 7 | `if (userId == 2)` ép điểm Bình | TC140 |
| 8 | SQL phiên chỉ JOIN theo `sender_id` | TC155 (Reputation 1.0 thay vì 0.85) |
| 8 | Điểm đánh giá lấy theo `reviewer_id` | TC155 |
| 8 | Bỏ `@JsonInclude(NON_NULL)` trên `backward` | TC185 |
| 8 | Bỏ luật `ACTIVE_STUDENT` trong `SecurityConfig` | TC75, TC173 (phần `/users` trả 200), TC174, D44 (xem ghi chú) |
| 8 | Gắn `ACTIVE_STUDENT` chỉ xét trạng thái, quên vai trò | D44 (admin + `type=FOO`) |
| 8 | SQL lọc bỏ kỹ năng bị ẩn | TC153 |
| 8 | `hasPendingRequest` theo `receiver_id` | TC152 |
| 8 | Không cắt `limit` | TC146, TC147 |
| 8 | `ratingAverage` = 0 thay vì `null` | TC151, TC185 |
| 8 | Bỏ lọc `ACTIVE`/`STUDENT` trong câu SQL nạp người dùng | **Không test nào đỏ, đúng thiết kế D43(a)**: `HardFilter` là chỗ lọc chính thức, lọc trong SQL chỉ để nhẹ |
| 9 | Không escape `q` | TC166, `LikePatternsTest` (2 test) |
| 9 | Escape `\` **sau** `%`, `_` | TC166, `LikePatternsTest` (2 test) |
| 9 | Tài khoản ẩn trả 403 thay vì 404 | TC160 |
| 9 | Tìm kiếm bỏ lọc `ACTIVE`/`STUDENT` | TC170 (khác bước 8: ở đây SQL là chỗ lọc duy nhất) |
| 9 | Không bỏ người gọi | TC170 |
| 9 | Xếp theo `id` thay vì tên | TC171 |
| 9 | `LIKE` thay `ILIKE` | TC165 |
| 9 | Không cắt khoảng trắng `q` | TC163, D45 (2 test) |
| 9 | Đếm độ dài `q` trước khi cắt | D45 |
| 9 | Tìm theo kỹ năng bỏ kỹ năng bị ẩn | TC168 |
| 9 | Hồ sơ bỏ kỹ năng bị ẩn | TC162 |
| 9 | `skillId` khớp cả danh sách muốn học | TC167 |
| 10 | Bỏ `?.` ở `match.theyLearn?.map` | Không có test frontend; `npm run build` (tsc) báo lỗi vì `theyLearn` khai `?` |

**Ghi chú về luật `ACTIVE_STUDENT`.** Lần làm hỏng ở bước 8, khi `ActiveAccessTest` mới có phần `/matches`, TC75 và TC173 **không** bắt: `MatchingService` có một nhánh phòng vệ cũng trả 403 khi người gọi không có trong danh sách sinh viên `ACTIVE` (D44). Hai lớp chặn chồng nhau, lớp sau che lớp trước, giống M4 của tuần 3. Lúc đó chỉ TC174 và test D44 bắt được, vì gọi với tham số sai: thiếu luật thì ra 400, không ra 403. Chạy lại 2026-11-08, sau khi bước 9 thêm phần `/users` (không có nhánh phòng vệ): TC75, TC173 đã đỏ, tổng cộng 4/8 test của `ActiveAccessTest` bắt được.

**Lỗi tìm thêm khi đối chiếu.** Trọng số `NaN` lọt qua phần kiểm của D47, vì mọi phép so sánh với `NaN` đều ra `false`. Trước commit `34a88f9` không test nào bắt. Đã thêm test (5 trường hợp, mỗi vị trí trọng số một lần), thấy đỏ, rồi sửa code.

## Đối chiếu code với quyết định (D31–D38, D43–D50)

Ba lượt review độc lập (phiên AI riêng, chỉ đọc, chia theo lõi matching; API `/matches` và phân quyền; `/users` và frontend) đối chiếu code với từng quyết định, spec 4.1–4.6, TK 3.5, 4.1, 4.3, 4.5, 6.1, 6.3. Mọi chỗ lệch được mở code kiểm lại trước khi sửa.

| Kết quả | Quyết định | Xử lý |
| --- | --- | --- |
| Khớp | D31, D32, D33, D35, D36, D37, D38, D43, D48, D50 | – |
| Code đúng, câu chữ quyết định chưa theo kịp kế hoạch | D34, D44, D46, D49 | Sửa câu chữ; thêm D51 (lõi xếp tên kỹ năng bằng `String.compareTo`) – `4066b85` |
| Code lệch | D47: trọng số `NaN` không bị chặn | Sửa code theo TDD – `34a88f9` |
| Code lệch | D45(b): ô tìm kiếm S10 có `maxLength=100`, cắt chuỗi trước khi bỏ khoảng trắng, trong khi backend đếm sau khi cắt | Bỏ `maxLength`, để backend là nơi kiểm duy nhất – `e8b835c` |

Commit `4066b85` cũng sửa ngược các tài liệu cũ: spec 4.3 (LevelFit chọn `skillId` nhỏ hơn khi bằng trình độ), TK 3.5 (RatingNorm nạp điểm thô), TK 4.5 và 6.1 (nhãn "Đã gửi yêu cầu", nút gửi yêu cầu ở tuần 6), kế hoạch bước 8 (9 câu SQL), bộ ca kiểm thử (cách tạo tài khoản, TC179, TC181, hai mục "Đã chốt").

## Kiểm thủ công

| Ca | Kết quả |
| --- | --- |
| TC157 – bật log SQL, gọi `/matches` với 20 rồi 40 ứng viên | Đạt: cả hai lần đều 9 câu SQL = 8 câu của `MatchingService` + 1 câu `AccountStatusFilter` đọc tài khoản người gọi (D13). Không N+1 |
| TC179 – An mở S05 | Đạt (08/11/2026). Ảnh `tc179_s05_an_hai_chieu.png`, `tc179_s05_an_mot_chieu.png` |
| TC180 – không có nút "Gửi yêu cầu", S06 không có phần đánh giá | Đạt (08/11/2026). Ảnh `tc180_s05_chi_tiet_diem.png`, `tc182_s06_ho_so_binh.png` |
| TC181 – tài khoản chưa khai kỹ năng muốn học mở S05 | Đạt (08/11/2026). Ảnh `tc181_s05_chua_khai_ky_nang.png` |
| TC182 – S06 hồ sơ Bình, không lộ thông tin riêng tư | Đạt (08/11/2026). Ảnh `tc182_s06_ho_so_binh.png`, thêm 3 ảnh trường hợp lỗi: `tc182_s06_admin.png`, `tc182_s06_khong_ton_tai.png`, `tc182_s06_id_abc.png` |
| TC183 – S10 tìm theo tên, theo kỹ năng | Đạt (08/11/2026). 5 ảnh `tc183_s10_*.png`: tìm "bình" có dấu và không dấu, tìm theo kỹ năng Python, tìm "%", bỏ trống cả hai ô |
| TC184 – S05, S06, S10 ở màn hình rộng 360 px | Đạt (08/11/2026): không cuộn ngang; menu xếp dọc, chiếm khoảng 170 px. 3 ảnh `tc184_360px_*.png` |

Bản sửa `maxLength` của S10 (`e8b835c`) làm sau lần kiểm thủ công này. Backend đã có test cho độ dài `q` (D45). Phía giao diện đã kiểm bằng đọc code (2026-11-08): lỗi `errors[{field: "q"}]` đi qua `fieldErrors.q` trong `api/client.ts` tới ô "Tên" của `SearchPage`, nên hiện ngay dưới ô. Chưa bấm thử trên trình duyệt.

## Giới hạn

- Reputation chưa có dữ liệu thật: bảng `reviews`, `exchange_sessions` trống tới tuần 6–7, nên khi chạy thật mọi người có Reputation 0.5 (D32). Công thức đã kiểm bằng unit test, truy vấn dữ liệu thô kiểm bằng TC155.
- So "bằng điểm" theo dung sai $10^{-9}$ không có tính bắc cầu (D48): về lý thuyết `List.sort` có thể ném lỗi khi ba điểm trở lên cách nhau dưới $10^{-9}$. Chấp nhận vì đúng câu chữ spec 4.4 và rất hiếm với điểm thật.
- Lõi xếp tên kỹ năng bằng `String.compareTo`, có thể khác collation `en_US.utf8` của PostgreSQL với tên có dấu hoặc bắt đầu bằng chữ thường (D51). 26 kỹ năng hiện có không bị ảnh hưởng.
- Chưa đo hiệu năng ≤ 2 giây với 1.000 hồ sơ (NFR-03): đo ở tuần 5 cùng thực nghiệm.
- Frontend chưa có test tự động; S05, S06, S10 chỉ kiểm thủ công. Ở frontend, kiểu TypeScript (`backward?`, `theyLearn?`) là lưới an toàn duy nhất.
- Chưa đo độ phủ mã (coverage).
- Tìm kiếm phân biệt dấu ("an" không ra "Ân", D37); bỏ dấu khi tìm là việc nên có, chưa làm.
- `MatchingEngineTest` có vài dữ liệu dùng trình độ 5, ngoài thang 1–4. Kết luận của test không sai nhưng dữ liệu đó không thể có thật.
