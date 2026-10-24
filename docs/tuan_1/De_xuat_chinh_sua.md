# Đề xuất chỉnh sửa tài liệu dự án Skill Exchange

- **Tài liệu áp dụng:**
  - `Skill_Exchange_Proposal_9_Tuan.md`
  - `Phan_tich_yeu_cau_Skill_Exchange_Tuan_1.md`
- **Mục đích:** Đồng bộ yêu cầu nghiệp vụ, thuật toán matching và phương pháp đánh giá trước khi bắt đầu triển khai.
- **Phạm vi:** Dự án cá nhân trong 9 tuần.
- **Nguyên tắc:** Giữ phạm vi MVP gọn, ưu tiên tính nhất quán và khả năng kiểm thử. Các trọng số thuật toán là giả thuyết ban đầu, chưa được coi là tối ưu.

---

## 1. Sửa quy tắc Ground Truth

**Mức độ ưu tiên: Rất cao**

### Vấn đề

Các điều kiện gán nhãn hiện tại có thể chồng chéo. Một ứng viên có kỹ năng phù hợp nhưng không có giờ rảnh chung có thể đồng thời thỏa mãn điều kiện phù hợp và không phù hợp.

Ngoài ra, hệ thống gợi ý Top-k theo từng người dùng, vì vậy cần gán nhãn cho từng hướng trao đổi, không chỉ gán nhãn chung cho một cặp người dùng.

### Đề xuất

Định nghĩa nhãn cho hướng `u học từ v`:

| Nhãn | Điều kiện |
|---|---|
| 2 — Phù hợp cao | Có ít nhất một kỹ năng trùng chính xác, ít nhất hai khung giờ trùng, hình thức học tương thích và trình độ tự khai báo của người dạy đạt hoặc vượt mục tiêu người học. |
| 1 — Phù hợp một phần | Vượt qua các điều kiện loại trừ cứng nhưng không đáp ứng đầy đủ điều kiện của nhãn 2 và không thuộc trường hợp nhãn 0. |
| 0 — Không phù hợp về chất lượng | Có kỹ năng được đánh dấu liên quan nhưng không trùng chính xác, đồng thời trình độ tự khai báo của người dạy thấp hơn mục tiêu người học từ hai bậc trở lên. |

Áp dụng quy tắc theo thứ tự:

1. Kiểm tra điều kiện loại trừ cứng.
2. Nếu không hợp lệ, loại ứng viên khỏi danh sách.
3. Nếu hợp lệ và đáp ứng đủ điều kiện nhãn 2, gán nhãn 2.
4. Nếu không thuộc nhãn 2 nhưng đáp ứng điều kiện nhãn 0, gán nhãn 0.
5. Các trường hợp hợp lệ còn lại nhận nhãn 1.

### Lưu ý

- Không đưa ứng viên vi phạm điều kiện loại trừ cứng vào danh sách được xếp hạng.
- Phân biệt dữ liệu chưa khai báo với dữ liệu đã khai báo nhưng không phù hợp.
- Dữ liệu giả lập phải được sinh theo quy tắc cố định, có thể tái tạo bằng random seed.
- Nhãn chỉ phản ánh kịch bản mô phỏng, không chứng minh mức độ phù hợp thực tế của người dùng.

**Tiêu chí hoàn thành:** Mỗi trường hợp chỉ nhận tối đa một nhãn; quy tắc gán nhãn không phụ thuộc vào điểm tổng của thuật toán đang được đánh giá.

---

## 2. Thống nhất điều kiện lọc ứng viên và công thức điểm

**Mức độ ưu tiên: Rất cao**

### Vấn đề

Nếu chỉ tính điểm mà không loại ứng viên vi phạm điều kiện bắt buộc, một cặp không có giờ rảnh chung vẫn có thể nhận điểm cao nhờ các thành phần khác.

### Đề xuất

Tách quy trình thành hai giai đoạn.

#### Giai đoạn 1: Candidate Retrieval và Hard Filtering

Chỉ xem xét ứng viên thỏa mãn các điều kiện:

- Không phải chính người đang tìm kiếm.
- Tài khoản đang hoạt động và đã xác thực email trường.
- Có ít nhất một kỹ năng trùng hoặc được quản trị viên đánh dấu là liên quan.
- Nếu cả hai đã khai báo lịch nhưng không có khung giờ trùng, loại ứng viên.
- Nếu cả hai đã khai báo hình thức học nhưng hai hình thức không tương thích, loại ứng viên.

