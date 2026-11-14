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
| 2026-11-11 | Lặp lại lỗi trên khi thử hook mở rộng cho `evaluation`: `echo`, `printf` rồi `node -e` đều làm mất `\`. Cách chặn cũ chỉ nằm trong file log này, lúc sửa hook không đọc tới | Hook thoát 1 với `SyntaxError`, ba lần | Script `.claude/hooks/thu-hook.mjs` dựng đường dẫn bằng `String.fromCharCode(92)`; đầu file hook ghi phải thử bằng script này |
| 2026-11-11 | Kiểm "`evaluation` không import Spring" ở tuần 5 bước 2 bằng `git grep`: file mới chưa `git add` nên không được tìm, kiểm tra luôn rỗng | Bước 3: Javadoc có chữ `ScoreCalculator` mà `git grep` không ra | Kiểm file mới bằng `grep -r` như kế hoạch ghi (hoặc `git grep --untracked`); đã kiểm lại cả bước 2 |
| 2026-11-14 | Lặp lại lỗi 2026-11-08 trong script làm hỏng code của README kiểm thử tuần 5: gọi `mvnw.cmd` bằng tên ngắn, test không chạy, script đọc báo cáo surefire cũ (của lần chạy đầy đủ) và báo "không test nào đỏ" cho cả 6 đột biến | Mâu thuẫn: `mvn exit=1` nhưng không test nào đỏ; ngày giờ của file báo cáo cũ hơn lượt chạy | Gọi `mvnw.cmd` bằng đường dẫn tuyệt đối; xóa báo cáo của lớp test trước mỗi lượt, thiếu báo cáo thì báo lỗi; thêm một lượt đối chứng không sửa gì, phải có báo cáo và 0 test đỏ |
| 2026-11-14 | Thử TC247 bằng cách sửa tạm `backend/HELP.md`: file này bị `.gitignore`, `git status` không thấy, giai đoạn báo cáo chạy hết thay vì dừng | Lượt chạy ra mã thoát 0; `git restore HELP.md` báo file không được git theo dõi | Chọn file để sửa tạm bằng `git ls-files --error-unmatch` trước; `--out` trỏ vào thư mục tạm nên lượt chạy nhầm không ghi vào `thuc_nghiem/ket_qua/` |
