# QUYẾT ĐỊNH TRIỂN KHAI – TUẦN 5

Chốt ngày 2026-11-11, từ đợt 3 của rà soát (`docs/ra_soat/Bao_cao_ra_soat.md`, R24–R32), các điểm mở ở spec 5.5 (N6, C4, T3, T5, T6) và phần danh mục kỹ năng của R13, R41, R42. Danh sách câu hỏi, các phương án đã cân nhắc và lý do chọn nằm ở `docs/tuan_5/Can_chot_truoc_tuan_5.md` (Q1–Q25, M1–M6). Áp dụng khi làm FR-11 (thực nghiệm), unit test nhóm 4 và FR-08 phần danh mục kỹ năng (màn hình A01).

**Tiêu chí chốt:** người dùng giao chốt theo tiêu chí "ít ảnh hưởng nhất tới việc đã làm" (code, test, tài liệu đã viết, báo cáo đã nộp). Ngoại lệ duy nhất là D54 (nhãn 2 theo R25), vì phương án sát chữ hơn có lỗi đã biết.

File này là tài liệu mới nhất cho phạm vi tuần 5. Nếu khác spec tuần 1, thiết kế tuần 2 hoặc quyết định tuần 3–4 thì file này đúng. Khi thêm hoặc đổi một quyết định ở đây, phải sửa luôn các tài liệu cũ nhắc tới điểm đó trong cùng commit. Mã quyết định đánh số tiếp theo tuần 4 (D52 trở đi).

**Nơi ghi các quy tắc của tuần 5** (file này không chép lại, chỉ trỏ tới):

| Nội dung | Ở đâu |
| --- | --- |
| Quy tắc gán nhãn, giá trị thật và tự khai, rel = 0 | Spec 5.1 |
| Các phương án B0, B1, P, R, danh sách của P dùng để đo, bỏ từng thành phần | Spec 5.2 |
| Precision@5 (hai bản), NDCG@5, IDCG, hai bản trung bình, Coverage, latency | Spec 5.3 |
| Bảng tham số sinh dữ liệu, mức nhiễu, seed, độ nhạy, kiểm định | Spec 5.4 |
| Ví dụ tính tay nhãn và chỉ số | Spec phụ lục A.3 |
| Unit test nhóm 4 | Spec 7.1 |
| API danh mục kỹ năng | TK 4.9 (danh sách), D73–D75 (chi tiết) |

## 1. Phạm vi tuần 5

| Mã | Quyết định |
| --- | --- |
| D52 | **Làm:** module `evaluation` (FR-11), unit test nhóm 4 (spec 7.1), API và màn hình quản trị danh mục kỹ năng A01 (FR-08, D73–D76), tổ chức kết quả theo `docs/To_chuc_ket_qua.md` §5 (`docs/ket_qua.md`, `thuc_nghiem/ket_qua/`, tag `v1.0-thuc-nghiem`, hook chặn ghi đè kết quả; D72). **Không làm:** khóa/mở khóa tài khoản và màn hình quản lý tài khoản (tuần 7, R41); đo end-to-end `GET /api/matches` trên DB (tuần 8, D61); đổi trọng số hay công thức mặc định của hệ thống (D69); sửa lõi `matching` (D68). |

## 2. Nhãn (spec 5.1)

