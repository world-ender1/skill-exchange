# Ca kiểm thử tuần 6 – Giao diện mới, yêu cầu trao đổi, phiên, báo cáo sự cố

Mục đích: liệt kê trước các ca kiểm thử cho chức năng tuần 6 (FR-05, FR-06, quyền xem liên hệ và địa điểm/link, FR-09 phần người dùng; giao diện Tailwind CSS + shadcn/ui), viết **chỉ từ tài liệu**, trước khi có code. Code tuần 6 phải làm các ca này pass. Ca nào thấy sai thì sửa tài liệu trước rồi mới sửa ca, không sửa ca cho khớp code.

Viết ngày 2026-11-18, sau khi chốt D78–D96. Mã ca bắt đầu từ TC301 để không trùng tuần 3 (TC01–TC93), tuần 4 (TC101–TC185) và tuần 5 (TC201–TC264).

## Nguồn

Khi các nguồn khác nhau, nguồn mới hơn đúng: quyết định tuần 6 > quyết định tuần 3–5 > thiết kế tuần 2 > spec tuần 1.

| Ký hiệu | Tài liệu |
| --- | --- |
| Spec 4.3, 6.2–6.4, 7.1 | `docs/tuan_1/Phan_tich_yeu_cau_Skill_Exchange_Tuan_1.md`: Reputation, trạng thái, phân quyền, nội dung yêu cầu, unit test nhóm 5 |
| FR-05, FR-06, FR-09, NFR-02, NFR-08, NFR-09 | Spec tuần 1, mục 3 và 7 |
| TK 4.5–4.8, 5.1, 5.2, 6.3 | `docs/tuan_2/Tuan_2_Thiet_ke.md`: API, sơ đồ trạng thái, wireframe |
| D13, D38, D44, D75, D76 | Quyết định tuần 3–5 |
| D78–D96, bảng 2.1–2.3 | `docs/tuan_6/Quyet_dinh_tuan_6.md` (2.1 câu báo lỗi, 2.2 dạng response, 2.3 bảng trạng thái × hành động) |

**Loại:**
- **Tích hợp**: `@SpringBootTest` trên DB test `skillexchange_test` (D39), kế thừa `IntegrationTestBase`, giả giờ bằng `MutableClock`.
- **Thủ công**: kiểm bằng trình duyệt (DevTools) hoặc dòng lệnh.

**Quy ước:**
- "400 `errors` `x`" nghĩa là 400, `detail` "Dữ liệu không hợp lệ", `errors` có phần tử `field` = `x` với `message` đúng bảng 2.1. Ca chỉ ghi `detail` thì response không có `errors`.
- "Dữ liệu không đổi" nghĩa là dòng trong DB (gồm `status`, các cột thời gian, `version`) và số phiên, số báo cáo giống trước lệnh.
- Số thực so bằng dung sai $10^{-9}$ (D36); giá trị dùng trong test là biểu thức ghi kèm.
- Tạo yêu cầu, chuyển trạng thái để dựng dữ liệu cho một ca thì đi qua API (trừ khi ca ghi "bằng SQL"), để dữ liệu đúng như service tạo.

## Dữ liệu mẫu dùng chung

**Người dùng** (đều `STUDENT`, `ACTIVE` trừ khi ghi khác; chưa khai lịch rảnh, hình thức học trừ khi ca ghi khác):

| Tên | Dạy (trình độ) | Muốn học (mục tiêu) | Liên hệ |
| --- | --- | --- | --- |
| An | Tiếng Anh (3) | Java (3), Python (trống) | `phone` "0901000001", `contactInfo` "Zalo: An" |
| Bình | Kotlin (3) | Tiếng Anh (trống), Figma (trống) | `phone` NULL, `contactInfo` "Facebook: Bình" |
| Chi | Java (2) | Kotlin (trống) | trống |
| Giang | Java (4) | – | trống |
| Dung | Java (2) | – | `PENDING_VERIFICATION` |
| Em | Java (2) | – | `LOCKED` |
| Admin | – | – | `role = ADMIN` |

Theo V2: Java–Kotlin liên quan ($Sim$ = 0.5); Python, Figma, Guitar không liên quan tới kỹ năng nào ở trên. Vì vậy An học Java từ Bình có $Sim$ = 0.5, An học Python từ Bình có $Sim$ = 0; Bình học Tiếng Anh từ An có $Sim$ = 1, Bình học Figma từ An có $Sim$ = 0.

**Mốc giờ:** đồng hồ giả bắt đầu ở T0 = `2026-11-18T08:00:00Z`. S = `2026-11-20T12:00:00Z` (= `2026-11-20T19:00:00+07:00`).

**Yêu cầu mẫu R0** (An gửi Bình, `ONE_WAY`):

```json
{"receiverId": <Bình>, "type": "ONE_WAY", "senderLearnSkillId": <Java>,
 "proposedStartAt": "2026-11-20T19:00:00+07:00", "durationMinutes": 90, "mode": "ONLINE",
 "locationOrLink": "https://meet.google.com/abc-defg-hij", "message": "Học Java nhé"}
```

