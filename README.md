# Skill Exchange

Nền tảng ghép cặp trao đổi kỹ năng giữa sinh viên: mỗi người khai báo kỹ năng có thể dạy, kỹ năng muốn học, lịch rảnh và hình thức học; hệ thống gợi ý người phù hợp (hai chiều hoặc học một chiều). Đồ án cá nhân 9 tuần.

## Công nghệ

- **Backend:** Java 21, Spring Boot 4.1 (Web MVC, Security + JWT, Data JPA, Mail), Flyway, PostgreSQL 17
- **Frontend:** React 19, TypeScript, Vite, React Router
- **Môi trường dev:** Docker Compose (PostgreSQL, Mailpit để bắt thư)

## Tiến độ

| Tuần | Nội dung | Trạng thái |
| --- | --- | --- |
| 1–2 | Phân tích yêu cầu, thiết kế CSDL / API | Xong |
| 3 | Đăng ký bằng email trường, xác thực email, đăng nhập JWT, hồ sơ, lịch rảnh, kỹ năng (màn hình S01–S04) | Xong |
| 4 | Thuật toán matching, tìm kiếm, hồ sơ công khai | Đang làm |
| 5–9 | Thực nghiệm đánh giá thuật toán, yêu cầu trao đổi, phiên, đánh giá, quản trị, triển khai | Chưa làm |

## Chạy trên máy

Yêu cầu: Java 21, Node.js, Docker Desktop.

```powershell
copy .env.example .env                      # điền giá trị (DB, JWT_SECRET, MAIL_*, ADMIN_*, FRONTEND_URL)
docker compose up -d                        # PostgreSQL + Mailpit (http://localhost:8025)
cd backend; .\mvnw.cmd spring-boot:run      # API: http://localhost:8080
cd frontend; npm install; npm run dev       # Giao diện: http://localhost:5173
```

## Kiểm thử

```powershell
docker compose exec db createdb -U skillexchange skillexchange_test   # tạo DB test một lần
cd backend; .\mvnw.cmd test                                           # 139 test
cd frontend; npm run build; npm run lint
```

## Tài liệu

- Yêu cầu và thuật toán: `docs/tuan_1/Phan_tich_yeu_cau_Skill_Exchange_Tuan_1.md`
- Thiết kế CSDL, API, màn hình: `docs/tuan_2/Tuan_2_Thiet_ke.md`
- Quyết định triển khai và ca kiểm thử theo tuần: `docs/tuan_<n>/`
