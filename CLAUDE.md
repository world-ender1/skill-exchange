# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Skill Exchange

Nền tảng ghép cặp trao đổi kỹ năng giữa sinh viên. Đồ án cá nhân 9 tuần, người làm là sinh viên đang học Spring Boot.

Tài liệu dự án (đọc trước khi làm tính năng):
- Spec yêu cầu + thuật toán matching: `docs/tuan_1/Phan_tich_yeu_cau_Skill_Exchange_Tuan_1.md` (FR-xx, NFR-xx, mục 4 công thức điểm, mục 6 trạng thái/phân quyền)
- Thiết kế DB, API, sơ đồ trạng thái, màn hình: `docs/tuan_2/Tuan_2_Thiet_ke.md`
- Kế hoạch 9 tuần: `docs/Skill_Exchange_Proposal_9_Tuan.md`
- Quyết định triển khai tuần 3 (auth, JWT, hồ sơ, kỹ năng; D1–D30): `docs/tuan_3/Quyet_dinh_tuan_3.md`
- Quyết định triển khai tuần 4 (matching, hồ sơ công khai, tìm kiếm; D31–D38): `docs/tuan_4/Quyet_dinh_tuan_4.md`
- Ca kiểm thử viết trước từ tài liệu: `docs/tuan_<n>/kiem_thu/`
- Rà soát toàn bộ tài liệu và các vấn đề còn chờ sửa theo đợt (R01–R56): `docs/ra_soat/Bao_cao_ra_soat.md`
- `hoc_vibe_coding/` là tài liệu học, không phải code hay spec dự án.
- Không dùng làm nguồn: `Noi_dung_bao_cao_*.md`, `.docx` (sinh lại từ nguồn khi viết báo cáo); `De_xuat_chinh_sua.md`, `de_xuat_sua_spec.md` (lịch sử).

Thứ bậc khi tài liệu mâu thuẫn: tài liệu giai đoạn sau đúng. Quyết định tuần mới nhất > thiết kế tuần 2 > spec tuần 1 > proposal. `V1__init.sql` là hiện thực của thiết kế; SQL lệch thiết kế là lỗi cần báo. Ngoại lệ: nếu bản mới làm mất một yêu cầu "Bắt buộc" thì hỏi tôi.

## Quy tắc sửa tài liệu
- Sửa ngược: khi thêm hoặc đổi một quy tắc ở tài liệu mới, grep mọi tài liệu cũ nhắc tới quy tắc đó và sửa trong cùng commit.
- Trước khi code một tuần: viết ca kiểm thử từ tài liệu (như `docs/tuan_3/kiem_thu/ca_kiem_thu.md`). Chỗ nào chưa quy định rõ thì chốt vào tài liệu trước, rồi mới code.

## Công nghệ
- Backend: Java 21, Spring Boot 4.1, Maven wrapper, Spring Security, Spring Data JPA, Flyway, PostgreSQL 17
- Frontend: React 19 + TypeScript + Vite, lint bằng oxlint
- Bí mật và cấu hình máy (DB, JWT_SECRET, MAIL_*, ADMIN_EMAIL/ADMIN_PASSWORD, FRONTEND_URL) nằm trong `.env` ở thư mục gốc; mẫu ở `.env.example`

## Lệnh thường dùng (PowerShell, chạy từ `C:\DACN`)
- Chạy DB: `docker compose up -d`
- Backend: `cd backend; .\mvnw.cmd spring-boot:run` (phải chạy từ `backend/` vì `application.yml` đọc `../.env`)
- Test backend: `cd backend; .\mvnw.cmd test`
- Một test: `.\mvnw.cmd test -Dtest=TenClassTest#tenPhuongThuc`
- Test `@SpringBootTest` cần PostgreSQL đang chạy (Flyway chạy migration vào DB thật)
- Frontend: `cd frontend; npm run dev` (http://localhost:5173)
- Build / lint frontend: `npm run build` (gồm `tsc -b`), `npm run lint`

## Kiến trúc
- Backend chia package theo tính năng dưới `com.skillexchange`: `auth`, `user`, `skill`, `matching`, `exchange`, `review`, `admin`, `evaluation`, `common`
- Luồng tầng: Controller (chỉ nhận request/trả response) → Service (logic nghiệp vụ, kiểm tra quyền) → Repository → Entity
- Lược đồ DB do Flyway quản lý (`backend/src/main/resources/db/migration`). `ddl-auto: validate`: Entity phải khớp bảng, Hibernate không tự sửa DB
- Không sửa migration đã chạy (`V1__init.sql`); thay đổi lược đồ thì thêm file `V<n>__mo_ta.sql` mới
- Ràng buộc nghiệp vụ quan trọng đã nằm trong DB (CHECK, unique index một phần). Đọc `V1__init.sql` trước khi viết Entity/Service
- Mã xác thực email/đặt lại mật khẩu chỉ lưu bản băm (`user_tokens.token_hash`)

## Quy ước API (thiết kế tuần 2, mục 4.1)
- REST JSON, tiền tố `/api`, xác thực `Authorization: Bearer <JWT>` (hạn 24 giờ, chưa có refresh token)
- Lỗi trả về dạng Problem Details (RFC 9457), kể cả 401/403 từ Spring Security; 400 dữ liệu sai (kể cả trùng lặp trong cùng request), 401 chưa đăng nhập, 403 không có quyền, 409 sai trạng thái hoặc trùng với dữ liệu đã có
- Đổi trạng thái dùng endpoint riêng (`POST /requests/{id}/accept`), không dùng `PATCH status`
- Danh sách lịch rảnh/kỹ năng của tôi: `PUT` thay cả danh sách
- Chỉ sinh viên đã xác thực (`status = ACTIVE` và `role = STUDENT`) mới được dùng matching, tìm kiếm và xem hồ sơ người khác; admin chỉ dùng API quản trị và không xuất hiện trong gợi ý, tìm kiếm
- Đuôi email trường lấy từ cấu hình, không ghi cứng
- `phone`, `contact_info`, `location_or_link` là riêng tư: chỉ trả trong `/api/me` và phiên đã `ACCEPTED` (NFR-02)

## Quy tắc làm việc với AI
- Khi tôi nói "đề xuất" hoặc "lập kế hoạch": KHÔNG tạo/sửa file
- Khi implement theo TDD: KHÔNG sửa test để test pass
- Phân quyền luôn kiểm tra ở backend (spec mục 6.3), không chỉ ẩn nút ở frontend
- Không commit `.env` hoặc mật khẩu
- Commit theo Conventional Commits (`feat(scope):`, `fix:`, `test:`, `docs:`, `chore:`, `refactor:`)

## Người dùng đang học
- Tôi đã học Java core, đang học Spring Boot qua dự án này
- Sau mỗi thay đổi code: tóm tắt file nào thuộc tầng nào, và 1–2 điểm "tại sao" quan trọng
- Lần đầu dùng annotation/khái niệm Spring trong dự án: giải thích ngắn, liên hệ Java core
- Ưu tiên code dễ đọc hơn code ngắn gọn khéo léo