Nếu một bên chưa khai báo lịch hoặc hình thức học, không tự động loại ứng viên; xử lý bằng giá trị trung tính theo công thức.

#### Giai đoạn 2: Scoring & Ranking

Tính điểm cho những ứng viên vượt qua bước lọc.

Công thức điểm đề xuất:

D(u,v) = 0.55 × SkillFit + 0.20 × LevelFit + 0.15 × ScheduleFit + 0.05 × ModeFit + 0.05 × Reputation(v)

Tất cả thành phần phải thuộc khoảng [0,1]. Tổng trọng số bằng 1.

Các trọng số trên là cấu hình ban đầu, cần được kiểm nghiệm bằng thực nghiệm.

### Tiêu chí hoàn thành

- Không có ứng viên vi phạm điều kiện cứng xuất hiện trong Top-k.
- Điểm của ứng viên chỉ được dùng để xếp hạng các ứng viên hợp lệ.
- Kiểm thử riêng từng điều kiện lọc và từng thành phần điểm.
- Không nhầm lẫn giữa cải thiện thứ tự xếp hạng và tăng số người có ứng viên phù hợp.

---

## 3. Làm rõ ghép hai chiều và mentoring một chiều

**Mức độ ưu tiên: Cao**

### Đề xuất

Duy trì hai loại đề xuất riêng biệt.

#### Ghép trao đổi hai chiều

Hai người đều có ít nhất một kỹ năng mà bên kia muốn học.

MutualScore(u,v) = sqrt(D(u,v) × D(v,u))

Đây là nhóm đề xuất được ưu tiên hiển thị.

#### Mentoring một chiều

Một người muốn học từ người kia nhưng không có nhu cầu phù hợp theo chiều ngược lại.

Quy tắc nghiệp vụ:

- Hiển thị rõ đây là hoạt động hỗ trợ tự nguyện.
- Người nhận có quyền chấp nhận hoặc từ chối.
- Không mặc định người nhận phải cung cấp kỹ năng để đổi lại.
- Không triển khai hệ thống tín chỉ hoặc time-banking trong MVP.
- Không so sánh trực tiếp điểm MutualScore với điểm D của mentoring một chiều; hai nhóm được xếp hạng riêng.

### Tiêu chí hoàn thành

- Phân loại đúng ghép hai chiều và một chiều.
- Giao diện giải thích được loại đề xuất.
- Người dùng không bị ép tham gia mentoring.
- Ghép vòng chỉ là chức năng mở rộng, không bắt buộc trong 9 tuần.

---

## 4. Tách trạng thái phiên và trạng thái đánh giá

**Mức độ ưu tiên: Cao**

### Vấn đề

Một phiên có thể đã hoàn thành dù chỉ một bên gửi đánh giá. Vì vậy, không nên coi `RATED` là trạng thái kết thúc bắt buộc của phiên.

### Đề xuất trạng thái yêu cầu

- `PENDING`: chờ người nhận phản hồi.
- `ACCEPTED`: được chấp nhận.
- `REJECTED`: bị từ chối.
- `CANCELLED`: người gửi hủy khi yêu cầu còn chờ.

### Đề xuất trạng thái phiên

- `SCHEDULED`: đã lên lịch.
- `IN_PROGRESS`: đang diễn ra.
- `WAITING_CONFIRMATION`: một bên xác nhận hoàn thành.
- `COMPLETED`: bên còn lại xác nhận hoàn thành.
- `CANCELLED`: phiên bị hủy trước khi bắt đầu hoặc không thể tiếp tục trong khi đang diễn ra.

Nếu hai bên bất đồng về việc hoàn thành:

- Không tự động chuyển phiên sang `CANCELLED`.
- Giữ trạng thái `WAITING_CONFIRMATION`.
- Cho phép người dùng báo cáo sự cố để quản trị viên xem xét.

### Quy tắc đánh giá

- Chỉ được đánh giá khi phiên đã ở trạng thái `COMPLETED`.
- Mỗi bên đánh giá đối phương độc lập.
- Mỗi người chỉ được gửi một đánh giá cho một phiên.
- Lưu trạng thái đánh giá riêng, không gộp vào trạng thái phiên.

### Tiêu chí hoàn thành

- Sơ đồ trạng thái không có chuyển tiếp mâu thuẫn.
- Mỗi chuyển trạng thái có tác nhân được phép rõ ràng.
- Kiểm tra quyền tại backend, không chỉ ẩn nút trên giao diện.

---

## 5. Hoàn thiện công thức Reputation

**Mức độ ưu tiên: Cao**