**R2** = R0 với `type` `TWO_WAY` và `receiverLearnSkillId` = Tiếng Anh. **Phiên P** = phiên sinh ra khi Bình chấp nhận R0 lúc T1 = T0 + 1 giờ; `start_at` = S, An là người gửi, Bình là người nhận.

## Tổng quan

| Nhóm | Số ca | Ý nghĩa |
| --- | --- | --- |
| A. Giao diện chung | 10 | Tailwind, shadcn/ui, màu, font, khung trang, chế độ tối, lưới lịch, thông báo, 360 px (D78–D85) |
| B. Tạo yêu cầu | 15 | Kiểm dữ liệu, thứ tự kiểm, $Sim$, danh sách học, trùng `PENDING` (D87–D89) |
| C. Hộp yêu cầu và phản hồi | 13 | Chấp nhận, từ chối, hủy, transaction, khóa lạc quan, quyền xem địa điểm/link (D87, D90, D91) |
| D. Phiên | 12 | Danh sách, chi tiết, liên hệ, bắt đầu, hoàn thành, hủy, CompletionRate, khóa lạc quan (D92, D93) |
| E. Báo cáo sự cố | 5 | Chỉ khi chờ xác nhận, một báo cáo mở, chặn xác nhận hoàn thành (D94) |
| F. Giao diện S07–S09 | 6 | Gửi yêu cầu, hộp yêu cầu, phiên (D95) |
| **Tổng** | **61** | |

## A. Giao diện chung (thủ công)

| Mã | Nguồn | Đầu vào | Mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC301 | D78, D83 | `npm run build`, `npm run lint`; mở `package.json`, `components.json` | Build và lint không lỗi. Có `tailwindcss`, `@tailwindcss/vite`, `lucide-react`; **không** có `react-hook-form`, `zod`. `components.json` có `"style": "radix-nova"`, `"baseColor": "neutral"` (D83: CLI không có `slate`), `"iconLibrary": "lucide"`; biến `--background` trong `index.css` là `slate-50`; component shadcn/ui nằm trong `src/components/ui/`; import bằng `@/` chạy được | Thủ công |
| TC302 | D79 | Đo tương phản bằng DevTools ở chế độ sáng và tối: nút chính, chữ trong `Alert` và `Badge` loại thành công, đang chờ, lỗi, chữ phụ, viền ô nhập; `grep` mã màu trong `src/pages`, `src/components` (trừ `ui/`) | Chữ ≥ 4.5:1, icon và viền ô nhập ≥ 3:1. Nút chính: sáng `#4F46E5` chữ trắng, tối `#818CF8` chữ `#0F172A`. Không có mã màu hex hay lớp màu bảng Tailwind (`bg-red-600`...) ghi thẳng ngoài `components/ui` và file biến màu | Thủ công |
| TC303 | D80 | S03 với họ tên "Nguyễn Thị Hường", giới thiệu "ấ ầ ẩ ẫ ậ ế ề ể ễ ệ ố ồ ổ ỗ ộ ớ ờ ở ỡ ợ ứ ừ ử ữ ự ỳ ỷ ỹ ỵ"; DevTools Network và mục "Rendered Fonts" | Tiêu đề dùng Be Vietnam Pro; nội dung, ô nhập dùng Inter; không ký tự nào rơi về font khác. Không có yêu cầu tới `fonts.googleapis.com`, `fonts.gstatic.com` | Thủ công |
| TC304 | D81, D76 | Sinh viên rồi admin đăng nhập; xem ở 1280 px và 360 px; bấm từng mục menu; "Đăng xuất" | 1280 px: thanh bên. 360 px: thanh trên cùng, nút mở menu dạng `Sheet`, chọn mục xong `Sheet` đóng. Sinh viên: Hồ sơ, Kỹ năng, Gợi ý, Tìm kiếm với icon đúng D81; admin: chỉ Danh mục kỹ năng. Chuyển trang không gọi lại `/api/me` (Network). Đăng xuất về `/login` | Thủ công |
| TC305 | D82 | DevTools → Rendering → giả `prefers-color-scheme: dark`, rồi `light`; tải lại trang | Cả 8 màn đổi màu theo, không có nút chuyển chế độ; tải lại vẫn theo hệ thống; `localStorage` không có khóa nào về chế độ | Thủ công |
| TC306 | D84, NFR-08 | S03 ở 1280 px và 360 px: tick "Sáng Thứ Hai" bằng chuột, "Tối Chủ nhật" bằng Tab + Space; lưu; tải lại. Tài khoản khác mở S06 của người này | 1280 px: 7 cột ngày × 3 hàng buổi; 360 px: 7 hàng ngày × 3 cột buổi, không cuộn ngang. Mỗi ô có `role="checkbox"` và `aria-label` dạng "Sáng Thứ Hai"; vùng bấm ≥ 44 × 44 px. Lưu xong tải lại còn đúng 2 ô. S06 hiện cùng lưới, không tick được | Thủ công |
| TC307 | D85 | S03: lưu hồ sơ với họ tên rỗng, rồi họ tên hợp lệ; tắt backend rồi mở S05 | Lỗi theo ô hiện ngay dưới ô; hộp lỗi có `role="alert"`, hộp "Đã lưu" có `role="status"`, nằm trong thẻ vừa thao tác, không có toast. Tắt backend: thấy `Skeleton` có `aria-busy` rồi `Alert` lỗi kèm nút "Thử lại" | Thủ công |
| TC308 | D78, NFR-08 | Mở S01–S06, S10, A01 ở 360 px; chạy `document.documentElement.scrollWidth` trên Console; thử tên 100 ký tự không có dấu cách | Mọi màn ≤ 360, không cuộn ngang; chữ dài xuống dòng | Thủ công |
| TC309 | D78 | Chạy lại các ca thủ công cũ: TC31 (tuần 3), TC184 (tuần 4), TC262–TC264 (tuần 5). Xem cây trợ năng (DevTools Accessibility) của tab ở S01, S05 | Các ca cũ vẫn đạt. Tab có `role="tab"` và `aria-selected` đúng tab đang chọn | Thủ công |
| TC310 | D78, NFR-09 | S05: mở "Chi tiết điểm" của một ứng viên, so với JSON `GET /api/matches` trong Network; mở, đóng bằng bàn phím | Mỗi thành phần và D in đúng số của API, không làm tròn; `Progress` đúng tỷ lệ trên thang 0–1; mở, đóng được bằng Enter/Space | Thủ công |

