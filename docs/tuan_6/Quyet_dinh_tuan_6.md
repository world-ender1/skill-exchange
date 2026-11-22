# QUYẾT ĐỊNH TRIỂN KHAI – TUẦN 6

Chốt ngày 2026-11-18. File này là tài liệu mới nhất cho phạm vi tuần 6. Nếu khác spec tuần 1, thiết kế tuần 2 hoặc quyết định tuần 3–5 thì file này đúng. Khi thêm hoặc đổi một quyết định ở đây, phải sửa luôn các tài liệu cũ nhắc tới điểm đó trong cùng commit. Mã quyết định đánh số tiếp theo tuần 5 (D78 trở đi).

Kế hoạch thực hiện: `docs/tuan_6/ke_hoach_tuan_6.md`.

## 1. Giao diện frontend

D79–D85 do người dùng duyệt ngày 2026-11-18, theo đề xuất dựng bằng bộ skill `ui-ux-pro-max` (đã lọc: bỏ Claymorphism, bố cục "Funnel", font Poppins vì thiếu bộ ký tự tiếng Việt, và GSAP).

| Mã | Quyết định |
| --- | --- |
| D78 | **Thư viện giao diện.** Bỏ cách "giao diện CSS viết tay, không thêm thư viện UI" của kế hoạch tuần 3 (bước 10). Frontend dùng **Tailwind CSS 4** (gói `tailwindcss` và plugin `@tailwindcss/vite`; lúc chốt là bản 4.3.5, plugin hỗ trợ Vite 8 dự án đang dùng) và **shadcn/ui**. Component của shadcn/ui sinh bằng CLI `shadcn` (lúc chốt là bản 4.23.0) và nằm trong `frontend/src/components/ui/` như code của dự án, không phải gói npm cài vào. Icon dùng `lucide-react`, bộ icon mà các component shadcn/ui import sẵn. **D23 giữ nguyên:** state của form vẫn viết tay bằng `useState`; dùng component nhập liệu của shadcn/ui (Input, Select, Textarea, Checkbox, Button...) nhưng không thêm `react-hook-form`, `zod` hay component `Form` của shadcn/ui. **Chỉ đổi phần trình bày:** không đổi đường dẫn, lời gọi API, cách hiện lỗi từ backend (D24: `detail` và lỗi theo từng ô), luật dữ liệu riêng tư (NFR-02) và luật lấy nguyên điểm từ API (NFR-09); màn hình rộng 360 px vẫn không cuộn ngang (NFR-08); giữ các thuộc tính trợ năng các ca kiểm thử đang dựa vào (`role="tab"` + `aria-selected`, `aria-label` của ô lịch rảnh, `role="alert"` của hộp lỗi). **Thứ tự làm:** cài Tailwind là phần đặt lại kiểu mặc định (Preflight) áp cho cả trang: tiêu đề `h1`/`h2` mất cỡ chữ riêng, danh sách mất dấu đầu dòng, nút mất viền. Vì vậy các màn hình đã có (S01–S06, S10, A01) chuyển hết sang thư viện mới trong cùng đợt cài, trước khi làm S07–S09; S07–S09 viết thẳng bằng thư viện mới. |
| D79 | **Màu.** Màu xám theo bảng `slate` của Tailwind 4, viết tay trong biến màu (CLI shadcn 4.23 không có lựa chọn `slate`, xem D83): nền trang `slate-50`, thẻ trắng (tối: nền `slate-950`, thẻ `slate-900`). Màu chính là chàm: `#4F46E5` (indigo-600 của Tailwind 3; Tailwind 4 đổi bảng màu sang OKLCH nên ghi theo mã hex) ở chế độ sáng, chữ trên nút màu trắng (tương phản 6.3:1); `#818CF8` (indigo-400 của Tailwind 3) ở chế độ tối, chữ trên nút `#0F172A` (slate-900). Màu trạng thái lấy từ bảng màu Tailwind 4: thành công `green`, đang chờ và sao đánh giá `amber`, lỗi `red`. Chữ trạng thái ở chế độ sáng dùng sắc độ 800 (riêng `red` dùng 700), ở chế độ tối dùng sắc độ 400 (riêng `red` dùng 300). Bản đầu dùng 700 (`red` 600) và `red-400`: đạt trên nền trắng nhưng chữ màu trên nền nhạt cùng màu của `Badge` và nút `destructive` chỉ còn 3.82–4.37:1 (đo ở kiểm tra cả tuần, TC302), nên đổi đậm thêm một bậc. Chữ phụ: `slate-600` (sáng), `slate-400` (tối); viền ô nhập `slate-500` ở cả hai chế độ (đo khi làm bước 1: `slate-500`, `slate-400`, `slate-600` ở các vị trí này không đủ tương phản). Mọi cặp chữ/nền đạt tối thiểu 4.5:1, kể cả chữ màu trên nền nhạt cùng màu; icon và viền ô nhập đạt 3:1 (WCAG AA). Màu khai báo thành biến ngữ nghĩa theo cách của shadcn/ui (`--primary`, `--destructive`, thêm `--success`, `--warning`); component không ghi mã màu trực tiếp. Không chọn xanh ngọc vì dễ lẫn với xanh lá "hoàn thành" ở trạng thái phiên. |
| D80 | **Font.** Tiêu đề (`h1`–`h3`) dùng Be Vietnam Pro (độ đậm 600, 700); nội dung, nút, ô nhập và con số dùng Inter (bản variable). Cả hai có bộ ký tự tiếng Việt. Font tự chứa bằng gói `@fontsource/be-vietnam-pro` và `@fontsource-variable/inter` (lúc chốt cùng bản 5.3.0), không tải từ Google Fonts, để chạy được khi không có mạng (buổi demo). Con số trong bảng điểm và bộ đếm dùng `tabular-nums`. |
| D81 | **Khung trang** dùng component `Sidebar` của shadcn/ui: từ 768 px là thanh bên; dưới 768 px là thanh trên cùng có nút mở menu dạng `Sheet` (cách `Sidebar` tự làm trên màn hẹp). Mục menu kèm icon lucide. Sinh viên: Hồ sơ (`User`), Kỹ năng (`BookOpen`), Gợi ý (`Sparkles`), Tìm kiếm (`Search`); Yêu cầu (`Inbox`) và Phiên (`CalendarCheck`) thêm khi có S08, S09. Admin: Danh mục kỹ năng (`Tags`). "Đăng xuất" ở cuối menu. `Layout` vẫn là route cha, đọc vai trò từ `/api/me` một lần và truyền xuống bằng `<Outlet context>` (D76). |
| D82 | **Chế độ tối** theo cài đặt hệ thống (`prefers-color-scheme`), không có nút chuyển, như hiện nay. Chỉ dùng CSS: biến màu tối đặt trong `@media (prefers-color-scheme: dark)`, biến thể `dark:` của Tailwind khai báo lại bằng `@custom-variant dark (@media (prefers-color-scheme: dark))`, thay cho lớp `.dark` mà shadcn/ui dùng mặc định. Không có JavaScript đọc hay lưu chế độ. |
| D83 | **Khởi tạo shadcn/ui:** thư viện nền Radix UI; các tùy chọn khác lấy preset mặc định của CLI (mã `b0`: kiểu `radix-nova`, màu nền `neutral`, icon `lucide`, font Inter) và được lưu trong `frontend/components.json` (có commit). CLI 4.23 không có màu nền `slate` (registry chỉ nhận `neutral`, `stone`, `zinc`, `mauve`, `olive`, `mist`, `taupe`), nên màu `slate` của D79 viết tay trong biến màu ở `index.css`, ghi đè các biến CLI sinh ra. Đường dẫn tắt `@/` trỏ tới `frontend/src/` (CLI cần), khai báo ở `tsconfig` và `vite.config.ts`. |
| D84 | **Lưới lịch rảnh.** Từ 640 px: 7 cột ngày × 3 hàng buổi như hiện nay. Dưới 640 px: xoay thành 7 hàng ngày × 3 cột buổi, không cuộn ngang. Mỗi ô là `Checkbox` của shadcn/ui (vẫn có `role="checkbox"`), giữ `aria-label` dạng "<Buổi> <Ngày>" như hiện nay (ví dụ "Sáng Thứ Hai"); cả ô bấm được, vùng bấm tối thiểu 44 × 44 px. S06 dùng lại lưới này ở chế độ chỉ đọc, thay cho dòng chữ liệt kê. |
| D85 | **Thông báo** vẫn hiện ngay trong thẻ hoặc form vừa thao tác, bằng `Alert` của shadcn/ui (thêm kiểu `success`), không dùng thông báo nổi (toast). Component `Notice` giữ nguyên tham số (`kind`: `error`, `success`, `info`), giữ `role="alert"` cho lỗi và `role="status"` cho loại khác. Component `Field` giữ nguyên tham số (`label`, `hint`, `error`), bên trong dùng `Field`, `FieldLabel`, `FieldDescription`, `FieldError` của shadcn/ui. Nhờ vậy chỗ gọi ở các trang không đổi. |

