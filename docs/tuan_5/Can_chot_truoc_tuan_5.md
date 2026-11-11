# VIỆC CẦN CHỐT VÀ SỬA TRƯỚC KHI CODE TUẦN 5

Lập ngày 2026-11-11, sau khi rà proposal (tuần 5), spec mục 5, thiết kế tuần 2, quyết định tuần 3–4, báo cáo rà soát và code `matching/`.

File này là danh sách tạm. *Cập nhật 2026-11-11: Bước 0 đã làm xong; các quyết định chính thức nằm ở `Quyet_dinh_tuan_5.md`, file này chỉ còn giá trị lịch sử (lý do chọn).* Bước 0 chuyển nội dung vào `docs/tuan_5/Quyet_dinh_tuan_5.md` (mã quyết định từ D52) và sửa ngược vào các tài liệu ở mục 5. Sau đó file này chỉ còn giá trị lịch sử.

**Ký hiệu:** Q = câu hỏi cần chốt, M = mâu thuẫn giữa các tài liệu, R = mục trong `docs/ra_soat/Bao_cao_ra_soat.md`, N6/C4/T3/T5/T6 = mã ở spec 5.5. Dòng của spec ghi theo file `docs/tuan_1/Phan_tich_yeu_cau_Skill_Exchange_Tuan_1.md` tại ngày lập.

## 0. Cách đã chốt

Ngày 2026-11-11, người dùng giao chốt mọi mục theo tiêu chí: **chọn phương án ít ảnh hưởng nhất tới việc đã làm** (code, test, tài liệu đã viết, báo cáo đã nộp). Tiêu chí áp dụng cho cả mục 2 và mục 3.

**Ngoại lệ duy nhất là Q2:** giữ quyết định R25, vì phương án ít ảnh hưởng hơn có lỗi đã biết (xem Q2).

Áp tiêu chí này cho cả mục 3 làm đổi 5 đề xuất của bản lập ban đầu:
- Q4: "chưa khai báo" giống nhau ở cả hai lớp dữ liệu, giữ nguyên chữ của Bước 1.
- Q7, Q8: báo cáo cả hai bản chỉ số, không định nghĩa lại.
- Q14: giữ cấu hình "Ưu tiên uy tín".
- Q16: không sửa lõi matching.

M6 cũng đổi: không sửa proposal.

**Kết quả:** code và test tuần 3–4 không đổi; spec mục 4, proposal và DB không đổi; báo cáo đã nộp chỉ lệch một chỗ (mục 6).

| Mã | Chốt |
| --- | --- |
| Q1 | Đo trên danh sách lõi trả về: nhóm hai chiều rồi nhóm một chiều, lấy 5 đầu |
| Q2 | Nhãn 2: có một kỹ năng trùng chính xác mà trình độ thật đạt mục tiêu (R25) |
| Q3 | Mục tiêu bỏ trống **không** tính là đạt |
| Q4 | "Chưa khai báo" rỗng ở cả giá trị thật lẫn tự khai; "tương thích hoàn toàn" = điều kiện ModeFit = 1.0 của spec 4.3 |
| Q6 | IDCG trên tập lý tưởng lấy từ giá trị thật |
| Q7, Q8 | Báo cáo cả bản theo spec lẫn bản theo R24, R26 |
| Q10 | Bootstrap + Wilcoxon + trung bình ± độ lệch chuẩn |
| Q13 | Chia đều trọng số, đúng chữ spec 5.2 |
| Q14 | Giữ 4 cấu hình trọng số |
| Q16 | Không sửa lõi; `evaluation` tự tính lại điểm với công thức ScheduleFit khác |
| Q17 | Không đổi mặc định của hệ thống trong tuần 5 |

## 1. Phạm vi tuần 5

Proposal §7 chỉ ghi phần thực nghiệm. Spec §9 (điều chỉnh 2026-10-29 theo R46) và TK §7 thêm quản trị danh mục kỹ năng. Theo thứ bậc tài liệu, tuần 5 gồm:

- FR-11: sinh dữ liệu giả lập, gán nhãn, chạy B0 / B1 / P / R, đo chỉ số, phân tích độ nhạy, bỏ từng thành phần.
- Unit test nhóm 4 (spec 7.1): gán nhãn, chỉ số, tái lập theo seed.
- FR-08 phần danh mục kỹ năng: API admin và màn hình A01.
- Tổ chức kết quả (`docs/To_chuc_ket_qua.md` §5): `docs/ket_qua.md`, `thuc_nghiem/ket_qua/`, tag `v1.0-thuc-nghiem`, hook chặn ghi đè thư mục kết quả.

## 2. Sáu mục có nhiều phương án

### Q1 (N6). Đo chỉ số trên danh sách nào của P?

P trả hai danh sách: hai chiều xếp theo MutualScore, một chiều xếp theo D(u,v). Nhãn chỉ gán theo hướng "u học từ v". B0 và B1 chỉ có một danh sách.

| Phương án | Hệ quả |
| --- | --- |
| (a) Danh sách chính xếp theo D(u,v), thêm bảng phụ cho nhóm hai chiều | Cần thêm khái niệm mới vào spec (nhãn của cặp, biến thể B1). Bảng chính đo một cách xếp mà hệ thống không dùng, trái với TK 3.5: "thực nghiệm đo đúng thuật toán đang chạy trong hệ thống" |
| (b) Như (a), bỏ bảng phụ | Như (a), và MutualScore không được đánh giá, trái với proposal §2.3 ("P dùng D(u,v) và MutualScore") |
| **(c) ✔ Danh sách đúng như lõi trả về:** nhóm hai chiều (theo MutualScore) rồi nhóm một chiều (theo D), lấy 5 đầu | Không thêm khái niệm. Khớp TK 3.5, proposal §2.3, spec 4.5 ("nhóm hai chiều được ưu tiên") và báo cáo tuần 1 dòng 136 |

**Chốt: (c).** Hạn chế phải ghi vào báo cáo: nhãn không thưởng tính hai chiều, nên P có thể bị điểm thấp khi đặt một cặp hai chiều nhãn 1 lên trên một ứng viên một chiều nhãn 2. RQ2 không bị ảnh hưởng, vì RQ2 so P với các phương án bỏ thành phần, và các phương án này dùng cùng cấu trúc hai nhóm.

### Q3. Kỹ năng muốn học bỏ trống mục tiêu có tính là "đạt mục tiêu" ở nhãn 2 không?

| Phương án | Hệ quả |
| --- | --- |
| Có | Đổi nghĩa so với spec 5.1 và Bảng 4.11 của báo cáo tuần 3: hai chỗ này ghi "đạt mục tiêu (LevelFit = 1.0)", mà bỏ trống mục tiêu thì LevelFit = 0.5 |
| **✔ Không** | Giữ đúng nghĩa đang viết. Kỹ năng bỏ trống mục tiêu không góp được nhãn 2 nhưng cũng không làm mất nhãn 2 mà kỹ năng khác đem lại (nhờ cách viết "có một kỹ năng" của Q2). Người chỉ có kỹ năng bỏ trống mục tiêu sẽ không có nhãn 2 nào; R24 loại họ khỏi bản trung bình thứ hai (Q8) và báo cáo số người bị loại |

**Chốt: Không.** Bỏ trống mục tiêu cũng không tính là "thấp hơn từ 2 bậc" ở nhãn 0.

### Q6 (C4). IDCG@5 tính trên tập ứng viên nào?

| Phương án | Hệ quả |
| --- | --- |
| **(a) ✔ Tập lý tưởng lấy từ giá trị thật:** mọi v qua Bước 1 của nhãn, xếp theo nhãn giảm dần. Dùng chung cho mọi phương án | Khớp chữ "trong tập dữ liệu" của spec 5.3 dòng 246. Mỗi người dùng có một mẫu số chung cho mọi phương án. Ứng viên tốt bị lọc nhầm vì nhiễu vẫn nằm trong IDCG |
| (b) Tập ứng viên chung của B1/P sau lọc trên dữ liệu tự khai | Phải sửa chữ "trong tập dữ liệu" |
| (c) Danh sách riêng của từng phương án | B0 dễ có NDCG cao giả tạo, đúng lý do C4 được nêu ra |

