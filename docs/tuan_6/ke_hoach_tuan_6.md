# KẾ HOẠCH TUẦN 6

Lập ngày 2026-11-18. Áp dụng `docs/tuan_6/Quyet_dinh_tuan_6.md`: D78–D85 (giao diện) và D86–D96 (yêu cầu, phiên, báo cáo sự cố; chốt ở Bước 0). Ca kiểm thử: `docs/tuan_6/kiem_thu/ca_kiem_thu.md` (61 ca, TC301–TC361, viết ở Bước 0).

**Phạm vi** (spec 9, dòng tuần 6): FR-05 (yêu cầu kèm lịch đề xuất), FR-06 (vòng đời phiên, xác nhận hoàn thành), quyền xem liên hệ và địa điểm/link, FR-09 phần người dùng (gửi báo cáo sự cố); unit test nhóm 5 của spec 7.1, trừ phần đánh giá. Thêm việc ngoài FR: chuyển giao diện sang Tailwind CSS + shadcn/ui (D78).

**Không làm tuần này:** đánh giá (FR-07), admin xử lý báo cáo (A02, FR-09 phần admin), khóa tài khoản (FR-08), thông báo trong ứng dụng (FR-13). Tất cả ở tuần 7, trừ FR-13 (Nên có).

Quy ước chung:
- **Bước 0 là cổng:** chưa có D86 trở đi và ca kiểm thử thì không code bước nào (CLAUDE.md: viết ca từ tài liệu trước khi code).
- **Phần A (bước 1–3, giao diện)** làm trên nhánh `feat/ui-tailwind` và gộp vào `main` sau bước 3: giữa chừng, màn hình chưa chuyển bị vỡ vì Preflight (D78). Phần B (bước 4–8, backend) không phụ thuộc phần A; nếu phần A kéo dài thì làm song song được. Phần C (bước 9–11) cần cả A và B.
- Backend: package `exchange`. Test tích hợp kế thừa `IntegrationTestBase`, giả giờ bằng `MutableClock`; mỗi test có `@DisplayName("TCxxx ...")`.
- **Bước 6–8 làm TDD hai lượt** như lõi tuần 4: lượt TEST viết test, chạy thấy đỏ, người học đọc từng ô của bảng trạng thái × hành động; lượt CODE viết code cho xanh, không sửa test. Gọi `/buoc-tuan` cho các bước này thì skill dừng sau lượt TEST.
- Sau mỗi bước backend: `git diff --stat -- backend/src/test` để chắc test đã duyệt không bị sửa; `.\mvnw.cmd test` xanh toàn bộ, kể cả test tuần 3–5. Lõi `matching/` không đổi.
- Sau mỗi bước frontend: `npm run build`, `npm run lint` không lỗi; xem ở 360 px và ở chế độ tối.

## 1. Việc làm trước (không phải code, tự làm)

| Trước bước | Việc |
| --- | --- |
| 0 | Trả lời Q1–Q11 ở Bước 0 |
| 1 | Đọc D78–D85. Chụp màn hình 8 màn hiện tại (S01–S06, S10, A01) ở 1280 px và 360 px để so sau khi chuyển |
| 4 | Đọc `exchange_requests`, `exchange_sessions`, `incident_reports` trong `V1__init.sql` (CHECK, unique một phần); đọc mục khóa lạc quan của skill `jpa-patterns` |
| 6 | Tự điền bảng trạng thái × hành động (phụ lục của `docs/ra_soat/Bao_cao_ra_soat.md`) theo D86 trở đi, trên giấy, rồi mới xem ca |
| 9 | Có ít nhất 2 tài khoản sinh viên `ACTIVE` ghép được với nhau (để thử bằng tay) |

## 2. Các bước

### Bước 0 – Chốt quyết định và viết ca kiểm thử (tài liệu, không code)

**Trạng thái:** xong 2026-11-18. Người dùng duyệt Q1–Q11 và các mục "chốt theo tiền lệ" theo gợi ý; kết quả là D86–D96.

- **Làm:**
  1. Chốt R33–R39 (đợt 4 của rà soát, phần tuần 6) và Q1–Q11 dưới đây thành D86 trở đi trong `Quyet_dinh_tuan_6.md`. Gồm: bảng DTO request/response của TK 4.6–4.8 (R38), mã lỗi và thứ tự kiểm (R36), câu `detail` của từng lỗi, bảng trạng thái × hành động đầy đủ (không còn ô `[Rxx]`).
  2. Sửa ngược trong cùng commit: spec 6.2 (hủy phiên `SCHEDULED`, R34), 6.3 và 6.4 (theo Q1), 7.1 nhóm 5; NFR-02; TK 3.3 (`responded_at`, R39), 3.6 và TC2 A19–A23 (R33, R39; chạy lại bộ ca ràng buộc), 4.6–4.8, 4.10, 5.1–5.2 (R33, R34), 7 (truy vết NFR-02), 8 (G2); CLAUDE.md dòng "`phone`, `contact_info`, `location_or_link` là riêng tư" (R35: phiên không có trạng thái `ACCEPTED`, câu này thiếu quyền của admin); bảng trạng thái của rà soát (đợt 4).
  3. Viết `docs/tuan_6/kiem_thu/ca_kiem_thu.md` (TC301 trở đi): ca thủ công cho D78–D85 và S07–S09; ca tự động cho từng ô của bảng trạng thái × hành động, spec 7.1 nhóm 5 (trừ đánh giá) và từng dòng mã lỗi của R36.
  4. Quay lại file này: điền dòng "Ca" của bước 1–11 và mục 3.

- **Câu hỏi cần chốt.** Gợi ý theo tiêu chí "ít ảnh hưởng nhất tới việc đã làm" như tuần 5.

  | Mã | Nguồn | Câu hỏi | Gợi ý | Lý do |
  | --- | --- | --- | --- | --- |
  | Q1 | R35, NFR-02 | Khi yêu cầu còn `PENDING`, ai thấy địa điểm/link? | Người nhận chỉ thấy sau khi chấp nhận (giữ NFR-02). Người gửi xem lại được địa điểm/link do chính mình nhập, giống `/api/me` trả số điện thoại của chính mình. `GET /api/requests/{id}` không bao giờ trả `phone`, `contactInfo`; sau `ACCEPTED` trả `sessionId` | Ngày 2026-10-29 đã duyệt phương án 2 của R35 (cả hai bên thấy ngay khi `PENDING`) theo gợi ý. Nhưng phương án đó nới NFR-02 ("chỉ hiển thị cho hai bên từ khi yêu cầu được chấp nhận") và spec 7.1 nhóm 5, nên phải hỏi lại. Gợi ý này giữ NFR-02, spec 6.4 và wireframe S07. Người nhận không hợp địa điểm thì hủy phiên `SCHEDULED`, không bị trừ CompletionRate (R34) |
  | Q2 | R39 | Thêm CHECK `responded_at` bằng migration mới? | Không. Service tự ghi `responded_at` khi `accept`, `reject`, `cancel`; ghi quy tắc vào TK 3.3 | `MatchesApiTest` (tuần 4) chèn yêu cầu `ACCEPTED` không có `responded_at`; thêm CHECK thì test tuần 4 đỏ, phải sửa test đã duyệt |
  | Q3 | Spec 6.4 | `locationOrLink` có bắt buộc? | Bắt buộc với cả `ONLINE` và `OFFLINE`: cắt khoảng trắng hai đầu, rỗng thì 400, tối đa 500 ký tự (cột DB) | Spec 6.4 xếp địa điểm/link vào "lịch đề xuất". Phiên chép nguyên từ yêu cầu và không có API sửa, nên trống lúc gửi là trống mãi |
  | Q4 | TK 3.3 | Độ dài và tính bắt buộc của lời nhắn, ghi chú từ chối, lý do hủy phiên, lý do báo cáo (DB đều là `TEXT`)? | Cắt khoảng trắng hai đầu (D16), tối đa 1000 ký tự, dài hơn thì 400. Lời nhắn và ghi chú từ chối không bắt buộc (rỗng lưu `NULL`); hai lý do bắt buộc, rỗng thì 400 | 1000 bằng giới hạn của `bio` và lời nhận xét đánh giá (R45). Hai lý do bắt buộc theo spec 6.2 và FR-09 |
  | Q5 | TK 4.6 | Có kiểm hình thức của yêu cầu với hình thức học của người nhận? | Không | TK 4.6 không có kiểm tra này. Người nhận thấy hình thức trước khi chấp nhận và từ chối được |
  | Q6 | – | Có chặn một người có hai phiên hoặc yêu cầu chồng giờ nhau? | Không; ghi vào phần hạn chế | Spec và TK không có quy tắc; muốn kiểm phải định nghĩa "chồng giờ" theo thời lượng và theo những trạng thái nào |
  | Q7 | TK 6.3 (S07) | Bấm một khung trùng gợi ý (ví dụ "Tối T3") thì điền giờ nào? | Ngày thứ Ba gần nhất mà giờ đó còn ở tương lai; giờ: Sáng 08:00, Chiều 14:00, Tối 19:00 (giờ máy người dùng); sửa được. Khung trùng tính ở giao diện từ `/api/me/availability` và lịch rảnh trong `/api/users/{id}`, không thêm API | Spec 4.6 không gán giờ cho buổi; wireframe S07 dùng 19:00 cho buổi tối |
  | Q8 | TK 6.1, 6.3 | S07 nằm ở đâu, chọn sẵn gì? | Trang riêng `/users/:id/request`, mở từ nút "Gửi yêu cầu" ở S05 và S06. Loại mặc định `ONE_WAY`; mở từ tab "Trao đổi hai chiều" của S05 thì mặc định `TWO_WAY`. Ô "Bạn muốn học" lấy danh sách học của tôi, ô "Bạn sẽ dạy" lấy danh sách học của người nhận (R36); điều kiện Sim > 0 do backend kiểm | Form 8 ô không vừa hộp thoại ở 360 px; trang riêng có đường dẫn để quay lại. Giao diện không đọc được quan hệ kỹ năng (API của admin) nên không tự lọc Sim > 0 |
  | Q9 | TK 6.3 (S05) | Menu có số yêu cầu đang chờ ("Yêu cầu (2)" trong wireframe)? | Không làm ở tuần 6 | Đây là thông báo (FR-13, Nên có). Muốn làm phải gọi thêm API mỗi lần chuyển trang, khác cách `Layout` đọc dữ liệu một lần (D76) |
  | Q10 | TK 6.1 | Đường dẫn của S08, S09? | `/requests` cho S08, không có trang chi tiết yêu cầu riêng (thẻ đã đủ thông tin; yêu cầu `ACCEPTED` có nút "Xem phiên"). `/sessions` (danh sách) và `/sessions/:id` (chi tiết, wireframe S09). `GET /api/requests/{id}` vẫn làm và có test theo TK 4.6 | Một yêu cầu = một phiên (spec 6.4), nên phần chi tiết nằm ở phiên |
  | Q11 | Rà soát §10 | R40, R41 (phần tài khoản), R44, R45, R52, R53, R54 thuộc đợt 4 nhưng chỉ dùng từ tuần 7: chốt luôn ở Bước 0 hay dời? | Dời sang "đợt 4b, trước tuần 7"; Bước 0 sửa dòng lịch của rà soát. Tuần 6, admin gọi `/api/sessions/**` nhận 403 như các API quyền *Active* (D44); admin xem phiên làm cùng A02 | Giữ Bước 0 gọn. R40 và R53 còn phương án "tùy chọn"/"hoặc", cần hỏi riêng |