### Công thức đề xuất

Reputation(v) = 0.7 × RatingNorm + 0.3 × CompletionRate

Trong đó:

RatingNorm = (RatingAverage - 1) / 4

Áp dụng khi thang đánh giá là 1–5.

### Quy tắc dữ liệu thiếu

- Chưa có đánh giá: `RatingNorm = 0.5`.
- Chưa có phiên đủ điều kiện tính tỷ lệ hoàn thành: `CompletionRate = 0.5`.
- Người dùng hoàn toàn chưa có lịch sử: `Reputation = 0.5`.

### Quy tắc CompletionRate

CompletionRate = Số phiên hoàn thành / Số phiên đã bắt đầu

Mẫu số bao gồm các phiên đã bắt đầu, đang chờ xác nhận hoặc đã hoàn thành; phiên bị hủy sau khi bắt đầu vẫn được tính vào mẫu số. Phiên bị hủy trước khi bắt đầu không được tính.

Cần thống nhất cách lưu và truy vấn dữ liệu để không đếm một phiên nhiều lần.

### Giới hạn

Mức thành thạo và đánh giá uy tín chưa được xác minh độc lập. Xác thực email trường chỉ xác minh quyền truy cập email trường, không chứng minh người dùng có trình độ kỹ năng như khai báo.

### Tiêu chí hoàn thành

- Không chia cho 0.
- Không phát sinh `NaN`.
- Người dùng mới không bị mặc định là người dùng có uy tín thấp.
- Có unit test cho dữ liệu thiếu, một đánh giá và nhiều phiên.

---

## 6. Chuẩn hóa phương pháp đánh giá thuật toán

**Mức độ ưu tiên: Rất cao**

### Các phương án thực nghiệm

| Mã | Phương án | Mục đích |
|---|---|---|
| B0 | Chỉ dùng kỹ năng trùng chính xác, xếp hạng theo số kỹ năng trùng. | Baseline. |
| B1 | Mở rộng ứng viên bằng kỹ năng liên quan, xếp hạng theo SkillFit. | Đánh giá tác động của mở rộng ứng viên. |
| P | Dùng cùng tập ứng viên với B1 nhưng xếp hạng theo công thức đa tiêu chí. | Đánh giá giá trị của công thức xếp hạng. |

### Chỉ số

#### Precision@5

Số ứng viên có nhãn 1 hoặc 2 trong năm vị trí đầu chia cho 5. Nếu có ít hơn năm ứng viên, vị trí thiếu được xem là không có ứng viên phù hợp.

#### NDCG@5

DCG@k = Σᵢ₌₁ᵏ (2^relᵢ − 1) / log₂(i + 1)

NDCG@k = DCG@k / IDCG@k

Nhãn `rel` nhận giá trị 0, 1 hoặc 2. Nếu `IDCG@5 = 0`, quy ước `NDCG@5 = 0` và báo cáo số trường hợp này.

#### Recommendation Coverage

Số người dùng đủ điều kiện có ít nhất một ứng viên sau bước lọc chia cho tổng số người dùng đủ điều kiện tìm kiếm.

#### Latency

Thời gian tạo và xếp hạng tối đa năm gợi ý cho một người dùng, tính bằng mili giây. Cần ghi rõ cấu hình máy và cách đo.

### Cách diễn giải kết quả

- So sánh B0 với B1 để đánh giá thay đổi về Coverage và chất lượng gợi ý khi mở rộng tập ứng viên.
- So sánh B1 với P trên cùng tập ứng viên để đánh giá tác động của công thức xếp hạng.
- Không khẳng định công thức P làm tăng Coverage nếu tập ứng viên không thay đổi.
- Không dùng điểm tương thích trung bình làm bằng chứng chính về chất lượng vì điểm này được tạo bởi chính thuật toán.

### Dữ liệu giả lập

- Quy mô: 100, 500 và 1.000 người dùng.
- Mỗi người có 2–5 kỹ năng có thể dạy và 1–3 kỹ năng muốn học.
- Sử dụng năm random seed để kiểm tra tính ổn định.
- Báo cáo trung bình và độ lệch chuẩn.
- Thực hiện phân tích độ nhạy với các cấu hình trọng số khác nhau.

Khảo sát 15–20 sinh viên chỉ là phần bổ trợ nếu còn thời gian; không phải điều kiện bắt buộc để hoàn thành MVP.

---

## 7. Điều chỉnh so sánh nền tảng và yêu cầu bảo mật

**Mức độ ưu tiên: Trung bình**