**Chốt: (a).** Đi kèm R26 (đã chốt, chỉ cần ghi vào spec): B0, B1, P, R dùng chung bước lọc lịch rảnh và hình thức học trên dữ liệu tự khai.

### Q10 (R31). Kiểm định thống kê

R31 được ghi "chốt theo gợi ý" nhưng gợi ý có hai phương án nối bằng chữ "hoặc" (M3). `docs/To_chuc_ket_qua.md:29` đòi khoảng tin cậy và p-value. Spec 5.4 và báo cáo tuần 1 dòng 84 đòi trung bình và độ lệch chuẩn.

| Phương án | Hệ quả |
| --- | --- |
| (a) Bootstrap, bỏ p-value | Phải sửa TCKQ:29 |
| **(b) ✔ Bootstrap + Wilcoxon + trung bình ± độ lệch chuẩn** | Đáp ứng đủ TCKQ:29, spec 5.4 và báo cáo tuần 1. Không sửa tài liệu nào, chỉ thêm code |
| (c) Chỉ trung bình ± độ lệch chuẩn | Phải sửa TCKQ:29 |

**Chốt: (b).** Chi tiết:
- **Phạm vi so sánh:** N = 500, nhiễu mặc định, gộp người dùng của 3 seed báo cáo.
- **Các cặp so sánh:** P − B1 và P − từng phương án bỏ thành phần, trên NDCG@5 và Precision@5 nghiêm ngặt của từng người.
- **Bootstrap ghép cặp:** lấy mẫu lại người dùng 2000 lần, mỗi người giữ nguyên kết quả của cả hai phương án, seed cố định, cho khoảng tin cậy 95% của hiệu trung bình.
- **Wilcoxon theo cặp:** dùng xấp xỉ chuẩn, bỏ các cặp có hiệu bằng 0, có hiệu chỉnh cho các giá trị bằng nhau.
- **Đa so sánh:** không hiệu chỉnh; báo cáo ghi rõ số phép so sánh.
- **Bảng chính:** trung bình ± độ lệch chuẩn trên 3 seed báo cáo.

### Q13. Bỏ từng thành phần: trọng số của thành phần bị bỏ chia lại thế nào?

| Phương án | Hệ quả |
| --- | --- |
| Chia theo tỷ lệ | Phải sửa spec 5.2 |
| **✔ Chia đều cho 4 thành phần còn lại, kể cả Reputation** (đúng chữ spec 5.2) | Không sửa tài liệu. Nhược điểm: trọng số các thành phần còn lại cũng đổi tỷ lệ, nên kết quả lẫn hai thay đổi |

**Chốt: chia đều.** Ba bộ trọng số (SkillFit, LevelFit, ScheduleFit, ModeFit, Reputation):
- Bỏ LevelFit: (0.60, 0, 0.20, 0.10, 0.10).
- Bỏ ScheduleFit: (0.5875, 0.2375, 0, 0.0875, 0.0875).
- Bỏ ModeFit: (0.5625, 0.2125, 0.1625, 0, 0.0625).

Nhược điểm trên ghi vào phần hạn chế.

### Q17. Chọn cấu hình bằng tiêu chí nào, và có đổi mặc định của hệ thống không?

| Phương án | Hệ quả |
| --- | --- |
| **(a) ✔ Không đổi** `MatchingWeights.DEFAULT` và công thức ScheduleFit trong tuần 5. "Cấu hình đã chọn" chỉ là một kết quả để báo cáo | Không chạm code, test, phụ lục A.3, JSON mẫu ở TK 4.5. Khớp câu "trước khi giữ hay đổi" của spec 4.3, vì việc đổi được hoãn thành quyết định riêng |
| (b) Cấu hình nào tốt hơn thì đổi luôn | Phải sửa `MatchingService.java:71`, test TC135 (D = 0.94), các test ScheduleFit (2/3), A.3, JSON mẫu ở TK 4.5 và số liệu trong báo cáo tuần 4 |