## B. Tạo yêu cầu – `POST /api/requests`

| Mã | Nguồn | Đầu vào | Mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC311 | D89, D91, D88 | Đồng hồ T0. An gửi R0, nhưng `locationOrLink` "  https://meet.google.com/abc-defg-hij  ", `message` "  Học Java nhé  " | 201. Body có đủ 17 khóa của `RequestItem` (bảng 2.2): `status` `PENDING`, `myRole` `SENDER`, `sender` = An, `receiver` = Bình (chỉ `id`, `fullName`), `senderLearnSkill` = {Java}, `receiverLearnSkill` null, `proposedStartAt` "2026-11-20T12:00:00Z", `locationOrLink` đã cắt, `message` "Học Java nhé", `responseNote`, `respondedAt`, `sessionId` null, `createdAt` = T0. Không có khóa `phone`, `contactInfo`, `email`. DB: một dòng `PENDING`, `responded_at` NULL | Tích hợp |
| TC312 | D89, D88 | An gửi R2 với `message` "   " | 201, `type` `TWO_WAY`, `receiverLearnSkill` = {Tiếng Anh}, `message` null | Tích hợp |
| TC313 | D89, bảng 2.1 | R0, mỗi lần thiếu đúng một trường: `receiverId`, `type`, `senderLearnSkillId`, `proposedStartAt`, `durationMinutes`, `mode`, `locationOrLink` | Mỗi lần 400 `errors` đúng trường thiếu; DB không có dòng mới | Tích hợp |
| TC314 | D89 (bước 1) | R0 thiếu cùng lúc `type` và `durationMinutes` | 400, `errors` có cả `type` lẫn `durationMinutes` (lỗi Bean Validation trả cùng lúc) | Tích hợp |
| TC315 | D89, D88 | Biên, mỗi lần một yêu cầu; lần nào 201 thì hủy yêu cầu đó bằng SQL (`CANCELLED`, có `responded_at`; API hủy có ở bước 6) trước lần sau: `durationMinutes` 14, 15, 480, 481; `locationOrLink` "   ", 500 ký tự kèm hai dấu cách ở hai đầu, 501 ký tự; `message` 1000, 1001 ký tự | 14, 481: 400 `errors` `durationMinutes`; "   ", 501: 400 `errors` `locationOrLink`; 1001: 400 `errors` `message`. 15, 480, 500 ký tự (đếm sau khi cắt), 1000: 201 | Tích hợp |
| TC316 | D89 (bước 2) | R0 với `type` `TWO_WAY` mà không có `receiverLearnSkillId`; R0 (`ONE_WAY`) có `receiverLearnSkillId` = Tiếng Anh | Cả hai 400 `errors` `receiverLearnSkillId`, câu khác nhau đúng bảng 2.1 | Tích hợp |
| TC317 | D89 (bước 3), D88 | Đồng hồ T0. R0 với `proposedStartAt` = T0 (viết "2026-11-18T15:00:00+07:00"), T0 − 1 giây, T0 + 1 giây; R0 với `mode` "ANY"; R0 với `proposedStartAt` "2026-11-20T19:00:00" (không có múi giờ) | T0 và T0 − 1 giây: 400 `errors` `proposedStartAt` "Thời điểm phải ở tương lai". T0 + 1 giây: 201. `mode` "ANY" và thiếu múi giờ: 400 `detail` "Nội dung JSON không đọc được" | Tích hợp |
| TC318 | D89 (bước 4) | An gửi R0 với `receiverId` = An | 400 `detail` "Không thể gửi yêu cầu cho chính mình", không có `errors` | Tích hợp |
| TC319 | D89 (bước 5) | R0 với `receiverId` = 99999, Dung, Em, Admin | Cả bốn 404, cùng `detail` "Không tìm thấy người dùng" | Tích hợp |
| TC320 | D89 (bước 6), R36 | An gửi `ONE_WAY` với `senderLearnSkillId` = Kotlin (không có trong danh sách học của An); An gửi R2 nhưng `receiverLearnSkillId` = Guitar (không có trong danh sách học của Bình) | 400 `errors` `senderLearnSkillId`; 400 `errors` `receiverLearnSkillId` (câu "...danh sách muốn học...") | Tích hợp |
| TC321 | D89 (bước 7), spec 4.1 | An gửi `ONE_WAY` Python tới Bình; An gửi R2 nhưng `receiverLearnSkillId` = Figma | 400 `errors` `senderLearnSkillId` "Người nhận không dạy kỹ năng này hay kỹ năng liên quan"; 400 `errors` `receiverLearnSkillId` "Bạn không dạy kỹ năng này hay kỹ năng liên quan". (Java → Kotlin, $Sim$ = 0.5, đã được nhận ở TC311) | Tích hợp |
| TC322 | D87, D89 | (a) R0 với `receiverId` 99999 và thiếu `durationMinutes`; (b) R0 tới Em với `senderLearnSkillId` Python; (c) Dung gửi `{}`; (d) Admin gửi R0 | (a) 400, không phải 404; (b) 404, không phải 400; (c) 403; (d) 403 | Tích hợp |
| TC323 | D89 (bước 8) | An gửi R0; An gửi R0 lần nữa; Bình gửi An `ONE_WAY` Tiếng Anh; hủy yêu cầu đầu bằng SQL (`CANCELLED`, có `responded_at`; API hủy có ở bước 6) rồi An gửi R0 lần ba | 201; 409 "Bạn đã có một yêu cầu đang chờ gửi tới người này"; 201 (chiều ngược được phép); 201 | Tích hợp |
| TC324 | D89 (D75) | Admin ẩn Java (`PUT /api/admin/skills/{Java}`, `isActive` false); An gửi R0 (Java vẫn trong danh sách học của An). Sau ca trả danh mục như V2 | 201, `senderLearnSkill.name` "Java" | Tích hợp |
| TC325 | D89 (Q5, Q6), TK 4.5 | Bình khai hình thức `ONLINE`. An gửi R0 với `mode` `OFFLINE`; An gửi Giang `ONE_WAY` Java cùng `proposedStartAt`; An gọi `GET /api/matches?type=TWO_WAY` | 201; 201 (không kiểm hình thức, không kiểm chồng giờ). Thẻ Bình có `hasPendingRequest` true | Tích hợp |