### Đề xuất

- Ghi nguồn chính thức và ngày truy cập cho từng nền tảng được so sánh.
- Tránh các kết luận tuyệt đối về việc nền tảng khác có hoặc không có tính năng nếu chưa kiểm chứng.
- Phân biệt chức năng đã được xác minh với nhận định của người thực hiện.
- Nêu rõ email trường giúp xác thực quyền truy cập email, không chống khai khống kỹ năng một cách tuyệt đối.
- Chỉ hiển thị thông tin liên hệ cho các bên tham gia sau khi yêu cầu được chấp nhận.
- Kiểm tra quyền truy cập ở backend cho tất cả API nhạy cảm.

---

## 8. Phạm vi ưu tiên trong 9 tuần

**Mức độ ưu tiên: Cao**

### Bắt buộc

- Tài khoản và xác thực email.
- Hồ sơ, danh mục kỹ năng.
- Lọc ứng viên và gợi ý Top-k.
- Công thức tính điểm và giải thích kết quả.
- Gửi, chấp nhận, từ chối yêu cầu.
- Quản lý trạng thái phiên và đánh giá.
- Phân quyền backend.
- Dữ liệu giả lập, baseline, các chỉ số đánh giá và báo cáo thực nghiệm.
- Unit test cho thuật toán và các quy tắc nghiệp vụ chính.

### Nên có nếu đủ thời gian

- Đặt lịch và thông báo cơ bản.
- Báo cáo sự cố.
- Khảo sát người dùng.

### Mở rộng

- Chat realtime.
- Ghép vòng trên đồ thị.
- Dashboard thống kê nâng cao.
- Cơ chế chứng nhận kỹ năng hoặc time-banking.

Không triển khai chức năng mở rộng trước khi hoàn thành thuật toán và thực nghiệm cốt lõi.

---

## 9. Đồng bộ hai tài liệu

### `Phan_tich_yeu_cau_Skill_Exchange_Tuan_1.md`

Đây là tài liệu đặc tả chi tiết. Cần cập nhật:

- Quy trình lọc ứng viên và tính điểm.
- Định nghĩa các thành phần trong công thức.
- Quy tắc gán nhãn Ground Truth.
- Baseline, chỉ số đánh giá và dữ liệu giả lập.
- Trạng thái phiên, đánh giá và phân quyền.
- Unit test tương ứng với các quy tắc trên.

### `Skill_Exchange_Proposal_9_Tuan.md`

Đây là tài liệu đề xuất tổng thể. Chỉ cần mô tả ở mức khái quát:

- Bài toán Top-k Recommendation.
- Công thức điểm và điều kiện lọc ở mức tổng quan.
- Phân biệt ghép hai chiều và mentoring một chiều.
- Các chỉ số đánh giá chính.
- Phạm vi MVP và chức năng mở rộng.
- Kế hoạch 9 tuần nhất quán với tài liệu phân tích yêu cầu.

Không cần đưa toàn bộ quy tắc gán nhãn, công thức chi tiết hoặc test case vào proposal.

---

## 10. Checklist trước khi bắt đầu triển khai

- [ ] Quy tắc Ground Truth không chồng chéo.
- [ ] Điều kiện lọc và công thức điểm nhất quán.
- [ ] Định nghĩa ghép hai chiều và một chiều rõ ràng.
- [ ] Trạng thái phiên tách biệt trạng thái đánh giá.
- [ ] Reputation xử lý đầy đủ trường hợp thiếu dữ liệu.
- [ ] Baseline và phương án đề xuất có tập ứng viên được xác định rõ.
- [ ] Precision@5, NDCG@5, Coverage và Latency có định nghĩa thống nhất.
- [ ] Có kế hoạch sinh dữ liệu giả lập và tái tạo thực nghiệm.
- [ ] Các quyền nhạy cảm được kiểm tra tại backend.
- [ ] Phạm vi bắt buộc có thể hoàn thành trong 9 tuần.
- [ ] Hai tài liệu sử dụng thuật ngữ và công thức thống nhất.

**Thứ tự thực hiện khuyến nghị:** mục 1 → mục 2 → mục 6 → mục 4 → mục 5 → mục 3 → mục 7 → mục 8 → kiểm tra đồng bộ mục 9.

**Kết quả mong muốn:** Sau khi hoàn thành các chỉnh sửa, tài liệu đủ rõ để triển khai database, API và unit test mà không phải tự suy diễn các quy tắc thuật toán hoặc nghiệp vụ quan trọng.