**Chốt: (a).** Tiêu chí chọn: NDCG@5 (bản loại người suy biến, Q8) trung bình trên seed 1–2, N = 500, nhiễu mặc định. Chọn riêng bộ trọng số tốt nhất (với công thức ScheduleFit mặc định) và công thức ScheduleFit tốt nhất (với bộ trọng số mặc định), theo spec 5.4. Bằng điểm thì giữ cấu hình mặc định. Trên seed 3–5 chạy cấu hình mặc định và cấu hình đã chọn.

## 3. Các mục chỉ có một lựa chọn hợp lý (đã chốt)

### 3.1. Nhãn và chỉ số

| Mã | Câu hỏi | Chốt |
| --- | --- | --- |
| Q2 (R25, C5) | Nhãn 2 và nhãn 0 khi người học muốn học nhiều kỹ năng | **Nhãn 2:** có ít nhất một kỹ năng trùng chính xác mà trình độ **thật** của v đạt mục tiêu (bỏ trống thì không đạt, Q3), đồng thời đạt điều kiện lịch và hình thức học ở Q4. **Nhãn 0:** Sim lớn nhất bằng 0.5 và **mọi** cặp kỹ năng có Sim > 0 đều có trình độ thật thấp hơn mục tiêu từ 2 bậc. Thêm ví dụ tính tay vào A.3. **Đây là ngoại lệ của tiêu chí:** cách hiểu sát chữ hơn ("LevelFit tính trên trình độ thật bằng 1.0") có lỗi đã nêu ở R25. Ví dụ u muốn học Java và SQL: người chỉ dạy Java (đạt mục tiêu) được nhãn 2, còn người dạy Java (đạt) cộng MySQL (thiếu 1 bậc) chỉ được nhãn 1, dù dạy được nhiều hơn |
| Q4 (T3, T6) | Người "chưa khai báo" có giá trị thật không? `ANY` + `ONLINE` có "tương thích hoàn toàn" không? | Việc khai hay không khai không có nhiễu: người chưa khai lịch thì lịch rỗng ở **cả hai lớp**; người chưa khai hình thức học thì để trống ở cả hai lớp. **Bước 1 của nhãn giữ nguyên chữ** (chỉ loại khi cả hai đã khai báo), riêng phần đếm khung trùng dùng lịch thật. Nhãn 2 cần ≥ 2 khung trùng trên lịch thật, nên cả hai phải đã khai lịch. "Hình thức học tương thích hoàn toàn" = điều kiện ModeFit = 1.0 có sẵn ở spec 4.3: cả hai đã khai báo và cùng hình thức hoặc ít nhất một bên `ANY`. Vậy `ANY` + `ONLINE` có tính; bên chưa khai báo thì không |
| Q5 (R26) | Ứng viên qua lọc trên dữ liệu tự khai nhưng trượt Bước 1 khi xét lịch thật (ví dụ nhiễu thêm khung giả) | Có mặt trong danh sách, nhận rel = 0 |
| Q7 (T5, R26) | Precision@5 có tính nhãn 1 không? | Giữ Precision@5 đúng định nghĩa spec 5.3 (nhãn ≥ 1) và thêm **Precision@5 nghiêm ngặt** (chỉ nhãn 2). Báo cáo cả hai; phần phân tích dựa chủ yếu vào bản nghiêm ngặt và NDCG@5 (R26). Ví dụ A.3 [2, 0, 1, 0, 0]: Precision@5 = 0.4, bản nghiêm ngặt = 0.2 |
| Q8 (R24, M4) | Lấy trung bình Precision và NDCG trên những người dùng nào? | Báo cáo hai bản: (1) theo spec 5.3: mọi người dùng, IDCG = 0 thì NDCG = 0, ghi số trường hợp; (2) theo R24: chỉ người có ít nhất 2 mức nhãn khác nhau trong tập lý tưởng, ghi số người bị loại. Chọn cấu hình (Q17) và kiểm định (Q10) dùng bản (2) |
| Q9 (R27) | Coverage gần như luôn bằng 1 | Dùng độ phổ biến kỹ năng lệch (Q12), thêm kịch bản thưa N = 50. Thêm 2 chỉ số: tỷ lệ người có ≥ 5 ứng viên, tỷ lệ người có ≥ 1 ứng viên nhãn 2. Mẫu số của Coverage: mọi người có ít nhất một kỹ năng muốn học |
| Q11 (R30) | Đo latency thế nào? | Đo trong bộ nhớ, N = 1000, cho từng phương án. Chạy làm nóng một lượt trước, rồi đo từng người bằng `System.nanoTime()`; ghi trung bình, p95, CPU, RAM. B0 và B1 chạy qua cùng lõi với P (Q15), nên latency của chúng gần bằng P; ghi chú điều này. Đo end-to-end `GET /api/matches` để tuần 8 |