## C. Hộp yêu cầu và phản hồi

| Mã | Nguồn | Đầu vào | Mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC326 | D90, D88 | R0 tạo lúc T0; đồng hồ T1. Bình `accept` | 200 `{requestId, sessionId}`. Yêu cầu `ACCEPTED`, `responded_at` = T1. Đúng một phiên của yêu cầu: `SCHEDULED`, `start_at` = S, `duration_minutes` 90, `mode` `ONLINE`, `location_or_link` bằng yêu cầu, `created_at` = T1; `started_at`, `first_completed_by`, `completed_at`, `cancelled_by`, `cancel_reason`, `cancelled_at` NULL | Tích hợp |
| TC327 | D90 (một transaction) | Chèn trước bằng SQL một phiên có `request_id` = R0 (để lệnh tạo phiên vi phạm `UNIQUE`); Bình `accept` | 409 (lưới D24), không 500. Yêu cầu vẫn `PENDING`, `responded_at` NULL, `version` không đổi: đổi trạng thái yêu cầu bị hoàn tác cùng lệnh tạo phiên | Tích hợp |
| TC328 | D87, bảng 2.3 | R0 `PENDING`: An `accept`, `reject`; Chi `accept`, `reject`, `cancel`, `GET /api/requests/{id}`; Bình `cancel` | An: 403 "Chỉ người nhận được chấp nhận hoặc từ chối yêu cầu" (2 lần). Chi: 403 "Bạn không tham gia yêu cầu này" (4 lần). Bình: 403 "Chỉ người gửi được hủy yêu cầu". Dữ liệu không đổi | Tích hợp |
| TC329 | D87, D90, spec 6.2 | Ba yêu cầu An → Bình ở `ACCEPTED`, `REJECTED`, `CANCELLED`. Trên mỗi yêu cầu: Bình `accept`, `reject`; An `cancel`; An `accept`; Bình `cancel` | Bình `accept`, `reject` và An `cancel`: 409 "Yêu cầu không còn ở trạng thái chờ". An `accept`, Bình `cancel`: 403 (sai vai trò xét trước trạng thái). Dữ liệu không đổi | Tích hợp |
| TC330 | D90 (R34) | (a) Đồng hồ S − 1 giây: Bình `accept` R0. (b) Dữ liệu mới, đồng hồ S: Bình `accept` R0; đồng hồ S + 1 ngày: An `GET` rồi An `cancel`. (c) Dữ liệu mới, đồng hồ S + 1 ngày: Bình `reject` R0 | (a) 200. (b) 409 "Đã tới thời điểm đề xuất, không thể chấp nhận yêu cầu", không có phiên; `GET` vẫn `PENDING` (không tự hết hạn); `cancel` 200. (c) 200 | Tích hợp |
| TC331 | D90 (R37) | R0 `PENDING`; khóa An bằng SQL (`status = LOCKED`); Bình `accept`; mở khóa An bằng SQL; Bình `accept` | 409 "Người gửi không còn hoạt động", yêu cầu vẫn `PENDING`, không có phiên; sau khi mở khóa: 200 | Tích hợp |
| TC332 | D90, D88 | Ba yêu cầu `PENDING` tới Bình. Bình `reject` cái thứ nhất với `{"note": "  Bận tuần này  "}`, cái thứ hai không có body, cái thứ ba với `note` 1001 ký tự | 200 `RequestItem`: `REJECTED`, `responseNote` "Bận tuần này", `respondedAt` = đồng hồ, `sessionId` null. Không body: 200, `responseNote` null. 1001 ký tự: 400 `errors` `note`, yêu cầu vẫn `PENDING` | Tích hợp |
| TC333 | D90 | An `cancel` R0 | 200 `RequestItem`: `CANCELLED`, `respondedAt` = đồng hồ, `sessionId` null | Tích hợp |
| TC334 | D90, TK 5.1 (khóa lạc quan) | Lặp 20 lần, mỗi lần một yêu cầu R0 mới: hai luồng chờ chung một `CountDownLatch` rồi gọi cùng lúc Bình `accept` và An `cancel` | Mỗi lần đúng một lệnh 200 và một lệnh 409; không lệnh nào 500. Sau 20 lần: mọi yêu cầu `ACCEPTED` có đúng một phiên, không yêu cầu `CANCELLED` nào có phiên | Tích hợp |
| TC335 | D87, D38 | `GET`, `accept`, `reject`, `cancel` với `{id}` 99999 và "abc"; `reject` `{id}` 99999 với `note` 1001 ký tự | 99999: 404 "Không tìm thấy yêu cầu". "abc": 400. `note` 1001 với 99999: 400 (400 trước 404) | Tích hợp |
| TC336 | D91 | Đồng hồ T0: An gửi Bình (R0), An gửi Giang (`ONE_WAY` Java). Đồng hồ T0 + 1 phút: An gửi Chi (`ONE_WAY` Java), Bình gửi An (`ONE_WAY` Tiếng Anh). An hủy yêu cầu tới Chi. An gọi `?box=sent`, `?box=received`, `?box=sent&status=PENDING`, không có `box`, `?box=FOO`, `?box=sent&status=FOO` | `box=sent`: 3 phần tử theo thứ tự Chi, Giang, Bình (`created_at` giảm dần; Giang và Bình cùng `created_at` nên `id` giảm dần), `myRole` `SENDER`. `box=received`: 1 phần tử (của Bình), `myRole` `RECEIVER`. `status=PENDING`: Giang, Bình. Thiếu `box`: 400 "Thiếu tham số bắt buộc: box"; `FOO`: 400 | Tích hợp |
| TC337 | D91 (Q1), NFR-02 | (a) R0 `PENDING`: An gọi `GET /api/requests/{id}` và `?box=sent`; Bình gọi `GET /api/requests/{id}` và `?box=received`. (b) Bình chấp nhận; Bình gọi lại. (c) Một yêu cầu bị `REJECTED`, một bị `CANCELLED`: người nhận gọi `GET` | (a) An thấy `locationOrLink`; Bình nhận `locationOrLink` null ở cả hai API, `sessionId` null. (b) Bình thấy `locationOrLink` và `sessionId` = id phiên. (c) Người nhận nhận `locationOrLink` null. Không response nào có khóa `phone`, `contactInfo`, `email` | Tích hợp |
| TC338 | D86, D44, D13 | Admin, Dung, Em (vé cấp trước khi bị khóa) gọi `GET /api/requests?box=sent`, `POST /api/requests` với `{}`, `GET /api/sessions` | Cả ba người, mọi API: 403 (ở filter, trước lỗi 400) | Tích hợp |

