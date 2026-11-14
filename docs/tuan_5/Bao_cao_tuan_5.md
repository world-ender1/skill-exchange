# Báo cáo tuần 5 (09/11 – 15/11/2026)

Chủ đề: thực nghiệm đánh giá thuật toán ghép cặp trên dữ liệu giả lập có nhãn (FR-11); quản trị danh mục kỹ năng (FR-08, màn hình A01).
Repo: https://github.com/world-ender1/skill-exchange

## 1. Việc đã làm

| Việc | Commit |
| --- | --- |
| Chốt 26 quyết định triển khai (D52–D77), đóng các điểm còn mở của thiết kế thực nghiệm (rà soát R24–R32, spec 5.5); viết 64 ca kiểm thử từ tài liệu trước khi code; lập kế hoạch tuần | [1cea8fe](https://github.com/world-ender1/skill-exchange/commit/1cea8fe) |
| Module `evaluation` bằng Java thuần (không Spring, không CSDL): người dùng giả lập có giá trị thật và giá trị tự khai, danh mục kỹ năng chép từ migration V2, bộ sinh dữ liệu tái lập theo seed | [8ff0c37](https://github.com/world-ender1/skill-exchange/commit/8ff0c37), [5b9b0c7](https://github.com/world-ender1/skill-exchange/commit/5b9b0c7) |
| Gán nhãn theo thứ tự thác nước; Precision@5, NDCG@5, Coverage; khoảng tin cậy bootstrap và kiểm định Wilcoxon. Ba phần này viết theo TDD | [2e2e50d](https://github.com/world-ender1/skill-exchange/commit/2e2e50d), [42f55da](https://github.com/world-ender1/skill-exchange/commit/42f55da), [648cfa1](https://github.com/world-ender1/skill-exchange/commit/648cfa1) |
| Các phương án B0, B1, R, P, bỏ từng thành phần, 4 bộ trọng số, 3 công thức ScheduleFit; tất cả dựng từ lõi matching tuần 4, không sửa lõi | [4fd60fb](https://github.com/world-ender1/skill-exchange/commit/4fd60fb) |
| Chương trình chạy thực nghiệm: mỗi lượt chạy ghi vào một thư mục mới, không ghi đè; dừng nếu mã nguồn có thay đổi chưa commit | [e87dd09](https://github.com/world-ender1/skill-exchange/commit/e87dd09) |
| Hook chặn sửa, xóa thư mục kết quả. Chạy giai đoạn chọn cấu hình (seed 1–2), ghi và commit cấu hình đã chọn **trước** khi chạy seed báo cáo; chạy giai đoạn báo cáo (seed 3–5), gắn tag `v1.0-thuc-nghiem`, ghi số liệu vào `docs/ket_qua.md` | [4d4bc65](https://github.com/world-ender1/skill-exchange/commit/4d4bc65), [3a01bfc](https://github.com/world-ender1/skill-exchange/commit/3a01bfc), [613bd4a](https://github.com/world-ender1/skill-exchange/commit/613bd4a) |
| 8 API quản trị danh mục kỹ năng: nhóm, kỹ năng, cặp kỹ năng liên quan | [cda468d](https://github.com/world-ender1/skill-exchange/commit/cda468d) |
| Giao diện A01; điều hướng theo vai trò (admin đăng nhập vào thẳng A01) | [911c6a8](https://github.com/world-ender1/skill-exchange/commit/911c6a8) |
| Kiểm tra cả tuần: 429/429 test đạt; 13 lần làm hỏng code, một lần không test nào đỏ nên đã thêm test (nhãn 2, D54); 6 ca thủ công đạt | [ceb97d3](https://github.com/world-ender1/skill-exchange/commit/ceb97d3), [31bae4c](https://github.com/world-ender1/skill-exchange/commit/31bae4c) |
| Review độc lập code với D52–D77: sửa câu ghi chú latency do chương trình sinh, bịt 7 lệnh ghi lọt qua hook bảo vệ kết quả, sửa tài liệu theo code | [7e625a5](https://github.com/world-ender1/skill-exchange/commit/7e625a5), [fafa779](https://github.com/world-ender1/skill-exchange/commit/fafa779), [ab2430e](https://github.com/world-ender1/skill-exchange/commit/ab2430e) |
| Báo cáo tuần 5 | [618c6e7](https://github.com/world-ender1/skill-exchange/commit/618c6e7) |

## 2. Kết quả thực nghiệm chính

Số lấy từ `docs/ket_qua.md`. Trừ khi ghi khác: N = 500 người dùng, nhiễu mặc định, trung bình ± độ lệch chuẩn trên 3 seed báo cáo (seed 3–5). NDCG@5 ở đây là bản (2), chỉ tính người có tập ứng viên lý tưởng chứa ít nhất 2 mức nhãn.

| Phương án | Precision@5 nghiêm ngặt | NDCG@5 |
| --- | --- | --- |
| R (xếp ngẫu nhiên) | 0.0817 ± 0.0044 | 0.4776 ± 0.0221 |
| B0 (chỉ kỹ năng trùng) | 0.1539 ± 0.0030 | 0.5815 ± 0.0144 |
| B1 (thêm kỹ năng liên quan, xếp theo SkillFit) | 0.1581 ± 0.0026 | 0.5858 ± 0.0154 |
| P (mặc định của hệ thống) | 0.1627 ± 0.0022 | 0.6010 ± 0.0186 |
| P với công thức ScheduleFit đã chọn (`tran3`) | 0.1825 ± 0.0018 | 0.6228 ± 0.0172 |

- **Phần lớn chất lượng xếp hạng đến từ kỹ năng.** B1 cách xa R; P chỉ thêm một phần nhỏ. Hiệu theo cặp P − B1 trên NDCG@5 là 0.0152, khoảng tin cậy 95% [0.0038, 0.0261], p = 0.0113. Trên Precision@5 nghiêm ngặt, P và B1 không khác nhau có ý nghĩa (p = 0.4992).
- **Bỏ LevelFit làm giảm nhiều nhất** (NDCG@5 giảm 0.0230, p < 0.0001). Bỏ ScheduleFit hay ModeFit giảm ít hơn (0.0073 và 0.0062).
- **Nhiễu càng cao, P càng ít hơn B1.** NDCG@5 của P so với B1: không nhiễu 0.6389 so với 0.6075, nhiễu mặc định 0.6010 so với 0.5858, nhiễu cao 0.5669 so với 0.5610.
- **Mở rộng tập ứng viên (B1 so với B0) chỉ có tác dụng khi dữ liệu thưa.** Ở N = 50, tỷ lệ người có đủ 5 ứng viên là 0.8333 với B1 và 0.6400 với B0. Từ N = 500 cả hai đều bằng 1.
- **Người khai ít khung rảnh có được lợi** (góp ý tuần 2 về ScheduleFit). Với công thức chia cho min, ứng viên khai 1–3 khung chiếm 0.1128 số vị trí trong top 5, trong khi họ chỉ chiếm 0.0972 số vị trí trong danh sách. Công thức `tran3` (trần 3 khung) đưa con số này xuống 0.0743.
- **NFR-03:** tạo danh sách gợi ý cho một người với N = 1.000, đo trong bộ nhớ: trung bình 1.258 ms, p95 2.799 ms, xa dưới giới hạn 2 giây. Thời gian end-to-end qua API và CSDL sẽ đo ở tuần 8.
- **Tái lập (NFR-06):** chạy lại giai đoạn báo cáo trong một JVM khác cho các file kết quả giống hệt, trừ các dòng latency.
- Trọng số và công thức mặc định của hệ thống **không đổi** (D69). Cấu hình đã chọn chỉ là một phương án để báo cáo.

## 3. Trạng thái chức năng bắt buộc

| Mã | Chức năng | Trạng thái |
| --- | --- | --- |
| FR-01 | Đăng ký email trường, xác thực, đăng nhập, đăng xuất | **Đã làm** (tuần 5 thêm: sau đăng nhập, admin vào trang quản trị, sinh viên vào hồ sơ) |
| FR-02 | Hồ sơ, lịch rảnh, hình thức học | **Đã làm** |
| FR-03 | Kỹ năng dạy (có trình độ) / muốn học (mục tiêu tùy chọn) | **Đã làm** |
| FR-04 | Matching, xếp hạng, giải thích điểm | **Đã làm** (Reputation đọc dữ liệu thật, nhưng bảng đánh giá và phiên chưa có dữ liệu tới tuần 6–7 nên mọi người đang là 0.5) |
| FR-10 | Tìm kiếm, hồ sơ công khai | **Đã làm** (phần đánh giá ở S06 làm ở tuần 7) |
| FR-11 | Thực nghiệm đánh giá thuật toán | **Đã làm** (đo end-to-end NFR-03 ở tuần 8; tổng hợp lại ở tuần 9) |
| FR-08 | Quản trị kỹ năng, khóa tài khoản | **Làm một phần:** quản trị danh mục kỹ năng (A01) đã làm; khóa/mở khóa tài khoản ở tuần 7 |
| FR-05, FR-06, FR-09 | Yêu cầu trao đổi, phiên, báo cáo sự cố | Chưa làm – tuần 6 (admin xử lý báo cáo sự cố ở tuần 7) |
| FR-07 | Đánh giá | Chưa làm – tuần 7 |

## 4. Điều chỉnh so với báo cáo trước

Proposal và các báo cáo tuần đã nộp không sửa lại (D77). Những chỗ dưới đây được điều chỉnh khi chốt thiết kế thực nghiệm ngày 11/11. Spec tuần 1 mục 5 đã sửa theo, và báo cáo chính sửa ở các mục tương ứng (`Noi_dung_bao_cao_Tuan_5.md`).

1. **Định nghĩa nhãn 2 (Bảng 4.11, báo cáo tuần 3).** Bản cũ đòi trình độ người dạy đạt mục tiêu "LevelFit = 1.0", tức mọi kỹ năng khớp đều phải đạt. Bản mới chỉ cần **ít nhất một** kỹ năng trùng chính xác có trình độ thật đạt mục tiêu; mục tiêu bỏ trống không tính là đạt. Lý do: theo bản cũ, người dạy Java (đạt mục tiêu) cộng MySQL (thiếu 1 bậc so với mục tiêu SQL) chỉ được nhãn 1, thấp hơn người chỉ dạy Java, dù dạy được nhiều hơn. Nhãn 0 sửa tương ứng: mọi cặp kỹ năng liên quan đều thấp hơn mục tiêu từ 2 bậc. Nhãn không còn dùng LevelFit. "Hình thức học tương thích" được định nghĩa bằng điều kiện ModeFit = 1.0, và bên chưa khai báo thì không đạt. Điều kiện lịch tính trên lịch thật.
2. **Câu "Qua đó chứng minh" ở proposal §2.3.** Thực nghiệm trên dữ liệu giả lập, với nhãn do chính người thiết kế thuật toán đặt ra, không chứng minh được hai khẳng định đó. Kết quả tuần 5 chỉ cho thấy: mở rộng tập ứng viên tăng độ bao phủ khi dữ liệu thưa (N = 50, 100), và công thức đa tiêu chí cải thiện thứ tự một phần nhỏ. Báo cáo dùng "đo", "kiểm tra" thay cho "chứng minh".
3. **Cách đo P (D56).** Danh sách của P dùng để đo là danh sách lõi trả về: nhóm hai chiều trước, nhóm một chiều sau. Nhãn chỉ xét hướng người học học từ ứng viên, nên không thưởng cho tính hai chiều. P có thể bị điểm thấp khi đặt một cặp hai chiều nhãn 1 lên trên một ứng viên một chiều nhãn 2. So sánh giữa các biến thể của P không bị ảnh hưởng, vì chúng có cùng cấu trúc hai nhóm.
4. **Bỏ từng thành phần (D66).** Trọng số của thành phần bị bỏ được chia đều cho 4 thành phần còn lại, kể cả Reputation, nên tỷ lệ giữa các thành phần còn lại cũng đổi (ví dụ bỏ LevelFit thì ModeFit tăng từ 0.05 lên 0.10). Hiệu đo được vì vậy lẫn hai thay đổi.
5. **Cấu hình "Ưu tiên uy tín" (D67).** Mọi người giả lập có Reputation = 0.5, nên cấu hình này thực chất chỉ giảm tỷ trọng các thành phần khác. Reputation không đánh giá được bằng nhãn trên dữ liệu giả lập.
6. **Quy mô và seed (Bảng 1.2, mục tiêu 3).** Thêm kịch bản thưa N = 50 vì với 100 người trở lên Coverage gần như luôn bằng 1. Seed 1–2 chỉ chạy ở N = 500 để chọn cấu hình; mỗi quy mô có 3 seed báo cáo (3–5), không phải 5 seed. Thêm mốc xếp ngẫu nhiên R, Precision@5 nghiêm ngặt (chỉ đếm nhãn 2) và bản thứ hai của NDCG@5.

## 5. Khó khăn

- **Bộ test có lỗ hổng mà vẫn xanh.** Làm hỏng bộ gán nhãn theo kiểu "mọi kỹ năng trùng chính xác phải đạt mục tiêu" thì cả 16 test của bộ gán nhãn vẫn xanh, vì chưa test nào có hai kỹ năng trùng chính xác, một đạt một không. Chỉ lượt làm hỏng code mới lộ ra; đã thêm test và chạy lại.
- **Script làm hỏng code báo sai.** Lần chạy đầu báo "không test nào đỏ" cho cả 6 lần làm hỏng: Maven không chạy được, script đọc báo cáo test của lần chạy trước. Đã thêm bước xóa báo cáo cũ và một lượt đối chứng không sửa gì (phải ra 0 test đỏ).
- **Cùng seed chưa chắc cùng dữ liệu.** Thứ tự duyệt `Set.copyOf` của Java đổi giữa các lần khởi động JVM, nên nếu rút số ngẫu nhiên trong lúc duyệt `Set` thì cùng seed vẫn ra dữ liệu khác. Test chạy trong một JVM không bắt được lỗi này. Bộ sinh chỉ duyệt `List`, và được kiểm bằng hai lần chạy Maven riêng, so dấu vân tay của bộ dữ liệu.
- **Coverage gần như luôn bằng 1.** Phải thêm kịch bản thưa N = 50 và hai chỉ số bao phủ phụ mới thấy được khác biệt giữa B0 và B1.
- **Diễn giải kết quả.** Phần hơn của P nhỏ và giảm khi nhiễu tăng; ở N = 50 và 100, trung bình của P còn thấp hơn B1 (chưa kiểm định ở các quy mô này). Phải tách rõ số có kiểm định với số chỉ là trung bình 3 seed, và không viết kết luận vượt quá dữ liệu.
- **Hook bảo vệ kết quả.** Hook so chuỗi trên cả câu lệnh, nên chặn nhầm cả commit message có tên thư mục kết quả. Review độc lập còn tìm ra 7 lệnh ghi lọt qua. Chọn chấp nhận chặn nhầm, vì kết quả thực nghiệm đã bị ghi đè thì không lấy lại được.

## 6. Kế hoạch tuần 6

- Chốt quyết định tuần 6 trước khi code (rà soát đợt 4: R33–R45, R52–R54), viết ca kiểm thử từ tài liệu, lập kế hoạch tuần.
- Yêu cầu trao đổi kèm lịch đề xuất (FR-05): gửi, chấp nhận, từ chối, hủy; nút "Gửi yêu cầu" ở S05, S06; màn hình S07, S08.
- Vòng đời phiên và xác nhận hoàn thành từ hai phía (FR-06); chỉ hiện thông tin liên hệ và địa điểm/link sau khi yêu cầu được chấp nhận (NFR-02); màn hình S09.
- Gửi báo cáo sự cố cho phiên (FR-09, phần người dùng).
- Unit test cho quy tắc chuyển trạng thái của yêu cầu và phiên (spec 7.1).
