# QUYẾT ĐỊNH TRIỂN KHAI – TUẦN 6

Chốt ngày 2026-11-18. File này là tài liệu mới nhất cho phạm vi tuần 6. Nếu khác spec tuần 1, thiết kế tuần 2 hoặc quyết định tuần 3–5 thì file này đúng. Khi thêm hoặc đổi một quyết định ở đây, phải sửa luôn các tài liệu cũ nhắc tới điểm đó trong cùng commit. Mã quyết định đánh số tiếp theo tuần 5 (D78 trở đi).

## 1. Giao diện frontend

| Mã | Quyết định |
| --- | --- |
| D78 | **Thư viện giao diện.** Bỏ cách "giao diện CSS viết tay, không thêm thư viện UI" của kế hoạch tuần 3 (bước 10). Frontend dùng **Tailwind CSS 4** (gói `tailwindcss` và plugin `@tailwindcss/vite`; lúc chốt là bản 4.3.5, plugin hỗ trợ Vite 8 dự án đang dùng) và **shadcn/ui**. Component của shadcn/ui sinh bằng CLI `shadcn` (lúc chốt là bản 4.23.0) và nằm trong `frontend/src/components/ui/` như code của dự án, không phải gói npm cài vào. Icon dùng `lucide-react`, bộ icon mà các component shadcn/ui import sẵn. **D23 giữ nguyên:** state của form vẫn viết tay bằng `useState`; dùng component nhập liệu của shadcn/ui (Input, Select, Textarea, Checkbox, Button...) nhưng không thêm `react-hook-form`, `zod` hay component `Form` của shadcn/ui. **Chỉ đổi phần trình bày:** không đổi đường dẫn, lời gọi API, cách hiện lỗi từ backend (D24: `detail` và lỗi theo từng ô), luật dữ liệu riêng tư (NFR-02) và luật lấy nguyên điểm từ API (NFR-09); màn hình rộng 360 px vẫn không cuộn ngang (NFR-08); giữ các thuộc tính trợ năng các ca kiểm thử đang dựa vào (`role="tab"` + `aria-selected`, `aria-label` của ô lịch rảnh, `role="alert"` của hộp lỗi). **Thứ tự làm:** cài Tailwind là phần đặt lại kiểu mặc định (Preflight) áp cho cả trang: tiêu đề `h1`/`h2` mất cỡ chữ riêng, danh sách mất dấu đầu dòng, nút mất viền. Vì vậy các màn hình đã có (S01–S06, S10, A01) chuyển hết sang thư viện mới trong cùng đợt cài, trước khi làm S07–S09; S07–S09 viết thẳng bằng thư viện mới. |

## 2. Chưa chốt (chốt sau khi duyệt đề xuất thiết kế)

Các điểm dưới đây chưa có đúng một cách làm nên chưa ghi thành quyết định:

- Bảng màu (màu chính, màu nhấn, màu trạng thái) và màu nền của shadcn/ui.
- Font chữ (phải hiển thị đủ dấu tiếng Việt).
- Khung trang: thanh menu trên cùng như hiện nay hay thanh bên.
- Chế độ tối: theo cài đặt hệ thống như hiện nay, hay thêm nút chuyển.
- Cách trình bày lưới lịch rảnh 7×3 (S03) và bảng điểm chi tiết (S05).