## D. Phiên

| Mã | Nguồn | Đầu vào | Mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC339 | D92 | Có phiên P và phiên Q (Chi gửi Bình `ONE_WAY` Kotlin, Bình chấp nhận sau P). An gọi `GET /api/sessions`; Bình gọi `GET /api/sessions`, `?status=SCHEDULED`, `?status=FOO` | An: 1 phần tử (P), `myRole` `SENDER`, `partner` = Bình (chỉ `id`, `fullName`). Bình: 2 phần tử Q rồi P (`created_at` giảm dần), `myRole` `RECEIVER`. Mỗi phần tử có đúng 12 khóa của `SessionItem`, không có `locationOrLink`, `phone`, `contactInfo`. `status=SCHEDULED`: 2 phần tử. `FOO`: 400 | Tích hợp |
| TC340 | D92, NFR-02, D87 | Phiên P: An và Bình gọi `GET /api/sessions/{P}`; Chi gọi; `GET /api/sessions/99999`; `GET /api/sessions/abc` | An: `partner.phone` null, `partner.contactInfo` "Facebook: Bình", có `locationOrLink`. Bình: `partner.phone` "0901000001". Cả hai: `myCompleted` false, `myReviewed` false, `cancelledBy`, `openReport`, `resolution` null. Chi: 403 "Bạn không tham gia phiên này". 99999: 404 "Không tìm thấy phiên". `abc`: 400 | Tích hợp |
| TC341 | D93, spec 6.2 | Phiên P. Đồng hồ S − 15 phút − 1 giây: Bình `start`. Đồng hồ S − 15 phút: Bình `start` | 409 "Chỉ bắt đầu được từ 15 phút trước giờ hẹn", dữ liệu không đổi; rồi 200 `SessionDetail`: `IN_PROGRESS`, `startedAt` = S − 15 phút | Tích hợp |
| TC342 | D93 (R34) | Phiên P; đồng hồ S + 3 ngày: An `GET`, rồi An `start` | `GET` vẫn `SCHEDULED` (không tự hết hạn); `start` 200, `IN_PROGRESS` (không có cận trên) | Tích hợp |
| TC343 | D93, spec 7.1 nhóm 5 | Phiên P đã `IN_PROGRESS`. An `complete`; An `complete` lần nữa; Bình `GET`; Bình `complete` | An: 200, `WAITING_CONFIRMATION`, `myCompleted` true. Lần hai: 409 "Bạn đã xác nhận hoàn thành, đang chờ bên kia", dữ liệu không đổi. Bình thấy `myCompleted` false. Bình: 200, `COMPLETED`, `completedAt` = đồng hồ | Tích hợp |
| TC344 | D93 (R34), D88 | (a) Phiên P, đồng hồ S − 1 ngày: An `cancel` `{"reason": "  Bận đột xuất  "}`; An và Bình `GET`. (b) Phiên P mới, đồng hồ S + 2 ngày: Bình `cancel` | (a) 200: `CANCELLED`, `cancelReason` "Bận đột xuất", `cancelledAt` = đồng hồ, `startedAt` null; An thấy `cancelledBy` `ME`, Bình thấy `PARTNER`. (b) 200 `CANCELLED` (hủy `SCHEDULED` được cả sau giờ hẹn) | Tích hợp |
| TC345 | D93, spec 4.3, TK 3.5 | Ba bộ dữ liệu riêng, mỗi bộ có đúng một phiên An–Bình, chưa có đánh giá: (a) hủy từ `SCHEDULED`; (b) hủy từ `IN_PROGRESS`; (c) `COMPLETED`. Mỗi bộ: An gọi `GET /api/matches?type=TWO_WAY` | `forward.reputation` của thẻ Bình: (a) 0.5 (chưa có phiên tính được); (b) 0.7 × 0.5 + 0.3 × 0 = 0.35; (c) 0.7 × 0.5 + 0.3 × 1 = 0.65 | Tích hợp |
| TC346 | D93, D88 | Phiên `IN_PROGRESS`, An `cancel` lần lượt: không có body; `{}`; `{"reason": "   "}`; `reason` 1001 ký tự; `{"reason": "Mất điện"}` | Không body: 400. `{}`, "   ", 1001 ký tự: 400 `errors` `reason`. Cuối cùng: 200 `CANCELLED`, `startedAt` vẫn có giá trị | Tích hợp |
| TC347 | D93, bảng 2.3, spec 7.1 nhóm 5 | Phiên `WAITING_CONFIRMATION`: An và Bình `cancel` (lý do hợp lệ), An `start`. Phiên `COMPLETED` và phiên `CANCELLED`: An `start`, `complete`, `cancel`. Phiên `SCHEDULED`: An `complete`. Phiên `IN_PROGRESS`: An `start` | Mọi lệnh 409, `detail` đúng bảng 2.1 ("Phiên đang chờ xác nhận, không hủy được...", "Phiên đã kết thúc", "Chỉ bắt đầu được phiên đã lên lịch", "Phiên không ở trạng thái xác nhận hoàn thành được"); dữ liệu không đổi | Tích hợp |
| TC348 | D87 | Phiên P: Chi `start`, `complete`, `cancel` (lý do hợp lệ), `reports` (lý do hợp lệ). `start` với `{id}` 99999 và "abc". `cancel` `{id}` 99999 với `{"reason": ""}` | Chi: 403 "Bạn không tham gia phiên này" (4 lần). 99999: 404. "abc": 400. `cancel` 99999 lý do rỗng: 400 (400 trước 404) | Tích hợp |
| TC349 | D93, TK 5.2 (khóa lạc quan) | Lặp 20 lần, mỗi lần một phiên `IN_PROGRESS` mới: An và Bình chờ chung một `CountDownLatch` rồi gọi `complete` cùng lúc | Mỗi lần chỉ là (một 200, một 409, phiên `WAITING_CONFIRMATION`) hoặc (hai 200, phiên `COMPLETED`); không lần nào hai 200 mà phiên vẫn `WAITING_CONFIRMATION`; không 500 | Tích hợp |
| TC350 | D93 (R37), D13 | Phiên P `SCHEDULED`, đồng hồ S; khóa Bình bằng SQL. An `start`, `GET`, `complete`; Bình gọi `GET /api/sessions/{P}` bằng vé cấp trước khi khóa | An: 200 cả ba lệnh (không xét trạng thái bên kia). Bình: 403 | Tích hợp |