## 2. Yêu cầu trao đổi, phiên, báo cáo sự cố

Chốt ngày 2026-11-18 ở Bước 0 của kế hoạch tuần 6. Nguồn: đợt 4 của rà soát (R33–R39; người dùng duyệt "theo gợi ý" ngày 2026-10-29) và Q1–Q11 của kế hoạch (người dùng duyệt "theo gợi ý" ngày 2026-11-18). Q1 thay phương án 2 của R35 bằng một phương án giữ NFR-02. Gợi ý chọn theo tiêu chí "ít ảnh hưởng nhất tới việc đã làm", như tuần 5.

| Mã | Quyết định |
| --- | --- |
| D86 | **Phạm vi tuần 6 (Q9, Q11; spec 9).** Làm: FR-05, FR-06, quyền xem liên hệ và địa điểm/link, FR-09 phần người dùng gửi báo cáo; API TK 4.6, 4.7 và `POST /api/sessions/{id}/reports`; màn hình S07–S09. Không làm: đánh giá (FR-07), admin xử lý báo cáo (A02), khóa tài khoản (FR-08), số yêu cầu đang chờ trên menu (FR-13, Nên có). Admin gọi `/api/requests/**`, `/api/sessions/**` nhận 403 ở tầng filter như mọi API quyền *Active* (D44); quyền admin xem chi tiết phiên ("tra cứu vi phạm") làm cùng A02 ở tuần 7. R40, R41 (phần tài khoản), R44, R45, R52–R54 dời sang đợt 4b của rà soát, chốt trước tuần 7. |
| D87 | **Quyền và thứ tự kiểm.** Mọi API ở `/api/requests/**`, `/api/sessions/**` cần quyền `ACTIVE_STUDENT` (D44). Thứ tự: 401 → 403 ở filter (D44) → 400 (tham số, body) → 404 (tài nguyên theo `{id}`) → 403 (không tham gia, hoặc sai vai trò người gửi/người nhận) → 409 (trạng thái, thời gian, trùng lặp). `{id}` không phải số trả 400 (D38). Vì 403 vai trò đứng trước 409, người gửi bấm "chấp nhận" yêu cầu đã `ACCEPTED` nhận 403, không phải 409. Câu `detail` ở bảng 2.1. |
| D88 | **Thời gian và chuỗi.** Mọi mốc thời gian mới (`created_at`, `responded_at` của yêu cầu; `created_at`, `started_at`, `completed_at`, `cancelled_at` của phiên; `created_at` của báo cáo) lấy từ bean `Clock`, mở rộng D18, để test giả được giờ. Ô thời điểm trong body nhận chuỗi ISO-8601 có độ lệch múi giờ, ví dụ `2026-11-24T19:00:00+07:00` (như ví dụ TK 4.6). Response ghi thời điểm dạng `Instant` của Jackson (ISO-8601 giờ UTC, ví dụ `2026-11-24T12:00:00Z`). Chuỗi tự do (`locationOrLink`, `message`, `note`, `reason`) cắt khoảng trắng hai đầu bằng `strip()` như `fullName` (D16), độ dài đếm sau khi cắt. `message`, `note`: tối đa 1000 ký tự, rỗng sau khi cắt thì lưu `NULL`. `reason` (hủy phiên, báo cáo): bắt buộc, tối đa 1000 ký tự (Q4); gửi `cancel`, `reports` không có body là JSON không đọc được (400 "Nội dung JSON không đọc được", không có `errors`), `{}` mới ra lỗi theo trường `reason`. |
| D89 | **Tạo yêu cầu `POST /api/requests` (R36, Q3, Q5, Q6; TK 4.6).** Body `{receiverId, type, senderLearnSkillId, receiverLearnSkillId, proposedStartAt, durationMinutes, mode, locationOrLink, message}`. Kiểm theo thứ tự, gặp lỗi đầu tiên thì dừng, trừ bước (1): (1) Bean Validation trả cùng lúc mọi lỗi "bắt buộc, khoảng giá trị, độ dài" ở dạng `errors[]`; (2) `receiverLearnSkillId` phải có khi `TWO_WAY`, phải bỏ trống khi `ONE_WAY`; (3) `proposedStartAt` phải sau thời điểm hiện tại (bằng thì sai), không có giới hạn trên; (4) gửi cho chính mình thì 400; (5) người nhận không có, không `ACTIVE` hoặc không `STUDENT` thì 404 "Không tìm thấy người dùng", cùng câu với `GET /api/users/{id}` để không lộ trạng thái tài khoản; (6) `senderLearnSkillId` phải thuộc danh sách muốn học của người gửi, `receiverLearnSkillId` phải thuộc danh sách muốn học của người nhận; (7) $Sim > 0$ giữa `senderLearnSkillId` và một kỹ năng dạy của người nhận, và với `TWO_WAY` giữa `receiverLearnSkillId` và một kỹ năng dạy của người gửi ($Sim$ của spec 4.1, tính bằng `SkillSimilarity` của lõi); (8) đã có yêu cầu `PENDING` cùng chiều thì 409, chiều ngược lại vẫn gửi được. `mode` chỉ nhận `ONLINE`, `OFFLINE`; `ANY` hay giá trị lạ khác làm JSON không đọc được (400, cách xử lý hiện có). `locationOrLink` bắt buộc, tối đa 500 ký tự (Q3). Kỹ năng đã ẩn mà còn trong danh sách học vẫn dùng được (như D75). **Không kiểm** hình thức của yêu cầu so với hình thức học của người nhận (Q5), lịch rảnh có trùng không, và giờ có chồng với yêu cầu, phiên khác không (Q6). Thành công: 201, body `RequestItem` (bảng 2.2), yêu cầu ở `PENDING`, `responded_at` NULL. |
| D90 | **Phản hồi yêu cầu (TK 5.1; R34, R37, R39).** `accept` (chỉ người nhận): 409 nếu yêu cầu không còn `PENDING`; 409 nếu thời điểm hiện tại ≥ `proposed_start_at` (R34); 409 nếu người gửi không còn `ACTIVE` (R37); xét theo đúng thứ tự này. Thành công trong **một transaction**: yêu cầu → `ACCEPTED`, `responded_at` = bây giờ; tạo phiên `SCHEDULED` với `start_at = proposed_start_at`, thời lượng, hình thức, địa điểm/link chép từ yêu cầu, `created_at` = bây giờ. Trả 200 `{requestId, sessionId}` (R38). `reject` (chỉ người nhận): body `{note}` không bắt buộc, gửi không có body cũng được; → `REJECTED`, lưu `response_note`, `responded_at`; trả 200 `RequestItem`. `cancel` (chỉ người gửi): → `CANCELLED`, ghi `responded_at`; trả 200 `RequestItem`. **`responded_at`** (R39, Q2): NULL khi `PENDING`, có giá trị ở mọi trạng thái khác; service bảo đảm, DB không thêm CHECK (test tuần 4 `MatchesApiTest` chèn yêu cầu `ACCEPTED` không có `responded_at`). **Không tự hết hạn** (spec 6.2, R34): yêu cầu `PENDING` đã quá giờ đề xuất vẫn `PENDING`, chỉ không chấp nhận được; người gửi vẫn hủy được, người nhận vẫn từ chối được. **Khóa lạc quan** (TK 5.1): hai thao tác chạy song song trên cùng một yêu cầu, thao tác ghi sau thất bại vì `version` → 409; thao tác đọc sau khi thao tác kia đã commit thì 409 theo trạng thái. Không bao giờ có yêu cầu `CANCELLED` hay `REJECTED` mà có phiên. |
| D91 | **Danh sách và chi tiết yêu cầu (R35, R38, Q1).** `GET /api/requests?box=sent\|received&status=`: `box` bắt buộc; `status` không bắt buộc, ∈ {`PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED`}; thiếu hoặc sai giá trị thì 400 (cách xử lý tham số hiện có, D38). Trả mảng `RequestItem` xếp `created_at` giảm dần rồi `id` giảm dần, chưa phân trang. `GET /api/requests/{id}`: chỉ người gửi và người nhận. **Địa điểm/link (Q1):** người gửi luôn thấy địa điểm/link do mình nhập; người nhận chỉ thấy khi yêu cầu `ACCEPTED`, các trạng thái khác nhận `null`. Đây là phương án thay cho phương án 2 của R35, để giữ NFR-02 ("chỉ hiển thị cho hai bên từ khi yêu cầu được chấp nhận"); việc người nhập xem lại dữ liệu của chính mình giống `/api/me` trả số điện thoại của chính mình. Người nhận không hợp địa điểm thì hủy phiên `SCHEDULED`, không bị trừ CompletionRate. API yêu cầu **không bao giờ** trả `phone`, `contactInfo`, email; liên hệ chỉ có ở chi tiết phiên (D92). `sessionId` có khi `ACCEPTED`, còn lại `null`. |
| D92 | **Danh sách và chi tiết phiên (R35, R38; NFR-02).** `GET /api/sessions?status=`: `status` không bắt buộc, ∈ 5 trạng thái phiên, sai thì 400. Trả mảng `SessionItem` (bảng 2.2) các phiên mình tham gia, xếp `created_at` giảm dần rồi `id` giảm dần, chưa phân trang; danh sách không có địa điểm/link và liên hệ. `GET /api/sessions/{id}`: chỉ hai người tham gia; trả `SessionDetail` gồm địa điểm/link và `phone`, `contactInfo` hiện tại trong hồ sơ của bên kia (`null` nếu chưa khai), ở mọi trạng thái phiên, kể cả sau khi phiên kết thúc (spec 6.3). `myCompleted`: mình là `first_completed_by`. `cancelledBy`: `ME`, `PARTNER`, `ADMIN` hoặc `null`. `myReviewed`: đã có dòng `reviews` do mình viết (tuần 6 chỉ có khi chèn tay). `openReport`: báo cáo `OPEN` của phiên, chỉ gồm `id`, `reportedByMe`, `createdAt`; không trả lý do, vì lý do dành cho admin (A02). `resolution`: kết quả của báo cáo `RESOLVED` mới nhất theo `resolved_at` (tuần 7 mới có dữ liệu thật). |
| D93 | **Chuyển trạng thái phiên (TK 5.2; R34, R37).** `start` (một trong hai người): phiên phải `SCHEDULED`; thời điểm hiện tại ≥ `start_at` − 15 phút (đúng mốc thì được), không có cận trên; → `IN_PROGRESS`, `started_at` = bây giờ. `complete`: từ `IN_PROGRESS` → `WAITING_CONFIRMATION`, `first_completed_by` = người gọi; từ `WAITING_CONFIRMATION`: người gọi là `first_completed_by` thì 409, phiên có báo cáo `OPEN` thì 409 (R33), xét theo thứ tự này, còn lại → `COMPLETED`, `completed_at` = bây giờ; trạng thái khác thì 409. `cancel`: body `{reason}` bắt buộc (D88); từ `SCHEDULED` (ở mọi thời điểm, kể cả sau giờ hẹn, R34) hoặc `IN_PROGRESS` → `CANCELLED`, ghi `cancelled_by`, `cancel_reason`, `cancelled_at`, giữ nguyên `started_at` (hủy từ `IN_PROGRESS` được tính vào mẫu số CompletionRate của cả hai bên; hủy từ `SCHEDULED` thì không); từ `WAITING_CONFIRMATION`, `COMPLETED`, `CANCELLED` thì 409. Ba thao tác trả 200 `SessionDetail` mới. Phiên `SCHEDULED` không tự hết hạn (R34). Thao tác trên phiên không xét trạng thái tài khoản của bên kia (R37). Mọi thao tác bị từ chối không đổi dữ liệu, kể cả `version` (spec 7.1 nhóm 5). Khóa lạc quan như D90: hai người cùng bấm "Hoàn thành" trên phiên `IN_PROGRESS` thì kết quả chỉ có thể là (một 200, một 409, phiên `WAITING_CONFIRMATION`) hoặc (hai 200 nối tiếp, phiên `COMPLETED`), không bao giờ hai 200 mà phiên vẫn `WAITING_CONFIRMATION`. |
| D94 | **Gửi báo cáo sự cố `POST /api/sessions/{id}/reports` (R33; FR-09 phần người dùng).** Body `{reason}` (D88). Sau 400, 404, 403 của D87: phiên không ở `WAITING_CONFIRMATION` thì 409; phiên đã có báo cáo `OPEN` (do ai gửi cũng vậy) thì 409; lỗi unique từ DB khi gửi song song cũng ra 409 (lưới D24). Cả hai người tham gia đều gửi được, kể cả người đã bấm hoàn thành. Thành công: 201 `{id, sessionId, status: "OPEN", createdAt}`. **Gửi báo cáo ép tăng `version` của phiên** (khóa lạc quan như D90, D93; chốt ở bước 8 của kế hoạch): gửi báo cáo và xác nhận hoàn thành chạy song song thì lệnh ghi sau nhận 409 "Dữ liệu vừa được thay đổi, hãy tải lại", nên không bao giờ có phiên `COMPLETED` mà còn báo cáo `OPEN`. Kiểm báo cáo mở trong cùng transaction không đủ, vì ở mức cô lập READ COMMITTED lệnh `complete` không thấy báo cáo chưa commit. Khi có báo cáo `OPEN`, bên còn lại không xác nhận hoàn thành được (D93) và không ai hủy được (phiên đang `WAITING_CONFIRMATION`); chỉ admin chốt (tuần 7). |
| D95 | **Giao diện S07–S09 (Q7, Q8, Q10; TK 6.1, 6.3).** **S07** ở `/users/:id/request`, mở từ nút "Gửi yêu cầu" trên thẻ S05 (ẩn khi `hasPendingRequest`, nhãn "Đã gửi yêu cầu" vẫn hiện) và trên S06 (luôn hiện, trừ khi xem hồ sơ của chính mình; gửi trùng thì S07 hiện lỗi 409). Loại mặc định `ONE_WAY`; mở từ tab "Trao đổi hai chiều" của S05 thì truyền `?type=TWO_WAY` và chọn sẵn `TWO_WAY`. Ô "Bạn muốn học" lấy từ `GET /api/me/learn-skills`; ô "Bạn sẽ dạy" (chỉ `TWO_WAY`) lấy danh sách muốn học trong `GET /api/users/{id}`; $Sim > 0$ do backend kiểm. Khung trùng = giao của `GET /api/me/availability` và lịch rảnh trong `GET /api/users/{id}`; bấm một khung thì điền ngày gần nhất có thứ đó mà giờ của buổi (Sáng 08:00, Chiều 14:00, Tối 19:00, giờ máy người dùng) còn ở tương lai, người dùng sửa được. Dưới ô địa điểm/link ghi "Người nhận chỉ thấy sau khi chấp nhận". Gửi xong chuyển tới `/requests?box=sent`. **S08** ở `/requests`, hai tab theo tham số `box` (mặc định `received`), không có trang chi tiết yêu cầu riêng; yêu cầu `ACCEPTED` có nút "Xem phiên" tới `/sessions/{sessionId}`. **S09** ở `/sessions` (danh sách) và `/sessions/:id` (chi tiết). Menu sinh viên thêm "Yêu cầu", "Phiên" (D81), không có số yêu cầu đang chờ. |
| D96 | **Hạn chế ghi vào báo cáo.** Không chặn một người có hai yêu cầu hay phiên chồng giờ nhau (Q6). Yêu cầu `PENDING` và phiên `SCHEDULED` quá giờ không tự hết hạn (R34). Khóa tài khoản không tự hủy yêu cầu, phiên dở dang của người đó (R37). |