### 3.2. Tham số dữ liệu giả lập (Q12, R29)

| Tham số | Chốt |
| --- | --- |
| Danh mục kỹ năng | Chép từ `V2__seed_skills.sql` (6 nhóm, 26 kỹ năng, 9 cặp liên quan) thành hằng số trong `evaluation`; có test đối chiếu số lượng |
| Kỹ năng dạy | 2–5 kỹ năng mỗi người, chọn theo phân bố lệch Zipf (s = 1) trên thứ tự kỹ năng xáo theo seed: vài kỹ năng rất nhiều người dạy, nhiều kỹ năng ít người dạy |
| Kỹ năng muốn học | 1–3 kỹ năng, chọn đều; không trùng kỹ năng dạy (D19) |
| Trình độ thật, mục tiêu | Trình độ chọn đều 1–4. Mục tiêu: 30% bỏ trống, còn lại chọn đều 1–4 |
| Lịch thật | Người có khai báo: 3–9 khung trên 21 khung, chọn đều |
| Hình thức học | Người có khai báo: `ONLINE` 35%, `OFFLINE` 25%, `ANY` 40% |
| Chưa khai báo (T6) | 15% không khai lịch, 15% không khai hình thức học, hai việc độc lập nhau. Để trống ở cả hai lớp (Q4) |
| Mức nhiễu | 3 mức: không nhiễu; mặc định ($p_{level}$ 0.3, $p_{miss}$ 0.2, $p_{extra}$ 0.05, đúng số ví dụ ở spec 5.4); cao (0.5, 0.4, 0.1) |
| Ca biên của nhiễu | Trình độ 1 chỉ dịch lên, trình độ 4 chỉ dịch xuống. Nhiễu xóa hết lịch của người đã khai thì giữ lại 1 khung thật, để người đó không thành "chưa khai báo" |
| Reputation, trạng thái | Mọi người có Reputation = 0.5, đều `ACTIVE` + `STUDENT` |
| Mã người dùng | Gán 1..N sau khi xáo thứ tự theo seed. Cần thiết vì B0, B1 bằng điểm rất nhiều và khi bằng điểm thì xếp theo mã |
| Quy mô | 50 (kịch bản thưa, thêm mới), 100, 500, 1000 |
| Seed | Seed 1–2 dùng để chọn cấu hình, seed 3–5 dùng để báo cáo |

### 3.3. Phương án và cấu hình