- **Chốt theo tiền lệ** (không hỏi lại; bạn phản đối mục nào thì nói):
  - R33 phương án 1, R34 (cả bốn ý), R36 (bảng mã lỗi), R37 phương án 1, R38 (DTO): đã duyệt theo gợi ý ngày 2026-10-29, là phương án ít ảnh hưởng hơn và không đụng yêu cầu Bắt buộc nào. Theo R37: các thao tác trên phiên không xét trạng thái tài khoản bên kia, chỉ `accept` trả 409 khi bên kia không còn `ACTIVE`.
  - Thứ tự kiểm: 401/403 (D44) → 400 → 404 → 409, như D74.
  - Mọi mốc thời gian mới (`created_at`, `responded_at`, `started_at`, `completed_at`, `cancelled_at`, `created_at` của báo cáo) lấy từ `Clock`, mở rộng D18, để test giả được giờ cho luật 15 phút và luật "quá giờ đề xuất".
  - `proposedStartAt` phải sau thời điểm hiện tại, không có giới hạn trên. `durationMinutes` 15–480 kiểm ở service (400) trước khi xuống DB.
  - Kỹ năng đã ẩn nhưng còn trong danh sách học vẫn gửi yêu cầu được (như D75: ẩn chỉ ngừng cho chọn mới).
  - Danh sách xếp `created_at` giảm dần rồi `id` giảm dần, chưa phân trang (R38).

- **Commit:** `docs: add week-6 decisions (D86-)` (gồm phần sửa ngược); `docs: add week-6 test cases`.

### Bước 1 – Nền giao diện (D78–D83, D85)

- **Cài:** `tailwindcss`, `@tailwindcss/vite`, `@fontsource/be-vietnam-pro`, `@fontsource-variable/inter`; `npx shadcn@4.23.0 init --template vite --base radix --preset b0` (D83). Thêm component khi cần, bắt đầu với: `button`, `card`, `input`, `label`, `textarea`, `select`, `checkbox`, `tabs`, `badge`, `alert`, `skeleton`, `field`, `separator`, `sidebar`, `sheet`.
- **Sửa:** `vite.config.ts` (plugin, alias `@/`), `tsconfig.app.json` và `tsconfig.json` (`paths`), `src/index.css` (Tailwind, biến màu D79, chế độ tối D82, font D80), `src/components/Layout.tsx` (D81), `src/components/Field.tsx`, `src/components/Notice.tsx` (D85), `.oxlintrc.json`.
- **Tạo:** `frontend/components.json`, `src/components/ui/*`, `src/hooks/use-mobile.ts`, `src/lib/utils.ts` (CLI sinh); `src/legacy.css` (CSS cũ, xóa ở bước 3).
- **Ca:** TC301–TC305, TC307 (thủ công).
- **Chốt ở bước này:**
  - Không sửa tay code trong `src/components/ui/`, trừ việc thêm kiểu `success` cho `Alert` (D85). Muốn đổi giao diện chung thì đổi biến màu.
  - oxlint báo lỗi ở code CLI sinh ra thì tắt đúng luật đó cho riêng file sinh ra trong `.oxlintrc.json`, không sửa code sinh ra. Khi làm: chỉ có cảnh báo (không có lỗi), gồm `only-export-components` ở `button`, `badge`, `tabs`, `sidebar` và `set-state-in-effect` ở `src/hooks/use-mobile.ts` (cũng do CLI sinh); tắt hai luật này cho `src/components/ui/**` và `src/hooks/use-mobile.ts`.
  - Đang tải: dùng `Skeleton` có hình giống nội dung, kèm `aria-busy`, thay chữ "Đang tải…". Lỗi tải: `Alert` lỗi và nút "Thử lại" như hiện nay. Kiểm tra cả tuần thấy S05, S06 và danh sách kỹ năng của S10 chỉ có hộp lỗi, không có nút (từ trước tuần 6); đã thêm `LoadError` (TC307).
  - **Màu nền `slate` không chọn được trong CLI** (phát hiện khi làm): CLI 4.23 liệt kê `neutral`, `stone`, `zinc`, `gray`, `mauve`, `olive`, `mist`, `taupe`, nhưng registry chỉ nhận `neutral | stone | zinc | mauve | olive | mist | taupe` (gửi `gray` nhận 400). Khởi tạo bằng preset mặc định của CLI (mã `b0`: kiểu `radix-nova`, màu nền `neutral`, icon `lucide`, font Inter); toàn bộ biến màu trong `index.css` viết tay theo bảng `slate` của Tailwind 4. `components.json` vì thế ghi `"baseColor": "neutral"`. Sửa ngược D79, D83, TC301.
  - **Mã màu:** Tailwind 4 đổi bảng màu sang OKLCH, nên `indigo-600` của Tailwind 4 (`oklch(51.1% 0.262 276.966)`) không còn là `#4F46E5`. Giữ mã hex D79 đã duyệt cho màu chính (`#4F46E5`, `#818CF8`, chữ `#0F172A`); màu xám và màu trạng thái dùng số OKLCH của Tailwind 4, ghi kèm tên sắc độ trong `index.css`.
  - **Đo tương phản** (script đổi OKLCH, hex sang độ sáng tương đối WCAG 2.x). Ba lựa chọn ban đầu trượt nên đổi: chữ phụ chế độ sáng `slate-500` trên nền `muted` `slate-100` chỉ 4.35:1 → dùng `slate-600`; viền ô nhập chế độ sáng `slate-400` 2.63:1 → `slate-500` (4.77:1); viền ô nhập chế độ tối `slate-600` trên `slate-900` 2.36:1 → `slate-500` (3.74:1). Các cặp đạt: nút chính sáng 6.29:1, tối 5.98:1; chữ phụ tối `slate-400` 6.79 trên `slate-900`. Màu trạng thái bản đầu (`green-700` 4.94, `amber-700` 5.05, `red-600` 4.76 trên nền trắng; `green-400`, `amber-400`, `red-400` trên `slate-900`) chỉ đo trên nền trắng và nền thẻ. **Kiểm tra cả tuần** đo thật trên Chrome (TC302) thấy chữ màu trên nền nhạt cùng màu của `Badge` (bước 10) và nút `destructive` trượt: sáng 4.37 (đang chờ), 4.29 (thành công), 3.97 (lỗi), nút "Từ chối" 3.82; tối nút "Từ chối" 4.24. Đổi đậm thêm một bậc (người dùng chọn 2026-11-22): sáng `red-700`, `green-800`, `amber-800`; tối `red-300`. Đo lại: Badge sáng 6.03, 6.07, 5.30, nút "Từ chối" 5.09; tối Badge 6.85, 6.61, 6.20, nút 5.59; chữ lỗi dưới ô sáng 6.42, tối 9.29; Alert thành công sáng 7.13, tối 10.02. Sao đánh giá ở S05 dùng `text-warning` (`amber-800`, 7.09:1 trên nền trắng). Kiểu `success` của `Alert` không làm mờ chữ 90% như `destructive` (quyết định từ bản đầu, khi `green-700` mờ 90% chỉ còn khoảng 4.2:1).
  - **CSS cũ** chuyển nguyên sang `src/legacy.css`, nạp bằng `@import "./legacy.css" layer(base)` để mọi lớp tiện ích Tailwind thắng; bỏ biến màu riêng (tên `--primary`, `--border`, `--muted` trùng biến shadcn/ui mà nghĩa khác) và dùng thẳng biến mới; bộ chọn theo thẻ viết `:where(...):where(:not([data-slot]))` để không chạm component shadcn/ui (component nào cũng có `data-slot`) và giữ độ ưu tiên bằng 0 như cũ; bỏ các lớp đã có component thay (`topbar`, `notice`, `field`...); trả lại lề của `<p>` mà Preflight xóa. Bước 3 xóa file này.
  - Font nạp bằng `@import` trong `index.css` (CLI tự thêm Inter vào đây), không import ở `main.tsx`.
  - `Field` nối nhãn và lỗi với ô nhập bằng `useId()` + `cloneElement` (gắn `id`, `aria-invalid`, `aria-describedby` vào ô con), vì nhãn của shadcn/ui đứng cạnh ô chứ không bọc ô như `<label>` cũ; chỗ gọi ở các trang không đổi.
  - CLI thêm dependency `cn` (gói chính chủ của shadcn, thay `clsx` + `tailwind-merge`) và `shadcn` (cho `@import "shadcn/tailwind.css"`).
  - Code sinh ra của `Sidebar` có tiêu đề ẩn tiếng Anh ("Sidebar") cho `Sheet` trên màn hẹp: giữ nguyên theo luật không sửa `components/ui`. Nút mở menu có `aria-label` "Mở menu". Phím tắt Ctrl+B của `Sidebar` lưu trạng thái đóng/mở vào cookie `sidebar_state`; đây là trạng thái menu, không phải chế độ màu (D82 vẫn đúng).