| Mã | Quyết định |
| --- | --- |
| D53 | **Hai lớp dữ liệu và "chưa khai báo".** Mỗi người giả lập có giá trị thật và giá trị tự khai. Chỉ trình độ dạy và lịch rảnh có nhiễu. Việc **có khai hay không** lịch rảnh, hình thức học không có nhiễu: người chưa khai thì phần đó để trống ở **cả hai lớp** (lịch rỗng, hình thức `null`). Bước 1 của nhãn giữ nguyên chữ của spec: chỉ loại vì lịch khi cả hai đã khai báo, và đếm khung trùng trên lịch **thật**. "Hình thức học tương thích hoàn toàn" của nhãn 2 = điều kiện $ModeFit = 1.0$ ở spec 4.3: cả hai đã khai báo và cùng hình thức, hoặc ít nhất một bên `ANY`. Vậy `ANY` + `ONLINE` đạt; bên chưa khai báo thì không đạt (đóng T3, T6). |
| D54 | **Nhãn 2 và nhãn 0 khi học nhiều kỹ năng (R25), mục tiêu bỏ trống.** Nhãn 2: có ít nhất một kỹ năng $s \in W_u$ mà $v$ dạy đúng kỹ năng đó với trình độ **thật** đạt mục tiêu của $u$ cho $s$; mục tiêu bỏ trống **không** tính là đạt; cộng điều kiện lịch, hình thức ở D53. Nhãn 0: không có cặp trùng chính xác nào, và **mọi** cặp $(s, t)$ có $Sim = 0.5$ đều có trình độ thật của $t$ thấp hơn mục tiêu của $s$ từ 2 bậc; mục tiêu bỏ trống không tính là thấp hơn. Nhãn không dùng LevelFit. Lý do giữ R25 dù khác Bảng 4.11 của báo cáo tuần 3: định nghĩa bằng "LevelFit = 1.0" cho người dạy Java (đạt) cộng MySQL (thiếu 1 bậc) nhãn 1, thấp hơn người chỉ dạy Java, dù dạy được nhiều hơn. Lý do chọn "bỏ trống không tính là đạt": giữ nghĩa "LevelFit = 1.0" đang viết (bỏ trống thì LevelFit = 0.5). Ví dụ ở spec A.3. |
| D55 | **Ứng viên có mặt trong danh sách nhưng trượt Bước 1 thật** (ví dụ nhiễu thêm khung giả nên lịch tự khai trùng mà lịch thật không trùng) nhận rel = 0 (R26). Ngược lại, ứng viên bị lọc nhầm trên dữ liệu tự khai không có mặt trong danh sách nhưng vẫn ở trong tập lý tưởng (D57). |

## 3. Phương án và chỉ số (spec 5.2, 5.3)

| Mã | Quyết định |
| --- | --- |
| D56 | **Danh sách của P dùng để đo (N6)** là danh sách đúng như lõi trả về: nhóm hai chiều (xếp theo MutualScore) rồi nhóm một chiều (xếp theo $D(u,v)$), lấy 5 vị trí đầu. Mọi biến thể của P (bỏ thành phần, bộ trọng số, công thức ScheduleFit) dùng cùng cấu trúc. B0, B1, R mỗi phương án một danh sách. Lý do: khớp TK 3.5 ("đo đúng thuật toán đang chạy trong hệ thống"), proposal §2.3 và spec 4.5, không thêm khái niệm. Hạn chế ghi vào báo cáo: nhãn chỉ xét hướng $u$ học từ $v$ nên không thưởng tính hai chiều. |
| D57 | **IDCG@5 (C4)** tính trên tập lý tưởng của $u$: mọi $v \ne u$ qua Bước 1 của nhãn (giá trị thật), xếp theo nhãn giảm dần, lấy 5 đầu. Chung cho mọi phương án, khớp chữ "trong tập dữ liệu" của spec 5.3. B0, B1, P, R dùng chung lọc lịch và hình thức trên dữ liệu tự khai (R26). |
| D58 | **Precision@5 (T5):** giữ đúng định nghĩa spec 5.3 (đếm nhãn 1 và 2) và thêm Precision@5 nghiêm ngặt (chỉ đếm nhãn 2). Báo cáo cả hai; phân tích và kiểm định dựa vào bản nghiêm ngặt và NDCG@5 (R26). |
| D59 | **Lấy trung bình NDCG@5 (R24):** báo cáo hai bản. (1) Theo spec 5.3: mọi người dùng đủ điều kiện, IDCG@5 = 0 thì NDCG@5 = 0, ghi số trường hợp. (2) Theo R24: chỉ người có tập lý tưởng chứa ít nhất 2 mức nhãn khác nhau, ghi số người bị loại; không còn ai thì bản (2) ghi `NaN` (kế hoạch tuần 5, bước 4). Chọn cấu hình (D69) và kiểm định (D62) dùng bản (2). Precision@5 (cả bản nghiêm ngặt), Coverage và hai chỉ số bao phủ chỉ có một bản, trên mọi người dùng đủ điều kiện, đúng các cột `chi_so` của D72; kiểm định D62 vẫn chỉ dùng người thuộc bản (2) cho cả hai chỉ số. |
| D60 | **Coverage (R27):** mẫu số là mọi người dùng có ít nhất một kỹ năng muốn học (trong dữ liệu giả lập là mọi người). Thêm hai chỉ số cho từng phương án: tỷ lệ người có ít nhất 5 ứng viên trong danh sách; tỷ lệ người có ít nhất một ứng viên nhãn 2 trong danh sách (cả danh sách, không chỉ 5 đầu). |
| D61 | **Latency (R30):** đo trong bộ nhớ, N = 1000, seed 3, nhiễu mặc định, đơn luồng, cho từng phương án. Chạy một lượt làm nóng trên mọi người dùng (không ghi), rồi đo thời gian tạo danh sách của từng người bằng `System.nanoTime()`: gồm lọc, chấm điểm, xếp hạng; không gồm gán nhãn và tính chỉ số. Ghi trung bình và p95 (phân vị theo thứ hạng gần nhất), đơn vị ms; `README.md` ghi CPU, RAM, phiên bản Java. B0, B1 chạy qua cùng lõi với P (D65) nên số của chúng gần P; ghi chú điều này. Đo end-to-end `/api/matches` để tuần 8. |
| D62 | **Kiểm định (R31; giữ nguyên TCKQ:29).** Dữ liệu: N = 500, nhiễu mặc định, gộp người dùng của seed 3–5, chỉ người thuộc bản (2) của D59. So sánh: P − B1 và P − từng phương án bỏ thành phần (4 cặp), trên NDCG@5 và Precision@5 nghiêm ngặt của từng người (8 phép so sánh, không hiệu chỉnh đa so sánh, báo cáo ghi rõ số phép). **Bootstrap ghép cặp:** lấy mẫu lại có hoàn lại $n$ người, 2000 lần, `new Random(3_000_000)`; khoảng tin cậy 95% là phân vị 2.5% và 97.5% của 2000 hiệu trung bình (phần tử thứ $\lceil q \cdot 2000 \rceil$ sau khi sắp xếp). **Wilcoxon theo cặp:** bỏ cặp có hiệu tuyệt đối dưới $10^{-9}$; xếp hạng giá trị tuyệt đối của hiệu, bằng nhau (lệch dưới $10^{-9}$ so với phần tử đầu nhóm, cùng ngưỡng bỏ hiệu 0) thì lấy hạng trung bình; $W^+$ = tổng hạng của hiệu dương; $z = (W^+ - n(n+1)/4) / \sqrt{n(n+1)(2n+1)/24 - \sum (t^3 - t)/48}$ với $t$ là cỡ từng nhóm bằng nhau; không hiệu chỉnh liên tục; $p = 2(1 - \Phi(|z|))$. Không còn cặp nào khác nhau thì ghi "không có cặp khác nhau". Bảng chính ghi trung bình ± độ lệch chuẩn (mẫu, chia $k - 1$) trên 3 seed báo cáo. |