| Mã | Câu hỏi | Chốt |
| --- | --- | --- |
| Q14 (R28) | Cấu hình "Ưu tiên uy tín" | **Giữ** đủ 4 cấu hình như spec 5.4 và RQ3 của báo cáo tuần 1, tuần 3. Ghi rõ trong báo cáo: dữ liệu giả lập có Reputation = 0.5 cho mọi người, nên cấu hình này thực chất chỉ giảm tỷ trọng của các thành phần khác; Reputation không đánh giá được bằng nhãn. Bảng §9 của rà soát gợi ý "bỏ", nhưng phần chi tiết của R28 cho phép "hoặc ghi rõ nó đo gì"; theo tiêu chí ở mục 0, chọn cách thứ hai |
| Q15 | B0, B1 chạy thế nào? | Cả hai chạy qua `MatchingEngine` với bộ trọng số (1, 0, 0, 0, 0), nên D = SkillFit. B1 dùng quan hệ kỹ năng thật, nên có cùng tập ứng viên với P. B0 dùng `SkillSimilarity` không có quan hệ nào, nên chỉ giữ ứng viên trùng chính xác, và SkillFit × số kỹ năng muốn học = số kỹ năng trùng. Gộp hai nhóm kết quả, đặt điểm = SkillFit chiều u học từ v, rồi xếp bằng `RankingComparator` (D36) |
| Q16 | So sánh 3 công thức ScheduleFit | **Không sửa lõi.** `evaluation` lấy `ScoreBreakdown` của lõi, thay ScheduleFit bằng công thức khác (Jaccard, trần 3 khung) để tính lại D và MutualScore, rồi xếp lại bằng `RankingComparator`. Chưa khai báo lịch vẫn là 0.5. Lọc cứng không đổi, vì cả 3 công thức đều > 0 khi và chỉ khi có khung trùng. Có test: dùng công thức "chia cho min" thì điểm tính lại phải bằng điểm của lõi |
| Q18 | Những tổ hợp nào được chạy? | **Bảng chính:** N = 500, nhiễu mặc định, gồm B0, B1, R, P và 3 phương án bỏ thành phần. **Coverage:** mọi quy mô. **Nhiễu:** P và các phương án bỏ thành phần ở 3 mức nhiễu. **Độ nhạy:** 4 bộ trọng số và 3 công thức ScheduleFit trên seed 1–2; cấu hình mặc định và cấu hình đã chọn chạy thêm trên seed 3–5 |

### 3.4. Cách chạy và lưu kết quả

| Mã | Câu hỏi | Chốt |
| --- | --- | --- |
| Q19 (R22) | Cách chạy thực nghiệm | `EvaluationMain` viết bằng Java thuần (không Spring, không DB). Chạy `.\mvnw.cmd -q compile` rồi `java -cp target/classes com.skillexchange.evaluation.EvaluationMain ...`; không thêm dependency |
| Q20 (R22) | Định dạng kết quả | Mỗi lượt chạy một thư mục `thuc_nghiem/ket_qua/<ngày>_<tên>/` gồm: `README.md` (lệnh, seed, commit, cấu hình máy, thời gian chạy, có thay đổi chưa commit không); `metrics.csv` dạng dài với các cột `seed, vai_tro_seed, n, muc_nhieu, phuong_an, chi_so, gia_tri, so_nguoi_tinh`; `per_user.csv` cho kiểm định; `log.txt`. Thư mục đã tồn tại thì dừng, không ghi đè |
| Q21 (NFR-06) | Bảo đảm cùng seed cho cùng dữ liệu | Chỉ dùng `java.util.Random(seed)`. Không rút số ngẫu nhiên trong lúc duyệt một `Set`: `Set.copyOf` (dùng trong `MatchProfile.java:35`) có thứ tự duyệt đổi giữa các lần khởi động JVM. Bộ sinh duyệt `List` có thứ tự cố định |

### 3.5. Quản trị danh mục kỹ năng (A01)