### 2.1. Câu báo lỗi

Lỗi theo trường dùng dạng `errors: [{field, message}]` với `detail` "Dữ liệu không hợp lệ" (D24). Các lỗi khác chỉ có `detail`.

| API | Tình huống | Mã | `detail` hoặc `errors[]` |
| --- | --- | --- | --- |
| Tạo yêu cầu | Thiếu `receiverId` | 400 | `receiverId`: "Chưa chọn người nhận" |
| Tạo yêu cầu | Thiếu `type` | 400 | `type`: "Chưa chọn loại yêu cầu" |
| Tạo yêu cầu | Thiếu `senderLearnSkillId` | 400 | `senderLearnSkillId`: "Chưa chọn kỹ năng bạn muốn học" |
| Tạo yêu cầu | `TWO_WAY` thiếu `receiverLearnSkillId` | 400 | `receiverLearnSkillId`: "Yêu cầu hai chiều cần kỹ năng người nhận muốn học" |
| Tạo yêu cầu | `ONE_WAY` có `receiverLearnSkillId` | 400 | `receiverLearnSkillId`: "Yêu cầu một chiều không có kỹ năng người nhận muốn học" |
| Tạo yêu cầu | Thiếu `proposedStartAt` | 400 | `proposedStartAt`: "Chưa chọn thời điểm" |
| Tạo yêu cầu | `proposedStartAt` không sau hiện tại | 400 | `proposedStartAt`: "Thời điểm phải ở tương lai" |
| Tạo yêu cầu | Thiếu `durationMinutes` hoặc ngoài 15–480 | 400 | `durationMinutes`: "Thời lượng từ 15 đến 480 phút" |
| Tạo yêu cầu | Thiếu `mode` | 400 | `mode`: "Chưa chọn hình thức" |
| Tạo yêu cầu | `locationOrLink` thiếu hoặc rỗng sau khi cắt | 400 | `locationOrLink`: "Chưa nhập địa điểm hoặc link" |
| Tạo yêu cầu | `locationOrLink` dài hơn 500 | 400 | `locationOrLink`: "Địa điểm hoặc link dài tối đa 500 ký tự" |
| Tạo yêu cầu | `message` dài hơn 1000 | 400 | `message`: "Lời nhắn dài tối đa 1000 ký tự" |
| Tạo yêu cầu | Gửi cho chính mình | 400 | "Không thể gửi yêu cầu cho chính mình" |
| Tạo yêu cầu | Người nhận không có, không `ACTIVE`, không `STUDENT` | 404 | "Không tìm thấy người dùng" |
| Tạo yêu cầu | `senderLearnSkillId` không thuộc danh sách muốn học của người gửi | 400 | `senderLearnSkillId`: "Kỹ năng này không có trong danh sách muốn học của bạn" |
| Tạo yêu cầu | `receiverLearnSkillId` không thuộc danh sách muốn học của người nhận | 400 | `receiverLearnSkillId`: "Kỹ năng này không có trong danh sách muốn học của người nhận" |
| Tạo yêu cầu | $Sim = 0$ ở chiều người gửi học | 400 | `senderLearnSkillId`: "Người nhận không dạy kỹ năng này hay kỹ năng liên quan" |
| Tạo yêu cầu | $Sim = 0$ ở chiều người nhận học | 400 | `receiverLearnSkillId`: "Bạn không dạy kỹ năng này hay kỹ năng liên quan" |
| Tạo yêu cầu | Đã có yêu cầu `PENDING` cùng chiều | 409 | "Bạn đã có một yêu cầu đang chờ gửi tới người này" |
| Tạo yêu cầu | Hai yêu cầu cùng chiều gửi đúng một lúc, cùng qua bước kiểm trên; chỉ mục `uq_requests_one_pending` chặn cái sau | 409 | "Dữ liệu xung đột với dữ liệu đã có" (lưới D24) |
| Yêu cầu `{id}` | Không có | 404 | "Không tìm thấy yêu cầu" |
| Yêu cầu `{id}` | Người gọi không phải người gửi hay người nhận | 403 | "Bạn không tham gia yêu cầu này" |
| `accept`, `reject` | Người gọi là người gửi | 403 | "Chỉ người nhận được chấp nhận hoặc từ chối yêu cầu" |
| `cancel` | Người gọi là người nhận | 403 | "Chỉ người gửi được hủy yêu cầu" |
| `accept`, `reject`, `cancel` | Yêu cầu không còn `PENDING` | 409 | "Yêu cầu không còn ở trạng thái chờ" |
| `accept` | Đã tới thời điểm đề xuất | 409 | "Đã tới thời điểm đề xuất, không thể chấp nhận yêu cầu" |
| `accept` | Người gửi không còn `ACTIVE` | 409 | "Người gửi không còn hoạt động" |
| `reject` | `note` dài hơn 1000 | 400 | `note`: "Ghi chú dài tối đa 1000 ký tự" |
| Phiên `{id}` | Không có | 404 | "Không tìm thấy phiên" |
| Phiên `{id}` | Người gọi không tham gia | 403 | "Bạn không tham gia phiên này" |
| `start` | Phiên không `SCHEDULED` | 409 | "Chỉ bắt đầu được phiên đã lên lịch" |
| `start` | Sớm hơn `start_at` − 15 phút | 409 | "Chỉ bắt đầu được từ 15 phút trước giờ hẹn" |
| `complete` | Phiên không `IN_PROGRESS`, không `WAITING_CONFIRMATION` | 409 | "Phiên không ở trạng thái xác nhận hoàn thành được" |
| `complete` | Người gọi đã bấm hoàn thành | 409 | "Bạn đã xác nhận hoàn thành, đang chờ bên kia" |
| `complete` | Phiên có báo cáo `OPEN` | 409 | "Phiên đang có báo cáo sự cố chờ quản trị viên xử lý" |
| `cancel` | Phiên `WAITING_CONFIRMATION` | 409 | "Phiên đang chờ xác nhận, không hủy được; hãy gửi báo cáo sự cố nếu có bất đồng" |
| `cancel` | Phiên `COMPLETED` hoặc `CANCELLED` | 409 | "Phiên đã kết thúc" |
| `cancel`, `reports` | Không có body | 400 | "Nội dung JSON không đọc được" (không có `errors`, ⚠2 của ca kiểm thử) |
| `cancel`, `reports` | `reason` thiếu hoặc rỗng sau khi cắt | 400 | `reason`: "Chưa nhập lý do" |
| `cancel`, `reports` | `reason` dài hơn 1000 | 400 | `reason`: "Lý do dài tối đa 1000 ký tự" |
| `reports` | Phiên không `WAITING_CONFIRMATION` | 409 | "Chỉ gửi được báo cáo khi phiên đang chờ xác nhận hoàn thành" |
| `reports` | Phiên đã có báo cáo `OPEN` | 409 | "Phiên đã có báo cáo sự cố đang chờ xử lý" |
| `reports` | Hai báo cáo gửi đúng một lúc, cùng qua bước kiểm trên; chỉ mục `uq_reports_one_open` chặn cái sau | 409 | "Dữ liệu xung đột với dữ liệu đã có" (lưới D24) |
| Mọi thao tác đổi trạng thái, `reports` | Thất bại vì khóa lạc quan | 409 | "Dữ liệu vừa được thay đổi, hãy tải lại" |