- **Tự kiểm:** tab Network không có yêu cầu tới `fonts.googleapis.com`; câu "Nguyễn Thị Hường – Kỹ năng" hiện đủ dấu ở cả hai font; DevTools giả chế độ tối thì cả trang đổi màu; menu chuyển thành `Sheet` dưới 768 px.
- **Commit:** `feat(frontend): set up Tailwind CSS and shadcn/ui with app shell`

### Bước 2 – Chuyển S01–S04 (D84)

- **Sửa:** `LoginPage.tsx`, `VerifyEmailPage.tsx`, `ProfilePage.tsx`, `SkillsPage.tsx`.
- **Tạo:** `src/components/AvailabilityGrid.tsx` (lưới D84, có chế độ chỉ đọc để S06 dùng ở bước 3).
- **Ca:** TC306 (thủ công).
- **Chốt ở bước này** (bố cục đã duyệt ngày 2026-11-18):
  - S01: thẻ giữa trang, tên ứng dụng phía trên, hai tab bằng `Tabs`.
  - S02: thẻ có icon trạng thái (`Loader2` quay khi đang xác thực, `CircleCheck`, `CircleX`).
  - S03: thẻ thông tin chia 2 cột từ 640 px; số điện thoại và liên hệ gom thành một nhóm có icon `Lock` và ghi chú "riêng tư"; lưới lịch rảnh D84.
  - S04: hai thẻ Dạy và Muốn học cạnh nhau từ 1024 px. Ô thêm kỹ năng là combobox (`Popover` + `Command`) gõ để lọc, gom theo nhóm; kỹ năng không chọn được vẫn hiện mờ kèm lý do như hiện nay. Bộ đếm là `Badge` "n/10". Nút bỏ là icon `X` có `aria-label` "Bỏ <tên kỹ năng>".
  - Không đổi lời gọi API, state và câu thông báo.
  - Phát hiện khi làm:
    - **Component dùng chung** (thêm ngoài danh sách "Tạo"): `AuthShell` (khung S01, S02), `AppLogo` (logo ở menu và trang đăng nhập), `PageSkeleton`, `LoadError` (hộp lỗi + "Thử lại", trước lặp ở 4 trang), `TextLink` (link trong câu chữ), `SkillCombobox` (S04, bước 3 dùng lại ở S10). Hằng số lưới (`DAYS`, `PERIODS`, `slotKey`) đặt ở `src/availability.ts` như `format.ts`: file component chỉ nên xuất component (luật `only-export-components` của oxlint, để hot reload chạy đúng).
    - **Ô chọn trình độ, hình thức học dùng `NativeSelect`** (thẻ `<select>` thật), không dùng `Select` của Radix: `Select` của Radix là nút bấm + danh sách, `Field` không gắn `id` vào được nên nhãn không nối với ô; `<select>` thật còn mở bộ chọn của hệ điều hành trên điện thoại.
    - Combobox lọc **không phân biệt dấu** (gõ "tieng" ra "Tiếng Anh"; bộ lọc mặc định của `cmdk` so nguyên văn) và lọc cả theo tên nhóm. Kỹ năng không chọn được hiện mờ kèm lý do: "đang ở danh sách …" như cũ, thêm "đã có trong danh sách" cho kỹ năng đã thêm. Vẫn chọn rồi bấm "Thêm" như cũ.
    - `AvailabilityGrid` vẽ hai bảng (ngang từ 640 px, dọc dưới 640 px), bảng không dùng ẩn bằng `display: none` nên không nhận Tab và trình đọc màn hình bỏ qua. Mỗi ô là `<label>` bọc `Checkbox` nên bấm cả ô 44 px được. Chế độ chỉ đọc (S06) không có ô chọn: dấu tick và câu ẩn "Sáng Thứ Hai: rảnh/không rảnh".
    - `Field` nhận thêm `className` tùy chọn (ví dụ `sm:col-span-2` cho ô "Giới thiệu"); chỗ gọi cũ không đổi.
    - S02 khi mở không có `token`: icon `Mail`.
    - Nút lưu đặt ở chân thẻ (`CardFooter`), nối với form bằng thuộc tính `form="profile-form"`; nhấn Enter trong ô nhập vẫn gửi form.
- **Tự kiểm:** đi trọn S01 → S04 như TC31 tuần 3; lưới lịch ở 360 px không cuộn ngang và tick được bằng bàn phím (Tab, Space).
- **Commit:** `feat(frontend): restyle auth, profile and skills pages`

### Bước 3 – Chuyển S05, S06, S10, A01; bỏ CSS cũ

- **Sửa:** `MatchesPage.tsx`, `MatchCard.tsx`, `UserProfilePage.tsx`, `SearchPage.tsx`, `AdminSkillsPage.tsx`, `src/index.css` (bỏ dòng nạp `legacy.css`). **Xóa:** `src/legacy.css`.
- **Ca:** TC308–TC310 (thủ công); chạy lại TC301–TC307 trên cả 8 màn.
- **Chốt ở bước này:**
  - S05: `Tabs` kèm số người. Thẻ ứng viên có avatar chữ cái đầu, % phù hợp cỡ chữ lớn và thanh tiến độ, các badge; mỗi lý do một dòng có icon; lịch trùng dạng chip. "Chi tiết điểm" mở bằng `Collapsible`: mỗi thành phần một dòng gồm nhãn, `Progress` thang 0–1 (spec 4.3: mọi thành phần trong [0, 1]) và số nguyên văn API (`tabular-nums`), cuối cùng là dòng D. Không làm tròn, không dùng biểu đồ tròn (NFR-09: cần so với bảng tính tay).
  - S06: phần đầu có avatar, tên, hình thức học và chỗ cho nút "Gửi yêu cầu" (bước 9); hai thẻ Dạy, Muốn học; lịch rảnh là `AvailabilityGrid` chỉ đọc.
  - S10: một hàng tìm kiếm (ô tên có icon `Search`, combobox kỹ năng, nút Tìm); kết quả là lưới thẻ, 2 cột từ 640 px; không có ai thì hiện lời gợi ý, không để trống.
  - A01: mỗi nhóm một thẻ; ẩn/hiện kỹ năng bằng `Switch` có nhãn "Đang dùng"; xóa cặp liên quan vẫn không hỏi xác nhận (bước 10 tuần 5).
  - Phát hiện khi làm:
    - `Progress` do CLI sinh không chuyển `value` xuống Radix nên trình đọc màn hình không đọc được giá trị. Mọi thanh tiến độ (mức phù hợp, 5 thành phần) đặt `aria-hidden`: con số in ngay cạnh mới là dữ liệu (NFR-09).
    - S05: hai tab dài kèm số đếm rộng khoảng 355 px, quá chỗ trống ở màn 360 px; cho nhãn tab xuống dòng (`whitespace-normal`, bỏ chiều cao cố định) thay vì cuộn ngang. Danh sách ứng viên giữ một cột.
    - S10: icon `Search` đặt ở nút "Tìm", không đặt trong ô tên: `Field` cần ô nhập là con duy nhất để gắn `id`, `aria-*`; bọc ô trong khung có icon thì nhãn nối vào khung chứ không vào ô. `SkillCombobox` nhận thêm `id`, `aria-invalid`, `aria-describedby` để nhãn "Kỹ năng người đó dạy" nối vào nút combobox; `ariaLabel` thành tùy chọn (chỉ S04 cần vì không có nhãn hiển thị).
    - A01: tên nhóm vẫn là tiêu đề cấp 2 như cũ. Công tắc, nút "Sửa", "Xóa" có `aria-label` kèm tên kỹ năng hoặc tên cặp ("Đang dùng Java", "Sửa Java", "Xóa cặp Java – Kotlin") để mỗi nút có tên riêng; chữ hiện trên màn hình nằm trong tên đó (WCAG 2.5.3). Ô chọn kỹ năng của admin vẫn là `<select>` (`NativeSelect`), gồm cả kỹ năng đã ẩn (D75).
    - Component dùng chung mới: `UserAvatar` (chữ cái đầu của từ đầu và từ cuối của tên, ẩn khỏi trình đọc màn hình vì tên đã hiện ngay cạnh).
    - S06: phần giới thiệu giữ xuống dòng người dùng gõ (`whitespace-pre-line`).
    - `src/legacy.css` đã xóa; kiểm bằng script tách `className` thành từng lớp: không còn lớp cũ nào.
- **Tự kiểm:** chạy hết các ca thủ công của D78–D85; so với ảnh chụp ở mục 1: không mất chữ, nút hay thông báo nào; `grep` không còn class cũ (`match-card`, `admin-row`, `skill-rows`...).
- **Commit:** `feat(frontend): restyle matching, search and admin pages`. Sau đó gộp `feat/ui-tailwind` vào `main`.

### Bước 4 – Entity và Repository cho yêu cầu, phiên, báo cáo

