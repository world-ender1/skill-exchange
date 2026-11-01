# Báo cáo tuần 3 (24/10 – 01/11/2026)

Chủ đề: đăng ký, xác thực email trường, đăng nhập; hồ sơ, lịch rảnh, kỹ năng.
Repo: https://github.com/world-ender1/skill-exchange

## 1. Việc đã làm

| Việc | Commit |
| --- | --- |
| Chốt quyết định triển khai (D1–D30, D39–D42), kế hoạch tuần, 93 ca kiểm thử viết trước | [0e09891](https://github.com/world-ender1/skill-exchange/commit/0e09891), [74b5380](https://github.com/world-ender1/skill-exchange/commit/74b5380), [e220e4e](https://github.com/world-ender1/skill-exchange/commit/e220e4e), [78a839f](https://github.com/world-ender1/skill-exchange/commit/78a839f) |
| Nạp danh mục kỹ năng (26 kỹ năng, 6 nhóm) và API danh mục | [c2f62b4](https://github.com/world-ender1/skill-exchange/commit/c2f62b4) |
| Cấu hình bảo mật, lỗi dạng Problem Details | [414ddd7](https://github.com/world-ender1/skill-exchange/commit/414ddd7) |
| Đăng ký bằng email trường | [d4ef4e4](https://github.com/world-ender1/skill-exchange/commit/d4ef4e4) |
| Xác thực email qua link (Mailpit), gửi lại link, đăng ký lại | [fa1f7f6](https://github.com/world-ender1/skill-exchange/commit/fa1f7f6), [237f933](https://github.com/world-ender1/skill-exchange/commit/237f933), [6d17ee4](https://github.com/world-ender1/skill-exchange/commit/6d17ee4) |
| Đăng nhập JWT, chặn tài khoản bị khóa | [23901ca](https://github.com/world-ender1/skill-exchange/commit/23901ca) |
| Tạo tài khoản admin lúc khởi động | [4711137](https://github.com/world-ender1/skill-exchange/commit/4711137) |
| API hồ sơ, lịch rảnh, kỹ năng dạy / muốn học | [cd8794b](https://github.com/world-ender1/skill-exchange/commit/cd8794b), [5a0e824](https://github.com/world-ender1/skill-exchange/commit/5a0e824) |
| Giao diện S01–S04 (đăng nhập/đăng ký, xác thực, hồ sơ, kỹ năng) | [4d9dcf2](https://github.com/world-ender1/skill-exchange/commit/4d9dcf2), [bfaf318](https://github.com/world-ender1/skill-exchange/commit/bfaf318), [d48726c](https://github.com/world-ender1/skill-exchange/commit/d48726c), [8be14ae](https://github.com/world-ender1/skill-exchange/commit/8be14ae) |
| Kiểm tra cả tuần: 139/139 test đạt, làm hỏng 6 quy tắc đều bị test bắt | [f416650](https://github.com/world-ender1/skill-exchange/commit/f416650) |

## 2. Trạng thái chức năng bắt buộc

| Mã | Chức năng | Trạng thái |
| --- | --- | --- |
| FR-01 | Đăng ký email trường, xác thực, đăng nhập, đăng xuất | **Đã làm** (giới hạn matching cho tài khoản đã xác thực làm cùng matching ở tuần 4) |
| FR-02 | Hồ sơ, lịch rảnh, hình thức học | **Đã làm** |
| FR-03 | Kỹ năng dạy (có trình độ) / muốn học (mục tiêu tùy chọn) | **Đã làm** |
| FR-04 | Matching, xếp hạng, giải thích điểm | Chưa làm – tuần 4 |
| FR-10 | Tìm kiếm, hồ sơ công khai | Chưa làm – tuần 4 |
| FR-11 | Thực nghiệm đánh giá thuật toán | Chưa làm – tuần 5 |
| FR-05, FR-06, FR-09 | Yêu cầu trao đổi, phiên, báo cáo sự cố | Chưa làm – tuần 6 |
| FR-07, FR-08 | Đánh giá; quản trị kỹ năng, khóa tài khoản | Chưa làm – tuần 7 (đã có sẵn phần chặn tài khoản bị khóa) |

## 3. Khó khăn

- Spring Boot 4 / Spring Security 7 khác nhiều tài liệu cũ (tên starter, cách cấu hình JWT); phải đối chiếu đúng phiên bản trong `pom.xml`.
- Kiểm thời gian (hạn link 24 giờ, giới hạn 60 giây, hạn JWT): phải dùng chung một đồng hồ `Clock` để test tua giờ được, kể cả bộ kiểm hạn JWT.
- Một quy tắc ("link chỉ dùng một lần") bị xóa mà test vẫn xanh, vì có lớp chặn khác che mất; phải thêm unit test riêng.
- Chưa tự bấm kiểm thủ công đủ 3 ca trên trình duyệt (TC04, TC31, TC74).

## 4. Kế hoạch tuần 4

- Làm xong kiểm thủ công TC04, TC31, TC74, chụp màn hình cho báo cáo.
- Viết ca kiểm thử tuần 4 từ tài liệu trước khi code.
- Lõi matching (lọc cứng, chấm điểm, phân loại, xếp hạng) kèm unit test cho từng hàm tính điểm.
- API `GET /api/matches`, `GET /api/users/{id}`, `GET /api/users/search`; màn hình S05, S06, S10.
