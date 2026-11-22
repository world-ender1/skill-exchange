# Kiểm thử tuần 6 – Giao diện mới, yêu cầu trao đổi, phiên, báo cáo sự cố

Mục đích: chứng minh code tuần 6 làm đúng 61 ca kiểm thử viết trước từ tài liệu (`ca_kiem_thu.md`), bộ test **bắt được lỗi thật** ở luật trạng thái, thứ tự kiểm, khóa lạc quan và quyền xem dữ liệu riêng tư, và giao diện mới (Tailwind CSS + shadcn/ui) đạt các yêu cầu của D78–D85, D95 khi chạy thật trên trình duyệt.

## Các file

| File | Nội dung |
| --- | --- |
| `ca_kiem_thu.md` | 61 ca viết từ tài liệu trước khi code (TC301–TC361) |
| `backend/src/test/java/com/skillexchange/exchange/*Test.java` | Test tự động: mỗi test có `@DisplayName("TCxxx ...")`, `"Dxx ..."` hoặc `"TK 3.3 ..."` |
| `ket_qua.txt` | Trích kết quả lần chạy gần nhất (`Tests run`, `FAIL`, `ERROR`) |
| `anh/` | 13 ảnh chụp các ca thủ công (tên file bắt đầu bằng mã ca) |

## Cách chạy lại

```powershell
# C:\DACN, Docker đang chạy; DB test tạo một lần (D39):
docker compose up -d
cd backend
.\mvnw.cmd test | Select-String "Tests run|FAIL|ERROR" | Out-File -Encoding utf8 ..\docs\tuan_6\kiem_thu\ket_qua.txt

# Chỉ test của tuần 6 (khoảng 1,5 phút):
.\mvnw.cmd test "-Dtest=com.skillexchange.exchange.*Test"
```

Frontend: `cd frontend; npm run build; npm run lint`.

Trong `ket_qua.txt` có 4 dòng `Error starting ApplicationContext`: đó là các test TC81 của tuần 3 **cố ý** khởi động ứng dụng thiếu hoặc sai `ADMIN_EMAIL`/`ADMIN_PASSWORD` để kiểm ứng dụng dừng lại. Không phải lỗi. Log đầy đủ còn có các dòng `WARN ... violates unique constraint` / `check constraint`: đó là các test cố ý ghi dữ liệu sai để chứng minh DB tự chặn (ví dụ hai báo cáo gửi đúng một lúc đụng chỉ mục `uq_reports_one_open`).

## Thiết kế kiểm thử

| Nhóm ca | Số ca | Loại | Lớp test |
| --- | --- | --- | --- |
| A. Giao diện chung | 10 | Thủ công | Chrome headless, ảnh trong `anh/` |
| B. Tạo yêu cầu | 15 | Tích hợp | `CreateRequestApiTest` |
| C. Hộp yêu cầu và phản hồi | 13 | Tích hợp | `RequestInboxApiTest` (TC326–TC337); TC338 ở `SessionLifecycleApiTest` |
| D. Phiên | 12 | Tích hợp | `SessionLifecycleApiTest`; phần `reports` của TC348 ở `IncidentReportApiTest` |
| E. Báo cáo sự cố | 5 | Tích hợp | `IncidentReportApiTest`; TC355 ở `SessionLifecycleApiTest` |
| F. Giao diện S07–S09 | 6 | Thủ công | Chrome headless, ảnh trong `anh/` |

| Loại test | Số lượng (thêm ở tuần 6) | Cách làm |
| --- | --- | --- |
| Test tích hợp (`@SpringBootTest` + MockMvc) | 63 test trong 4 lớp | Gọi 12 API của TK 4.6–4.8 trên DB `skillexchange_test` (D39), giả giờ bằng `MutableClock` để kiểm luật 15 phút và luật "quá giờ đề xuất". Dữ liệu dựng qua API như service tạo; chỉ dựng bằng SQL những gì API chưa có (khóa tài khoản, đánh giá, báo cáo đã xử lý của tuần 7) hoặc không thể có qua API (TC327) |
| Test ánh xạ Entity | 5 test trong `ExchangeMappingTest` | Lưu và đọc lại ba Entity; CHECK `TWO_WAY` và chỉ mục một phần `uq_requests_one_pending` của DB vẫn chặn (Entity không tự kiểm, D89) |
| Kiểm bằng Node (không trình duyệt) | 33 ca trên 3 file TypeScript thuần | `requestTime.ts` (bước 9), `formatDateTime` trong `format.ts` (bước 10), `sessionView.ts` (bước 11), chạy thẳng file `.ts` bằng Node 24 với `TZ=Asia/Ho_Chi_Minh` |
| Kiểm thủ công | 16 ca, 149 lượt kiểm | TC301–TC310, TC356–TC361: Chrome headless điều khiển qua DevTools Protocol (xem "Kiểm thủ công") |

Bước 6–8 viết theo TDD hai lượt: viết test, chạy thấy đỏ đúng lý do, người học đọc từng ô của bảng trạng thái × hành động, rồi mới viết code cho xanh, không sửa test.