| Mã | Câu hỏi | Chốt |
| --- | --- | --- |
| Q22 (R41) | API nào? | 5 API ở TK §4.9, cộng phần liên quan tới kỹ năng của R41: `GET /api/admin/skills` (kể cả kỹ năng ẩn, nếu thiếu thì admin không hiện lại được kỹ năng đã ẩn), `GET /api/admin/skill-relations`, `PUT /api/admin/skill-categories/{id}` (đổi tên nhóm). Chỉ thêm API, không đổi API cũ |
| Q23 (R13, R42) | Mã lỗi | Tên kỹ năng hoặc tên nhóm trùng, so sau khi cắt khoảng trắng và không phân biệt hoa thường: 409. Quan hệ giữa hai kỹ năng khác nhóm, hoặc một kỹ năng với chính nó: 400. Quan hệ đã có: 409. Đổi nhóm cho kỹ năng đang có quan hệ: 409. Không tìm thấy kỹ năng, nhóm hoặc quan hệ: 404. `DELETE` quan hệ nhận hai mã theo thứ tự bất kỳ. Kiểm tra trùng tên làm ở service, không thêm migration |
| Q24 | Ẩn kỹ năng ảnh hưởng gì? | Giữ nguyên các quan hệ liên quan và dữ liệu người dùng; kỹ năng ẩn vẫn dùng trong matching (spec 4.1) |
| Q25 | Giao diện | Admin đăng nhập xong vào thẳng A01; route admin không cho sinh viên vào. Backend đã chặn sẵn bằng `hasRole("ADMIN")` cho `/api/admin/**` |

## 4. Mâu thuẫn và cách xử lý

| Mã | Ở đâu | Mâu thuẫn | Xử lý |
| --- | --- | --- | --- |
| M1 | Spec 5.1: dòng 190 so với dòng 204, 208 | Đoạn đầu (thêm ngày 2026-11-01, commit `64c9e83`) nói nhãn tính trên **giá trị thật**. Bước 2–3 (viết ngày 2026-10-24) vẫn ghi "trình độ **tự khai**" | Đổi "tự khai" thành "thật" ở Bước 2–3. Viết lại nhãn 2, nhãn 0 theo Q2, Q3. Bước 1 giữ nguyên chữ (Q4) |
| M2 | Rà soát dòng 611–617 so với spec 5.5 | Rà soát ghi mọi mục chốt theo gợi ý ngày 2026-10-29, nhưng đợt 3 (R24–R32) vẫn "chưa sửa", spec 5.5 còn để mở N6, C4, T3, T5, T6 | Đóng 5.5; cập nhật bảng trạng thái của rà soát, kèm các chỗ chốt khác bảng §9: R24, R26 (báo cáo hai bản), R28 (giữ cấu hình 4), R31 (phương án b) |
| M3 | R31 (rà soát dòng 339) so với `docs/To_chuc_ket_qua.md:29` | Gợi ý của R31 có chữ "hoặc", nên chưa thực sự chọn | Q10 (b), không sửa TCKQ:29 |
| M4 | Spec 5.3 dòng 246 so với R24 | Spec tính người có IDCG = 0 vào trung bình với NDCG = 0; R24 loại người chỉ có một mức nhãn | Q8: báo cáo cả hai bản |
| M5 | Rà soát R13, R41, R42 so với R46 | A01 đã dời lên tuần 5, nhưng phần trùng tên kỹ năng của R13 vẫn ghi "dời sang tuần 7" (rà soát dòng 615). R41, R42 vẫn ở đợt 4 "trước tuần 6". TK §4.9 (`docs/tuan_2/Tuan_2_Thiet_ke.md:547`) chưa có các API này | Kéo phần liên quan tới kỹ năng của R13, R41, R42 vào tuần 5 (Q22, Q23) |
| M6 | Proposal dòng 49 | Vẫn ghi "Qua đó **chứng minh**...", trong khi spec 5.2 (C5) nói lợi thế của P chỉ giảm được, và R27 gợi ý sửa thành "kiểm tra giả thuyết" | **Không sửa proposal** (tài liệu đã nộp, cùng tiền lệ của đợt 2). Theo thứ bậc, spec đúng. Ghi vào quyết định tuần 5 và mục "Điều chỉnh so với báo cáo trước" của báo cáo tuần 5 |

