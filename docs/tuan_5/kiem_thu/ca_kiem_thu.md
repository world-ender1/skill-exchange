# Ca kiểm thử tuần 5 – Thực nghiệm thuật toán, quản trị danh mục kỹ năng

Mục đích: liệt kê trước các ca kiểm thử cho chức năng tuần 5 (FR-11, FR-08 phần danh mục kỹ năng), viết **chỉ từ tài liệu**, trước khi có code. Code tuần 5 phải làm các ca này pass. Ca nào thấy sai thì sửa tài liệu trước rồi mới sửa ca, không sửa ca cho khớp code.

Viết ngày 2026-11-11, sau khi chốt D52–D77. Mã ca bắt đầu từ TC201 để không trùng tuần 3 (TC01–TC93) và tuần 4 (TC101–TC185).

## Nguồn

Khi các nguồn khác nhau, nguồn mới hơn đúng: quyết định tuần 5 > quyết định tuần 3–4 > thiết kế tuần 2 > spec tuần 1.

| Ký hiệu | Tài liệu |
| --- | --- |
| Spec 5.1–5.4, A.3 | `docs/tuan_1/Phan_tich_yeu_cau_Skill_Exchange_Tuan_1.md`: nhãn, phương án, chỉ số, dữ liệu giả lập, ví dụ tính tay |
| Spec 7.1 | Spec tuần 1, unit test nhóm 4 |
| FR-08, FR-11, NFR-03, NFR-06 | Spec tuần 1, mục 3 và 7 |
| TK 4.9, 6.2 | `docs/tuan_2/Tuan_2_Thiet_ke.md`: API quản trị, luồng của admin |
| D13, D15, D19, D38, D40 | Quyết định tuần 3, tuần 4 |
| D52–D77 | `docs/tuan_5/Quyet_dinh_tuan_5.md` |

**Loại:**
- **Unit**: JUnit thuần, không Spring, không DB, chạy trên `evaluation` và lõi `matching` (D71).
- **Tích hợp**: `@SpringBootTest` trên DB test `skillexchange_test` (D39), kế thừa `IntegrationTestBase`.
- **Thủ công**: kiểm tra bằng giao diện, dòng lệnh hoặc đọc file kết quả.

**Quy ước:**
- Số thực so bằng dung sai $10^{-9}$ (D36), trừ khi ca ghi khác. "≈" chỉ để đọc; giá trị dùng trong test là biểu thức ghi kèm.
- Nhiễu ghi theo thứ tự ($p_{level}$, $p_{miss}$, $p_{extra}$). "Nhiễu mặc định" = (0.3, 0.2, 0.05), "nhiễu cao" = (0.5, 0.4, 0.1) (spec 5.4).
- Khung giờ ghi tắt: "Tối T4" = `{dayOfWeek: 3, period: EVENING}` (1 = Thứ Hai, 7 = Chủ nhật).
- "Loại" = bị loại ở Bước 1 của nhãn (không có nhãn, không vào tập lý tưởng).
- Ca tích hợp có thêm, sửa danh mục kỹ năng phải trả danh mục về như `V2__seed_skills.sql` sau khi chạy, vì các test khác dùng chung DB test.

## Dữ liệu mẫu dùng chung

**Người học $u$ (ví dụ A.3):** muốn học Java (mục tiêu 3) và SQL (mục tiêu 2); không dạy gì liên quan tới các ứng viên dưới đây. Lịch thật = lịch tự khai = Tối T2, Tối T4, Tối T6. Hình thức `ONLINE`.

**Ứng viên gốc:** đã khai lịch, lịch thật = lịch tự khai = Tối T4, Tối T6, Sáng T7 (trùng $u$ đúng 2 khung). Hình thức `ANY`. Trình độ tự khai = trình độ thật, trừ khi ca ghi khác. Các ứng viên $v_1$–$v_9$ ở nhóm B là ứng viên gốc với phần dạy và phần khác ghi trong ca. Cặp liên quan theo V2: Java–Kotlin, SQL–MySQL.

## Tổng quan