- **Tạo** trong `exchange`: Entity `ExchangeRequest`, `ExchangeSession`, `IncidentReport`; enum `RequestType`, `RequestStatus`, `SessionStatus`, `MeetingMode` (`ONLINE`, `OFFLINE`), `ReportStatus`; ba Repository.
- **Migration:** không có nếu Q2 theo gợi ý.
- **Test:** `ExchangeMappingTest`: lưu và đọc lại mỗi Entity; vi phạm CHECK của DB (ví dụ `TWO_WAY` thiếu kỹ năng chiều ngược) ra lỗi, chứng tỏ Entity khớp bảng.
- **Ca:** không có ca TC (bước chỉ dựng Entity); `ExchangeMappingTest` là test riêng của bước.
- **Chốt ở bước này:**
  - Kiểu theo D30: `duration_minutes` → `Short`, `TIMESTAMPTZ` → `Instant`, `version` → `Integer` + `@Version`, cột có `CHECK IN` → enum `@Enumerated(EnumType.STRING)`.
  - Khóa ngoại ánh xạ như các Entity tuần 3 trong `user/`, `skill/`; danh sách đọc bằng truy vấn kéo sẵn tên người và tên kỹ năng, không để N+1 (skill `jpa-patterns`).
  - Báo cáo sự cố phía người dùng nằm ở `exchange`, không ở `review` như tiêu đề TK 4.8: `complete` phải kiểm báo cáo mở trong cùng transaction (R33). Phần admin (A02) ở `admin`, tuần 7.
  - Phát hiện khi làm:
    - Tuần 3 có hai cách ánh xạ khóa ngoại (`Skill` dùng `@ManyToOne` tới nhóm; `UserTeachSkill` giữ `skillId` dạng `Long`). Chọn cách thứ hai cho cả ba Entity: mọi khóa ngoại (`senderId`, `receiverId`, các mã kỹ năng, `requestId`, `firstCompletedBy`, `cancelledBy`, `sessionId`, `reporterId`, `resolvedBy`) là `Long`; danh sách cần tên thì Repository JOIN và trả DTO (bước 6–7), nên không có quan hệ LAZY để lỡ đọc ngoài transaction.
    - **`version` để `null`, không gán `0`** dù DB có `DEFAULT 0` (ngoại lệ của quy ước D30): với `@Version Integer`, Spring Data coi entity là mới khi version bằng `null`; gán `0` thì `save()` gọi `merge` thay vì `persist`, chèn một bản sao, object đang cầm không có `id`. Hibernate tự ghi `0` khi INSERT. Sửa ngược D30 và skill `jpa-patterns`.
    - Thêm enum `ReportResolution` (`SESSION_COMPLETED`, `SESSION_CANCELLED`) cho cột `resolution` có `CHECK IN` (D30).
    - Tạo bằng hàm tĩnh như `User.register`, `Skill.create`: `ExchangeRequest.create(...)` (trạng thái `PENDING`), `ExchangeSession.scheduleFrom(request, now)` (chép giờ, thời lượng, hình thức, địa điểm theo D90), `IncidentReport.open(...)`. `createdAt` truyền vào từ `Clock` của service (D88). Chưa có hàm đổi trạng thái: thêm ở bước 6–8 khi có test dùng tới.
    - Entity không tự kiểm luật nghiệp vụ (ví dụ `TWO_WAY` phải có kỹ năng chiều ngược): service kiểm theo D89, CHECK của DB là lưới cuối; test ghim rằng DB vẫn chặn.
    - `ExchangeMappingTest` (5 test): lưu, đọc lại đủ cột của ba Entity; enum lưu bằng tên (đọc lại bằng SQL); `save()` trả lại chính object đã truyền; CHECK `TWO_WAY` và chỉ mục một phần `uq_requests_one_pending` của DB vẫn chặn.
- **Tự kiểm:** đổi `duration_minutes` sang `Integer` thì app không khởi động (`ddl-auto: validate`). Khi làm (làm hỏng rồi trả lại): field `durationMinutes` kiểu `Integer` → 5 test lỗi "wrong column type encountered in column [duration_minutes]"; bỏ `@Enumerated(EnumType.STRING)` ở `status` → 5 test lỗi "wrong column type ... [status]"; gán `version = 0` → test lưu yêu cầu đỏ ở chỗ `save()` phải trả lại chính object.
- **Commit:** `feat(exchange): add request, session and report entities`

### Bước 5 – Tạo yêu cầu: `POST /api/requests` (FR-05; R36; TK 4.6)

- **Tạo:** `ExchangeRequestController`, `ExchangeRequestService`, DTO `CreateRequestRequest`, `RequestResponse` (theo bảng DTO của Bước 0).
- **Sửa:** `common/SecurityConfig` (`/api/requests/**`, `/api/sessions/**` cần `ACTIVE_STUDENT`, D44).
- **Ca:** TC311–TC325.
- **Phải phủ:** từng dòng của bảng R36; Q3, Q4; gửi cho admin hoặc người bị khóa thì 404; trùng `PENDING` cùng chiều thì 409, kể cả khi DB báo lỗi unique (D24); chiều ngược lại thì được.
- **Chốt ở bước này:**
  - Sim > 0 tính bằng `SkillSimilarity` của lõi matching (Java thuần, D34) dựng từ `skill_relations`; dùng lại hàm đọc quan hệ của `MatchingDataRepository`, không viết câu SQL thứ hai cho cùng bảng.
  - **Hủy yêu cầu trong TC315, TC323 làm bằng SQL** (`status = 'CANCELLED'`, `responded_at` = giờ đồng hồ giả, như D90): API hủy có ở bước 6. Điều bước này cần kiểm là tạo yêu cầu chỉ chặn trùng `PENDING`. Sửa ngược TC315, TC323.
  - Bước (1) của D89 là Bean Validation trên DTO; bước (2)–(8) kiểm ở service theo đúng thứ tự. Trong bước (6) và (7), kiểm `senderLearnSkillId` trước `receiverLearnSkillId`. Thêm test `D89` ghim thứ tự các bước mà TC chưa ghim: (2) trước (3), (3) trước (8), (6) trước (7), (7) trước (8).
  - `durationMinutes` trong DTO là `Integer`, đổi sang `Short` sau khi qua kiểm: số quá lớn (ví dụ 40000) ra lỗi theo ô, không thành "JSON không đọc được" vì tràn `Short`. `type` là enum như `mode`: giá trị lạ → 400 "Nội dung JSON không đọc được".
  - Mã kỹ năng không tồn tại cũng là "không có trong danh sách muốn học" (400 ở bước 6 của D89), không phải 404.
  - Người nhận hợp lệ = `ACTIVE` và `STUDENT`, cùng điều kiện với `GET /api/users/{id}` (TK 4.3).
  - Trùng `PENDING` lọt qua kiểm của service (hai request chạy song song) thì chỉ mục `uq_requests_one_pending` chặn, lưới D24 trả 409 với câu chung "Dữ liệu xung đột với dữ liệu đã có". Không có test song song ở bước này: `ExchangeMappingTest` đã ghim DB chặn, `ErrorHandlingTest` đã ghim lưới 409.
  - Response dựng từ một câu JPQL (`ExchangeRequestRepository.findRowById`) JOIN tên hai người, tên kỹ năng (kể cả kỹ năng đã ẩn) và LEFT JOIN phiên để có `sessionId`; bước 6 dùng lại cho danh sách, chi tiết. `RequestItem.from(row, viewerId)` áp luôn luật địa điểm/link của D91 (người gửi luôn thấy, người nhận chỉ thấy khi `ACCEPTED`) vì đó là hàm ánh xạ duy nhất; bước 5 chỉ test nhánh người gửi, nhánh người nhận test ở bước 6.
- **Tự kiểm:** ghi lệnh thử vào `hoc_vibe_coding/http/tuan_6.http`; bỏ kiểm "kỹ năng thuộc danh sách học" thì ca R36 tương ứng phải đỏ. Khi làm (`CreateRequestApiTest`, 17 test: TC311–TC325, D87 không vé 401, D89 thứ tự kiểm; làm hỏng rồi trả lại):
  - bỏ kiểm danh sách học của người gửi → TC320 đỏ (201 thay vì 400), test D89 đỏ (ra câu của bước 7);
  - `proposedStartAt` bằng hiện tại được nhận (`isBefore` thay `!isAfter`) → TC317 đỏ;
  - bỏ `/api/requests/**` khỏi luật `ACTIVE_STUDENT` → TC322 đỏ (tài khoản chưa xác thực nhận 400 thay vì 403);
  - người gửi không thấy địa điểm/link của mình → TC311 đỏ;
  - kiểm Sim (7) trước danh sách học (6) → TC320 và test D89 đỏ.
- **Commit:** `feat(exchange): add create request API`

### Bước 6 – Hộp yêu cầu và phản hồi (TDD hai lượt; TK 4.6, 5.1; R34, R35, R37, R39)

- **API:** `GET /api/requests?box=sent|received&status=`, `GET /api/requests/{id}`, `POST /api/requests/{id}/accept`, `/reject` (`note` tùy chọn), `/cancel`.
- **Sửa:** `common/GlobalExceptionHandler` (lỗi khóa lạc quan `ObjectOptimisticLockingFailureException` → 409).
- **Ca:** TC326–TC337.
- **Phải phủ:** bảng "Yêu cầu" của phụ lục rà soát, đủ mọi ô; `accept` tạo phiên `SCHEDULED` với giờ, thời lượng, hình thức, địa điểm chép từ yêu cầu, **trong cùng transaction**; `accept` sau `proposed_start_at` thì 409 (R34); `accept` khi người gửi không còn `ACTIVE` thì 409 (R37); `responded_at` được ghi (R39); địa điểm/link và liên hệ theo Q1.
- **Chốt ở bước này:**
  - `box` chỉ nhận đúng chữ thường `sent`, `received` như TK 4.6; giá trị khác trả 400 "Tham số box có giá trị không hợp lệ: <giá trị>", cùng câu với D38. `status` là enum `RequestStatus` nên giá trị lạ đi qua cách xử lý D38 sẵn có.
  - `reject` nhận cả ba dạng: không có body, `{}`, `{"note": ...}`. `note` toàn khoảng trắng lưu `NULL`, dài đúng 1000 ký tự thì được (D88). Thêm test `D88` cho hai biên này, TC332 chỉ có 1001 ký tự.
  - Thứ tự kiểm của các API theo `{id}`: 400 (`{id}` không phải số, body) → 404 → 403 không tham gia → 403 sai vai trò → 409 (D87). Trong `accept`, ba lỗi 409 xét theo thứ tự `PENDING` → thời điểm → người gửi `ACTIVE` (D90); thêm test `D90` ghim thứ tự này (yêu cầu đã hủy và đã quá giờ ra câu "không còn ở trạng thái chờ"; người gửi bị khóa và đã quá giờ ra câu "đã tới thời điểm").
  - TC332 "ba yêu cầu `PENDING` tới Bình": tạo nối tiếp (An gửi R0, Bình từ chối, An gửi lại...), vì mỗi chiều chỉ có một `PENDING` (D89 bước 8); lúc Bình từ chối, yêu cầu đang `PENDING` đúng như ca.
  - **Khóa lạc quan có test chắc chắn xảy ra tranh chấp** (⚠1 của ca kiểm thử): TC334 có thể không lần nào gặp tranh chấp thật. Thêm test `D90`: một kết nối JDBC riêng hủy yêu cầu bằng SQL (tăng `version`) nhưng **chưa commit**, nên dòng bị khóa; gọi `accept` ở luồng khác; đợi tới khi `pg_stat_activity` cho thấy lệnh `accept` đang chờ khóa (nghĩa là nó đã đọc bản `PENDING` cũ và đang ghi); commit. `accept` phải nhận 409 "Dữ liệu vừa được thay đổi, hãy tải lại", không có phiên. Bỏ `@Version` thì `accept` ghi đè và nhận 200, nên test này là phần "Tự kiểm" chạy chắc chắn.
  - TC327 so `detail` với câu chung của lưới D24 "Dữ liệu xung đột với dữ liệu đã có".
  - `accept` trả record `{requestId, sessionId}` (bảng 2.2), không phải `RequestItem`.
  - Dữ liệu mẫu và lời gọi API dùng chung cho test bước 6–8 đặt ở `ExchangeTestData` (thư mục test, như `TestAccounts`). `CreateRequestApiTest` giữ bản riêng vì là test đã duyệt, không sửa.
