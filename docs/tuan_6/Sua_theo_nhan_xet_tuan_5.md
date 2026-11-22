# Sửa báo cáo theo nhận xét báo cáo tuần 5

Ngày 22/11/2026. Các sửa đổi dưới đây đã đưa vào báo cáo tổng (`docs/tuan_6/Update báo cáo tuần 6.docx`), chủ yếu ở mục 6.3, 6.4 và chương 7, kèm một trang tóm tắt ở đầu báo cáo. Mọi số lấy từ `docs/ket_qua.md`, mục "Bổ sung theo nhận xét báo cáo tuần 5".

## 1. Tóm tắt

Để trả lời các câu hỏi về thống kê và diễn giải bằng số liệu chứ không chỉ bằng lập luận, đề tài chạy thêm một lượt thực nghiệm (lượt bổ sung): cùng dữ liệu seed 3–5 với lượt báo cáo, thêm ba phân tích, không sửa lõi thuật toán ghép cặp. Lượt bổ sung cho số của B1 và P giống hệt lượt báo cáo (216/216 ô), nên các số cũ trong báo cáo không đổi. Báo cáo có thêm 6 bảng (6.14, 6.16, 6.18–6.21), cột p sau hiệu chỉnh ở Bảng 6.13 và một tài liệu tham khảo (Holm, 1979).

| Nhận xét | Đã làm | Vị trí trong báo cáo tổng |
| --- | --- | --- |
| 1a. Đơn vị của kiểm định, tính độc lập | Nêu rõ đơn vị là từng người dùng, gộp 3 seed, và vì sao không hoàn toàn độc lập; chạy lại mỗi phép riêng từng seed | 6.3: đoạn "Đơn vị của kiểm định", Bảng 6.10 (dòng Thống kê), Bảng 6.14; 6.4 "Độ tin cậy của kết quả"; 7.2 |
| 1b. Kiểm định bội | Hiệu chỉnh Holm–Bonferroni theo hai họ phép so sánh | 6.3: đoạn "Kiểm định bội", Bảng 6.13 (cột p Holm), Bảng 6.16, 6.19, 6.20; 6.4 đoạn đầu; tài liệu tham khảo [18] |
| 2a. P thấp hơn B1 ở N = 50, 100 | Thêm phương án "P một danh sách" để tách cách đặt nhóm khỏi công thức điểm | 6.3 RQ2 "Theo quy mô", Bảng 6.18, 6.19; 6.4 "Ở quy mô nhỏ, P thua B1 vì cách đặt nhóm" |
| 2b. Cơ sở của mô hình nhiễu | Giải thích nhiễu đặt vào dữ liệu nào, dạng nhiễu, vì sao dùng ba mức | 6.3 đoạn "Cơ sở của mô hình nhiễu"; 7.3 |
| 2c. Bỏ từng thành phần, chia theo tỉ lệ | Chạy thêm ba phương án chia lại trọng số theo tỉ lệ | Bảng 6.10 (dòng Phương án); 6.3 RQ2 "Từng tiêu chí", Bảng 6.16; 6.4 |
| 2d. Đánh giá riêng từng nhóm | Đo hai chỉ số chính trong nhóm hai chiều và nhóm một chiều | 6.3 RQ2 "Trong từng nhóm", Bảng 6.20; 6.4 |
| 3. Cấu hình hệ thống dùng | Kiểm định cấu hình đã chọn so với mặc định; nêu cấu hình hệ thống dùng và lý do giữ | 6.3 RQ3, Bảng 6.21; 6.4 đoạn "Cấu hình hệ thống đang dùng"; 7.3 |
| 4. Mã nội bộ | Bỏ mã quyết định, mã rà soát, số mục của tài liệu nội bộ khỏi toàn bộ báo cáo | Chương 4, 5, 6, 7 |

## 2. Phương pháp thống kê

**1a. Đơn vị của kiểm định.** Bootstrap và Wilcoxon tính trên từng người dùng thuộc bản (2) của NDCG@5, gộp người dùng của 3 seed (1091 người ở N = 500). Người dùng trong cùng một seed không hoàn toàn độc lập: mọi danh sách lấy từ chung một tập người, và một người vừa là người học vừa là ứng viên của người khác. Báo cáo nêu rõ điều này và hệ quả: khoảng tin cậy có thể hẹp hơn thực tế. Vì ba seed là ba bộ dữ liệu sinh độc lập, lượt bổ sung chạy lại mỗi phép riêng từng seed. Với P − B1 ở N = 500 trên NDCG@5, hiệu dương ở cả ba seed (0.0053, 0.0237, 0.0167) nhưng chỉ seed 4 có p dưới 0.05. Mọi so sánh của lượt bổ sung còn ý nghĩa sau hiệu chỉnh đều cùng chiều ở cả ba seed, trừ một trường hợp hiệu đúng bằng 0 ở một seed. Chỉ có 3 seed nên không kiểm định được với đơn vị là bộ dữ liệu (với 3 quan sát, p nhỏ nhất của Wilcoxon là 0.25); điều này ghi ở mục Hạn chế.