Ngoài TC còn 18 test ghi mã quyết định tuần 6 cho phần TC chưa phủ: D87 (4: không vé 401, admin và tài khoản chưa xác thực 403 ở filter), D88 (3: biên 1000 ký tự sau khi cắt, toàn khoảng trắng lưu `NULL`, không có body), D89 (1: thứ tự các bước kiểm), D90 (2: thứ tự ba lỗi 409 của `accept`; khóa lạc quan tranh chấp chắc chắn), D92 (1: `openReport`, `resolution` mới nhất), D93 (2: khóa lạc quan tranh chấp chắc chắn; thứ tự hai lỗi 409 của `complete`), D94 (2: báo cáo chạy chồng với xác nhận hoàn thành; hai báo cáo chạy chồng), D30 (3, ở `ExchangeMappingTest`). 2 test ghi "TK 3.3" (ràng buộc của DB).

Không test cũ nào bị sửa: `git diff --diff-filter=M baa1831 HEAD -- backend/src/test` rỗng (`baa1831` là commit cuối tuần 5).

## Truy vết ca → test

Kiểm bằng script: đọc 61 ca của `ca_kiem_thu.md` (cột "Loại") và mọi `@DisplayName` trong `backend/src/test/java/com/skillexchange/exchange/`, đếm `@Test` và `@DisplayName` của từng lớp cho khớp.

| Ca | Trạng thái | Ghi chú |
| --- | --- | --- |
| 45 ca tích hợp: TC311–TC355 | **Có test** | Mỗi ca có ít nhất một test mang mã `TCxxx` trong `@DisplayName` (48 test; TC345 tách ba test `tc345a_`, `tc345b_`, `tc345c_`; TC348 chia hai lớp); không test nào ghi mã ca không tồn tại |
| 16 ca thủ công: TC301–TC310, TC356–TC361 | Thủ công | Không ca thủ công nào có test tự động; kết quả ở phần "Kiểm thủ công" |

## Kết quả

**497/497 test đạt** (lần chạy 2026-11-22, PostgreSQL 17.11, Spring Boot 4.1.1), gồm toàn bộ 429 test của tuần 3–5. Tuần 6 thêm 68 test: 63 tích hợp, 5 ánh xạ Entity.

Frontend: `npm run build` và `npm run lint` không lỗi. Build có một cảnh báo: gói JavaScript chính 540 kB (gzip 166 kB), lớn hơn ngưỡng 500 kB Vite đặt sẵn (xem "Giới hạn").

Các điều kiện của cả tuần (kế hoạch tuần 6, mục 4), kiểm 2026-11-22:

| Điều kiện | Kết quả |
| --- | --- |
| Lõi `matching/` không đổi so với cuối tuần 5 | `git diff baa1831 HEAD -- backend/src/main/java/com/skillexchange/matching backend/src/test/java/com/skillexchange/matching` rỗng; `evaluation/` cũng không đổi |
| Không thêm migration (Q2) | Vẫn chỉ có `V1__init.sql`, `V2__seed_skills.sql` |
| Test đã duyệt không bị sửa | Như trên: không file test nào có từ cuối tuần 5 bị sửa |
| Truy vết ca → test | Đủ 45/45 ca tích hợp |
| Làm hỏng code thì test đỏ | 25/25 lần ở backend, 12/13 lần ở frontend (lần còn lại là đột biến tương đương, đã ghi ở kế hoạch bước 9) |
| Đối chiếu code với D78 trở đi | Ba lượt review độc lập; lệch đã sửa ở `2b71563` (code) và `e341fac` (tài liệu), xem bên dưới |

## Làm hỏng code, test phải đỏ

Chạy lại 2026-11-22 bằng script trên một **git worktree riêng** (bản sao của `427d313`), để không đụng thư mục làm việc trong lúc các lượt review đang đọc code. Mỗi lượt: kiểm chuỗi gốc xuất hiện đúng một lần, sửa, xóa báo cáo surefire của lớp test, chạy lớp test liên quan bằng `mvnw.cmd` gọi theo đường dẫn tuyệt đối, đọc test đỏ, trả file về nguyên văn. Lượt đầu là đối chứng không sửa gì: có báo cáo, 0 test đỏ (bài học `docs/harness-log.md`, 2026-11-14). Sau khi chạy, worktree vẫn sạch. Các dòng là phần "Tự kiểm" của kế hoạch bước 4–11.

