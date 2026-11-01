# Kiểm thử tuần 3 – Tài khoản, hồ sơ, kỹ năng

Mục đích: chứng minh code tuần 3 làm đúng các ca kiểm thử viết trước từ tài liệu (`ca_kiem_thu.md`), và bộ test **bắt được lỗi thật**, không chỉ chạy xanh.

## Các file

| File | Nội dung |
| --- | --- |
| `ca_kiem_thu.md` | 93 ca kiểm thử viết từ tài liệu trước khi code (TC01–TC93) |
| `backend/src/test/java/com/skillexchange/**` | Test tự động: mỗi test có `@DisplayName("TCxx ...")` hoặc `"Dxx ..."` |
| `ket_qua.txt` | Trích kết quả lần chạy gần nhất (`Tests run`, `FAIL`, `ERROR`) |

## Cách chạy lại

```powershell
# C:\DACN, Docker đang chạy; DB test tạo một lần (D39):
docker compose up -d
docker compose exec db createdb -U skillexchange skillexchange_test
cd backend
.\mvnw.cmd test | Select-String "Tests run|FAIL|ERROR" | Out-File -Encoding utf8 ..\docs\tuan_3\kiem_thu\ket_qua.txt
```

Trong `ket_qua.txt` có vài dòng `Error starting ApplicationContext`: đó là các test của TC81 **cố ý** khởi động ứng dụng thiếu hoặc sai `ADMIN_EMAIL`/`ADMIN_PASSWORD` để kiểm ứng dụng dừng lại. Không phải lỗi.

Frontend: `cd frontend; npm run build; npm run lint`.

## Thiết kế kiểm thử

| Loại | Số lượng | Cách làm |
| --- | --- | --- |
| Test tích hợp (`@SpringBootTest` + MockMvc) | 135 test trong 11 lớp | Gọi API thật trên DB `skillexchange_test`; thư gửi qua `JavaMailSender` giả; đồng hồ giả `MutableClock` (D18) để kiểm hạn 24 giờ, giới hạn 60 giây |
| Unit test thuần Java | 3 test (`UserTokenTest`) | Kiểm `UserToken.isUsableAt` không cần DB (thêm ở bước 11, xem phần làm hỏng code) |
| Kiểm khởi động | 1 test (`BackendApplicationTests`) | Ứng dụng khởi động được, Flyway chạy hết migration |
| Kiểm thủ công | TC04, TC31, TC74 | Trên trình duyệt (S01–S04) |

Code viết theo TDD: test của mỗi bước được viết trước và chạy đỏ đúng lý do (404, sai mã, thiếu dữ liệu) rồi mới code.

## Truy vết ca → test

| Ca | Trạng thái | Ghi chú |
| --- | --- | --- |
| 88 ca tự động (TC01–TC93 trừ 5 ca dưới) | **Có test** | Mỗi ca ít nhất một test có tên `TCxx` trong `@DisplayName`; không có test nào ghi mã ca không tồn tại |
| TC04, TC31, TC74 | Thủ công | Kết quả ở phần "Kiểm thủ công" |
| TC75, TC76 | Tuần 4 | Cần API `GET /api/users/{id}` |

Ngoài TC còn 40 test ghi mã quyết định (`Dxx`) cho những phần quyết định mà TC chưa phủ (ví dụ D24 dạng lỗi, D14 cấu trúc JWT, D4 tạo admin).

## Kết quả

**139/139 test đạt** (lần chạy 2026-11-01, PostgreSQL 17, Spring Boot 4.1.1). Frontend: `npm run build` và `npm run lint` không lỗi.

Kiểm thử lược đồ tuần 2 chạy lại trên DB tạm sau khi có `V2__seed_skills.sql`: vẫn **73/73**.

## Làm hỏng code, test phải đỏ

Sửa tạm một dòng code, chạy lớp test liên quan, rồi `git restore`. Test vẫn xanh nghĩa là test chưa đủ.

| # | Quy tắc | Làm hỏng | Test đỏ |
| --- | --- | --- | --- |
| M1 | Chỉ nhận email trường (D9) | `isSchoolEmail` luôn trả đúng | TC32 (Gmail), TC33 (tên miền con) |
| M2 | Mật khẩu phải băm (NFR-01) | Lưu mật khẩu gốc | TC60, TC85, TC77, D8 |
| M3 | Tài khoản khóa bị chặn ngay (D13) | Bỏ kiểm `LOCKED` trong `AccountStatusFilter` | TC69 |
| M4 | Link xác thực chỉ dùng một lần (D26) | Bỏ điều kiện `usedAt == null` | **Lần đầu không test nào đỏ** → thêm `UserTokenTest`, nay `d26_usedTokenIsNotUsable` đỏ |
| M5 | Vé hết hạn bị từ chối (D14) | Bỏ bộ kiểm hạn của `JwtDecoder` | TC64 |
| M6 | Không sửa được dữ liệu người khác (spec 6.3) | `/api/me/*` luôn dùng user id 1 | TC67 |

**Vì sao M4 lúc đầu không bị bắt:** sau lần bấm link đầu tiên tài khoản đã `ACTIVE`, và `verifyEmail` còn chặn token của tài khoản không phải `PENDING_VERIFICATION`. Gọi qua API, link cũ vẫn bị từ chối nhờ lớp chặn thứ hai, nên điều kiện `usedAt` không thể kiểm qua API. Điều kiện này sẽ quan trọng với token đặt lại mật khẩu (D2, tài khoản vẫn `ACTIVE`), nên đã thêm unit test riêng.

## Đối chiếu code với quyết định (D1–D30, D39–D42)

Một lượt review độc lập (phiên AI riêng, chỉ đọc) đối chiếu từng quyết định với code:

- 31 quyết định làm đúng; D12 (phần quyền *Active*), D17 (hồ sơ công khai) chưa có API dùng tới, làm ở tuần 4.
- **1 chỗ lệch, đã sửa tài liệu:** D39 ghi profile test "chỉ đổi tên database", thực tế còn đặt đuôi email, `frontend-url` và tài khoản admin để test không phụ thuộc `.env`.

## Kiểm thủ công

Đã kiểm qua proxy Vite bằng `curl`: đăng ký 201, Gmail 400 có `errors[].field = email`, `GET /api/me` 200, thư trong Mailpit có link `http://localhost:5173/verify-email?token=...`, backend tắt thì proxy trả 502 và giao diện báo "Không kết nối được máy chủ".

| Ca | Kết quả |
| --- | --- |
| TC04 – thư trong Mailpit, bấm link mở S02 báo thành công | [CẦN BỔ SUNG – tự bấm trên trình duyệt] |
| TC31 – trọn luồng S01 → S04, F5 vẫn đăng nhập và còn dữ liệu | Đạt (kiểm thủ công trên trình duyệt, 01/11/2026) |
| TC74 – đăng xuất rồi gõ `/profile` → về trang đăng nhập, `localStorage` hết vé | [CẦN BỔ SUNG] |

## Giới hạn

- Frontend chưa có test tự động; S01–S04 chỉ kiểm thủ công.
- Chưa đo độ phủ mã (coverage).
- Quyền *Active* (TC75) và hồ sơ công khai không lộ thông tin riêng tư (TC76) kiểm ở tuần 4.