| Nhóm | Số ca | Ý nghĩa |
| --- | --- | --- |
| A. Bộ sinh dữ liệu | 10 | Tái lập, danh mục, tham số, nhiễu, "chưa khai báo" (spec 5.4, D53, D63, D64) |
| B. Gán nhãn | 13 | Thác nước trên giá trị thật, ví dụ A.3 (spec 5.1, D53–D55) |
| C. Chỉ số | 7 | Precision@5 hai bản, NDCG@5, IDCG trên tập lý tưởng, hai bản trung bình, bao phủ (spec 5.3) |
| D. Kiểm định | 3 | Wilcoxon, bootstrap (D62) |
| E. Phương án | 10 | B0, B1, R, P, bộ trọng số, công thức ScheduleFit (spec 5.2, 5.4, D65–D68) |
| F. Chạy và lưu kết quả | 6 | Thư mục kết quả, cột CSV, chạy lại, hook, latency (D61, D71, D72) |
| G. API danh mục kỹ năng | 12 | `/api/admin/skill*` (TK 4.9, D73–D75) |
| H. Giao diện A01 | 3 | Thủ công (D76) |
| **Tổng** | **64** | |

---

## A. Bộ sinh dữ liệu (unit)

| Mã | Nguồn | Đầu vào | Mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC201 | D64, NFR-06 | Sinh hai lần với cùng tham số: N = 100, seed 1, nhiễu mặc định | Hai bộ giống hệt nhau từng người: mã, kỹ năng dạy, trình độ thật và tự khai, kỹ năng muốn học, mục tiêu, lịch thật và tự khai, hình thức. Seed 2 cho bộ khác | Unit |
| TC202 | D64 | Seed 1, N = 100, ba mức nhiễu: không, mặc định, cao | Giá trị thật giống hệt nhau ở cả ba mức. Mọi trình độ bị lệch ở mức mặc định cũng bị lệch (cùng hướng) ở mức cao | Unit |
| TC203 | D63 | Danh mục kỹ năng của `evaluation` | 6 nhóm, 26 kỹ năng, 9 cặp. Tên kỹ năng khớp `V2__seed_skills.sql`. Các cặp: Java–Kotlin, JavaScript–TypeScript, SQL–MySQL, Excel–Power BI, Photoshop–Illustrator, Figma–Canva, Guitar–Ukulele, Tiếng Nhật–Tiếng Trung, Tiếng Nhật–Tiếng Hàn; hai kỹ năng của mỗi cặp cùng nhóm | Unit |
| TC204 | Spec 5.4, D19 | N = 1000, seed 1, nhiễu mặc định | Mỗi người: 2–5 kỹ năng dạy, 1–3 kỹ năng muốn học, không trùng trong một danh sách, không kỹ năng nào vừa dạy vừa học. Trình độ thật và tự khai trong 1–4, lệch nhau tối đa 1. Mục tiêu `null` hoặc 1–4. Người có khai lịch có 3–9 khung thật. Mã là 1..1000 không trùng. Mọi người `ACTIVE`, `STUDENT`, Reputation 0.5 | Unit |
| TC205 | Spec 5.4 | N = 200, nhiễu (0, 0, 0) | Với mọi người: trình độ tự khai = thật, lịch tự khai = thật | Unit |
| TC206 | Spec 5.4 | N = 200, nhiễu (1, 0, 0) | Trình độ thật 1 → tự khai 2; thật 4 → tự khai 3; thật 2 hoặc 3 → lệch đúng 1 bậc. Lịch không đổi | Unit |
| TC207 | Spec 5.4 | N = 200, nhiễu (0, 1, 0) | Mọi người có khai lịch còn đúng 1 khung tự khai, khung đó thuộc lịch thật. Người không khai vẫn rỗng | Unit |
| TC208 | D53 | N = 1000, nhiễu cao | Người không khai lịch: lịch rỗng ở cả hai lớp (nhiễu không thêm khung). Người không khai hình thức: `null` ở cả hai lớp. Kỹ năng, mục tiêu, hình thức giống nhau ở hai lớp với mọi người | Unit |
| TC209 | Spec 5.4 | N = 1000, seed 1, nhiễu mặc định | Tỷ lệ không khai lịch, tỷ lệ không khai hình thức đều trong 0.15 ± 0.05. Tỷ lệ mục tiêu bỏ trống (trên mọi kỹ năng muốn học) trong 0.30 ± 0.05. Kỹ năng có nhiều người dạy nhất có số người dạy ít nhất gấp 3 kỹ năng ít người dạy nhất | Unit |
| TC210 | Spec 5.1, D53 | Một người có trình độ tự khai khác thật và lịch tự khai khác thật; dựng `MatchProfile` | `MatchProfile` mang trình độ và lịch **tự khai**, `status = ACTIVE`, `role = STUDENT`, `reputation = 0.5` | Unit |