## 4. Dữ liệu giả lập (spec 5.4)

| Mã | Quyết định |
| --- | --- |
| D63 | **Tham số sinh dữ liệu (R29, T6):** theo bảng ở spec 5.4. Danh mục chép từ `V2__seed_skills.sql` thành hằng số trong `evaluation`, có test đối chiếu 6 nhóm, 26 kỹ năng, 9 cặp và tên từng cặp. Quy mô 50 (thưa, R27), 100, 500, 1000. Ba mức nhiễu: không nhiễu, mặc định (0.3, 0.2, 0.05), cao (0.5, 0.4, 0.1). |
| D64 | **Tái lập (NFR-06).** Ba luồng số ngẫu nhiên tách riêng: dữ liệu thật `new Random(seed)`; nhiễu `new Random(seed + 1_000_000)`; mốc R `new Random(seed + 2_000_000 + userId)` cho từng người. Nhờ vậy cùng seed thì dữ liệu thật giống nhau ở mọi mức nhiễu. Bộ sinh nhiễu luôn rút đủ số ngẫu nhiên cho mọi phần tử (kể cả khi xác suất bằng 0), để mức nhiễu cao hơn dùng cùng dãy số và chỉ thêm thay đổi. Không rút số ngẫu nhiên trong lúc duyệt `Set` hay `Map` không có thứ tự: `Set.copyOf` (trong `MatchProfile`) có thứ tự duyệt đổi giữa các lần khởi động JVM. Bộ sinh chỉ duyệt `List` có thứ tự cố định (kỹ năng theo thứ tự của V2, khung giờ theo ngày rồi buổi). |

## 5. Cách chạy các phương án và cấu hình

