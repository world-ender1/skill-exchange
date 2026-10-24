# Skill Exchange — Proposal dự án cá nhân 9 tuần

ĐỀ XUẤT CHỦ ĐỀ DỰ ÁN CÔNG NGHỆ
SKILL EXCHANGE – NỀN TẢNG GHÉP CẶP TRAO ĐỔI KỸ NĂNG GIỮA SINH VIÊN

Môn: Dự án Công nghệ

## 1. Bối cảnh và lý do chọn đề tài

Sinh viên thường có nhu cầu học thêm các kỹ năng ngoài chương trình chính khóa như lập trình, ngoại ngữ, thiết kế, âm nhạc hoặc kỹ năng mềm. Đồng thời, mỗi sinh viên lại sở hữu những kỹ năng khác nhau và có khả năng hỗ trợ lẫn nhau. Hiện nay việc tìm người phù hợp chủ yếu thực hiện thủ công qua các nhóm mạng xã hội, nên khó biết ai thực sự phù hợp về kỹ năng, trình độ và thời gian.

Đề tài hướng tới xây dựng Skill Exchange – một ứng dụng web tập trung vào bài toán ghép cặp người dùng dựa trên kỹ năng có thể dạy, kỹ năng muốn học và các tiêu chí tương thích. Điểm khác biệt của hệ thống so với việc đăng bài trên mạng xã hội là hệ thống chủ động tính điểm và đề xuất các đối tượng phù hợp, thay vì để người dùng tự tìm kiếm hoàn toàn.

## 2. Mục tiêu và bài toán trọng tâm

Mục tiêu chính là xây dựng một nền tảng giúp sinh viên tìm được người phù hợp để trao đổi kỹ năng, tổ chức phiên trao đổi và hình thành độ tin cậy qua lịch sử hoạt động và đánh giá.

### 2.1. Ghép cặp hai chiều và Quy trình đề xuất

Hệ thống ưu tiên cao nhất cho trường hợp hai người có nhu cầu tương hỗ (Ví dụ: A có thể dạy Java và muốn học Guitar; B có thể dạy Guitar và muốn học Java).

Quy trình gợi ý Top-k được tách thành hai giai đoạn cốt lõi:
1. **Giai đoạn 1 - Lọc ứng viên cứng (Hard Filtering):** Loại bỏ ngay các ứng viên không khả thi (tài khoản chưa xác thực email trường, không có kỹ năng trùng hay liên quan, đã khai báo lịch nhưng không có khung giờ trùng, hoặc xung đột hoàn toàn về hình thức học Online/Offline).
2. **Giai đoạn 2 - Chấm điểm và Xếp hạng (Scoring & Ranking):** Chỉ tính điểm cho các ứng viên vượt qua bước lọc cứng theo mô hình đa tiêu chí:

$$D(u,v) = 0.55 \times SkillFit + 0.20 \times LevelFit + 0.15 \times ScheduleFit + 0.05 \times ModeFit + 0.05 \times Reputation(v)$$

Đối với cặp ghép hai chiều, điểm tương thích tổng hợp sử dụng trung bình nhân:
$$MutualScore(u,v) = \sqrt{D(u,v) \times D(v,u)}$$

Tất cả các thành phần điểm đều được chuẩn hóa trong đoạn $[0, 1]$. Người dùng mới chưa có lịch sử được gán $Reputation = 0.5$ (trung tính) để giải quyết bài toán Cold Start.

### 2.2. Mentoring một chiều và Chức năng mở rộng

- **Mentoring một chiều ($u \rightarrow v$):** Nếu một người chỉ muốn học mà đối phương không có nhu cầu học lại, hệ thống tính điểm $D(u,v)$ và xếp hạng trong nhóm riêng. Đây là hoạt động hỗ trợ tự nguyện; người nhận có quyền từ chối và không bị ép buộc dạy lại. MVP không áp dụng hệ thống tín chỉ hay time-banking.
- **Ghép vòng (Chức năng mở rộng):** Khi không có cặp trực tiếp (A dạy B, B dạy C, C dạy A), có thể mở rộng mô hình hóa đồ thị có hướng để tìm chu trình. Đây là chức năng mở rộng, chỉ thực hiện khi hoàn thành tốt MVP.