| Bước | Làm hỏng | Test đỏ |
| --- | --- | --- |
| – | Không sửa gì (đối chứng) | 0/5 |
| 4 | `durationMinutes` của `ExchangeRequest` kiểu `Integer` thay `Short` | 5/5 lớp `ExchangeMappingTest` (ứng dụng không khởi động: `ddl-auto: validate` thấy sai kiểu cột) |
| 4 | Bỏ `@Enumerated(EnumType.STRING)` ở `status` | 5/5 (như trên) |
| 4 | Gán `version = 0` | D30 lưu yêu cầu (`save()` không trả lại chính object) |
| 5 | Bỏ kiểm danh sách học của người gửi | TC320 (201 thay vì 400), D89 |
| 5 | `proposedStartAt` bằng hiện tại được nhận | TC317 |
| 5 | Bỏ `/api/requests/**` khỏi luật `ACTIVE_STUDENT` | TC322 (400 thay vì 403) |
| 5 | Người gửi không thấy địa điểm/link của mình | TC311 |
| 5 | Kiểm $Sim$ (7) trước danh sách học (6) | TC320, D89 |
| 6 | Bỏ khóa lạc quan của yêu cầu | D90 tranh chấp chắc chắn (200 thay vì 409), TC334 ngay lần 1 (`[200, 200]`) |
| 6 | Ném lỗi ngay sau khi lưu phiên trong `accept` | TC326 (500) và 5 test khác có `accept` |
| 6 | Nhận `accept` đúng lúc `proposed_start_at` | TC330, D90 thứ tự |
| 6 | `accept` kiểm `PENDING` trước vai trò | TC329 (409 thay vì 403) |
| 7 | Mốc 15 phút thành 0 | TC341 |
| 7 | `start` dùng `Instant.now()` thay `Clock` | TC341, TC342, TC346 |
| 7 | Bỏ khóa lạc quan của phiên | D93 tranh chấp chắc chắn, TC349 ngay lần 1 (`[200, 200] WAITING_CONFIRMATION`) |
| 7 | `start` xét mốc 15 phút trước trạng thái | TC347 |
| 7 | Hủy phiên thì xóa `started_at` | TC345b (Reputation 0.5 thay vì 0.35), TC346 |
| 7 | Bỏ `/api/sessions/**` khỏi luật `ACTIVE_STUDENT` | TC338, D87 |
| 8 | Bỏ kiểm báo cáo `OPEN` trong `complete` | TC354 |
| 8 | Bỏ ép tăng `version` khi gửi báo cáo | D94 báo cáo chạy chồng với xác nhận (201 thay vì 409); chỉ test tranh chấp chắc chắn bắt được lỗi này |
| 8 | `reports` kiểm trạng thái trước người tham gia | TC348 (409 thay vì 403) |
| 8 | Bỏ kiểm "đã có báo cáo OPEN" | TC353 (vẫn 409 nhưng là câu chung của lưới D24) |
| 8 | `complete` xét báo cáo `OPEN` trước người bấm trước | D93 thứ tự |
| 8 | `reports` dùng `Instant.now()` thay `Clock` | TC351 |
| 8 | `reports` nhận cả phiên `IN_PROGRESS` | TC352 |

Frontend (bước 9–11), làm hỏng trên bản chép của `requestTime.ts`, `format.ts`, `sessionView.ts` trong thư mục tạm, chạy 33 ca kiểm bằng Node. Lượt đối chứng: 0/33 đỏ.

| Bước | Làm hỏng | Ca đỏ |
| --- | --- | --- |
| 9 | Coi "bằng 19:00" là còn ở tương lai | đúng 19:00 → tuần sau |
| 9 | Ghép ngày giờ như giờ UTC (thêm `Z`) | ghép ô ngày giờ ra ISO giờ UTC |
| 9 | `localDateString` dùng `toISOString()` | 00:30 ngày 1/12 vẫn là 1/12 |
| 9 | Bỏ `% 7` khi đổi `dayOfWeek` | **Không ca nào đỏ**: đột biến tương đương, `daysAhead` đã lấy `% 7` (đã ghi ở kế hoạch bước 9) |
| 10 | `formatDateTime` quên dời thứ | 5 ca (3 của `formatDateTime`, 2 chữ "Mở lúc" của bước 11) |
| 10 | `formatDateTime` lấy giờ UTC | 5 ca (như trên) |
| 11 | Hiện "Báo cáo sự cố" cả khi đã có báo cáo mở | 2 ca có báo cáo mở |
| 11 | Hiện "Xác nhận hoàn thành" cho người đã bấm | ca người đã bấm |
| 11 | Coi đúng mốc 15 phút là chưa tới | ca đúng mốc |
| 11 | Cho hủy phiên `WAITING_CONFIRMATION` | 4 ca chờ xác nhận |
| 11 | Tính mốc bằng giây thay vì phút | 4 ca của mốc 15 phút |
| 11 | Đảo kỹ năng học/dạy của người nhận | 2 ca người nhận |
| 11 | Phiên đã hủy vẫn ở bước cuối của thanh | ca `CANCELLED` |

## Phần "Tự kiểm" của từng bước