## B. Gán nhãn (unit)

Mọi ca dùng người học $u$ và ứng viên gốc ở mục "Dữ liệu mẫu", trừ phần ghi khác. Ví dụ ở spec A.3.

| Mã | Nguồn | Đầu vào | Mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC211 | Spec 5.1, A.3 | $v_1$ dạy Java (3) | Nhãn 2 | Unit |
| TC212 | D54, A.3 | $v_2$ dạy Java (3), MySQL (1) | Nhãn 2 (một kỹ năng đạt là đủ) | Unit |
| TC213 | D54, A.3 | $v_3$ dạy Kotlin (1), MySQL (3) | Nhãn 1 (MySQL đạt mục tiêu SQL nên không phải mọi cặp thấp hơn từ 2 bậc) | Unit |
| TC214 | Spec 5.1, A.3 | $v_4$ dạy Kotlin (1) | Nhãn 0 | Unit |
| TC215 | D53, A.3 | $v_5$ dạy Java (3), chưa khai lịch (rỗng ở cả hai lớp) | Nhãn 1, không bị loại | Unit |
| TC216 | Spec 5.1, A.3 | $v_6$ dạy Java, tự khai 3, thật 2 | Nhãn 1 (nhãn dùng trình độ thật) | Unit |
| TC217 | Spec 5.1, A.3 | $v_7$ dạy Java (3), lịch thật trùng $u$ đúng 1 khung | Nhãn 1 | Unit |
| TC218 | D53, A.3 | (a) $v_8$ dạy Java (3), chưa khai hình thức. (b) $u$ chưa khai hình thức, $v_1$ | (a) Nhãn 1. (b) Nhãn 1. Cả hai không bị loại | Unit |
| TC219 | D53 | $v_1$ với: (a) $u$ `ANY`, $v$ `ONLINE`; (b) $u$ `OFFLINE`, $v$ `OFFLINE`; (c) $u$ `ONLINE`, $v$ `ANY` | Cả ba nhãn 2 (`ANY` + `ONLINE` tính là tương thích hoàn toàn) | Unit |
| TC220 | Spec 5.1, A.3 | (a) $v_9$ dạy Java (3), hình thức `OFFLINE`. (b) $v = u$. (c) $v$ chỉ dạy Python. (d) $v$ dạy Java (3), cả hai đã khai lịch, lịch thật của $v$ là Sáng T2 (không trùng $u$) nhưng lịch tự khai của $v$ có Tối T4 | Cả bốn: Loại. Ca (d) cho thấy Bước 1 dùng lịch thật | Unit |
| TC221 | D54, A.3 | (a) $u$ bỏ trống mục tiêu Java (giữ SQL mục tiêu 2), $v_1$. (b) $u$ chỉ muốn học Java, bỏ trống mục tiêu, $v$ dạy Kotlin (1) | (a) Nhãn 1. (b) Nhãn 1, không phải nhãn 0 (bỏ trống mục tiêu không tính là thấp hơn) | Unit |
| TC222 | Spec 5.1, 7.1 | $v_1$, đổi trình độ tự khai Java thành 1 và lịch tự khai thành Sáng CN (giữ giá trị thật) | Vẫn nhãn 2: nhãn không phụ thuộc giá trị tự khai | Unit |
| TC223 | Spec 5.1, 7.1 | N = 200, seed 1, nhiễu mặc định; gán nhãn mọi cặp $(u, v)$ | Mỗi cặp nhận đúng một kết quả trong {Loại, 0, 1, 2}. Mọi cặp nhãn 2 có kỹ năng trùng chính xác; mọi cặp nhãn 0 không có. Cặp "Loại" gồm đúng các cặp vi phạm Bước 1, kiểm bằng code độc lập trong test | Unit |

## C. Chỉ số (unit)

