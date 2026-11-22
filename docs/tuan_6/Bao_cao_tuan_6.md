# Báo cáo tuần 6 (16/11 – 22/11/2026)

Chủ đề: chuyển giao diện sang Tailwind CSS + shadcn/ui; yêu cầu trao đổi kèm lịch đề xuất (FR-05), vòng đời phiên và xác nhận hoàn thành (FR-06), quyền xem liên hệ và địa điểm/link (NFR-02), gửi báo cáo sự cố (FR-09, phần người dùng).
Repo: https://github.com/world-ender1/skill-exchange

## 1. Việc đã làm

| Việc | Commit |
| --- | --- |
| Chốt 19 quyết định triển khai (D78–D96), gồm rà soát đợt 4 (R33–R39) và 11 câu hỏi Q1–Q11; sửa ngược spec, thiết kế; viết 61 ca kiểm thử từ tài liệu trước khi code; lập kế hoạch tuần | [9b9ac01](https://github.com/world-ender1/skill-exchange/commit/9b9ac01), [161ed66](https://github.com/world-ender1/skill-exchange/commit/161ed66), [d7a0281](https://github.com/world-ender1/skill-exchange/commit/d7a0281) |
| Giao diện mới: khung trang có menu bên và menu thu gọn cho màn hình hẹp, màu, font, chế độ tối theo hệ thống; chuyển 8 màn hình đã có sang shadcn/ui | [0555446](https://github.com/world-ender1/skill-exchange/commit/0555446), [9fb5d43](https://github.com/world-ender1/skill-exchange/commit/9fb5d43), [cbb2ca2](https://github.com/world-ender1/skill-exchange/commit/cbb2ca2) |
| Entity, Repository cho yêu cầu, phiên, báo cáo sự cố; không thêm migration | [76d9544](https://github.com/world-ender1/skill-exchange/commit/76d9544) |
| API tạo yêu cầu: kiểm 8 bước theo thứ tự cố định | [a0e1f71](https://github.com/world-ender1/skill-exchange/commit/a0e1f71) |
| Hộp yêu cầu; chấp nhận (tạo phiên trong cùng transaction), từ chối, hủy; khóa lạc quan. Viết theo TDD | [660ad71](https://github.com/world-ender1/skill-exchange/commit/660ad71) |
| Vòng đời phiên: bắt đầu từ 15 phút trước giờ hẹn, hoàn thành hai bước, hủy; chi tiết phiên có liên hệ bên kia. Viết theo TDD | [99ee00a](https://github.com/world-ender1/skill-exchange/commit/99ee00a) |
| Gửi báo cáo sự cố; báo cáo đang mở chặn xác nhận hoàn thành. Viết theo TDD | [f949362](https://github.com/world-ender1/skill-exchange/commit/f949362) |
| Màn hình S07 Gửi yêu cầu, S08 Hộp yêu cầu, S09 Phiên của tôi và chi tiết phiên | [532f7f3](https://github.com/world-ender1/skill-exchange/commit/532f7f3), [c7fd9f1](https://github.com/world-ender1/skill-exchange/commit/c7fd9f1), [427d313](https://github.com/world-ender1/skill-exchange/commit/427d313) |
| Kiểm tra cả tuần: 497/497 test đạt; 38 lần làm hỏng code; ba lượt review độc lập; 16 ca thủ công trên trình duyệt. Sửa 5 lỗi giao diện tìm ra (tương phản màu trạng thái, thiếu nút "Thử lại", tên dài kéo rộng trang 360 px, mất tiêu đề `h1`/`h2`, `aria-current` thừa) và sửa tài liệu theo code | [2b71563](https://github.com/world-ender1/skill-exchange/commit/2b71563), [e341fac](https://github.com/world-ender1/skill-exchange/commit/e341fac), [3539e90](https://github.com/world-ender1/skill-exchange/commit/3539e90) |
| Sửa câu thông báo sau khi đăng ký (S01) | [4d940cb](https://github.com/world-ender1/skill-exchange/commit/4d940cb) |
| Báo cáo tuần 6 | [afe2f99](https://github.com/world-ender1/skill-exchange/commit/afe2f99) |

## 2. Kết quả chính

- **Kiểm thử:** backend đạt 497/497 test, gồm 429 test của tuần 3–5; tuần 6 thêm 68 test. Cả 45 ca tích hợp có test; 16 ca thủ công đạt (149 lượt kiểm trên Chrome điều khiển tự động, CSDL tạm). Lõi matching không đổi, không thêm migration, không sửa test cũ.
- **Bộ test bắt được lỗi thật:** 25 lần làm hỏng backend đều có test đỏ; 12/13 lần làm hỏng luật hiển thị của giao diện có ca đỏ, lần còn lại là đột biến tương đương (kết quả không đổi).
- **Thao tác đồng thời:** ngoài hai ca chạy song song 20 lần, ba test tạo tranh chấp chắc chắn (một kết nối riêng giữ khóa dòng) chứng minh: hủy đúng lúc chấp nhận, hai người cùng bấm hoàn thành, gửi báo cáo đúng lúc xác nhận hoàn thành đều cho một lệnh 409, không ghi đè.
- **Riêng tư (NFR-02):** API yêu cầu không bao giờ trả số điện thoại, liên hệ; người nhận chỉ thấy địa điểm/link sau khi chấp nhận; liên hệ chỉ có ở chi tiết phiên cho hai người tham gia (TC337, TC339, TC340).
- **Giao diện:** mọi cặp chữ/nền đo được từ 5.09:1 trở lên (sau khi đổi màu trạng thái), viền ô nhập 3.74:1; 12 màn hình ở 360 px không cuộn ngang; chế độ tối đổi theo hệ thống trên 11 màn; font đúng thiết kế, không gọi Google Fonts.

## 3. Trạng thái chức năng bắt buộc

| Mã | Chức năng | Trạng thái |
| --- | --- | --- |
| FR-01 | Đăng ký email trường, xác thực, đăng nhập, đăng xuất | **Đã làm** |
| FR-02 | Hồ sơ, lịch rảnh, hình thức học | **Đã làm** |
| FR-03 | Kỹ năng dạy (có trình độ) / muốn học (mục tiêu tùy chọn) | **Đã làm** |
| FR-04 | Matching, xếp hạng, giải thích điểm | **Đã làm** (tỉ lệ hoàn thành trong Reputation đã có dữ liệu phiên thật từ tuần 6; điểm đánh giá có từ tuần 7) |
| FR-05 | Yêu cầu trao đổi kèm lịch đề xuất | **Đã làm** (tuần 6) |
| FR-06 | Phiên trao đổi, xác nhận hoàn thành từ hai phía | **Đã làm** (tuần 6) |
| FR-09 | Báo cáo sự cố | **Làm một phần:** người tham gia gửi báo cáo (tuần 6); quản trị viên xử lý (A02) ở tuần 7 |
| FR-10 | Tìm kiếm, hồ sơ công khai | **Đã làm** (phần đánh giá ở S06 làm ở tuần 7) |
| FR-11 | Thực nghiệm đánh giá thuật toán | **Đã làm** (đo end-to-end NFR-03 ở tuần 8; tổng hợp lại ở tuần 9) |
| FR-08 | Quản trị kỹ năng, khóa tài khoản | **Làm một phần:** quản trị danh mục kỹ năng (A01) đã làm; khóa/mở khóa tài khoản ở tuần 7 |
| FR-07 | Đánh giá | Chưa làm – tuần 7 |

## 4. Điều chỉnh so với báo cáo trước

Proposal và các báo cáo tuần đã nộp không sửa lại (D77). Những chỗ dưới đây được điều chỉnh khi chốt quyết định tuần 6 (bước 0, ngày 18/11) và trong lúc làm. Spec tuần 1 mục 6.2–6.4 và thiết kế tuần 2 đã sửa theo; báo cáo chính sửa ở các mục tương ứng (`Noi_dung_bao_cao_Tuan_6.md`).

1. **Giao diện đổi sang Tailwind CSS + shadcn/ui (D78–D85).** Kế hoạch tuần 3 chốt "giao diện CSS viết tay, không thêm thư viện UI". Tuần 6 dùng Tailwind CSS 4 và bộ component shadcn/ui; màu, font, khung trang, chế độ tối và cách hiện thông báo chốt ở D79–D85. Chỉ đổi phần trình bày: đường dẫn, lời gọi API, cách hiện lỗi, luật dữ liệu riêng tư không đổi. Hệ quả cho báo cáo chính: thêm thư viện vào Bảng 2.1 và 5.1; các hình 5.1–5.8, 5.10, 5.11 là ảnh giao diện trước tuần 6.
2. **Báo cáo sự cố chỉ gửi khi phiên chờ xác nhận (R33).** Thiết kế tuần 2 không đặt điều kiện trạng thái khi gửi báo cáo, trong khi quản trị viên chỉ chốt được phiên đang `WAITING_CONFIRMATION`: báo cáo gửi lúc phiên còn `SCHEDULED`, `IN_PROGRESS` sẽ treo mãi. Nay báo cáo chỉ gửi được khi phiên `WAITING_CONFIRMATION`, mỗi phiên một báo cáo mở, và khi có báo cáo mở thì không xác nhận hoàn thành được, chỉ quản trị viên chốt. Trong lúc làm, thêm một điều chỉnh nữa: lệnh gửi báo cáo ép tăng `version` của phiên, vì ở mức cô lập của PostgreSQL việc kiểm "có báo cáo mở không" trong cùng transaction không chặn được hai lệnh chạy chồng (Báo cáo chính, mục 4.5).
3. **Các mốc thời gian đã trôi qua (R34).** Spec cũ ghi hủy phiên "trước giờ hẹn"; không có quy tắc cho việc chấp nhận yêu cầu đã quá giờ đề xuất. Nay: chấp nhận khi đã tới thời điểm đề xuất trả 409 (từ chối, hủy vẫn được); hủy phiên `SCHEDULED` được ở mọi thời điểm, kể cả sau giờ hẹn, và không tính vào tỉ lệ hoàn thành; "Bắt đầu" không có giới hạn trên; phiên `SCHEDULED` không tự hết hạn (G2 sửa theo).
4. **Ai thấy địa điểm/link (Q1, thay R35).** Ngày 29/10 đã chọn cho cả hai bên thấy địa điểm/link ngay khi yêu cầu còn `PENDING` (R35). Phương án đó nới NFR-02 ("chỉ hiển thị cho hai bên từ khi yêu cầu được chấp nhận"), nên được hỏi lại ở bước 0 và đổi: người nhận chỉ thấy sau khi chấp nhận; người gửi luôn xem lại nội dung do chính mình nhập. Số điện thoại, liên hệ chỉ ở chi tiết phiên. Ma trận phân quyền (Bảng 3.9) tách thành hai dòng.
5. **Kiểm thêm khi tạo và chấp nhận yêu cầu (R36, R37).** Kỹ năng đề nghị học phải có trong danh sách muốn học của bên học (trước đây chỉ kiểm $Sim > 0$); người nhận không hợp lệ (không tồn tại, chưa xác thực, bị khóa, quản trị viên) cùng trả 404; chấp nhận khi người gửi đã bị khóa trả 409. Các thao tác trên phiên thì không xét trạng thái tài khoản bên kia.
6. **Màu trạng thái đậm thêm một bậc (D79, kiểm tra cả tuần).** Màu đã chốt đạt 4.5:1 trên nền trắng, nhưng chữ trên nền nhạt cùng màu của nhãn trạng thái và nút "Từ chối" chỉ đạt 3.82–4.37:1 khi đo thật. Đổi sang `red-700`, `green-800`, `amber-800` (chế độ tối `red-300`); đo lại đạt hết.

## 5. Khó khăn

- **Chạy chồng giữa báo cáo sự cố và xác nhận hoàn thành.** Quy tắc R33 ghi "kiểm báo cáo mở trong cùng transaction", nhưng ở mức cô lập READ COMMITTED, hai lệnh chạy chồng không thấy dữ liệu chưa commit của nhau nên cả hai cùng thành công. Chỉ phát hiện được khi viết test tạo tranh chấp chắc chắn; đã sửa bằng khóa lạc quan ép tăng `version`.
- **Test song song không bảo đảm có tranh chấp.** Chạy hai lệnh cùng lúc 20 lần có thể không lần nào chồng nhau thật. Phải dựng tranh chấp bằng một kết nối JDBC riêng giữ khóa dòng và đợi tới khi `pg_stat_activity` cho thấy lệnh của ứng dụng đang chờ khóa.
- **Chuyển thư viện giao diện giữa chừng.** Cài Tailwind là kiểu mặc định (Preflight) áp lên cả trang, làm vỡ CSS cũ ngay lập tức, nên 8 màn hình phải chuyển trong cùng một đợt trên nhánh riêng. CLI của shadcn/ui không có màu nền `slate`, phải viết tay toàn bộ biến màu; vài component sinh ra không hợp nhu cầu (ô chọn của Radix không nối được nhãn, thanh tiến độ không đọc được giá trị) nên dùng cách khác.
- **Lỗi giao diện không lộ ra khi đọc code.** Tương phản và bố cục ở 360 px chỉ đo được khi chạy thật trên trình duyệt; ngược lại, việc mất tiêu đề `h1`/`h2` không nhìn thấy trên màn hình mà chỉ review đọc code mới thấy.
- **Giờ hiển thị và giờ gửi đi.** Giao diện hiện giờ theo giờ máy người dùng nhưng gửi giờ UTC; mốc mở nút "Bắt đầu" có thể rơi vào ngày hôm trước (hẹn 00:10 thì mở lúc 23:55 hôm trước), nên chữ "Mở lúc" phải ghi kèm ngày.

## 6. Kế hoạch tuần 7

- Chốt quyết định tuần 7 trước khi code (rà soát đợt 4b: R40, R41 phần tài khoản, R44, R45, R52–R54), viết ca kiểm thử từ tài liệu, lập kế hoạch tuần.
- Đánh giá sau phiên (FR-07): mỗi người đánh giá bên kia một lần cho mỗi phiên hoàn thành; hiện đánh giá ở hồ sơ công khai S06; điểm đánh giá vào Reputation.
- Quản trị viên xử lý báo cáo sự cố (A02, FR-09 phần quản trị): hàng đợi báo cáo, xem phiên, chốt phiên sang hoàn thành hoặc hủy kèm ghi chú.
- Khóa / mở khóa tài khoản (FR-08 phần còn lại).
- Xem lại các điểm nhỏ còn để lại ở `docs/tuan_6/kiem_thu/README.md` (mục "Giới hạn").