### 2.2. Dạng dữ liệu trả về

Mọi khóa luôn có mặt; không có giá trị thì là `null`. `myRole` ∈ {`SENDER`, `RECEIVER`} theo người gọi. Tên kỹ năng lấy cả kỹ năng đã ẩn.

`RequestItem` (D89–D91):

```json
{
  "id": 41, "type": "TWO_WAY", "status": "PENDING", "myRole": "SENDER",
  "sender": {"id": 7, "fullName": "Nguyễn An"},
  "receiver": {"id": 17, "fullName": "Trần Bình"},
  "senderLearnSkill": {"id": 1, "name": "Java"},
  "receiverLearnSkill": {"id": 15, "name": "Tiếng Anh"},
  "proposedStartAt": "2026-11-24T12:00:00Z", "durationMinutes": 90, "mode": "ONLINE",
  "locationOrLink": "https://meet.google.com/abc-defg-hij",
  "message": "Mình học Java tối thứ Ba được không?", "responseNote": null,
  "createdAt": "2026-11-18T08:00:00Z", "respondedAt": null, "sessionId": null
}
```

`accept` trả `{"requestId": 41, "sessionId": 12}`.

`SessionItem` (D92), phần tử của `GET /api/sessions`:

```json
{
  "id": 12, "requestId": 41, "type": "TWO_WAY", "status": "SCHEDULED", "myRole": "RECEIVER",
  "partner": {"id": 7, "fullName": "Nguyễn An"},
  "senderLearnSkill": {"id": 1, "name": "Java"},
  "receiverLearnSkill": {"id": 15, "name": "Tiếng Anh"},
  "startAt": "2026-11-24T12:00:00Z", "durationMinutes": 90, "mode": "ONLINE",
  "createdAt": "2026-11-18T09:00:00Z"
}
```