## 5. Danh sách sửa tài liệu (bước 0)

Làm trong một commit `docs:` theo quy tắc sửa ngược.

- [x] Tạo `docs/tuan_5/Quyet_dinh_tuan_5.md`, mã từ D52, ghi Q1–Q25 như mục 0–3.
- [x] Spec 5.1: "tự khai" thành "thật" ở Bước 2–3; nhãn 2, nhãn 0 theo Q2, Q3; định nghĩa "chưa khai báo" và "tương thích hoàn toàn" theo Q4; thêm Q5; bỏ dòng ⚠ ở nhãn 2.
- [x] Spec 5.2: danh sách của P dùng để đo (Q1); ba bộ trọng số khi bỏ thành phần (Q13).
- [x] Spec 5.3: thêm Precision@5 nghiêm ngặt (Q7), IDCG trên tập lý tưởng (Q6), hai bản trung bình (Q8), hai chỉ số mới (Q9), cách đo latency (Q11).
- [x] Spec 5.4: bảng tham số (Q12), 3 mức nhiễu, quy mô 50, seed, ghi chú cấu hình "Ưu tiên uy tín" (Q14), tiêu chí chọn cấu hình (Q17), kiểm định (Q10).
- [x] Spec 5.5: đóng N6, C4, T3, T5, T6, trỏ tới mã D tương ứng.
- [x] Spec phụ lục A.2: bỏ các ô ⚠. Phụ lục A.3: thêm ví dụ nhãn khi học nhiều kỹ năng, Precision@5 nghiêm ngặt = 0.2, ghi rõ giả định "tập ứng viên chỉ có 5 người" của ví dụ IDCG (R32).
- [x] Spec 7.1 nhóm 4: thêm ca cho Q3, Q4, Q5 và hai bản trung bình của Q8.
- [x] TK §4.9: thêm API ở Q22 và mã lỗi ở Q23. TK §7: cập nhật truy vết FR-08, FR-11.
- [x] Rà soát: bảng trạng thái đợt 3 thành "đã sửa"; ghi các chỗ chốt khác bảng §9 (M2); phần kỹ năng của R13, R41, R42 chuyển sang tuần 5; R27 ghi "không sửa proposal".
- [x] `docs/To_chuc_ket_qua.md` mục 3.2: thêm `per_user.csv` (dòng 29 giữ nguyên).
- [x] CLAUDE.md: thêm dòng quyết định tuần 5, lệnh chạy thực nghiệm, lệnh chạy test `evaluation`; bỏ dòng trỏ tới file này.
- [x] Viết `docs/tuan_5/kiem_thu/ca_kiem_thu.md`: bộ sinh dữ liệu, nhãn, chỉ số, các phương án, A01.
- [x] Viết `docs/tuan_5/ke_hoach_tuan_5.md` để `/buoc-tuan` dùng được.

**Không sửa:** spec mục 4, proposal, TCKQ dòng 29, migration, code và test tuần 3–4.

## 6. Ảnh hưởng tới phần đã làm

- **Code và test tuần 3–4:** không đổi. Lõi `matching/` không bị sửa (Q15, Q16 làm hết trong `evaluation`), nên hook test lõi không chạy lại.
- **DB:** không thêm migration (Q23 kiểm tra trùng tên ở service).
- **Báo cáo đã nộp:** chỉ lệch một chỗ. Bảng 4.11 (`docs/tuan_3/Noi_dung_bao_cao_Tuan_3.md:64`) định nghĩa nhãn 2 bằng "LevelFit = 1.0", nay xét trên chính kỹ năng trùng chính xác (Q2). Báo cáo tuần 5 thêm mục "Điều chỉnh so với báo cáo trước" nêu chỗ này và lý do ở R25, cùng việc không sửa câu "chứng minh" của proposal (M6). Các chỗ khác vẫn đúng: 4 cấu hình trọng số, Precision@5, 5 seed, trung bình ± độ lệch chuẩn.