### 2.3. Đánh giá thuật toán ghép cặp

Hệ thống định nghĩa bài toán cốt lõi là **Gợi ý Top-k ứng viên phù hợp cho từng người dùng (Top-k Recommendation)**, không phải bài toán ghép cặp toàn cục 1-1 cố định. Người dùng nhận danh sách đề xuất xếp hạng cao nhất và chủ động quyết định kết nối.

Để kiểm chứng hiệu quả, hệ thống sử dụng dữ liệu giả lập có gán nhãn "đáp án đúng" (Ground Truth) ngầm định theo quy tắc nghiệp vụ độc lập (không dùng chính hàm điểm để tạo nhãn). Thuật toán được đánh giá qua các chỉ số chuẩn trong Hệ thống gợi ý: **Precision@5, NDCG@5, Recommendation Coverage và Thời gian xử lý (Latency)**. 

Hệ thống so sánh 3 kịch bản:
- Baseline $B0$: Chỉ tìm ứng viên trùng chính xác kỹ năng.
- Mở rộng $B1$: Mở rộng tập ứng viên bằng danh mục kỹ năng liên quan.
- Đa tiêu chí $P$: Dùng chung tập ứng viên mở rộng như $B1$ nhưng áp dụng hàm chấm điểm đa tiêu chí $D(u,v)$ và $MutualScore$.

Qua đó chứng minh: Mở rộng tập ứng viên giúp cải thiện độ bao phủ (Coverage), trong khi công thức chấm điểm đa tiêu chí giúp tối ưu hóa thứ tự xếp hạng (Precision@k, NDCG@k). Chỉ số "điểm tương thích trung bình" không được sử dụng để tránh thiên vị thuật toán.

## 3. Đối tượng sử dụng và chức năng

Đối tượng sử dụng gồm Sinh viên/Người dùng và Quản trị viên. Phạm vi chức năng được chia thành ba mức để phù hợp với phạm vi thực hiện cá nhân trong 9 tuần.

### 3.1. Chức năng bắt buộc

- Đăng ký, đăng nhập và xác thực email trường.

- Quản lý hồ sơ cá nhân và hồ sơ kỹ năng: kỹ năng có thể dạy, kỹ năng muốn học (chọn từ danh mục chuẩn hóa do admin quản lý) và mức độ tự khai báo.

- Khai báo lịch rảnh và hình thức học (trực tiếp / trực tuyến) trong hồ sơ; hai dữ liệu này được dùng trong lọc cứng và chấm điểm ghép cặp.

- Tìm kiếm người dùng/kỹ năng và xem hồ sơ công khai (bảo vệ thông tin liên hệ).

- Gợi ý Top-k ứng viên hai chiều và một chiều theo điểm tương thích đa tiêu chí kèm giải thích lý do phù hợp.

- Gửi yêu cầu trao đổi kỹ năng kèm lịch đề xuất (thời điểm, thời lượng, hình thức, địa điểm/link); chấp nhận, từ chối (`Rejected`) hoặc hủy (`Cancelled`) yêu cầu.

- Quản lý vòng đời phiên trao đổi với đầy đủ trạng thái: `Scheduled` → `In Progress` → `Waiting Confirmation` (chờ đối phương xác nhận) → `Completed` (hoặc `Cancelled`).

- Báo cáo sự cố phiên: người tham gia gửi báo cáo khi có bất đồng; quản trị viên chốt phiên sang `Completed` hoặc `Cancelled` kèm lý do.

- Phân quyền chặt chẽ ở backend: chỉ hai người tham gia phiên `Completed` mới được mở quyền đánh giá lẫn nhau (Rating & Review) và chỉ xem được liên hệ sau khi yêu cầu được chấp nhận.

- Quản trị cơ bản: khóa/mở khóa tài khoản, danh mục kỹ năng chuẩn hóa và nhóm kỹ năng liên quan.

- Dữ liệu giả lập, baseline và các chỉ số đánh giá thuật toán (mục 2.3); unit test cho thuật toán và các quy tắc nghiệp vụ chính.

### 3.2. Chức năng nên có

- Quên mật khẩu (đặt lại qua email trường).

- Thông báo khi có yêu cầu mới, thay đổi trạng thái hoặc lịch trao đổi.