| Mã | Nguồn | Đầu vào | Mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC224 | Spec A.3 | Danh sách nhãn [2, 0, 1, 0, 0]; tập lý tưởng là đúng 5 nhãn đó | DCG@5 = `3 / log2(2) + 1 / log2(4)` = 3.5. IDCG@5 = `3 + 1 / log2(3)`. NDCG@5 = `3.5 / (3 + 1 / log2(3))` ≈ 0.9639. Precision@5 = 0.4. Precision@5 nghiêm ngặt = 0.2 | Unit |
| TC225 | Spec 5.3 | Danh sách [2, 1]; tập lý tưởng {2, 1} | Precision@5 = 2/5; nghiêm ngặt = 1/5; NDCG@5 = 1.0 | Unit |
| TC226 | D57 | Danh sách [1, 1]; tập lý tưởng {2, 1, 1} | DCG@5 = `1 + 1 / log2(3)`. IDCG@5 = `3 + 1 / log2(3) + 1 / log2(4)`. NDCG@5 = tỷ số hai số trên. Precision@5 = 2/5; nghiêm ngặt = 0 | Unit |
| TC227 | Spec 5.3 | (a) Danh sách [0, 0], tập lý tưởng {0, 0}. (b) Danh sách rỗng, tập lý tưởng rỗng | NDCG@5 = 0, không phải NaN; mỗi ca được đếm vào số trường hợp IDCG@5 = 0. Precision@5 = 0 | Unit |
| TC228 | D55 | Danh sách [$x$, $v_1$], $x$ có trong danh sách nhưng trượt Bước 1 thật; tập lý tưởng {2} | rel của $x$ = 0; DCG@5 = `3 / log2(3)`; NDCG@5 = `(3 / log2(3)) / 3` | Unit |
| TC229 | D59 | Ba người: A có tập lý tưởng {2, 1}, danh sách [1, 2]; B có tập lý tưởng {1, 1, 1}, danh sách [1, 1, 1]; C có tập lý tưởng rỗng | NDCG@5 của A = `(1 + 3 / log2(3)) / (3 + 1 / log2(3))` ≈ 0.7967; B = 1; C = 0. Bản (1) = `(nA + 1 + 0) / 3`; bản (2) = `nA`, số người bị loại = 2 | Unit |
| TC230 | D60 | Bốn người có danh sách 0, 1, 5, 6 ứng viên; chỉ người có 5 ứng viên có một ứng viên nhãn 2 (ở vị trí thứ 5) | Coverage = 3/4; tỷ lệ có ≥ 5 ứng viên = 2/4; tỷ lệ có ứng viên nhãn 2 = 1/4 | Unit |

## D. Kiểm định (unit)

| Mã | Nguồn | Đầu vào | Mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC231 | D62 | Wilcoxon, hiệu [0.1, 0.2, 0.3, 0.4, −0.5] | n = 5, $W^+$ = 10, z = `2.5 / Math.sqrt(13.75)` ≈ 0.6742, p ≈ 0.5002 (dung sai $10^{-3}$) | Unit |
| TC232 | D62 | Wilcoxon: (a) hiệu [0, 0.2, −0.2, 0.5]; (b) hiệu [0, 0, 0] | (a) Bỏ hiệu 0 nên n = 3; hạng 1.5, 1.5, 3; $W^+$ = 4.5; z = `1.5 / Math.sqrt(3.375)` ≈ 0.8165; p ≈ 0.414 (dung sai $10^{-3}$). (b) Kết quả "không có cặp khác nhau", không lỗi, không NaN | Unit |
| TC233 | D62 | Bootstrap 2000 lần: (a) mọi hiệu bằng 0.1; (b) cùng một dữ liệu bất kỳ, chạy hai lần | (a) Khoảng tin cậy [0.1, 0.1]. (b) Hai lần ra cùng khoảng (seed `3_000_000`) | Unit |

## E. Phương án (unit)

