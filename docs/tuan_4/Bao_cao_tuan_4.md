# Báo cáo tuần 4 (02/11 – 08/11/2026)

Chủ đề: thuật toán ghép cặp (matching) kèm unit test cho các hàm tính điểm; hồ sơ công khai và tìm kiếm.
Repo: https://github.com/world-ender1/skill-exchange

## 1. Việc đã làm

| Việc | Commit |
| --- | --- |
| Chốt quyết định triển khai (D43–D50), viết 87 ca kiểm thử từ tài liệu trước khi code, lập kế hoạch tuần | [f619fe8](https://github.com/world-ender1/skill-exchange/commit/f619fe8), [f400cff](https://github.com/world-ender1/skill-exchange/commit/f400cff), [488e6d1](https://github.com/world-ender1/skill-exchange/commit/488e6d1), [5b88532](https://github.com/world-ender1/skill-exchange/commit/5b88532) |
| Lõi matching bằng Java thuần, viết theo TDD: khung lõi, Sim và SkillFit, LevelFit, ScheduleFit và ModeFit, Reputation, lọc cứng, điểm D, MutualScore, phân loại và xếp hạng | [45f2b9b](https://github.com/world-ender1/skill-exchange/commit/45f2b9b), [395a2cd](https://github.com/world-ender1/skill-exchange/commit/395a2cd), [1d5dfde](https://github.com/world-ender1/skill-exchange/commit/1d5dfde), [fd62529](https://github.com/world-ender1/skill-exchange/commit/fd62529), [47488ff](https://github.com/world-ender1/skill-exchange/commit/47488ff), [7010438](https://github.com/world-ender1/skill-exchange/commit/7010438), [557d824](https://github.com/world-ender1/skill-exchange/commit/557d824) |
| API gợi ý `GET /api/matches`; quyền *Active* chỉ dành cho sinh viên đã xác thực | [cf4a902](https://github.com/world-ender1/skill-exchange/commit/cf4a902) |
| Hồ sơ công khai `GET /api/users/{id}`, tìm kiếm `GET /api/users/search` | [2b8c0d8](https://github.com/world-ender1/skill-exchange/commit/2b8c0d8) |
| Giao diện S05 (gợi ý ghép cặp), S06 (hồ sơ người khác), S10 (tìm kiếm) | [90cfc33](https://github.com/world-ender1/skill-exchange/commit/90cfc33), [dd0d832](https://github.com/world-ender1/skill-exchange/commit/dd0d832), [fd08bff](https://github.com/world-ender1/skill-exchange/commit/fd08bff) |
| Hook tự chạy test lõi sau mỗi lần sửa code matching | [474d0b5](https://github.com/world-ender1/skill-exchange/commit/474d0b5) |
| Đối chiếu code với quyết định: sửa 2 lỗi (trọng số `NaN` không bị chặn, ô tìm kiếm giới hạn độ dài sai chỗ), thêm D51, sửa ngược tài liệu | [34a88f9](https://github.com/world-ender1/skill-exchange/commit/34a88f9), [e8b835c](https://github.com/world-ender1/skill-exchange/commit/e8b835c), [4066b85](https://github.com/world-ender1/skill-exchange/commit/4066b85) |
| Kiểm tra cả tuần: 288/288 test đạt; 47 lần làm hỏng code, mỗi lần đều có test đỏ (trừ 2 trường hợp có giải thích); 7 ca thủ công đạt | [2b807e9](https://github.com/world-ender1/skill-exchange/commit/2b807e9) |
| Báo cáo tuần 4 | [64f13e5](https://github.com/world-ender1/skill-exchange/commit/64f13e5) |

## 2. Trạng thái chức năng bắt buộc

| Mã | Chức năng | Trạng thái |
| --- | --- | --- |
| FR-01 | Đăng ký email trường, xác thực, đăng nhập, đăng xuất | **Đã làm** (tuần 4 thêm: gợi ý, tìm kiếm, xem hồ sơ người khác chỉ dành cho sinh viên đã xác thực) |
| FR-02 | Hồ sơ, lịch rảnh, hình thức học | **Đã làm** |
| FR-03 | Kỹ năng dạy (có trình độ) / muốn học (mục tiêu tùy chọn) | **Đã làm** |
| FR-04 | Matching, xếp hạng, giải thích điểm | **Đã làm** (Reputation đọc dữ liệu thật, nhưng bảng đánh giá và phiên chưa có dữ liệu tới tuần 6–7 nên mọi người đang là 0.5) |
| FR-10 | Tìm kiếm, hồ sơ công khai | **Đã làm** (phần đánh giá ở S06 làm ở tuần 7) |
| FR-11 | Thực nghiệm đánh giá thuật toán | Chưa làm – tuần 5 |
| FR-05, FR-06, FR-09 | Yêu cầu trao đổi, phiên, báo cáo sự cố | Chưa làm – tuần 6 |
| FR-07, FR-08 | Đánh giá; quản trị kỹ năng, khóa tài khoản | Chưa làm – quản trị danh mục kỹ năng (A01) ở tuần 5 theo spec mục 9; đánh giá, khóa/mở khóa tài khoản ở tuần 7 |

## 3. Khó khăn

- **Số thực.** Điểm tính bằng code lệch ở chữ số rất nhỏ (ví dụ 0.94 ra `0.9400000000000001`) nên test phải so bằng dung sai $10^{-9}$. Số mong đợi phải viết bằng biểu thức, vì làm tròn tay đã sai ở chữ số thứ năm (ghi 0.7975, giá trị đúng là 0.797457). Phép chia 0/0 cho ra `NaN` mà không báo lỗi; một trọng số `NaN` vẫn lọt qua phần kiểm trọng số cho tới lượt đối chiếu cuối tuần mới phát hiện.
- **So "bằng điểm" theo dung sai** không có tính bắc cầu, trái với yêu cầu của `Comparator` trong Java (D48). Vẫn giữ vì đúng câu chữ spec 4.4 và rất hiếm gặp với điểm thật; ghi vào phần hạn chế.
- **Hai lớp kiểm quyền che nhau.** Khi thử bỏ luật quyền ở cấu hình bảo mật, một số test vẫn xanh vì service cũng trả 403. Phải dùng ca gọi với tham số sai (403 phải đứng trước 400) mới bắt được, và bảng "làm hỏng code" phải chạy lại khi bộ test thay đổi.
- **Tài liệu không theo kịp code.** Kế hoạch tuần chốt nhiều chi tiết ngay lúc code nhưng không sửa ngược vào file quyết định và bộ ca kiểm thử. Lượt đối chiếu cuối tuần tìm ra hơn 10 chỗ tài liệu lệch với code, cộng 2 lỗi code nhỏ.
- **Thứ tự tên kỹ năng.** Lõi Java không có quy tắc so sánh chữ (collation) của PostgreSQL, nên tên có dấu hoặc bắt đầu bằng chữ thường có thể xếp khác với CSDL (D51). 26 kỹ năng hiện có không bị ảnh hưởng.

## 4. Kế hoạch tuần 5

- Chốt các điểm còn mở của thiết kế thực nghiệm trước khi code (rà soát đợt 3, R24–R32): nhãn suy biến, nhãn khi người học muốn học nhiều kỹ năng, Precision@5 với ứng viên bị loại, Coverage của B0/B1, cấu hình "ưu tiên uy tín", bảng tham số sinh dữ liệu, cách đo latency, số seed so với yêu cầu p-value.
- Module `evaluation`: sinh dữ liệu giả lập có giá trị thật và giá trị tự khai có nhiễu, gán nhãn theo quy tắc thác nước (Bảng 4.11), gọi đúng lõi matching của tuần 4.
- Chạy B0, B1, P và mốc ngẫu nhiên R; đo Precision@5, NDCG@5, Coverage, Latency; phân tích độ nhạy bộ trọng số và 3 công thức ScheduleFit; thí nghiệm bỏ từng thành phần.
- Đo NFR-03 (gợi ý ≤ 2 giây với 1.000 hồ sơ).
- Quản trị danh mục kỹ năng và quan hệ liên quan (FR-08, màn hình A01) theo spec mục 9.