| Mã | Quyết định |
| --- | --- |
| D65 | **B0, B1, R không cần code xếp hạng riêng (Q15).** B1: `MatchingEngine` với quan hệ kỹ năng thật và bộ trọng số (1, 0, 0, 0, 0), nên $D = SkillFit$ và tập ứng viên bằng tập của P. B0: như B1 nhưng `SkillSimilarity` không có quan hệ nào, nên chỉ giữ ứng viên trùng chính xác và $SkillFit \times |W_u|$ = số kỹ năng trùng. Với B0, B1: gộp hai nhóm của lõi, dựng lại `MatchResult` với `score` = `forward().d()` (SkillFit chiều $u$ học từ $v$), xếp bằng `RankingComparator` (D36). R: lấy tập ứng viên của B1 xếp theo `userId` tăng dần, xáo bằng luồng R của D64. |
| D66 | **Bỏ từng thành phần (spec 5.2):** trọng số bị bỏ chia đều cho 4 thành phần còn lại, kể cả Reputation, đúng chữ spec. Ba bộ (SkillFit, LevelFit, ScheduleFit, ModeFit, Reputation): bỏ LevelFit (0.60, 0, 0.20, 0.10, 0.10); bỏ ScheduleFit (0.5875, 0.2375, 0, 0.0875, 0.0875); bỏ ModeFit (0.5625, 0.2125, 0.1625, 0, 0.0625). Hạn chế ghi vào báo cáo: tỷ lệ giữa các thành phần còn lại cũng đổi, nên kết quả lẫn hai thay đổi. |
| D67 | **Giữ 4 bộ trọng số độ nhạy** như spec 5.4 và RQ3 của báo cáo tuần 1, tuần 3, kể cả "Ưu tiên uy tín" (R28 chọn nhánh "ghi rõ nó đo gì"). Báo cáo ghi: mọi người giả lập có Reputation = 0.5, nên cấu hình này chỉ giảm tỷ trọng các thành phần khác; Reputation không đánh giá được bằng nhãn trên dữ liệu giả lập. |
| D68 | **So sánh công thức ScheduleFit không sửa lõi (Q16).** `evaluation` lấy `ScoreBreakdown` của lõi, thay ScheduleFit bằng công thức khác, tính lại $D$ bằng tổng trọng số nhân 5 thành phần, tính lại MutualScore $= \sqrt{D_{forward} \cdot D_{backward}}$ cho nhóm hai chiều, rồi xếp lại từng nhóm bằng `RankingComparator`. Công thức: `MIN` (của lõi), `JACCARD` = số khung trùng / số khung của hợp hai lịch, `TRAN_3` = min(số khung trùng, 3) / 3; một bên chưa khai báo thì 0.5 ở cả ba. Lọc cứng và phân loại hai nhóm không đổi, vì cả ba công thức đều > 0 khi và chỉ khi có khung trùng. Có test: dùng `MIN` thì điểm tính lại bằng điểm của lõi (dung sai $10^{-9}$). |
| D69 | **Chọn cấu hình, không đổi mặc định của hệ thống (Q17).** Tiêu chí: NDCG@5 bản (2) của D59, trung bình trên seed 1–2, N = 500, nhiễu mặc định. Chọn riêng bộ trọng số tốt nhất (với `MIN`) và công thức ScheduleFit tốt nhất (với bộ mặc định), theo spec 5.4; chênh dưới $10^{-9}$ thì giữ mặc định. Kết quả chọn được ghi bổ sung vào quyết định này **trước** khi chạy seed 3–5. `MatchingWeights.DEFAULT`, công thức ScheduleFit của lõi và API không đổi trong tuần 5; có đổi hay không là quyết định riêng sau khi xem khoảng tin cậy. |
| D70 | **Các tổ hợp chạy (Q18).** Giai đoạn chọn (seed 1–2, N = 500, nhiễu mặc định): P với 4 bộ trọng số (công thức `MIN`); P với 3 công thức (bộ mặc định). Giai đoạn báo cáo (seed 3–5): (a) **bảng chính** N = 500, nhiễu mặc định: B0, B1, R, P, 3 phương án bỏ thành phần, và P với cấu hình đã chọn nếu khác mặc định, kèm kiểm định D62; (b) **theo quy mô** N = 50, 100, 500, 1000, nhiễu mặc định: B0, B1, R, P, mọi chỉ số (đáp ứng mục tiêu 3 của báo cáo tuần 1); (c) **theo mức nhiễu** N = 500, 3 mức: B1, P, 3 phương án bỏ thành phần; (d) **nhóm khai ít khung** (spec 5.4) N = 500, nhiễu mặc định, P với 3 công thức: Precision@5 (hai bản) và NDCG@5 riêng cho người dùng $u$ có lịch tự khai từ 1 đến 3 khung, và chỉ số phụ "tỷ lệ vị trí trong top 5 do ứng viên khai 1–3 khung chiếm" so với tỷ lệ của họ trong danh sách ứng viên; (e) **latency** theo D61. |