- **Tự kiểm:** bỏ `@Version` thì ca "hủy đúng lúc chấp nhận" phải đỏ; ném lỗi sau khi lưu phiên thì yêu cầu vẫn `PENDING` (transaction rollback). Khi làm (`RequestInboxApiTest`, 16 test: TC326–TC337, D87, D88, hai test D90; làm hỏng rồi trả lại):
  - bỏ khóa lạc quan (thay `@Version` bằng `@Column(insertable = false, updatable = false)`, vì cột `version` NOT NULL nên không xóa trơn được) → test D90 khóa đỏ (200 thay vì 409), TC334 đỏ ngay lần 1 (`[200, 200]`: một thao tác bị ghi đè mất);
  - ném lỗi ngay sau khi lưu phiên → TC326 đỏ (500); trong DB yêu cầu vẫn `PENDING`, `version` 0, không có phiên, dù câu UPDATE yêu cầu đã flush trước khi tạo phiên;
  - nhận `accept` đúng lúc `proposed_start_at` (`isAfter` thay `!isBefore`) → TC330 đỏ (200 thay vì 409), test D90 thứ tự đỏ (ra câu "Người gửi không còn hoạt động");
  - kiểm `PENDING` trước vai trò trong `accept` → TC329 đỏ (409 thay vì 403).
- **Commit:** `feat(exchange): add request inbox and responses`

### Bước 7 – Vòng đời phiên (TDD hai lượt; TK 4.7, 5.2; R34, R35, R38)

- **API:** `GET /api/sessions?status=`, `GET /api/sessions/{id}` (kèm `phone`, `contactInfo` của bên kia và địa điểm/link, NFR-02), `POST /api/sessions/{id}/start`, `/complete`, `/cancel` (`reason` bắt buộc).
- **Ca:** TC338–TC350 (TC348 trừ phần `reports`), TC355.
- **Chốt ở bước này:**
  - `SessionDetail` đọc đủ `myReviewed`, `openReport`, `resolution` từ DB ngay ở bước này (D92), không để giá trị cứng chờ bước 8: TC355 dựng đánh giá, báo cáo bằng SQL nên không cần API báo cáo.
  - **TC348 chia hai bước:** phần `start`, `complete`, `cancel` và `{id}` sai làm ở bước này; phần Chi gọi `reports` làm ở bước 8, vì API báo cáo chưa có (gọi bây giờ ra 404 vì không có đường dẫn, không kiểm được quyền). Sửa ngược bước 8 và mục 3.
  - `complete` ở bước này chưa kiểm báo cáo `OPEN`: kiểm đó thêm ở bước 8 cùng TC354 (dòng "Sửa" của bước 8).
  - Hai lỗi 409 của `start` xét theo thứ tự: trạng thái `SCHEDULED` trước, mốc 15 phút sau (như `accept` xét `PENDING` trước thời điểm). TC347 ghim thứ tự này: phiên bị hủy lúc còn sớm hơn mốc 15 phút, `start` ra câu "Chỉ bắt đầu được phiên đã lên lịch".
  - Người tham gia phiên = người gửi và người nhận của yêu cầu gốc; `myRole`, `partner` tính theo người gọi. `cancelledBy`: `cancelled_by` là mình → `ME`, là bên kia → `PARTNER`, người khác (admin chốt, tuần 7) → `ADMIN`. `myCompleted` vẫn `true` sau khi phiên `COMPLETED` (D92: "mình là `first_completed_by`").
  - Body `{reason}` của `cancel` và của `reports` (bước 8) dùng chung một DTO `ReasonRequest`: cắt khoảng trắng hai đầu, rỗng sau khi cắt thì coi như thiếu → "Chưa nhập lý do"; dài hơn 1000 → "Lý do dài tối đa 1000 ký tự" (D88). Không có body: 400 "Nội dung JSON không đọc được" (⚠2).
  - Danh sách và chi tiết đọc bằng một câu JPQL (JOIN yêu cầu, hai người, hai kỹ năng, kể cả kỹ năng đã ẩn) như `findRowById` của bước 5. Chi tiết thêm ba câu nhỏ: báo cáo `OPEN` của phiên; báo cáo `RESOLVED` mới nhất (`resolved_at` rồi `id` giảm dần); mình đã có đánh giá cho phiên chưa. Bảng `reviews` chưa có Entity (tuần 7) nên câu cuối là native query `EXISTS`; tuần 7 có Entity `Review` thì đổi sang JPQL.
  - `start`, `complete`, `cancel` ghi xuống DB (`flush`) rồi đọc lại `SessionDetail` trong cùng transaction, như `flushAndRead` của bước 6: lỗi khóa lạc quan nổ ra trước khi dựng response.
  - **Khóa lạc quan có test chắc chắn tranh chấp** (phần TC349 của ⚠1): thêm test `D93` theo mẫu `d90_staleAcceptIsRejectedByVersion`. Phiên `IN_PROGRESS`; một kết nối JDBC riêng làm thay lệnh "hoàn thành" của Bình (`WAITING_CONFIRMATION`, `first_completed_by` = Bình, tăng `version`) nhưng chưa commit; An gọi `complete`; đợi tới khi lệnh của An chờ khóa trong `pg_stat_activity`; commit. An phải nhận 409 "Dữ liệu vừa được thay đổi, hãy tải lại", phiên vẫn `WAITING_CONFIRMATION` với `first_completed_by` = Bình. Bỏ `@Version` thì An ghi đè thành người bấm trước và nhận 200.
  - TC345 có ba bộ dữ liệu riêng nên là ba test (`tc345a_`, `tc345b_`, `tc345c_`): `IntegrationTestBase` xóa dữ liệu trước mỗi test.
  - Thêm test cho phần D mà TC chưa phủ: `D87` (không vé 401; admin, tài khoản chưa xác thực 403 ở filter với cả năm API phiên), `D88` (lý do dài đúng 1000 ký tự sau khi cắt được nhận), `D92` (báo cáo `OPEN` chèn bằng SQL: `openReport` đúng ba khóa, `reportedByMe` theo người xem; hai báo cáo `RESOLVED` thì lấy cái `resolved_at` muộn hơn).
  - `ExchangeTestData` thêm lời gọi API phiên, `sessionSnapshot` (cho "dữ liệu không đổi" của phiên: mọi cột và số báo cáo), Em (vé cấp trước khi bị khóa) và hàm đợi lệnh chờ khóa; `RequestInboxApiTest` giữ bản riêng của hàm đợi vì là test đã duyệt.
- **Phải phủ:** bảng "Phiên" của phụ lục rà soát, trừ cột đánh giá và cột admin; `start` từ đúng 15 phút trước `start_at` (giả giờ bằng `MutableClock`: 15 phút 0 giây được, sớm hơn 1 giây thì 409), không có cận trên (R34); `complete` lần 1 ghi `first_completed_by`, lần 2 phải là bên còn lại; hủy từ `IN_PROGRESS` giữ `started_at` (được tính vào CompletionRate); hủy `WAITING_CONFIRMATION` thì 409; người ngoài 403; mọi ca bị cấm không đổi dữ liệu (spec 7.1 nhóm 5).
- **Tự kiểm:** đổi 15 phút thành 0 hoặc dùng `Instant.now()` thay `Clock` thì ca biên phải đỏ. Gọi `GET /api/matches` sau khi có phiên hoàn thành và phiên hủy: Reputation đổi khác 0.5 (D32 đọc dữ liệu thật). Khi làm (`SessionLifecycleApiTest`, 20 test: TC338–TC350 với TC345 tách ba, TC355, D87, D88, D92, D93; làm hỏng rồi trả lại):
  - 15 phút thành 0 → TC341 đỏ (409 thay vì 200 đúng mốc S − 15 phút);
  - `start` dùng `Instant.now()` → TC341 đỏ (200 thay vì 409 lúc S − 15 phút − 1 giây), TC342 và TC346 đỏ (`startedAt` là giờ thật của máy);
  - bỏ khóa lạc quan (thay `@Version` bằng `@Column(insertable = false, updatable = false)` như bước 6) → test D93 đỏ (200 thay vì 409), TC349 đỏ ngay lần 1 (`[200, 200] WAITING_CONFIRMATION`: một lần bấm bị ghi đè mất). Xóa trơn `@Version` thì `accept` chèn `version` NULL, lưới D24 trả 409 và 18 test đỏ vì không tạo được phiên, không nói gì về khóa lạc quan;
  - `start` xét mốc 15 phút trước trạng thái → TC347 đỏ (ra câu "Chỉ bắt đầu được từ 15 phút trước giờ hẹn");
  - hủy thì xóa `started_at` → TC345b đỏ (Reputation 0.5 thay vì 0.35), TC346 đỏ (`startedAt` null): phần Reputation của "Tự kiểm" được TC345a–c làm qua `GET /api/matches?type=TWO_WAY` (0.5; 0.35; 0.65);
  - bỏ `/api/sessions/**` khỏi luật `ACTIVE_STUDENT` của `SecurityConfig` → TC338 và D87 đỏ (200 thay vì 403): hai test giữ cửa này xanh sẵn từ lượt TEST vì luật đã có từ bước 5.
- **Commit:** `feat(exchange): add session lifecycle API`

### Bước 8 – Gửi báo cáo sự cố (TDD hai lượt; FR-09 phần người dùng; R33)