`SessionDetail` (D92, D93) = các khóa của `SessionItem`, trong đó `partner` có thêm `phone`, `contactInfo`, cộng các khóa sau:

```json
{
  "partner": {"id": 7, "fullName": "Nguyễn An", "phone": "0901234567", "contactInfo": "Zalo: 0901234567"},
  "locationOrLink": "https://meet.google.com/abc-defg-hij",
  "startedAt": null, "completedAt": null, "myCompleted": false,
  "cancelledBy": null, "cancelReason": null, "cancelledAt": null,
  "myReviewed": false,
  "openReport": null,
  "resolution": null
}
```

`openReport` khi có: `{"id": 3, "reportedByMe": true, "createdAt": "..."}`. `resolution` khi có: `{"result": "SESSION_CANCELLED", "adminNote": "...", "resolvedAt": "..."}`. Báo cáo vừa tạo (D94): `{"id": 3, "sessionId": 12, "status": "OPEN", "createdAt": "..."}`.

### 2.3. Bảng trạng thái × hành động

Tổng hợp từ D87, D90, D93, D94; thay cho phụ lục của rà soát. Admin và tài khoản không `ACTIVE` nhận 403 ở filter với mọi ô (D44, D86).

**Yêu cầu.** Gửi = người gửi, Nhận = người nhận, Khác = sinh viên không tham gia.

