# Harness log

Ghi lại lỗi AI mắc khi làm dự án, cách phát hiện, và cơ chế đã thêm để lỗi không lặp lại.
Mỗi dòng là một lỗi có thật; không ghi lỗi giả định.

| Ngày | Lỗi AI mắc | Phát hiện bằng | Chặn bằng |
| --- | --- | --- | --- |
| 2026-11-07 | Tạo `LikePatternsTest` bằng heredoc của Git Bash: các dấu `\` bị giảm một nửa, test đỏ sai lý do | Chạy test, đọc lý do đỏ (skill `buoc-tuan` mục 3.2) | Tạo file có `\` bằng công cụ Write, không dùng heredoc hay `echo` |
| 2026-11-07 | Script Python sửa tài liệu ghi `ESCAPE ''` thay vì `ESCAPE '\'` (chuỗi Python thường nuốt `\`) | Đọc lại file sau khi sửa | Dùng raw string hoặc `chr(92)` khi ghi `\` |
| 2026-11-07 | Comment trong `frontend/src/format.ts` ghi sai ví dụ phép nhân số thực | Chạy thử biểu thức bằng Node | Ví dụ số trong comment phải chạy thật trước khi ghi |
| 2026-11-08 | Hook C13 bản đầu gọi `mvnw.cmd` bằng tên ngắn: `cmd` báo "not recognized", hook báo đỏ mà không có dòng lỗi nào (bộ lọc bỏ mất) | Thử hook với code đúng: báo đỏ sau 0,17 giây | Gọi `mvnw.cmd` bằng đường dẫn tuyệt đối; lọc không ra dòng nào thì in 20 dòng cuối log |
| 2026-11-08 | Thử hook bằng `echo '{"file_path":"C:\\..."}'`: chuỗi JSON mất `\`, `JSON.parse` lỗi | Hook thoát 1 với `SyntaxError` | Tạo file JSON mẫu bằng Write rồi chuyển vào stdin |