| Mã | Nguồn | Đầu vào | Mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC234 | Spec 5.2, D65 | $u$ chỉ muốn học Java. $a$ dạy Java, $b$ chỉ dạy Kotlin | B0 = [$a$]; B1, R, P đều có cả $a$ và $b$ | Unit |
| TC235 | Spec 5.2, D65 | $u$ muốn học Java, SQL. Mã 1 dạy SQL; mã 2 dạy Java; mã 3 dạy Java và SQL | B0 = [3, 1, 2]: mã 3 trùng 2 kỹ năng; mã 1 và 2 bằng điểm nên mã nhỏ trước | Unit |
| TC236 | Spec 5.2, D65 | $u$ muốn học Java, SQL. Mã 1 dạy Java (SkillFit 0.5); mã 2 dạy Kotlin và MySQL (0.5); mã 3 dạy Java và MySQL (0.75) | B1 = [3, 1, 2] | Unit |
| TC237 | Spec 5.2, D65 | N = 200, seed 1, nhiễu mặc định; với mọi $u$ | Tập ứng viên của B1, R, P bằng nhau. Tập của B0 là tập con của B1, và mọi ứng viên của B0 có ít nhất một kỹ năng trùng chính xác | Unit |
| TC238 | Spec 5.2 | $u$ muốn học Java, `ONLINE`. $a$ dạy Java, `OFFLINE`. $b$ dạy Java, cả hai đã khai lịch tự khai, không trùng khung nào | $a$, $b$ không có trong B0, B1, R, P | Unit |
| TC239 | D64, D65 | N = 200; R với seed 1 chạy hai lần; R với seed 2 | Seed 1 hai lần cùng thứ tự với mọi người; tập ứng viên của R bằng B1; seed 2 khác thứ tự ở ít nhất một người | Unit |
| TC240 | D56 | Người dùng có 3 ứng viên hai chiều và 4 ứng viên một chiều; một ứng viên một chiều có $D$ cao hơn mọi MutualScore | 5 vị trí đầu của P = 3 ứng viên hai chiều theo thứ tự lõi trả, rồi 2 ứng viên một chiều đầu tiên | Unit |
| TC241 | D66, D67 | Các bộ trọng số của thực nghiệm | Bỏ LevelFit = (0.60, 0, 0.20, 0.10, 0.10); bỏ ScheduleFit = (0.5875, 0.2375, 0, 0.0875, 0.0875); bỏ ModeFit = (0.5625, 0.2125, 0.1625, 0, 0.0625); 4 bộ độ nhạy đúng spec 5.4. Mọi bộ tạo `MatchingWeights` không lỗi | Unit |
| TC242 | D68 | (a) N = 200, seed 1, nhiễu mặc định: tính lại điểm với `MIN`. (b) $u$ 4 khung, $v$ 3 khung, trùng 2. (c) $u$ 6 khung, $v$ 5 khung, trùng 5. (d) một bên chưa khai lịch | (a) Mọi $D$ và MutualScore tính lại bằng của lõi (dung sai $10^{-9}$). (b) `MIN` 2/3, `JACCARD` 2/5, `TRAN_3` 2/3. (c) `MIN` 1, `JACCARD` 5/6, `TRAN_3` 1. (d) Cả ba bằng 0.5 | Unit |
| TC243 | D68 | $u$ muốn học Java, 6 khung. $a$ dạy Java, 1 khung, trùng 1. $b$ dạy Java (cùng trình độ, hình thức với $a$), 6 khung, trùng 4. Mã $a$ < mã $b$ | `MIN`: $a$ (1.0) trước $b$ (4/6). `JACCARD`: $b$ (4/8) trước $a$ (1/6). `TRAN_3`: $b$ (1.0) trước $a$ (1/3). Lõi không đổi: gọi lõi trực tiếp vẫn ra thứ tự của `MIN` | Unit |

## F. Chạy và lưu kết quả