- **API:** `POST /api/sessions/{id}/reports` `{reason}`.
- **Sửa:** `complete` trả 409 khi phiên có báo cáo `OPEN` (D93; `openReport` của chi tiết phiên đã có từ bước 7).
- **Ca:** TC351–TC354; phần `reports` của TC348 (Chi gọi `reports` → 403 "Bạn không tham gia phiên này"; chốt ở bước 7).
- **Phải phủ:** chỉ nhận khi `WAITING_CONFIRMATION`, trạng thái khác thì 409; mỗi phiên một báo cáo mở, người gửi sau nhận 409 (kể cả lỗi unique từ DB); người ngoài 403; lý do rỗng 400 (Q4).
- **Chốt ở bước này:**
  - **Gửi báo cáo ép tăng `version` của phiên** (khóa lạc quan ép tăng, như D90, D93; người dùng chọn 2026-11-21). Phát hiện khi viết test: ở mức cô lập READ COMMITTED của PostgreSQL, "kiểm báo cáo mở trong cùng transaction" (R33) chưa đủ. An gửi báo cáo và Bình xác nhận hoàn thành chạy chồng nhau thì lệnh `complete` không thấy báo cáo chưa commit, còn lệnh gửi báo cáo chỉ ghi `incident_reports` nên không đụng `version` của phiên: cả hai thành công, phiên `COMPLETED` mà báo cáo vẫn `OPEN`, đúng "kịch bản treo" của R33 (admin chỉ chốt được phiên `WAITING_CONFIRMATION`). Ép tăng `version` khi gửi báo cáo thì lệnh ghi sau thua khóa lạc quan, nhận 409 "Dữ liệu vừa được thay đổi, hãy tải lại"; `complete` không đổi. Báo cáo thành công làm `version` của phiên tăng 1; báo cáo bị từ chối không đổi gì (rollback). Sửa ngược D94, bảng 2.1 (dòng khóa lạc quan), ⚠1 của ca kiểm thử, skill `jpa-patterns`.
  - Thêm test `D94` tranh chấp chắc chắn theo mẫu `d93_staleCompleteIsRejectedByVersion`: phiên `WAITING_CONFIRMATION` (An bấm trước); một kết nối JDBC riêng làm thay lệnh "xác nhận hoàn thành" của Bình (`COMPLETED`, tăng `version`) nhưng chưa commit; An gọi `reports`; đợi lệnh của An chờ khóa trong `pg_stat_activity`; commit. An nhận 409 "Dữ liệu vừa được thay đổi, hãy tải lại", phiên `COMPLETED`, không có báo cáo.
  - **"Kể cả lỗi unique từ DB"** có test `D94` tranh chấp chắc chắn: kết nối riêng chèn báo cáo `OPEN` của Bình nhưng chưa commit (An không thấy nên qua bước kiểm báo cáo mở); An gọi `reports`, lệnh chèn đứng chờ ở chỉ mục `uq_reports_one_open`; commit. An nhận 409 với câu chung của lưới D24 "Dữ liệu xung đột với dữ liệu đã có", vẫn đúng một báo cáo (của Bình). Kết nối riêng chỉ chèn, không tăng `version` phiên: đúng thời điểm đó của lệnh báo cáo thật, vì khóa lạc quan ép tăng ghi `version` lúc commit.
  - Hai lỗi 409 của `reports` xét theo thứ tự viết trong D94: trạng thái `WAITING_CONFIRMATION` trước, báo cáo `OPEN` sau. Qua API không dựng được phiên ngoài `WAITING_CONFIRMATION` mà có báo cáo `OPEN`, nên thứ tự này không quan sát được và không có test riêng.
  - Thứ tự hai lỗi 409 của `complete` từ `WAITING_CONFIRMATION` (D93: người bấm trước rồi mới tới báo cáo `OPEN`) có test `D93`: phiên có báo cáo `OPEN`, An (người bấm trước) gọi `complete` thì ra câu "Bạn đã xác nhận hoàn thành, đang chờ bên kia".
  - TC354: báo cáo `OPEN` do An gửi qua API (ca không ghi "bằng SQL"); hai lệnh `cancel` ra câu của bảng 2.1 "Phiên đang chờ xác nhận, không hủy được...".
  - Mọi lệnh báo cáo bị từ chối so "dữ liệu không đổi" bằng `sessionSnapshot` (mọi cột của phiên, kể cả `version`, và số báo cáo).
  - 201 không có header `Location`, như `POST /api/requests` (bước 5); body đúng bốn khóa `{id, sessionId, status, createdAt}` (bảng 2.2).
  - Test ở lớp mới `IncidentReportApiTest`; phần `reports` của TC348 cũng ở đây (`SessionLifecycleApiTest` là test đã duyệt, không sửa). Thêm test cho phần D mà TC chưa phủ: `D87` (không vé 401; admin, tài khoản chưa xác thực 403 ở filter; `{id}` 99999 → 404, "abc" → 400; 99999 với lý do rỗng → 400 trước 404), `D88` (lý do dài đúng 1000 ký tự sau khi cắt được nhận; không có body → 400 "Nội dung JSON không đọc được", ⚠2). `ExchangeTestData` thêm lời gọi `report`.
- **Tự kiểm:** bỏ kiểm báo cáo mở trong `complete` thì ca tương ứng phải đỏ. Khi làm (`IncidentReportApiTest`, 10 test: TC351–TC354, TC348 phần `reports`, D87, D88, D93, hai test D94; làm hỏng rồi trả lại, mỗi lần một chỗ):
  - bỏ kiểm báo cáo mở trong `complete` → TC354 đỏ (Bình xác nhận được: 200 thay vì 409);
  - bỏ ép tăng `version` (đọc phiên bằng `findById` thường) → test D94 tranh chấp với `complete` đỏ (201 thay vì 409: báo cáo treo trên phiên `COMPLETED`); chín test kia vẫn xanh, chỉ test tranh chấp chắc chắn bắt được lỗi này;
  - `report` kiểm trạng thái trước người tham gia → TC348 đỏ (Chi nhận 409 thay vì 403);
  - bỏ kiểm "đã có báo cáo OPEN" trong `report` → TC353 đỏ: vẫn 409 nhưng là câu chung của lưới D24 "Dữ liệu xung đột với dữ liệu đã có" (chỉ mục `uq_reports_one_open` chặn thay);
  - `complete` xét báo cáo `OPEN` trước người bấm trước → test D93 đỏ (ra câu "Phiên đang có báo cáo sự cố...");
  - `report` dùng `Instant.now()` thay `Clock` → TC351 đỏ (`createdAt` là giờ thật của máy);
  - `report` nhận cả phiên `IN_PROGRESS` → TC352 đỏ (201 thay vì 409);
  - Log SQL (`-Dlogging.level.org.hibernate.SQL=DEBUG`) của một lệnh báo cáo: `SELECT` phiên không có `FOR UPDATE`, rồi `INSERT` báo cáo, rồi lúc commit mới `update exchange_sessions set version=? where id=? and version=?`.
- **Commit:** `feat(exchange): add incident report API`

### Bước 9 – S07 Gửi yêu cầu (Q7, Q8; wireframe TK 6.3)

- **Tạo:** trang S07 theo đường dẫn của Q8. **Sửa:** `MatchCard.tsx`, `UserProfilePage.tsx` (nút "Gửi yêu cầu"; ẩn khi `hasPendingRequest`, giữ badge "Đã gửi yêu cầu"), `App.tsx`, `api/types.ts`.
- **Ca:** TC356–TC358 (thủ công).
- **Chốt ở bước này:** chọn loại bằng `RadioGroup`; ngày và giờ dùng ô `date`, `time` của trình duyệt (không thêm thư viện lịch); chip khung trùng theo Q7; hình thức chỉ `ONLINE`/`OFFLINE`; dưới ô địa điểm/link ghi ai được xem theo Q1. Gửi xong chuyển sang S08, tab "Đã gửi". Lỗi theo ô hiện dưới ô (D24).
  - `RadioGroup` sinh bằng CLI như các component khác (`npx shadcn@4.23.0 add radio-group`), thành `src/components/ui/radio-group.tsx`; không thêm gói npm (`radix-ui` đã có).
  - S07 tải song song `GET /api/users/{id}`, `/api/me/learn-skills`, `/api/me/availability`. Tải lỗi (kể cả 404 người nhận) thì hiện `detail` kèm nút "Thử lại" (`LoadError`).
  - Tiêu đề "Gửi yêu cầu trao đổi tới {họ tên}" như wireframe. Loại: "Hai chiều" và "Một chiều", mỗi nút kèm một dòng giải thích. Đổi sang `ONE_WAY` thì ẩn ô "Bạn sẽ dạy" và không gửi `receiverLearnSkillId`; giá trị đã chọn vẫn giữ, đổi lại `TWO_WAY` thì hiện lại. `?type=` khác `TWO_WAY` (hoặc không có) coi là `ONE_WAY`.
  - Hai ô kỹ năng là `NativeSelect` như ô "Hình thức học" của S03, dòng đầu "Chọn kỹ năng" (rỗng). Danh sách rỗng thì dưới ô có gợi ý: của tôi trỏ tới `/skills`; của người nhận ghi "Người này chưa khai báo kỹ năng muốn học, hãy gửi yêu cầu một chiều".
  - Giá trị mặc định lấy theo wireframe: thời lượng 90 phút, hình thức "Trực tuyến"; ngày và giờ để trống. Nhãn hình thức dùng `formatMode` ("Trực tuyến", "Gặp trực tiếp") như S03, S05. Ô thời lượng là `number` (min 15, max 480, bước 15); ô trống gửi `null` để backend báo lỗi theo ô.
  - Ngày và giờ ghép thành giờ máy người dùng rồi gửi dạng ISO giờ UTC (`toISOString()`, D88 nhận cả `Z`). Thiếu một trong hai thì gửi `null`. Lỗi của `proposedStartAt` hiện dưới cụm ngày giờ. Ô ngày có `min` là hôm nay; "phải ở tương lai" vẫn do backend kiểm.
  - Chip khung trùng: giao của hai lịch rảnh, xếp theo thứ rồi buổi, ghi như `formatSlot` ("Tối T3"). Không có buổi trùng thì ghi "Không có buổi rảnh trùng". Phần tính ngày giờ của chip nằm trong hàm thuần ở `src/requestTime.ts`, để chạy thử được bằng Node mà không cần trình duyệt.
  - Ô địa điểm/link luôn có nhãn "Địa điểm hoặc link"; chữ mờ đổi theo hình thức. Dòng gợi ý dưới ô dùng câu của D95 "Người nhận chỉ thấy sau khi chấp nhận" (wireframe cũ ghi "Chỉ hiển thị cho ... sau khi chấp nhận"; D95 mới hơn nên theo D95).
  - Lỗi: `detail` hiện ở hộp lỗi đầu form (409 trùng, 400 gửi cho chính mình, "Dữ liệu không hợp lệ" khi có lỗi theo ô), lỗi theo ô hiện dưới đúng ô như S03.
  - Nút "Hủy" quay lại trang trước (`navigate(-1)`); mở S07 bằng cách gõ thẳng đường dẫn (không có trang trước) thì về S06 của người nhận.
  - S06 biết "hồ sơ của chính mình" bằng cách gọi thêm `GET /api/me` song song với hồ sơ: các trang trong `RequireStudent` không nhận được context từ `Layout`. Nút đặt trong thẻ đầu trang.
  - Thẻ S05 dùng `match.type` để biết đang ở tab nào: thẻ `TWO_WAY` thêm `?type=TWO_WAY`.
  - Trước khi có S08 (bước 10), `/requests?box=sent` rơi vào route `*` và về trang chính. Vì vậy phần "thấy yêu cầu `PENDING`" của TC358 kiểm lại ở bước 10.