## 6. Chạy và lưu kết quả

| Mã | Quyết định |
| --- | --- |
| D71 | **Cách chạy (R22).** Package `com.skillexchange.evaluation` là Java thuần như lõi (D34): không import `org.springframework`, không dùng DB, test JUnit thuần. Điểm vào `EvaluationMain`, chạy từ `backend/`: `.\mvnw.cmd -q compile` rồi `java -cp target/classes com.skillexchange.evaluation.EvaluationMain --giai-doan chon --ten <tên>`; giai đoạn báo cáo thêm `--trong-so <mac_dinh\|ky_nang\|lich\|uy_tin>` và `--schedule <min\|jaccard\|tran3>` (cấu hình đã chọn ở D69). `--out <thư mục>` thay thư mục kết quả mặc định (dùng cho test). Không thêm dependency. Chương trình tự gọi `git rev-parse --short HEAD` và `git status --porcelain -- .` để ghi commit; giai đoạn báo cáo **dừng** nếu `backend/` có thay đổi chưa commit, để mọi con số truy được về một commit. |
| D72 | **Định dạng kết quả (R22; TCKQ §3).** Mỗi lượt chạy một thư mục mới `thuc_nghiem/ket_qua/<yyyy-MM-dd>_<tên>/`; thư mục đã tồn tại thì dừng, không ghi đè. Gồm: `README.md` (lệnh, giai đoạn, seed, commit, có thay đổi chưa commit không, CPU, RAM, phiên bản Java, thời gian chạy); `metrics.csv` dạng dài, cột `seed, vai_tro_seed, n, muc_nhieu, phuong_an, chi_so, gia_tri, so_nguoi_tinh`; `per_user.csv`, cột `seed, n, muc_nhieu, phuong_an, user_id, so_ung_vien, precision_at_5, precision_at_5_nghiem_ngat, ndcg_at_5, so_muc_nhan_ly_tuong`; `kiem_dinh.csv` (D62); `log.txt`. Giá trị: `vai_tro_seed` ∈ {`chon`, `bao_cao`}; `muc_nhieu` ∈ {`khong`, `mac_dinh`, `cao`}; `phuong_an` ∈ {`B0`, `B1`, `R`, `P`, `P_bo_level`, `P_bo_schedule`, `P_bo_mode`, `P_w_ky_nang`, `P_w_lich`, `P_w_uy_tin`, `P_sf_jaccard`, `P_sf_tran3`}; `chi_so` ∈ {`precision_at_5`, `precision_at_5_nghiem_ngat`, `ndcg_at_5_tat_ca`, `ndcg_at_5_loai_suy_bien`, `so_ca_idcg_0`, `so_nguoi_bi_loai`, `coverage`, `ty_le_du_5_ung_vien`, `ty_le_co_nhan_2`, `latency_tb_ms`, `latency_p95_ms`}, cộng các chỉ số nhóm khai ít khung của D70 (d) có hậu tố `_it_khung`. Lượt chạy hỏng vẫn giữ thư mục, ghi lý do trong `README.md`. Gắn tag `v1.0-thuc-nghiem` cho commit chạy seed báo cáo; `docs/ket_qua.md` ghi mọi con số đưa vào báo cáo kèm nguồn, commit, ngày. Hook `.claude/hooks/` chặn Write/Edit file có sẵn trong `thuc_nghiem/ket_qua/` và lệnh Bash xóa hay ghi đè thư mục này (TCKQ §1.2 nguyên tắc 5). |

## 7. Quản trị danh mục kỹ năng (A01)