| Mã | Nguồn | Đầu vào | Mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC244 | D72 | Chạy vào một thư mục kết quả đã tồn tại | Dừng, báo lỗi; không tạo hay sửa file nào trong thư mục đó | Unit |
| TC245 | D72 | Chạy thử vào thư mục tạm (`--out`), N = 50, một seed | Có `README.md` (lệnh, seed, commit, CPU, RAM, phiên bản Java, thời gian chạy), `metrics.csv` đúng 8 cột theo D72, `per_user.csv` đúng 10 cột, `kiem_dinh.csv`, `log.txt`. Mọi giá trị `phuong_an`, `chi_so`, `muc_nhieu`, `vai_tro_seed` thuộc danh sách của D72 | Unit |
| TC246 | NFR-06 | Chạy thử hai lần cùng tham số vào hai thư mục tạm | Hai `metrics.csv` giống hệt nhau, trừ các dòng `latency_tb_ms`, `latency_p95_ms` | Unit |
| TC247 | D71 | Sửa tạm một file trong `backend/` (chưa commit), chạy `--giai-doan bao-cao` | Dừng, báo có thay đổi chưa commit; không tạo thư mục kết quả. Giai đoạn `chon` vẫn chạy và ghi "có thay đổi chưa commit" trong `README.md` | Thủ công |
| TC248 | D72 | Nhờ AI sửa một file trong thư mục kết quả đã có; nhờ AI xóa thư mục đó bằng lệnh shell; chạy thực nghiệm tạo thư mục mới | Hai thao tác đầu bị hook chặn; thư mục mới vẫn tạo được | Thủ công |
| TC249 | D61, NFR-03 | Lượt chạy giai đoạn báo cáo | Có `latency_tb_ms`, `latency_p95_ms` > 0 cho mọi phương án ở N = 1000; P có latency trung bình ≤ 2000 ms. `README.md` ghi CPU, RAM | Thủ công |

## G. API danh mục kỹ năng (tích hợp)

Danh mục ban đầu là của `V2__seed_skills.sql`.

| Mã | Nguồn | Đầu vào | Mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC250 | D73, D13 | Gọi 8 API danh mục kỹ năng của TK 4.9 bằng: sinh viên `ACTIVE`; không có token; admin; admin vừa bị đặt `LOCKED` trong DB | Sinh viên: 403. Không token: 401. Admin: không phải 401/403. Admin bị khóa: 403 | Tích hợp |
| TC251 | D73, D75 | Ẩn Kotlin bằng SQL; admin gọi `GET /api/admin/skills` | 26 phần tử, mỗi phần tử đủ `id, name, categoryId, categoryName, isActive`; Kotlin có `isActive = false`. Xếp theo `name` rồi `id` (so các tên không dấu) | Tích hợp |
| TC252 | D73, D74 | `POST /api/admin/skill-categories` với `name`: `"  Khoa học  "`; `"lập trình"`; `"   "`; chuỗi 101 ký tự; body `{}` | 201 `{id, name: "Khoa học"}`; 409; 400; 400 có `errors[{field: "name"}]`; 400 | Tích hợp |
| TC253 | D73, D74 | `PUT /api/admin/skill-categories/{id}`: đổi "Âm nhạc" thành "Nhạc cụ"; đổi "Âm nhạc" thành "DỮ LIỆU"; đổi "Lập trình" thành "LẬP TRÌNH"; `id` không tồn tại; `id` = `abc` | 200 và `/api/skill-categories`, `/api/skills` thấy tên mới; 409; 200; 404; 400 | Tích hợp |
| TC254 | D73, D74 | `POST /api/admin/skills`: `{name: "Rust", categoryId: <Lập trình>}`; `{name: "java", …}`; `categoryId` không tồn tại; thiếu `categoryId`; `name` rỗng | 201 với `isActive = true` và có trong `/api/skills`; 409; 404; 400; 400 | Tích hợp |
| TC255 | D73, D75 | Một sinh viên đang dạy Kotlin. `PUT /api/admin/skills/{Kotlin}` với `isActive = false`, rồi `isActive = true` | Sau khi ẩn: 200; `/api/skills` không có Kotlin; `/api/admin/skills` vẫn có; `GET /api/me/teach-skills` của sinh viên vẫn có Kotlin. Sau khi hiện lại: `/api/skills` có Kotlin | Tích hợp |
| TC256 | D74 | `PUT /api/admin/skills/{Python}`: đổi tên thành "PYTHON" (giữ nhóm); đổi tên thành "java"; thiếu `isActive`; `id` không tồn tại | 200; 409; 400; 404 | Tích hợp |
| TC257 | D74, R42 | `PUT /api/admin/skills/{id}` đổi `categoryId` sang "Dữ liệu": với Java (đang liên quan Kotlin); với Python (không có quan hệ). Đổi tên Java, giữ nhóm | 409; 200; 200 | Tích hợp |
| TC258 | D73, D74, D75 | `POST /api/admin/skill-relations`: `{skillAId: <Kotlin>, skillBId: <Python>}` (Kotlin có mã lớn hơn); gửi lại theo thứ tự ngược; `{Java, SQL}`; `{Java, Java}`; một mã không tồn tại; cặp với kỹ năng đang bị ẩn (cùng nhóm, chưa liên quan) | 201, response có `skillAId` < `skillBId` và tên đúng; 409; 400 (khác nhóm); 400; 404; 201 | Tích hợp |
| TC259 | D73 | `GET /api/admin/skill-relations` trên danh mục ban đầu | 9 phần tử, mỗi phần tử đủ `skillAId, skillAName, skillBId, skillBName`, `skillAId < skillBId`, xếp theo `skillAId` rồi `skillBId` | Tích hợp |
| TC260 | D73, spec 4.1 | `DELETE /api/admin/skill-relations/{Kotlin}/{Java}` (thứ tự ngược); gọi lại lần hai. Có An (muốn học Java mục tiêu 3, Tiếng Anh) và Bình (dạy Kotlin, Tiếng Anh) như tuần 4 | 204; 404. Sau khi xóa: không còn cặp Java–Kotlin trong GET; An gọi `/api/matches` thì `forward.skillFit` của Bình là 0.5 (trước khi xóa là 0.75) | Tích hợp |
| TC261 | D74 | `POST /api/admin/skill-relations` `{skillAId: 99999, skillBId: 99999}`; `PUT /api/admin/skills/99999` với `name` rỗng | Cả hai 400, không phải 404 (400 dữ liệu sai được kiểm trước 404) | Tích hợp |