- **Tự kiểm:** gửi yêu cầu hai chiều từ S05 ở 360 px; gửi trùng nhận thông báo 409. Khi làm: `npm run build` (gồm `tsc -b`) và `npm run lint` sạch. Phần ngày giờ của TC357 chạy bằng Node (`TZ=Asia/Ho_Chi_Minh`, gọi thẳng `src/requestTime.ts`): khung trùng của An và Bình chỉ còn "Tối T3"; thứ Ba 20:00 → thứ Ba tuần sau 19:00; thứ Hai → thứ Ba ngày mai; thứ Ba 18:59 → hôm nay; đúng 19:00 → tuần sau; ghép ô ngày giờ ra ISO giờ UTC. Làm hỏng rồi trả lại:
  - coi "bằng 19:00" là còn ở tương lai → đỏ ở ca đúng 19:00;
  - ghép ngày giờ như giờ UTC (thêm `Z`) → đỏ (`19:00Z` thay vì `12:00Z`);
  - `localDateString` dùng `toISOString()` → đỏ (00:30 ngày 1/12 giờ Việt Nam thành `2026-11-30`);
  - bỏ `% 7` khi đổi `dayOfWeek` → vẫn xanh: đột biến tương đương, vì `daysAhead` đã lấy `% 7` nên thứ 7 và 0 cho cùng kết quả.
  - Phần cần trình duyệt (TC356–TC358, 360 px, hộp lỗi 409) người dùng tự làm; `npm run lint` từng báo `react(purity)` khi gọi `new Date()` lúc vẽ, đã sửa bằng `useState(() => ...)` cho "hôm nay" và đọc đồng hồ trong `onClick` của chip.
- **Commit:** `feat(frontend): add send request page`

### Bước 10 – S08 Hộp yêu cầu (Q10)

- **Tạo:** trang `/requests`. **Sửa:** `Layout.tsx` (mục "Yêu cầu", D81), `App.tsx`, `api/types.ts`.
- **Ca:** TC359 (thủ công).
- **Chốt ở bước này:** hai tab "Đã nhận", "Đã gửi"; ô lọc trạng thái mặc định "Tất cả". Mỗi thẻ: người kia (link hồ sơ), loại, kỹ năng hai chiều, giờ, thời lượng, hình thức, địa điểm/link theo Q1, lời nhắn, ghi chú từ chối, `Badge` trạng thái (`PENDING` màu `warning`, `ACCEPTED` `success`, `REJECTED` `destructive`, `CANCELLED` màu xám). Nút theo trạng thái và vai trò: Chấp nhận, Từ chối (hộp thoại nhập ghi chú tùy chọn), Hủy; yêu cầu `ACCEPTED` có "Xem phiên". Gặp 409 thì hiện `detail` và tải lại danh sách.
  - Tab là tham số `box` trên URL (D95): đổi tab thì đổi URL. `box` thiếu hoặc lạ coi là `received`. Ô lọc trạng thái là `NativeSelect` (Tất cả, Đang chờ, Đã chấp nhận, Đã từ chối, Đã hủy), giữ nguyên khi đổi tab, gọi API với `status` thay vì lọc ở giao diện. Sau khi tải lại vì vừa bấm nút, danh sách cũ vẫn hiện tới khi có danh sách mới; chỉ đổi tab hay đổi bộ lọc mới hiện khung đang tải.
  - Thẻ ghi theo góc nhìn người xem: "Từ" (tab Đã nhận) hoặc "Tới" (tab Đã gửi) kèm tên người kia, link tới `/users/{id}`; "Bạn học", "Bạn dạy" thay cho kỹ năng của người gửi/người nhận (người nhận thì "Bạn dạy" là `senderLearnSkill`). Thời điểm ghi "19:00, T3 24/11/2026" theo giờ máy (thêm `formatDateTime` vào `format.ts`, bước 11 dùng lại), thời lượng "90 phút", hình thức theo `formatMode`. Có thêm dòng nhỏ "Gửi lúc ...".
  - Địa điểm/link: có giá trị thì hiện; chuỗi bắt đầu bằng `http://` hoặc `https://` là link mở tab mới. Người nhận xem yêu cầu `PENDING` (API trả `null`, Q1) thì ghi "Hiện sau khi bạn chấp nhận"; `null` ở các trạng thái khác thì không có dòng này.
  - `Badge` thêm hai variant `success`, `warning`, tô cùng cách với `destructive` (chữ màu trạng thái trên nền nhạt cùng màu, D79). `CANCELLED` dùng variant `secondary` (xám). Nhãn: Đang chờ, Đã chấp nhận, Đã từ chối, Đã hủy.
  - Nút chỉ theo trạng thái và vai trò, không xét giờ: người nhận thấy Chấp nhận, Từ chối khi `PENDING` (quá giờ đề xuất thì backend trả 409 "Đã tới thời điểm đề xuất..."); người gửi thấy Hủy khi `PENDING`. "Chấp nhận" và "Hủy" bấm là gửi ngay, không hỏi lại (kế hoạch chỉ ghi hộp thoại cho "Từ chối"). Đang gửi thì các nút của thẻ đó bị khóa.
  - Thành công: hộp thông báo đầu danh sách ("Đã chấp nhận yêu cầu, phiên đã được tạo.", "Đã từ chối yêu cầu.", "Đã hủy yêu cầu.") rồi tải lại danh sách, vì khi đang lọc "Đang chờ" thì thẻ vừa xử lý biến mất. Mọi lỗi của nút (409, 403, 404) hiện `detail` ở hộp lỗi đầu danh sách rồi tải lại. Trong hộp thoại từ chối, lỗi theo ô `note` hiện dưới ô; lỗi khác thì đóng hộp thoại, hiện ở hộp lỗi đầu danh sách. Ô ghi chú tối đa 1000 ký tự.
  - "Xem phiên" mở `/sessions/{sessionId}`. Trước bước 11, đường dẫn này rơi vào route `*` và về trang chính.
  - Nút đóng (dấu ×) của `Dialog` đổi chữ cho trình đọc màn hình từ "Close" thành "Đóng": bước này là chỗ đầu tiên dùng `Dialog`.
  - Menu: "Yêu cầu" (`Inbox`) đứng sau "Tìm kiếm" (D81), không có số đếm (D86).
- **Tự kiểm:** hai trình duyệt, hai tài khoản: một bên hủy, bên kia bấm chấp nhận thì nhận 409 và thấy trạng thái mới. Khi làm: `npm run build` và `npm run lint` sạch. `formatDateTime` chạy bằng Node (`TZ=Asia/Ho_Chi_Minh`): "2026-11-24T12:00:00Z" → "19:00, T3 24/11/2026"; Chủ nhật; giờ UTC còn ngày hôm trước nhưng giờ máy đã sang ngày mới. Làm hỏng rồi trả lại: quên dời thứ (`getDay()` bắt đầu từ Chủ nhật) → đỏ ("T4" thay vì "T3"); lấy giờ UTC → đỏ ("12:00" thay vì "19:00"). Phần hai trình duyệt (TC359) người dùng tự làm.
- **Commit:** `feat(frontend): add request inbox page`

### Bước 11 – S09 Phiên của tôi và chi tiết phiên (Q10; wireframe TK 6.3)