- Lịch sử các phiên trao đổi và điểm uy tín trên hồ sơ.

- Lọc ứng viên theo hình thức học trên giao diện; thống kê đơn giản.

- Khảo sát 15–20 sinh viên thật (bổ trợ định tính).

### 3.3. Chức năng mở rộng

- Chat realtime bằng WebSocket.

- Báo cáo vi phạm nâng cao; dashboard thống kê nâng cao.

- Ghép vòng bằng thuật toán đồ thị khi không có cặp hai chiều trực tiếp.

- Chuỗi nhiều buổi trong một thỏa thuận trao đổi.

- Chứng nhận kỹ năng (chứng chỉ/bài kiểm tra) hoặc time-banking.

## 4. Một số quy tắc nghiệp vụ

### 4.1. Mức độ thành thạo và giới hạn xác thực

Mức độ kỹ năng ban đầu do người dùng tự khai báo theo danh mục chuẩn và được hiển thị rõ là “tự khai báo”. Để giảm thiểu rủi ro khai khống, hệ thống tích hợp điểm uy tín ($Reputation$) dựa trên tỷ lệ hoàn thành phiên và đánh giá thực tế. Xác thực email trường giúp bảo đảm danh tính sinh viên trong trường, nhưng không được coi là chứng nhận chuyên môn tuyệt đối.

### 4.2. Tách biệt vòng đời phiên và đánh giá

- Vòng đời phiên trao đổi kết thúc khi cả hai bên cùng xác nhận hoàn thành (`Completed`).
- Nếu xảy ra tranh chấp (một bên bấm hoàn thành nhưng bên kia bất đồng/không phản hồi), hệ thống giữ nguyên trạng thái `Waiting Confirmation` và mở luồng Báo cáo sự cố (Report) cho Quản trị viên, không tự động hủy phiên.
- Quy trình đánh giá (Review & Rating) là thực thể độc lập, chỉ mở sau khi phiên đã `Completed`. Mỗi bên gửi đánh giá độc lập 1 lần, không bắt buộc cả hai bên phải cùng đánh giá mới kết thúc phiên.

### 4.3. Bảo vệ thông tin liên hệ

Tài khoản được xác thực bằng email trường. Các thông tin liên hệ nhạy cảm (email cá nhân, số điện thoại, mạng xã hội) hoàn toàn không hiển thị công khai; hệ thống chỉ mở quyền truy cập thông tin liên hệ cho hai bên sau khi yêu cầu trao đổi được chấp nhận (`Accepted`). Backend kiểm soát quyền truy cập nghiêm ngặt ở tầng API.

### 4.4. Trường hợp trao đổi một chiều (Mentoring)

Hệ thống cho phép một người dạy người khác mà không bắt buộc nhận lại một kỹ năng theo mô hình mentoring tự nguyện. Hai nhóm đề xuất được hiển thị và xếp hạng ở hai tab riêng biệt, trong đó ghép hai chiều được ưu tiên cao hơn. Không áp dụng hệ thống tín chỉ hay time-banking trong phạm vi MVP.

## 5. So sánh ngắn với nền tảng tương tự

| Nền tảng | Điểm mạnh | Hạn chế thực tế so với đề tài Skill Exchange |
| --- | --- | --- |
| Facebook Groups | Dễ đăng bài và tiếp cận cộng đồng lớn | Người dùng tự tìm thủ công; không có matching hai chiều; dễ lộ thông tin liên hệ; không có cơ chế đánh giá uy tín. |
| LinkedIn | Hồ sơ kỹ năng và mạng lưới nghề nghiệp lớn | Hướng tới tuyển dụng; không tối ưu cho mô hình trao đổi kỹ năng ngang hàng (P2P) học tập giữa sinh viên. |
| Tandem | Nền tảng trao đổi ngôn ngữ P2P chuyên sâu, có chat/gọi video | Chỉ phục vụ học ngoại ngữ, không hỗ trợ đa kỹ năng (lập trình, đồ họa, kỹ năng mềm); không ghép cặp theo thời khóa biểu học tập. |
| TimeRepublik / Simbi | Mô hình ngân hàng thời gian (Time-banking) với hệ thống credit | Cơ chế tích lũy credit gián tiếp phức tạp; không tập trung vào bài toán ghép tương hỗ 2 chiều trực tiếp (A dạy B và B dạy A). |
| Skill Exchange (Đề tài) | Gợi ý Top-k đa tiêu chí (kỹ năng, lịch rảnh, uy tín); quy trình phiên xác nhận hai bên; bảo vệ thông tin liên hệ | **Hạn chế thực tế:** Kỹ năng ban đầu là tự khai báo (chưa có chứng chỉ sát hạch độc lập); quy mô người dùng ban đầu trong trường đại học nhỏ khiến xác suất tìm thấy cặp ghép hai chiều trực tiếp thấp hơn các nền tảng toàn cầu. |