## E. Báo cáo sự cố – `POST /api/sessions/{id}/reports`

| Mã | Nguồn | Đầu vào | Mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC351 | D94, D92 | (a) Phiên `WAITING_CONFIRMATION` (An bấm hoàn thành trước): An `reports` `{"reason": "  Bình không đến  "}`; An và Bình `GET` phiên. (b) Phiên khác cùng trạng thái (An bấm trước): Bình `reports` | (a) 201 `{id, sessionId, status: "OPEN", createdAt = đồng hồ}`; DB: `reason` đã cắt, `reporter_id` = An. An thấy `openReport` = {`id`, `reportedByMe` true, `createdAt`}, không có khóa `reason`; Bình thấy `reportedByMe` false. (b) 201 | Tích hợp |
| TC352 | D94 (R33) | Phiên `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`: An `reports` (lý do hợp lệ) | 409 "Chỉ gửi được báo cáo khi phiên đang chờ xác nhận hoàn thành"; không có dòng `incident_reports` mới | Tích hợp |
| TC353 | D94, D88 | Phiên `WAITING_CONFIRMATION` có báo cáo `OPEN` của An: An gửi lần nữa; Bình gửi. Phiên `WAITING_CONFIRMATION` chưa có báo cáo: `{}`, `{"reason": "  "}`, `reason` 1001 ký tự | 409 "Phiên đã có báo cáo sự cố đang chờ xử lý" (2 lần), vẫn đúng một báo cáo. 400 `errors` `reason` (3 lần) | Tích hợp |
| TC354 | D93, D94 (R33) | Phiên `WAITING_CONFIRMATION` (An bấm trước) có báo cáo `OPEN`: Bình `complete`; An `cancel`; Bình `cancel` | Bình `complete`: 409 "Phiên đang có báo cáo sự cố chờ quản trị viên xử lý", phiên vẫn `WAITING_CONFIRMATION`. Hai lệnh `cancel`: 409. Dữ liệu không đổi | Tích hợp |
| TC355 | D92 | (a) Phiên P `COMPLETED` qua API; chèn bằng SQL một đánh giá của An cho Bình; An và Bình `GET`. (b) Phiên khác `WAITING_CONFIRMATION` có báo cáo `OPEN`; bằng SQL: báo cáo → `RESOLVED`, `resolution` `SESSION_CANCELLED`, `admin_note` "Không diễn ra", `resolved_by` = Admin; phiên → `CANCELLED`, `cancelled_by` = Admin. Hai bên `GET` | (a) An: `myReviewed` true; Bình: false. (b) Cả hai: `resolution` = {`result` `SESSION_CANCELLED`, `adminNote` "Không diễn ra", `resolvedAt`}, `openReport` null, `cancelledBy` `ADMIN` | Tích hợp |