| Bước | Tự kiểm của kế hoạch | Kết quả (2026-11-22) |
| --- | --- | --- |
| 1 | Không gọi `fonts.googleapis.com`; chữ có dấu đủ ở hai font; giả chế độ tối thì cả trang đổi màu; dưới 768 px menu là `Sheet` | Đạt: TC303, TC305, TC304 |
| 2 | Đi trọn S01 → S04 như TC31; lưới lịch 360 px không cuộn ngang, tick được bằng bàn phím | Đạt: TC309 (TC31), TC306 |
| 3 | Chạy hết ca thủ công D78–D85; không mất chữ, nút hay thông báo so với ảnh chụp trước khi chuyển; không còn lớp CSS cũ | Ca thủ công: đạt sau khi sửa ở `2b71563` (xem "Đối chiếu"). Lớp cũ (`match-card`, `admin-row`, `skill-rows`): `grep` trong `frontend/src` không ra dòng nào. **Chưa so** với ảnh chụp trước khi chuyển (ảnh ở mục 1 của kế hoạch do người học giữ); review tìm ra một phần mất mát (tiêu đề `h1`, `h2`), đã trả lại |
| 4–8 | Làm hỏng code | Xem bảng trên |
| 7 | Reputation đổi khác 0.5 sau phiên hoàn thành, phiên hủy | Đạt qua TC345a–c (0.5; 0.35; 0.65) |
| 9 | Gửi yêu cầu hai chiều từ S05 ở 360 px; gửi trùng nhận 409 | Đạt một phần: S07 ở 360 px không cuộn ngang, gửi trùng hiện câu 409 (TC358); lượt gửi chạy ở 1280 px |
| 10 | Hai trình duyệt: một bên hủy, bên kia chấp nhận thì nhận 409 | Đạt: TC359 |
| 11 | Đi trọn luồng hai tài khoản tới báo cáo; bấm xác nhận thì nhận 409 | Đạt: TC360 (sau báo cáo, nút xác nhận biến mất; gọi `complete` bằng tay nhận 409) |

## Đối chiếu code với quyết định (D78–D96)

Ba lượt review độc lập (phiên AI riêng, chỉ đọc, không được đưa kết luận của người chạy) đối chiếu code với từng câu của quyết định và phần "Chốt ở bước này" của kế hoạch, chia theo: (1) nền giao diện D78–D85, bước 1–3; (2) backend `exchange` D86–D96, bảng 2.1–2.3, bước 4–8; (3) màn S07–S09, D95 và phần giao diện của D91–D94, bước 9–11. Mọi chỗ lệch được mở code, đo hoặc chạy thử lại trước khi xử lý; cách xử lý do người học chọn (2026-11-22).

| Kết quả | Quyết định | Xử lý |
| --- | --- | --- |
| Khớp | D78, D80–D84, D86–D96, bảng 2.1–2.3 (backend: 41 câu `detail` so nguyên văn bằng script, số khóa JSON của `RequestItem` 17, `SessionItem` 12, `SessionDetail` 22); phần còn lại của D79, D85 | – |
| Code lệch | D79: chữ màu trên nền nhạt cùng màu của `Badge` (bước 10) và nút `destructive` dưới 4.5:1. Review tính từ mã hex; đo thật trên Chrome: sáng 4.37 (đang chờ), 4.29 (thành công), 3.97 (lỗi), nút "Từ chối" 3.82; tối nút 4.24 | Đổi màu trạng thái đậm thêm một bậc (sáng `red-700`, `green-800`, `amber-800`; tối `red-300`); đo lại đạt hết – `2b71563`; sửa D79, kế hoạch bước 1 |
| Code lệch | Kế hoạch bước 1 "lỗi tải: `Alert` lỗi và nút Thử lại": S05, S06 và danh sách kỹ năng của S10 chỉ có hộp lỗi (TC307 trượt khi đo) | Thêm `LoadError` – `2b71563` |
| Code lệch | NFR-08 (TC308): S10 với tên 100 ký tự không dấu cách làm trang 360 px rộng 1146 px. Review không thấy; lượt kiểm thủ công tìm ra. Nguyên nhân: ô lưới mặc định `min-width: auto` không co nhỏ hơn từ dài nhất, `break-words` không có tác dụng | Thêm `min-w-0` cho ô – `2b71563` |
| Code lệch | D78 "chỉ đổi phần trình bày", D80 "tiêu đề `h1`–`h3`": thẻ chuyển sang `CardTitle` (thẻ `div`) làm mất các `h2` ở S02–S04, S06, A01 và `h1` "Skill Exchange" ở S01, S02 | Đặt `h2` trong `CardTitle` như A01 đã làm; `h1` trong `AuthShell` – `2b71563` |
| Code lệch nhẹ | Kế hoạch bước 11: phiên `COMPLETED` vẫn gắn `aria-current` ở bước 4 dù cả 4 bước "đã qua" | Bỏ `aria-current` khi `COMPLETED` – `2b71563` |
| Tài liệu chưa theo kịp code | D88, bảng 2.1: `cancel`, `reports` không body; hai lỗi trùng do chạy song song (lưới D24). D18 và CLAUDE.md: `Clock` cho mốc thời gian tuần 6; liên hệ cũng trả trong response của `start`, `complete`, `cancel`. Kế hoạch: tên `RequestItem`, D ở tiêu đề bảng, `PageSkeleton`, hai ngoại lệ sau của `components/ui`, câu mô tả ẩn của `Sidebar`, kiểu `hint` của `Field`, nút "Hủy yêu cầu" (cả TC359). TC304: `Layout` không gọi lại `/api/me`, S03 và S06 tự gọi. Ví dụ `detail` ở TK 4.1 | Sửa tài liệu – `e341fac` |
| Điểm nhỏ, không trái tài liệu | 14 điểm ở frontend, 4 ở backend, 4 chỗ tài liệu chưa nói tới | Người học chọn ghi lại, xem ở tuần 7–8: mục "Giới hạn" |