## H. Giao diện A01 (thủ công)

| Mã | Nguồn | Đầu vào | Mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC262 | D76, TK 6.2 | Admin đăng nhập; sinh viên đăng nhập | Admin vào `/admin/skills`, menu chỉ có "Danh mục kỹ năng" và đăng xuất. Sinh viên vào `/profile` như cũ | Thủ công |
| TC263 | D76 | Sinh viên gõ `/admin/skills` | Chuyển về `/profile` | Thủ công |
| TC264 | D76, NFR-08 | Trên A01: thêm kỹ năng, đổi tên, ẩn rồi hiện lại, thêm nhóm, đổi tên nhóm, thêm cặp, xóa cặp; thử thêm tên trùng; mở ở màn hình rộng 360 px | Bảng nhóm theo nhóm, kỹ năng ẩn có đánh dấu; mỗi thao tác hiện đúng sau khi tải lại; lỗi 409/400 hiện `detail` của backend; đọc được ở 360 px, không cuộn ngang | Thủ công |

---

## Không kiểm ở tuần 5

- Thời gian end-to-end của `GET /api/matches` trên 1.000 hồ sơ trong DB (NFR-03): tuần 8 (D61).
- Khóa/mở khóa tài khoản và màn hình quản lý tài khoản: tuần 7 (D52).
- Vi phạm `UNIQUE` khi hai request thêm cùng tên đúng một lúc trả 409 (D74): khó tái hiện ổn định bằng test; kiểm bằng đọc code (exception của DB được đổi thành 409).
- Con số kết quả thực nghiệm (P có tốt hơn B1 không...) không phải ca kiểm thử: chúng là kết quả, ghi ở `docs/ket_qua.md`.

## Những điểm tài liệu chưa quy định rõ (phát hiện khi viết ca)

| # | Chỗ chưa rõ | Ca liên quan | Cách xử lý trong bộ ca |
| --- | --- | --- | --- |
| ⚠1 | Quy tắc "nhiễu xóa hết lịch thì giữ lại một khung thật" cần một số ngẫu nhiên để chọn khung. D64 yêu cầu luôn rút đủ số cho mọi phần tử, nhưng chưa nói số này có rút cho mọi người có khai lịch hay chỉ khi cần | TC202, TC207 | TC202 chỉ so trình độ giữa các mức nhiễu, không so lịch. Chốt ở kế hoạch tuần 5, bước 2 |
| ⚠2 | Câu chữ `detail` tiếng Việt của các lỗi API danh mục kỹ năng chưa quy định | TC252–TC261 | Các ca chỉ so mã lỗi và `errors[].field`. Chốt câu chữ ở kế hoạch tuần 5, bước 9 |
| ⚠3 | `GET /api/admin/skills` xếp theo `name` bằng collation của PostgreSQL (như D40) | TC251 | So thứ tự giữa các tên không dấu, như D41 |