## F. Giao diện S07–S09 (thủ công)

| Mã | Nguồn | Đầu vào | Mong đợi | Loại |
| --- | --- | --- | --- | --- |
| TC356 | D95 | An mở S05 tab "Trao đổi hai chiều", bấm "Gửi yêu cầu" trên thẻ Bình. An mở S06 của Bình, bấm "Gửi yêu cầu". An mở `/users/{An}`. Sau khi gửi xong, An quay lại S05 | Từ S05: tới `/users/{Bình}/request?type=TWO_WAY`, loại chọn sẵn `TWO_WAY`. Từ S06: loại `ONE_WAY`. S06 của chính An không có nút. Sau khi gửi: thẻ Bình ở S05 không còn nút, có nhãn "Đã gửi yêu cầu" | Thủ công |
| TC357 | D95 (Q7) | An khai Sáng T2, Tối T3; Bình khai Tối T3, Tối T6. Ở S07: xem hai ô kỹ năng; bấm chip "Tối T3" khi máy đang ở thứ Ba 20:00, rồi khi máy ở thứ Hai; đổi loại sang `ONE_WAY` | Chỉ có chip "Tối T3". Ô "Bạn muốn học" = danh sách học của An; ô "Bạn sẽ dạy" = danh sách học của Bình. Thứ Ba 20:00 → điền thứ Ba tuần sau, 19:00; thứ Hai → thứ Ba ngày mai, 19:00. `ONE_WAY` thì ô "Bạn sẽ dạy" ẩn. Dưới ô địa điểm/link có câu "Người nhận chỉ thấy sau khi chấp nhận" | Thủ công |
| TC358 | D95, D89 | Ở S07: gửi khi địa điểm để trống; gửi với kỹ năng Python; gửi hợp lệ; mở lại S07 và gửi lần nữa; xem ở 360 px | Lỗi hiện dưới đúng ô (địa điểm/link; "Bạn muốn học"). Hợp lệ: chuyển tới `/requests?box=sent`, thấy yêu cầu `PENDING`. Gửi lại: hộp lỗi "Bạn đã có một yêu cầu đang chờ gửi tới người này". 360 px không cuộn ngang | Thủ công |
| TC359 | D95, D90, D91 | Bình mở S08 tab "Đã nhận"; An mở tab "Đã gửi". Bình từ chối một yêu cầu kèm ghi chú, chấp nhận một yêu cầu khác. Hai trình duyệt: An bấm "Hủy" một yêu cầu, sau đó Bình bấm "Chấp nhận" yêu cầu đó trên trang chưa tải lại | Thẻ `PENDING` phía Bình không có địa điểm/link, phía An có. Badge trạng thái theo màu của kế hoạch bước 10. "Từ chối" mở hộp thoại nhập ghi chú. Chấp nhận xong thẻ có "Xem phiên" mở `/sessions/{id}`. Ca hai trình duyệt: Bình thấy `detail` 409, danh sách tải lại, yêu cầu hiện `CANCELLED` | Thủ công |
| TC360 | D95, D93, D94 | Hai tài khoản, một phiên giờ hẹn sau 1 ngày và một phiên giờ hẹn sau 10 phút. Mở S09 chi tiết; bấm "Bắt đầu" ở phiên sau 10 phút; An "Hoàn thành"; Bình "Báo cáo sự cố" (nhập lý do); Bình gọi `complete` bằng tay (Console hoặc file `.http`); xem ở 360 px và chế độ tối | Phiên sau 1 ngày: nút "Bắt đầu" bị khóa, có chữ "Mở lúc HH:mm" kèm ngày (ví dụ "Mở lúc 18:45, T3 24/11/2026"). Thanh tiến trình đúng trạng thái sau mỗi bước. Hủy và báo cáo mở hộp thoại bắt nhập lý do. Sau báo cáo: không còn nút xác nhận, có dòng báo cáo đang chờ xử lý; gọi `complete` bằng tay nhận 409. Liên hệ và địa điểm/link hiện cho cả hai. Đọc được ở 360 px và chế độ tối | Thủ công |
| TC361 | D95, D81, D76 | Sinh viên xem menu; admin gõ `/requests`, `/sessions/1` | Sinh viên có "Yêu cầu" (`Inbox`) và "Phiên" (`CalendarCheck`), không có số đếm. Admin bị chuyển về `/admin/skills` | Thủ công |