### Truy vết quyết định → ca, test

| Quyết định | Kiểm bằng |
| --- | --- |
| D78 thư viện giao diện | TC301, TC308, TC309 (thủ công) |
| D79 màu | TC302 (thủ công; 26 lượt đo, hai chế độ) |
| D80 font | TC303 (thủ công) |
| D81 khung trang | TC304, TC361 (thủ công) |
| D82 chế độ tối | TC305 (thủ công) |
| D83 khởi tạo shadcn/ui | TC301 |
| D84 lưới lịch rảnh | TC306 (thủ công) |
| D85 thông báo | TC307 (thủ công) |
| D86 phạm vi | TC338; 403 cho admin ở `/api/requests/**`, `/api/sessions/**` |
| D87 quyền, thứ tự kiểm | TC322, TC328, TC329, TC335, TC338, TC340, TC348, TC350; 4 test D87 |
| D88 thời gian, chuỗi | TC311, TC312, TC315, TC317, TC326, TC332, TC344, TC346, TC351; 3 test D88 |
| D89 tạo yêu cầu | TC311–TC325; 1 test D89 (thứ tự bước kiểm) |
| D90 phản hồi yêu cầu | TC326–TC334; 2 test D90 (thứ tự 409; khóa lạc quan chắc chắn tranh chấp) |
| D91 danh sách, chi tiết yêu cầu | TC336, TC337 |
| D92 danh sách, chi tiết phiên | TC339, TC340, TC355; 1 test D92 |
| D93 chuyển trạng thái phiên | TC341–TC350, TC354; 2 test D93 |
| D94 báo cáo sự cố | TC351–TC354, TC348 (phần `reports`); 2 test D94 |
| D95 giao diện S07–S09 | TC356–TC361 (thủ công); 33 ca kiểm bằng Node |
| D96 hạn chế | TC325 (không chặn chồng giờ, không kiểm hình thức) |

## Kiểm thủ công

TC301–TC310 và TC356–TC361 chạy bằng Chrome 157 headless điều khiển qua DevTools Protocol (script viết riêng, **149 lượt kiểm, đạt 149**), trên một DB tạm `skillexchange_w6m` tạo bằng `createdb` và xóa sau khi xong, để tài khoản và kỹ năng thử không vào DB dev. Mỗi người dùng một browser context riêng (vé trong `localStorage` không lẫn nhau). Bấm chuột và gõ phím đi qua sự kiện thật của Chrome (`Input.dispatchMouseEvent`, `Input.insertText`, `Input.dispatchKeyEvent`), vì `Tabs` của Radix chỉ đổi tab khi có `mousedown` thật. Tài khoản tạo qua API đăng ký, mã xác thực lấy từ thư trong Mailpit. Chưa bấm thử bằng tay trên trình duyệt thật.

Lượt đầu (trước khi sửa) tìm ra 4 lỗi thật: TC302 (tương phản), TC307 (thiếu "Thử lại" ở S05), TC308 (S10 tên dài); các chỗ trượt khác là lỗi của script, đã sửa script. Bảng dưới là lượt chạy trên code đã sửa (`2b71563`).