**1b. Kiểm định bội.** Dùng Holm–Bonferroni. Họ 1 là 8 phép của lượt báo cáo, đặt ra trước khi chạy: mọi phép có p dưới 0.05 vẫn dưới 0.05 sau hiệu chỉnh; P − B1 trên NDCG@5 tăng từ 0.0113 lên 0.0339. Họ 2 là 38 phép của lượt bổ sung, đặt ra sau khi xem kết quả nên được ghi là phân tích thăm dò; trong họ này P − B1 ở N = 500 có p = 0.0904. Bốn bộ trọng số và ba công thức ScheduleFit ở giai đoạn chọn chỉ được so bằng trung bình để chọn cấu hình, không kiểm định. Theo các số này, nhận định ở 6.4 sửa thành: phần hơn của P so với B1 ở N = 500 nhỏ và chưa vững.

## 3. Diễn giải kết quả

**2a. N = 50 và 100.** Giả thuyết ở bản tuần 5 là cách P luôn đặt nhóm hai chiều lên trước nhóm một chiều. Để kiểm, lượt bổ sung thêm phương án P một danh sách: dùng đúng điểm D của P nhưng gộp hai nhóm và xếp chung. P một danh sách hơn B1 ở mọi quy mô (NDCG@5 hơn 0.0890–0.1077, có ý nghĩa sau hiệu chỉnh, cùng chiều ở cả ba seed) và hơn P ở mọi quy mô. Vậy phần thua đến từ cách đặt nhóm, không từ công thức điểm. Trên giao diện, hai nhóm nằm ở hai tab riêng, nên danh sách ghép mà thực nghiệm dùng để đo không phải thứ người dùng thấy. Ở N = 50 và 100, hiệu P − B1 âm ở cả ba seed nhưng không có ý nghĩa (p = 0.1322 và 0.0934).

**2b. Mô hình nhiễu.** Báo cáo bổ sung: nhiễu chỉ đặt vào trình độ dạy và lịch rảnh, hai loại dữ liệu hệ thống không xác minh được; kỹ năng, mục tiêu, hình thức học không có nhiễu vì người dùng biết rõ. Dạng nhiễu là kiểu sai đơn giản nhất của từng loại: trình độ lệch sang bậc kề; lịch có quên khai và khai thừa, xét độc lập từng khung. Các xác suất là giả định vì chưa có số liệu thật; do đó thực nghiệm dùng ba mức (không nhiễu làm đối chứng, mặc định, cao) và kết luận theo chiều thay đổi qua ba mức. Hướng phát triển thêm việc ước lượng mức nhiễu bằng cách so lịch đã khai với giờ hẹn của các phiên thật.

**2c. Chia lại trọng số theo tỉ lệ.** Tách được: chia theo tỉ lệ giữ nguyên tỷ lệ giữa các thành phần còn lại, nên hiệu so với P chỉ còn do mất thành phần. Kết luận giống cách chia đều: hiệu NDCG@5 lớn hơn một chút (LevelFit 0.0273, ScheduleFit 0.0123, ModeFit 0.0072, so với 0.0230, 0.0073, 0.0062), thứ tự đóng góp không đổi, ScheduleFit vẫn không có ý nghĩa trên Precision@5 nghiêm ngặt. Hạn chế "hiệu lẫn hai thay đổi" được bỏ khỏi mục 7.2.

**2d. Đánh giá riêng từng nhóm.** Có, và cách đo này gần với giao diện hơn. Ở N = 500, P hơn B1 0.0313 NDCG@5 trong nhóm hai chiều và 0.0913 trong nhóm một chiều, đều có ý nghĩa sau hiệu chỉnh, trong khi đo cả danh sách chỉ hơn 0.0152. Trong nhóm hai chiều, P một danh sách còn cao hơn P, vì nhãn chỉ xét chiều người học học từ ứng viên. Chỉ số trong từng nhóm mới đo ở N = 500; đo ở N = 50, 100 ghi vào hướng phát triển.

## 4. Cấu hình hệ thống dùng

Hệ thống dùng bộ trọng số mặc định (SkillFit 0.55, LevelFit 0.20, ScheduleFit 0.15, ModeFit 0.05, Reputation 0.05) và ScheduleFit chia cho min. Cấu hình đã chọn ở seed 1–2 chỉ là phương án so sánh trong thực nghiệm. Lượt bổ sung kiểm định cấu hình đã chọn so với mặc định:

- Bộ "ưu tiên lịch": hiệu NDCG@5 0.0032, khoảng tin cậy [−0.0003, 0.0067], p = 0.0916. Không khác mặc định, nên không đổi.
- Công thức `tran3`: hiệu NDCG@5 0.0217, Precision@5 nghiêm ngặt 0.0273, p < 0.0001, có ý nghĩa ở từng seed. Vẫn chưa đổi vì hai lý do. Thứ nhất, nhãn 2 đòi lịch thật trùng ít nhất 2 khung, đúng thứ `tran3` đếm, nên dữ liệu giả lập không tách được phần hơn thật khỏi phần hơn do cách đặt nhãn. Thứ hai, `tran3` không bỏ thiên lệch mà đảo chiều nó: ứng viên khai 1–3 khung chiếm 0.0743 số vị trí trong top 5 so với 0.0972 trong danh sách; với công thức hiện tại là 0.1128.

Việc có đổi công thức hay không chuyển sang hướng phát triển: so lại với một bản nhãn không đếm khung trùng, và đối chiếu với kết quả của phiên thật.

## 5. Mã nội bộ

Đã bỏ khỏi toàn bộ báo cáo: mã quyết định triển khai, mã rà soát, số mục của tài liệu phân tích và thiết kế nội bộ ("spec 5.4", "TK 4.3", "phụ lục A.3"), "kế hoạch tuần 6", "bước 1". Thay bằng nội dung, hoặc bằng chỉ dẫn tới bảng, mục có trong báo cáo:

- Cột "Yêu cầu" của các bảng ca kiểm thử (Bảng 6.2, 6.4, 6.6, 6.8) chỉ còn mã yêu cầu FR/NFR và số bảng trong báo cáo.
- Cột "Test bắt được" của các bảng làm hỏng code (Bảng 6.5, 6.7, 6.9) ghi tên test theo nội dung thay cho mã quyết định.
- Các đoạn ở chương 4, 5, 6, 7 có mã được viết lại.

Các mã còn giữ đều được định nghĩa ngay trong báo cáo: yêu cầu FR/NFR (Bảng 3.1, 3.2), giả định G (Bảng 3.10), màn hình S/A (Bảng 4.14), mã ca kiểm thử TC. Script dựng báo cáo tự kiểm rằng không còn mã thuộc các dạng đã bỏ.

## 6. Đổi số bảng

| Bảng cũ | Bảng mới | Nội dung |
| --- | --- | --- |
| – | 6.14 | P − B1 theo từng seed (mới) |
| 6.14 | 6.15 | Kết quả theo quy mô |
| – | 6.16 | Bỏ từng thành phần, chia đều và chia theo tỉ lệ (mới) |
| 6.15 | 6.17 | Kết quả theo mức nhiễu (dời lên ngay sau đoạn nói về nó) |
| – | 6.18 | P một danh sách so với P và B1 theo quy mô (mới) |
| – | 6.19 | Kiểm định theo quy mô (mới) |
| – | 6.20 | Chỉ số trong từng nhóm (mới) |
| – | 6.21 | Cấu hình đã chọn so với P (mới) |
| 6.16 | 6.22 | Nhóm khai ít khung |
| 6.17 | 6.23 | Thời gian tạo danh sách gợi ý |

## 7. Nguồn số liệu và cách chạy lại

- Lượt bổ sung: `thuc_nghiem/ket_qua/2026-11-22_bo-sung/`, commit `10ecc31`, chạy 22.0 giây. Lệnh (từ `backend/`): `java -cp target/classes com.skillexchange.evaluation.EvaluationMain --giai-doan bo-sung --ten bo-sung`. Chương trình chỉ chạy khi `backend/` đã commit sạch.
- Lõi thuật toán ghép cặp không đổi; số của B1 và P ở lượt bổ sung giống hệt lượt báo cáo (216/216 ô và hai phép P − B1).
- Hiệu chỉnh Holm và kiểm định cấu hình đã chọn: `python thuc_nghiem/phan_tich/bo_sung_tuan_6.py`, chạy từ thư mục gốc. Script chỉ đọc CSV và tự kiểm rằng tính lại P − B1 khớp kết quả của chương trình thực nghiệm.
- Mọi số: `docs/ket_qua.md`, mục "Bổ sung theo nhận xét báo cáo tuần 5" (bảng 8–14). Các bảng mới trong báo cáo dựng thẳng từ các bảng này.

## 8. Còn lại

- Chỉ số trong từng nhóm mới đo ở N = 500.
- Mức nhiễu vẫn là giả định, chưa đo từ dữ liệu thật.
- Các phép của lượt bổ sung đặt ra sau khi xem kết quả lượt báo cáo, nên là kết quả thăm dò.
- Chưa có nguồn nhãn độc lập; câu hỏi có đổi công thức ScheduleFit hay không còn mở.