## 6. Công nghệ dự kiến

- Frontend: React + TypeScript.

- Backend: Java Spring Boot.

- Cơ sở dữ liệu: PostgreSQL.

- Xác thực: JWT + xác thực email.

- Realtime (nếu triển khai): WebSocket.

- Triển khai: Docker và một nền tảng cloud phù hợp.

## 7. Kế hoạch thực hiện dự kiến

| Tuần | Công việc chính |
| --- | --- |
| 1 | Phân tích yêu cầu, use case, chốt phạm vi, công thức điểm và thiết kế đánh giá thuật toán. |
| 2 | Thiết kế cơ sở dữ liệu, API, sơ đồ trạng thái (State Diagram) và prototype giao diện. |
| 3 | Đăng ký, xác thực email trường, đăng nhập; hồ sơ người dùng gồm lịch rảnh và hình thức học; khai báo kỹ năng; nạp sẵn danh mục kỹ năng chuẩn hóa. |
| 4 | Xây dựng thuật toán matching, công thức chấm điểm đa tiêu chí và **viết ngay Unit test cho các hàm tính điểm**; tìm kiếm và hồ sơ công khai. |
| 5 | Tạo dữ liệu giả lập có nhãn Ground Truth độc lập; chạy baseline; đo Precision@5, NDCG@5, Coverage, Latency và phân tích độ nhạy bộ trọng số. |
| 6 | Quản lý yêu cầu trao đổi, vòng đời phiên (Scheduled, In Progress, Waiting Confirmation, Completed), báo cáo sự cố và phân quyền backend. |
| 7 | Đánh giá sau phiên (Rating/Review) và điểm uy tín, bảo vệ thông tin liên hệ; quản trị danh mục kỹ năng, xử lý báo cáo sự cố, khóa/mở khóa tài khoản; lịch sử trao đổi nếu kịp. |
| 8 | Kiểm thử tích hợp & bảo mật backend, sửa lỗi, đo hiệu năng; khảo sát thử nghiệm 15–20 sinh viên thật nếu còn thời gian. |
| 9 | Deploy cloud, kiểm thử cuối, tổng hợp kết quả thực nghiệm thuật toán, hoàn thiện báo cáo và demo. |

## 8. Tiêu chí hoàn thành

- Hệ thống hoạt động ổn định với đầy đủ chức năng bắt buộc.

- Thuật toán ghép cặp trả về danh sách ứng viên Top-k có điểm tương thích và giải thích được các tiêu chí chấm điểm.

- Người dùng có thể hoàn thành đầy đủ một luồng: tạo hồ sơ → tìm/được gợi ý Top-k → gửi yêu cầu → xác nhận trao đổi → phiên hoàn thành (hai bên cùng xác nhận) → đánh giá.

- Có bộ dữ liệu giả lập có nhãn độc lập (Ground Truth) để đánh giá thuật toán, so sánh phương án Baseline $B0$, Mở rộng $B1$ và Đa tiêu chí $P$, báo cáo đầy đủ các chỉ số: Precision@5, NDCG@5, Recommendation Coverage và Thời gian xử lý.

- Kiểm thử đơn vị (Unit Test) cho hàm tính điểm được hoàn thành ngay tại tuần 4–5; phân quyền kiểm tra chặt chẽ ở tầng backend.

- Trong phạm vi 9 tuần thực hiện cá nhân, ưu tiên hoàn thành đầy đủ chức năng bắt buộc và phần đánh giá thuật toán; chức năng mở rộng như chat realtime hoặc ghép vòng chỉ triển khai khi còn thời gian.