| Trạng thái | `accept` | `reject` | `cancel` | `GET /requests/{id}` |
| --- | --- | --- | --- | --- |
| `PENDING` | Nhận: → `ACCEPTED` + phiên `SCHEDULED` cùng transaction; đã tới giờ đề xuất hoặc người gửi không `ACTIVE`: 409. Gửi, Khác: 403 | Nhận: → `REJECTED`. Gửi, Khác: 403 | Gửi: → `CANCELLED`. Nhận, Khác: 403 | Gửi: 200, có địa điểm/link. Nhận: 200, địa điểm/link `null`. Khác: 403 |
| `ACCEPTED` | Nhận: 409. Gửi, Khác: 403 | Nhận: 409. Gửi, Khác: 403 | Gửi: 409. Nhận, Khác: 403 | Gửi, Nhận: 200, có địa điểm/link và `sessionId`. Khác: 403 |
| `REJECTED`, `CANCELLED` | Nhận: 409. Gửi, Khác: 403 | Nhận: 409. Gửi, Khác: 403 | Gửi: 409. Nhận, Khác: 403 | Gửi: 200, có địa điểm/link. Nhận: 200, địa điểm/link `null`. Khác: 403 |

**Phiên.** Hai người tham gia làm được như nhau trừ ô ghi rõ; người không tham gia nhận 403 ở mọi cột.