| Mã | Quyết định |
| --- | --- |
| D73 | **API (TK 4.9; phần kỹ năng của R41).** `GET /api/admin/skills` → 200 `[{id, name, categoryId, categoryName, isActive}]`, mọi kỹ năng kể cả bị ẩn, xếp theo `name` rồi `id` như D40. `POST /api/admin/skill-categories` `{name}` → 201 `{id, name}`. `PUT /api/admin/skill-categories/{id}` `{name}` → 200 `{id, name}`. `POST /api/admin/skills` `{name, categoryId}` → 201 cùng dạng phần tử của GET, `isActive = true`. `PUT /api/admin/skills/{id}` `{name, categoryId, isActive}` (bắt buộc đủ ba trường) → 200 cùng dạng. `GET /api/admin/skill-relations` → 200 `[{skillAId, skillAName, skillBId, skillBName}]`, `skillAId < skillBId`, xếp theo `skillAId` rồi `skillBId`. `POST /api/admin/skill-relations` `{skillAId, skillBId}` theo thứ tự bất kỳ → 201 một phần tử cùng dạng, đã đổi về `skillAId < skillBId`. `DELETE /api/admin/skill-relations/{aId}/{bId}`, hai mã theo thứ tự bất kỳ → 204. Danh sách nhóm dùng `GET /api/skill-categories` có sẵn (D40, quyền *User*). Không có API xóa kỹ năng hay nhóm. Quyền: luật `hasRole("ADMIN")` cho `/api/admin/**` đã có; sinh viên 403, không token 401, admin bị `LOCKED` 403 (D13). |
| D74 | **Kiểm tra dữ liệu và mã lỗi (R13, R42).** `name` cắt khoảng trắng hai đầu bằng `strip()` như D15; rỗng sau khi cắt hoặc dài hơn 100 ký tự thì 400 dạng `errors[{field, message}]`; thiếu `categoryId`, `isActive`, `skillAId`, `skillBId` thì 400. Trùng tên kỹ năng (hoặc tên nhóm) với một kỹ năng (nhóm) **khác**, so không phân biệt hoa thường sau khi cắt: 409; đổi tên chính mình khác hoa thường (`Java` → `JAVA`) được. Kiểm ở service, không thêm migration; vi phạm `UNIQUE` của DB khi hai request cùng lúc cũng trả 409, không 500. Không tìm thấy kỹ năng, nhóm (cả `{id}` trên đường dẫn lẫn mã trong body) hoặc quan hệ khi `DELETE`: 404; `id` không phải số: 400 (D38). Quan hệ: hai mã bằng nhau 400; hai kỹ năng khác nhóm 400; đã có 409. Đổi `categoryId` của kỹ năng đang có ít nhất một quan hệ: 409 (R42); đổi tên, ẩn, hiện không bị chặn. Thứ tự kiểm: 400 dữ liệu sai → 404 → 409. |
| D75 | **Kỹ năng bị ẩn** (`isActive = false`): không hiện trong `/api/skills` (D40) nên không chọn mới được ở S04, S10; người đang có vẫn giữ; vẫn dùng trong matching và tìm kiếm (spec 4.1); quan hệ liên quan giữ nguyên; vẫn được tạo quan hệ mới với kỹ năng bị ẩn. Hiện lại bằng `isActive = true`. |
| D76 | **Giao diện A01** ở đường dẫn `/admin/skills`, sau `RequireAuth`. Sau đăng nhập, giao diện đọc `role` từ `/api/me` (đã có): `ADMIN` vào `/admin/skills`, `STUDENT` vào `/profile` như cũ. Menu của admin chỉ có "Danh mục kỹ năng" (A02 thêm ở tuần 7). Sinh viên mở `/admin/*` thì chuyển về `/profile`; quyền vẫn kiểm ở backend. A01 gồm: bảng kỹ năng nhóm theo `categoryName`, đánh dấu kỹ năng đã ẩn, sửa tên/nhóm, ẩn/hiện; thêm kỹ năng; thêm, đổi tên nhóm; danh sách cặp liên quan, thêm cặp, xóa cặp. Lỗi hiện `detail` của backend bằng `Notice`; giao diện không kiểm lặp quy tắc của D74. TK 6.2 ghi luồng này bằng chữ, không vẽ lại sơ đồ. |

## 8. Tài liệu đã nộp

| Mã | Quyết định |
| --- | --- |
| D77 | **Không sửa proposal và các báo cáo đã nộp** (cùng tiền lệ của đợt 2 rà soát). Câu "Qua đó chứng minh" ở proposal §2.3 (R27) và định nghĩa nhãn 2 bằng "LevelFit = 1.0" ở Bảng 4.11 của báo cáo tuần 3 (D54) được nêu trong mục "Điều chỉnh so với báo cáo trước" của báo cáo tuần 5, kèm lý do. Theo thứ bậc tài liệu, spec đúng. |

## 9. Việc cần xong trước khi code tuần 5

- Spec 5.1–5.5, A.2, A.3, 7.1, TK 3.5, 4.9, 6.2, 7, rà soát và CLAUDE.md đã sửa ngược theo các quyết định trên (Bước 0, ngày 2026-11-11).
- Ca kiểm thử tuần 5 viết từ tài liệu: `docs/tuan_5/kiem_thu/ca_kiem_thu.md`.
- Kế hoạch tuần 5: `docs/tuan_5/ke_hoach_tuan_5.md`.