- **Tạo:** trang `/sessions`, `/sessions/:id`. **Sửa:** `Layout.tsx` (mục "Phiên"), `App.tsx`, `api/types.ts`.
- **Ca:** TC360, TC361 (thủ công).
- **Chốt ở bước này:** chi tiết có thanh tiến trình trạng thái (Đã lên lịch → Đang diễn ra → Chờ xác nhận → Hoàn thành; Đã hủy hiện riêng kèm lý do), giờ, hình thức, địa điểm/link, liên hệ bên kia. Nút theo trạng thái và vai trò, đúng bảng của Bước 0: Bắt đầu (trước mốc 15 phút thì khóa, kèm chữ "Mở lúc HH:mm" và ngày), Hoàn thành, Xác nhận hoàn thành, Hủy (hộp thoại nhập lý do), Báo cáo sự cố (hộp thoại nhập lý do; chỉ khi `WAITING_CONFIRMATION` và chưa có báo cáo mở). Nút chỉ ẩn để dễ dùng, quyền do backend kiểm (spec 6.3). Gặp 409 thì hiện `detail` và tải lại phiên. Chưa có nút đánh giá (tuần 7).
  - Hai trang `SessionsPage.tsx` (`/sessions`) và `SessionDetailPage.tsx` (`/sessions/:id`). Phần quyết định nút nào hiện, thanh tiến trình đang ở bước nào và mấy giờ mở nút "Bắt đầu" nằm trong hàm thuần ở `src/sessionView.ts`, để chạy thử được bằng Node như `requestTime.ts`.
  - Danh sách: tiêu đề "Phiên của tôi"; ô lọc trạng thái là `NativeSelect` (Tất cả, Đã lên lịch, Đang diễn ra, Chờ xác nhận, Hoàn thành, Đã hủy) như S08, gọi API với `status`. Mỗi thẻ: người kia (link hồ sơ), loại, "Bạn học", "Bạn dạy" theo góc nhìn người xem như S08, thời điểm kèm thời lượng, hình thức, `Badge` trạng thái, nút "Xem chi tiết". Thẻ không có địa điểm/link và liên hệ vì API danh sách không trả (D92). Danh sách rỗng ghi "Chưa có phiên nào. Phiên được tạo khi một yêu cầu được chấp nhận."; đang lọc thì ghi "Không có phiên nào ở trạng thái này."
  - Nhãn trạng thái lấy theo thanh tiến trình: Đã lên lịch, Đang diễn ra, Chờ xác nhận, Hoàn thành, Đã hủy. Màu `Badge`: `SCHEDULED` `outline`, `IN_PROGRESS` `default` (màu chính), `WAITING_CONFIRMATION` `warning`, `COMPLETED` `success`, `CANCELLED` `secondary` (xám như yêu cầu đã hủy ở S08). Hai màu trạng thái theo D79: đang chờ màu hổ phách, hoàn thành xanh lá.
  - Chi tiết: link "Phiên của tôi" về danh sách; đầu thẻ "Phiên #12", dòng kỹ năng "Java ↔ Tiếng Anh" (một chiều chỉ có một kỹ năng), `Badge` loại và trạng thái. Thân thẻ: thanh tiến trình; "Học cùng" (link hồ sơ), "Bạn học", "Bạn dạy", thời điểm kèm thời lượng, hình thức, địa điểm/link; "Điện thoại", "Liên hệ khác" của bên kia (chưa khai thì ghi "Chưa khai báo"); "Bắt đầu lúc", "Hoàn thành lúc" khi đã có.
  - Thanh tiến trình là danh sách có thứ tự 4 bước: bước đã qua có dấu ✓, bước hiện tại có `aria-current="step"`; `COMPLETED` thì cả 4 bước đều đã qua và không bước nào có `aria-current` (bản đầu còn gắn ở bước 4, sửa ở kiểm tra cả tuần). `CANCELLED` không vẽ thanh mà thay bằng hộp "Phiên đã hủy" ghi người hủy (Bạn, tên bên kia, hoặc Quản trị viên, theo `cancelledBy`), lúc hủy và lý do.
  - `InfoLine` và phần "chuỗi bắt đầu bằng `http://` hoặc `https://` thì thành link" chuyển từ `RequestsPage.tsx` sang `src/components/InfoLine.tsx` để S08 và S09 dùng chung. Như vậy luật không cho chuỗi `javascript:...` thành link chỉ nằm một chỗ.
  - Nút theo bảng 2.3 (D93, D94), chỉ xét trạng thái, `myCompleted`, `openReport`. `SCHEDULED`: Bắt đầu, Hủy phiên. `IN_PROGRESS`: Hoàn thành, Hủy phiên. `WAITING_CONFIRMATION` chưa có báo cáo mở: người chưa bấm thấy "Xác nhận hoàn thành"; cả hai thấy "Báo cáo sự cố" (D94: người đã bấm hoàn thành cũng gửi được). `WAITING_CONFIRMATION` có báo cáo mở: không còn nút nào. `COMPLETED`, `CANCELLED`: không có nút.
  - "Bắt đầu" bị khóa khi giờ máy còn trước `startAt` − 15 phút; đúng mốc thì mở, như D93. Chữ cạnh nút ghi "Mở lúc 18:45, T3 24/11/2026" (dùng `formatDateTime`). Kèm ngày vì mốc có thể không rơi vào hôm nay: TC360 dùng phiên hẹn sau 1 ngày. Trang đọc lại đồng hồ mỗi 30 giây, nên nếu để trang mở thì nút tự mở, không cần F5. Giờ máy lệch giờ máy chủ thì backend vẫn trả 409 "Chỉ bắt đầu được từ 15 phút trước giờ hẹn".
  - Dòng hướng dẫn ở chân thẻ: `SCHEDULED` "Một trong hai bạn bấm Bắt đầu khi buổi học diễn ra."; `IN_PROGRESS` "Học xong thì một trong hai bạn bấm Hoàn thành, bên kia xác nhận."; `WAITING_CONFIRMATION` người đã bấm thấy "Bạn đã bấm Hoàn thành, đang chờ {tên} xác nhận.", người chưa bấm thấy "{tên} đã bấm Hoàn thành. Bạn xác nhận buổi học đã diễn ra?" (wireframe). Có báo cáo mở thì thay bằng hộp thông báo "Bạn (hoặc {tên}) đã gửi báo cáo sự cố lúc ..., phiên đang chờ quản trị viên xử lý.", không có lý do (D92). `COMPLETED`: "Phiên đã hoàn thành."
  - "Bắt đầu", "Hoàn thành", "Xác nhận hoàn thành" bấm là gửi ngay. "Hủy phiên" và "Báo cáo sự cố" mở hộp thoại nhập lý do, dùng chung một component. Lý do bắt buộc do backend kiểm: gửi rỗng thì lỗi `reason` "Chưa nhập lý do" hiện dưới ô và hộp thoại vẫn mở; lỗi khác thì đóng hộp thoại, hiện ở hộp lỗi đầu trang. Ô lý do tối đa 1000 ký tự. Hộp thoại hủy ghi hệ quả theo D93: hủy khi đang lên lịch không tính vào tỉ lệ hoàn thành; hủy khi đang diễn ra thì tính cho cả hai bên. Hộp thoại báo cáo ghi chỉ quản trị viên đọc lý do (D92).
  - Thành công thì hiện hộp thông báo đầu trang: "Đã bắt đầu phiên.", "Đã báo hoàn thành.", "Phiên đã hoàn thành.", "Đã hủy phiên.", "Đã gửi báo cáo sự cố.". `start`, `complete`, `cancel` trả `SessionDetail` mới (D93), nên vẽ lại bằng chính response, không gọi lại `GET`. `reports` chỉ trả báo cáo nên phải tải lại phiên. Mọi lỗi của nút (409, 403, 404) hiện `detail` ở hộp lỗi đầu trang rồi tải lại phiên; trong lúc tải lại vẫn giữ phiên cũ trên màn hình. Đang gửi thì mọi nút bị khóa.
  - Tải trang lỗi (404 không có phiên, 403 không tham gia, 400 `{id}` không phải số) thì hiện `detail` kèm "Thử lại" (`LoadError`).
  - Chưa hiện `resolution` và `myReviewed`: tuần 6 chưa có dữ liệu thật; hai khóa này hiện cùng A02 và đánh giá ở tuần 7.
  - Menu: "Phiên" (`CalendarCheck`) đứng sau "Yêu cầu" (D81). Hai route nằm trong `RequireStudent`, nên admin gõ `/sessions/1` bị chuyển về `/admin/skills` như `/requests` (TC361). "Xem phiên" của S08 từ giờ mở đúng trang chi tiết.
- **Tự kiểm:** đi trọn luồng hai tài khoản: gửi → chấp nhận → cả hai thấy liên hệ → bắt đầu → một bên hoàn thành → bên kia báo cáo, rồi thử bấm xác nhận thì nhận 409. Khi làm: `npm run build` và `npm run lint` sạch. `src/sessionView.ts` chạy bằng Node (`TZ=Asia/Ho_Chi_Minh`), 19 ca: nút của từng ô trạng thái (riêng `WAITING_CONFIRMATION` tách theo đã bấm hay chưa bấm hoàn thành và có hay không có báo cáo mở), thanh tiến trình (`CANCELLED` không nằm trên thanh), mốc 15 phút (trước mốc 1 giây, đúng mốc, trễ 1 ngày), chữ "Mở lúc 18:45, T3 24/11/2026", hẹn 00:10 thì mốc rơi vào hôm trước, kỹ năng theo góc nhìn người gửi và người nhận. Làm hỏng rồi trả lại, cả 7 lần đều đỏ:
  - hiện "Báo cáo sự cố" cả khi đã có báo cáo mở → đỏ ở hai ca có báo cáo mở;
  - hiện "Xác nhận hoàn thành" cho cả người đã bấm → đỏ ở ca người đã bấm;
  - coi đúng mốc 15 phút là chưa tới (`>=` thành `>`) → đỏ ở ca đúng mốc;
  - cho hủy phiên `WAITING_CONFIRMATION` → đỏ ở cả bốn ca chờ xác nhận;
  - tính mốc bằng giây thay vì phút → đỏ ở bốn ca của mốc 15 phút;
  - đảo kỹ năng học/dạy của người nhận → đỏ ở hai ca người nhận;
  - phiên đã hủy vẫn ở bước cuối của thanh → đỏ ở ca `CANCELLED`.
  - Phần cần trình duyệt (TC360, TC361, 360 px, chế độ tối) người dùng tự làm. Ở TC360, bước "Bình gọi `complete` bằng tay" dùng yêu cầu "Bình xác nhận hoàn thành" của `hoc_vibe_coding/http/tuan_6.http` hoặc Console của trình duyệt.
- **Commit:** `feat(frontend): add sessions pages`

## 3. Đối chiếu ca kiểm thử

Đã đối chiếu 61 ca: mọi ca thuộc đúng một bước, trừ TC348 chia cho bước 7 và 8 (phần `reports` cần API của bước 8; chốt ở bước 7).

| Bước | Ca |
| --- | --- |
| 1 | TC301–TC305, TC307 (thủ công) |
| 2 | TC306 (thủ công) |
| 3 | TC308–TC310 (thủ công) |
| 4 | – |
| 5 | TC311–TC325 |
| 6 | TC326–TC337 |
| 7 | TC338–TC350 (TC348 trừ phần `reports`), TC355 |
| 8 | TC351–TC354, phần `reports` của TC348 |
| 9 | TC356–TC358 (thủ công) |
| 10 | TC359 (thủ công) |
| 11 | TC360, TC361 (thủ công) |

## 4. Sau bước 11 – Kiểm tra cả tuần

- `.\mvnw.cmd test` xanh toàn bộ; `npm run build`, `npm run lint` không lỗi.
- Lõi `matching/` không đổi so với cuối tuần 5.
- Truy vết ca → test; làm hỏng code để chắc test đỏ (các mục "Tự kiểm").
- Đối chiếu code với D78 trở đi; viết `docs/tuan_6/kiem_thu/README.md`.
- Nội dung báo cáo tuần 6 có mục "Điều chỉnh so với báo cáo trước": giao diện mới (D78–D85) và các quy tắc đợt 4 làm đổi spec (R33, R34, Q1).