| Ca | Kết quả |
| --- | --- |
| TC301 – cấu hình | Đạt. `package.json` có `tailwindcss` ^4.3.5, `@tailwindcss/vite` ^4.3.5, `lucide-react`; không có `react-hook-form`, `zod` (`zod` chỉ có trong lockfile do gói CLI `shadcn` kéo theo). `components.json`: `radix-nova`, `neutral`, `lucide`. `--background` là `slate-50`. 26 file trong `src/components/ui/`. Build, lint sạch |
| TC302 – tương phản, mã màu | Đạt sau khi sửa (bảng số đo bên dưới). Không có mã hex hay lớp màu bảng Tailwind trong 25 file của `src/pages`, `src/components` (trừ `ui/`). Nút chính đúng mã: sáng `#4F46E5` chữ `#FFFFFF`, tối `#818CF8` chữ `#0F172A` |
| TC303 – font | Đạt. Font thật Chrome dùng (`CSS.getPlatformFontsForNode`): tiêu đề "Nguyễn Thị Hường" và "Hồ sơ của tôi" chỉ dùng Be Vietnam Pro; đoạn giới thiệu 29 chữ có dấu chỉ dùng Inter; ô nhập dùng Inter. Không có yêu cầu nào tới `fonts.googleapis.com`, `fonts.gstatic.com` trong toàn bộ lượt chạy |
| TC304 – khung trang | Đạt. 1280 px: thanh bên, không có nút "Mở menu". Sinh viên có 6 mục và Đăng xuất, icon đúng D81 (`lucide-user`, `book-open`, `sparkles`, `search`, `inbox`, `calendar-check`, `log-out`); admin chỉ "Danh mục kỹ năng" (`tags`). Chuyển qua 5 trang, `Layout` không gọi lại `/api/me`. 360 px: nút "Mở menu" mở `Sheet` đủ 7 mục, chọn "Gợi ý" thì tới `/matches` và `Sheet` đóng. "Đăng xuất" về `/login`, vé bị xóa. Ảnh `tc304_360_menu_mo.png` |
| TC305 – chế độ tối | Đạt trên 11 màn (8 màn của ca và S07–S09): giả `prefers-color-scheme: dark` thì độ sáng nền < 0.05, tải lại vẫn tối; trả về sáng thì > 0.8. Không có nút chuyển chế độ, `localStorage` không có khóa về màu. Ảnh `tc305_s05_toi.png` |
| TC306 – lưới lịch | Đạt. 1280 px: 3 hàng buổi × 7 cột ngày; 360 px: 7 hàng × 3 cột, bảng kia `display: none`, không cuộn ngang. Ô `role="checkbox"`, `aria-label` "Sáng Thứ Hai"; vùng bấm nhỏ nhất 119 × 44 px (1280) và ≥ 44 × 44 (360). Tick "Sáng Thứ Hai" bằng chuột, "Tối Chủ nhật" bằng Tab + Space; lưu, tải lại còn đúng 2 ô. S06 của người này (tài khoản khác) chỉ đọc. Ảnh `tc306_s03_360.png` |
| TC307 – thông báo | Đạt sau khi sửa. Họ tên rỗng: lỗi dưới ô + hộp `role="alert"` trong thẻ đang sửa; hợp lệ: "Đã lưu hồ sơ." `role="status"` trong thẻ, không có toast. "Tắt backend" giả bằng cách giữ request `/api/matches` 2 giây rồi cho thất bại như mất kết nối: thấy `Skeleton` có `aria-busy`, rồi `Alert` lỗi kèm "Thử lại"; bấm lại thì tải được. S06, S10 cũng vậy. Ảnh `tc307_s05_loi_tai.png` |
| TC308 – 360 px | Đạt sau khi sửa trên 12 màn (8 màn của ca và S07, S08, danh sách, chi tiết S09) với tên 100 ký tự không dấu cách (`Hoàng` × 20) và link 144 ký tự: `scrollWidth` = 360 ở mọi màn. Ảnh `tc308_s10_360.png` |
| TC309 – ca thủ công cũ, `role="tab"` | Đạt. TC31 làm lại trên giao diện mới: đăng ký trên S01 → bấm link trong Mailpit (S02 báo thành công) → đăng nhập → lưu hồ sơ, tick lịch (S03) → thêm kỹ năng bằng combobox (gõ không dấu "tieng anh" ra "Tiếng Anh") và lưu (S04) → F5 vẫn đăng nhập, dữ liệu còn. TC184 nằm trong TC308. TC262, TC263: admin vào `/admin/skills`; admin gõ `/matches`, `/profile` về `/admin/skills`; sinh viên gõ `/admin/skills` về `/profile`, gọi API admin nhận 403. TC264: thêm "Rust", thêm trùng "rust" hiện "Tên kỹ năng đã tồn tại", ẩn rồi hiện (có nhãn "Đã ẩn"), đúng sau khi tải lại, 360 px không cuộn ngang (chưa làm lại đổi tên, nhóm, cặp liên quan). Tab ở S01, S05 có `role="tab"`, `aria-selected` đổi theo tab đang chọn. Tiêu đề `h1`, `h2` như tuần 5. Ảnh `tc309_a01_360.png` |
| TC310 – chi tiết điểm | Đạt. Enter mở "Chi tiết điểm", Space đóng. D và 5 thành phần của cả hai chiều in đúng nguyên văn số của `GET /api/matches` (so chuỗi, không làm tròn); mỗi thanh `Progress` có `translateX` đúng −(100 − giá trị × 100)%. Ảnh `tc310_chi_tiet_diem.png` |
| TC356 – mở S07 | Đạt. Từ thẻ Bình ở tab hai chiều: `/users/{Bình}/request?type=TWO_WAY`, chọn sẵn `TWO_WAY`; từ S06: `ONE_WAY`; S06 của chính An không có nút. Gửi xong, thẻ Bình ở S05 không còn nút, có "Đã gửi yêu cầu". Ảnh `tc356_s07_hai_chieu.png` |
| TC357 – khung trùng | Đạt. Chỉ có chip "Tối T3". Đồng hồ trang giả ở thứ Ba 24/11 20:00: bấm chip điền 01/12, 19:00; giả ở thứ Hai: điền 24/11, 19:00. Ô "Bạn muốn học" = Java, Python (danh sách học của An); "Bạn sẽ dạy" = Figma, Tiếng Anh (của Bình); `ONE_WAY` thì ô này ẩn. Dưới ô địa điểm/link có "Người nhận chỉ thấy sau khi chấp nhận" |
| TC358 – gửi yêu cầu | Đạt. Địa điểm trống: "Chưa nhập địa điểm hoặc link" dưới ô; Python: "Người nhận không dạy kỹ năng này hay kỹ năng liên quan" dưới ô "Bạn muốn học"; hợp lệ: tới `/requests?box=sent`, thấy "Đang chờ"; gửi lại: hộp lỗi "Bạn đã có một yêu cầu đang chờ gửi tới người này". 360 px không cuộn ngang. Ảnh `tc358_loi_dia_diem.png` |
| TC359 – hộp yêu cầu | Đạt. Bình mặc định ở tab "Đã nhận"; thẻ `PENDING` phía Bình ghi "Hiện sau khi bạn chấp nhận", phía An có link mở tab mới (`rel="noopener noreferrer"`). Badge "Đang chờ" màu `warning`. "Từ chối" mở hộp thoại "Từ chối yêu cầu của Lê Minh Chi", xong thẻ có "Đã từ chối" và ghi chú. Hai trình duyệt: An bấm "Hủy yêu cầu", Bình bấm "Chấp nhận" trên trang cũ thấy "Yêu cầu không còn ở trạng thái chờ", danh sách tải lại, thẻ "Đã hủy". Chấp nhận yêu cầu khác: "Xem phiên" mở `/sessions/{id}`. Ảnh `tc359_s08_binh_1280.png`, `tc359_409_tai_lai.png` |
| TC360 – phiên | Đạt. Phiên sau 1 ngày: "Bắt đầu" bị khóa, chữ "Mở lúc …" kèm ngày đúng giờ hẹn − 15 phút, nối với nút bằng `aria-describedby`. Hủy phiên gửi rỗng: "Chưa nhập lý do" dưới ô, hộp thoại vẫn mở; hủy xong có hộp "Phiên đã hủy" ("Bạn đã hủy lúc …" phía An, "Nguyễn Văn An đã hủy lúc …" phía Bình). Phiên sau 10 phút: Bắt đầu → bước 2; An "Hoàn thành" → bước 3, An chỉ còn "Báo cáo sự cố"; Bình thấy "Xác nhận hoàn thành", "Báo cáo sự cố" và liên hệ của An. Bình báo cáo (gửi rỗng thì lỗi): không còn nút, có dòng "Bạn đã gửi báo cáo sự cố lúc …"; gọi `complete` bằng tay nhận 409 "Phiên đang có báo cáo sự cố chờ quản trị viên xử lý". 360 px chế độ tối: không cuộn ngang, badge "Chờ xác nhận" 6.85:1. Phiên `COMPLETED`: 4 bước "đã qua", không còn `aria-current`. Ảnh `tc360_s09_cho_mo.png`, `tc360_s09_360_toi.png` |
| TC361 – menu, quyền | Đạt. Sinh viên có "Yêu cầu" (`Inbox`), "Phiên" (`CalendarCheck`), không có số đếm; admin gõ `/requests`, `/sessions/1` về `/admin/skills` |