| Trạng thái | `start` | `complete` | `cancel` (kèm lý do) | `reports` |
| --- | --- | --- | --- | --- |
| `SCHEDULED` | → `IN_PROGRESS` từ `start_at` − 15 phút, không cận trên; sớm hơn: 409 | 409 | → `CANCELLED` ở mọi thời điểm; `started_at` NULL nên không tính CompletionRate | 409 |
| `IN_PROGRESS` | 409 | → `WAITING_CONFIRMATION`, ghi `first_completed_by` | → `CANCELLED`, giữ `started_at`, tính CompletionRate của cả hai bên | 409 |
| `WAITING_CONFIRMATION` | 409 | Bên còn lại → `COMPLETED`. Người đã bấm: 409. Có báo cáo `OPEN`: 409 | 409 | → báo cáo `OPEN`; đã có báo cáo `OPEN`: 409 |
| `COMPLETED` | 409 | 409 | 409 | 409 |
| `CANCELLED` | 409 | 409 | 409 | 409 |

Đánh giá (`COMPLETED`) và admin chốt phiên (`WAITING_CONFIRMATION` có báo cáo `OPEN`): tuần 7.

## 3. Việc cần xong trước khi code tuần 6

- Bước 0 của kế hoạch (xong 2026-11-18): D86–D96; sửa ngược spec 6.1–6.4, NFR-02, 7.1; TK 3.3, 3.6, 4.6–4.8, 4.10, 5.1, 5.2, 8; bộ kiểm thử ràng buộc tuần 2 (A19–A23); CLAUDE.md; rà soát (đợt 4a, 4b, phụ lục).
- Ca kiểm thử tuần 6: `docs/tuan_6/kiem_thu/ca_kiem_thu.md`.

## 4. Thực nghiệm bổ sung theo nhận xét báo cáo tuần 5

Chốt 2026-11-22: người học chọn chạy thêm thực nghiệm cho các câu hỏi của giảng viên, thay vì chỉ trả lời bằng lập luận. Module `evaluation` vẫn không sửa lõi `matching` (D68), không dùng Spring, CSDL (D71).