---

## Không kiểm ở tuần 6

- Đánh giá sau phiên (FR-07), admin xử lý báo cáo và xem phiên (A02), khóa/mở khóa tài khoản bằng API (FR-08): tuần 7 (D86). Ca tuần 6 khóa tài khoản bằng SQL.
- Thông báo trong ứng dụng, số yêu cầu đang chờ trên menu (FR-13, D86).
- Chồng giờ giữa các yêu cầu, phiên của cùng một người (D96): không có quy tắc nên không có ca chặn; TC325 chỉ kiểm là không bị chặn.

## Những điểm tài liệu chưa quy định rõ (phát hiện khi viết ca)

| # | Chỗ chưa rõ | Ca liên quan | Cách xử lý trong bộ ca |
| --- | --- | --- | --- |
| ⚠1 | Hai ca song song (TC334, TC349) không ép được lần nào cũng xảy ra tranh chấp thật: có lần hai lệnh chạy nối tiếp | TC334, TC349 | Điều kiện mong đợi đúng cho mọi thứ tự chạy, nên ca không chập chờn. Lặp 20 lần để tăng khả năng gặp tranh chấp. Phần của TC334: bước 6 thêm test tích hợp `D90` (`d90_staleAcceptIsRejectedByVersion`) tạo tranh chấp chắc chắn: một kết nối JDBC riêng giữ khóa dòng chưa commit, đợi `accept` đứng chờ khóa trong `pg_stat_activity` rồi mới commit (kế hoạch bước 6, "Chốt ở bước này"). Phần của TC349: bước 7 thêm test `D93` (`d93_staleCompleteIsRejectedByVersion`) cùng cách làm, kết nối riêng làm thay lệnh "hoàn thành" của Bình rồi giữ khóa trong lúc An gọi `complete` (kế hoạch bước 7, "Chốt ở bước này"). Báo cáo sự cố (D94): bước 8 thêm hai test `D94` cùng cách làm, một cho báo cáo chạy chồng với lệnh xác nhận hoàn thành (409 khóa lạc quan), một cho hai báo cáo chạy chồng (409 từ chỉ mục `uq_reports_one_open`) (kế hoạch bước 8, "Chốt ở bước này") |
| ⚠2 | Gửi `cancel`, `reports` không có body: D88 chỉ nói `reason` bắt buộc | TC346 | Không body là JSON không đọc được (400 `detail`, không có `errors`), theo cách xử lý hiện có; `{}` mới ra 400 `errors` `reason`. Ca chỉ so mã 400 cho trường hợp không body |
| ⚠3 | TC327 cần một cách làm lệnh tạo phiên thất bại mà không sửa code | TC327 | Chèn trước một phiên trùng `request_id` bằng SQL: dữ liệu này không thể có qua API, chỉ dùng để chứng minh hai việc nằm trong một transaction |
| ⚠4 | Màu badge trạng thái yêu cầu không nằm trong D79, D95 | TC359 | Theo "Chốt ở bước này" của kế hoạch bước 10 |