Số đo tương phản (TC302) trên code đã sửa, màu đọc bằng `getComputedStyle` rồi trộn lớp nền có độ trong suốt bằng `canvas` như trình duyệt vẽ:

| Phần tử | Sáng | Tối |
| --- | --- | --- |
| Badge "Đang chờ" (`warning`) | 6.03 | 6.85 |
| Badge "Đã chấp nhận" (`success`) | 6.07 | 6.61 |
| Badge "Đã từ chối" (`destructive`) | 5.30 | 6.20 |
| Nút "Từ chối" (`destructive`) | 5.09 | 5.59 |
| Nút chính | 6.29 | 5.98 |
| Chữ phụ "Gửi lúc …" | 7.58 | 6.78 |
| Chữ trong `Alert` lỗi | 5.85 | 7.76 |
| Chữ lỗi dưới ô | 6.42 | 9.29 |
| Chữ và icon trong `Alert` thành công | 7.13 | 10.02 |
| Viền ô nhập so với nền thẻ (cần ≥ 3) | 4.76 | 3.74 |

## Giới hạn

- **Frontend chưa có test tự động.** 33 ca kiểm bằng Node và script Chrome headless chạy từ thư mục tạm, không lưu trong repo; kiểu TypeScript vẫn là lưới an toàn chính. Kiểm thủ công chỉ đọc thuộc tính DOM (`role`, `aria-*`, `aria-describedby`), chưa thử bằng trình đọc màn hình thật.
- "Tắt backend" ở TC307 giả bằng cách cho request thất bại ngay tại trình duyệt, không tắt máy chủ thật (khi tắt thật, proxy của Vite trả lỗi khác, trang vẫn đi vào cùng nhánh lỗi tải). TC357 giả đồng hồ bằng cách thay `Date` của trang.
- Chế độ dev chạy React `StrictMode`: mỗi effect chạy hai lần nên mỗi lần mở trang có thể có hai request giống nhau; bản build không có.
- Gói JavaScript 540 kB (gzip 166 kB) vượt ngưỡng cảnh báo 500 kB của Vite; chưa tách code theo trang (xem lại khi deploy, tuần 8).
- Viền ô nhập ở chế độ tối đạt 3.74:1 so với nền thẻ (cách đo của kế hoạch bước 1) nhưng chỉ 2.66:1 so với nền bên trong ô (`dark:bg-input/30` của shadcn/ui).
- Hai ca song song (TC334, TC349) không ép được lần nào cũng tranh chấp; ba test D90, D93, D94 tạo tranh chấp chắc chắn bằng kết nối JDBC giữ khóa.
- Không chặn yêu cầu, phiên chồng giờ (D96, Q6).
- **Điểm nhỏ review tìm ra, người học chọn để lại (xem ở tuần 7–8):**
  - S08: thông báo thành công của tab cũ còn hiện khi đổi `box` bằng menu hay nút Back (`RequestsPage.tsx`, chỉ xóa khi bấm tab); một `busyId` cho cả danh sách, bấm nút ở hai thẻ liên tiếp thì thẻ đầu xong sẽ mở khóa thẻ sau (backend vẫn trả 409); "Bạn học/Bạn dạy" tính lại ở `RequestsPage.tsx:205–206` thay vì dùng `viewerSkills`, nên lượt làm hỏng "đảo kỹ năng người nhận" không phủ S08.
  - S07 đọc `?type` một lần (`SendRequestPage.tsx:95`, `key` chỉ theo người nhận): Back/Forward giữa hai `?type` của cùng người nhận không đổi loại đã chọn.
  - S09: trong lúc tải lại sau báo cáo hoặc sau lỗi, các nút cũ vẫn bấm được (chỉ nhận 409); nút "Bắt đầu" khóa bằng `disabled` nên phím Tab không tới, `aria-describedby` không có tác dụng với người dùng bàn phím (`SessionDetailPage.tsx:249`); link địa điểm mở tab mới mà không báo cho trình đọc màn hình (`InfoLine.tsx:25`).
  - `Field` chỉ hiện gợi ý khi ô không có lỗi (`Field.tsx:57`): khi ô địa điểm/link báo lỗi thì mất câu "Người nhận chỉ thấy sau khi chấp nhận" (D95); danh sách học rỗng mà bấm gửi thì lỗi che mất link tới `/skills`.
  - `PageSkeleton.tsx:9`: `aria-label` trên `div` không có `role` nên trình đọc màn hình bỏ qua. Từ 768 px, Ctrl+B ẩn thanh bên mà không có nút mở lại (`SidebarTrigger` chỉ hiện dưới 768 px). S04 có hai combobox cùng tên "Chọn kỹ năng để thêm" (`SkillsPage.tsx:238`); A01 các nút "Đổi tên nhóm" trùng tên (`AdminSkillsPage.tsx:289`). `Badge` có `whitespace-nowrap`: tên kỹ năng rất dài ở S10 có thể bị cắt. Biến `--chart-*` (`index.css`) còn màu xám do CLI sinh, không dùng. Nút "Close" ở `DialogFooter` (`ui/dialog.tsx:118`) chưa đổi chữ, hiện không trang nào dùng.
  - Backend: `GlobalExceptionHandler.java:196` chỉ bắt `ObjectOptimisticLockingFailureException` (đủ cho mọi đường đã test, nên bắt loại cha cho chắc); `switch` của `cancel` (`ExchangeSessionService.java:130`) không có nhánh `default`, thêm trạng thái mới thì `cancel` trả 200 mà không đổi gì; `findRowsOf` (`ExchangeSessionRepository.java:65`) đọc cả `phone`, `contact_info` cho danh sách phiên dù `SessionItem` không trả ra; chú thích ở `ExchangeSessionRepository.java:48` ghi `SET version = version + 1` trong khi câu Hibernate thật là `set version=? where id=? and version=?`.
  - Tài liệu chưa nói tới: `status=` để rỗng được coi là "mọi trạng thái" thay vì 400; độ dài đếm theo đơn vị UTF-16 như D16 (emoji tính 2); `proposedStartAt` dạng số epoch cũng được Jackson nhận; `durationMinutes` vượt giới hạn `int` ra "Nội dung JSON không đọc được" thay vì lỗi theo ô.
- Hook `protect-results` chặn lệnh `git stash -u -- frontend docs` (định dùng để so cảnh báo kích thước gói với bản trước bước 11), vì so chuỗi trên cả lệnh như đã ghi ở tuần 5; không tìm cách lách.
- Chưa đo độ phủ mã (coverage).