| Mã | Quyết định |
| --- | --- |
| D97 | **Giai đoạn bổ sung và "P một danh sách".** Thêm giai đoạn thứ ba `--giai-doan bo-sung` (vai_tro_seed `bo_sung`), cùng seed 3–5, N chính 500, quy mô 50 / 100 / 500 / 1.000, nhiễu mặc định và 2.000 lần bootstrap như giai đoạn báo cáo, nên số của B1, P lặp lại đúng lượt báo cáo (có test). Như giai đoạn báo cáo, chỉ chạy khi `backend/` đã commit sạch (D71), ghi vào thư mục kết quả mới (D72). Phương án mới `P_mot_ds`: chấm điểm như P (bộ mặc định, ScheduleFit của lõi) nhưng gộp hai nhóm và xếp chung theo D chiều u học từ v, như B1 xếp theo SkillFit. So P, `P_mot_ds`, B1 ở mọi quy mô tách được hai nguyên nhân có thể khiến P thua B1 ở N nhỏ: việc đặt nhóm hai chiều lên trước (D56) và công thức điểm. |
| D98 | **Bỏ từng thành phần, chia lại theo tỉ lệ.** Ba phương án `P_bo_level_tl`, `P_bo_schedule_tl`, `P_bo_mode_tl`: thành phần bị bỏ bằng 0, mỗi trọng số còn lại chia cho (1 − trọng số bị bỏ), tính từ bộ mặc định chứ không chép số. Tỷ lệ giữa các thành phần còn lại giữ như bộ mặc định, nên hiệu so với P chỉ còn một thay đổi (cách chia đều của D66 lẫn hai thay đổi). D66 và các số cũ giữ nguyên. |
| D99 | **Chỉ số trong từng nhóm.** Với B1, P, `P_mot_ds` ở N = 500: lọc danh sách của từng người theo nhóm lõi xếp ứng viên vào (hai chiều / một chiều; nhóm do lọc cứng hai chiều quyết định, không phụ thuộc trọng số nên như nhau với mọi phương án), giữ thứ tự. Tập lý tưởng của nhóm là chính các ứng viên của nhóm đó, nên chỉ số đo thứ tự xếp trong nhóm, không đo việc lọc ứng viên. Ghi `precision_at_5_nghiem_ngat_<nhóm>`, `ndcg_at_5_loai_suy_bien_<nhóm>` (bản (2) của D59); người không có ứng viên nào ở nhóm thì không tính cho nhóm đó. |
| D100 | **Kiểm định của giai đoạn bổ sung, kiểm định bội.** Mỗi phép so sánh (ở mỗi quy mô: P − B1, `P_mot_ds` − B1, P − `P_mot_ds`; ở N = 500: P − từng phương án chia theo tỉ lệ; trong từng nhóm: P − B1, `P_mot_ds` − B1) chạy như D62 trên hai chỉ số, gộp người dùng của 3 seed, rồi **từng seed riêng** (mỗi seed là một bộ dữ liệu sinh độc lập). Hiệu chỉnh kiểm định bội bằng Holm–Bonferroni, tính bằng `thuc_nghiem/phan_tich/bo_sung_tuan_6.py` từ cột `p` của `kiem_dinh.csv` rồi ghi vào `docs/ket_qua.md`: họ 1 = 8 phép của giai đoạn báo cáo (D62); họ 2 = các phép gộp 3 seed của giai đoạn bổ sung (38 phép). Phép từng seed chỉ để xem kết quả có cùng chiều ở cả 3 bộ dữ liệu không, không hiệu chỉnh. |
| D101 | **Giữ cấu hình mặc định của hệ thống (trả lời câu để ngỏ ở D69); kiểm định cấu hình đã chọn.** `MatchingWeights.DEFAULT` và ScheduleFit chia cho min giữ nguyên trong lõi và API. Căn cứ: kiểm định sau khi chạy `P_w_lich` − P và `P_sf_tran3` − P (N = 500, nhiễu mặc định, gộp seed 3–5, người thuộc bản (2) của D59, từ `per_user.csv` của lượt báo cáo; Wilcoxon theo công thức D62, bootstrap 2000 lần; tính bằng `thuc_nghiem/phan_tich/bo_sung_tuan_6.py`, có bước tự kiểm tính lại P − B1 khớp `kiem_dinh.csv`; nằm ngoài hai họ của D100; số ở `docs/ket_qua.md` bảng 14). (a) `P_w_lich` không hơn P có ý nghĩa (NDCG@5 hiệu 0.0032, khoảng tin cậy chứa 0, p = 0.0916), nên không đổi trọng số. (b) `P_sf_tran3` hơn P ở cả hai chỉ số và cả ba seed (NDCG@5 hiệu 0.0217) nhưng chưa đổi, vì: nhãn 2 đòi lịch thật trùng ít nhất 2 khung, đúng thứ `TRAN_3` đếm, nên một phần phần hơn do cách đặt nhãn và dữ liệu giả lập không tách được; `TRAN_3` đảo chiều thiên lệch chứ không bỏ nó (ứng viên khai 1–3 khung chiếm 0.0743 vị trí top 5 so với 0.0972 vị trí trong danh sách; `MIN` là 0.1128). Đổi công thức là đổi quy tắc chấm điểm của sản phẩm (lời giải thích trên giao diện, ví dụ tính tay, test lõi), chỉ làm khi có bằng chứng không dựa vào nhãn do người thiết kế đặt: bản nhãn không đếm khung trùng, hoặc dữ liệu phiên thật. |
