# THIẾT KẾ HỆ THỐNG – TUẦN 2

# SKILL EXCHANGE – NỀN TẢNG GHÉP CẶP TRAO ĐỔI KỸ NĂNG GIỮA SINH VIÊN

Dự án cá nhân | Thời gian thực hiện: 9 tuần

## 1. Mục tiêu và cơ sở thiết kế

Tuần 2 chuyển đặc tả yêu cầu của tuần 1 thành thiết kế kỹ thuật đủ chi tiết để bắt đầu lập trình từ tuần 3. Tài liệu gồm bốn phần:

1. Thiết kế cơ sở dữ liệu (mục 3).
2. Thiết kế API (mục 4).
3. Sơ đồ trạng thái của yêu cầu và phiên trao đổi (mục 5).
4. Prototype giao diện (mục 6).

**Cơ sở thiết kế:** tài liệu *Phân tích yêu cầu – Tuần 1*, bao gồm các quyết định đã chốt:

| Quyết định | Nội dung |
| --- | --- |
| Thang trình độ | 4 bậc: 1 Mới bắt đầu, 2 Cơ bản, 3 Khá, 4 Thành thạo. Mục tiêu học không bắt buộc |
| Khung giờ rảnh | 21 khung/tuần = 7 ngày × 3 buổi (Sáng / Chiều / Tối), lặp lại hằng tuần |
| Ghép hai chiều | $SkillFit(u,v) > 0$ và $SkillFit(v,u) > 0$ |
| LevelFit nhiều kỹ năng | Trung bình trên các kỹ năng có $Sim > 0$ |
| Yêu cầu và phiên | Một yêu cầu = một phiên = một buổi gặp. Yêu cầu ghi rõ loại, kỹ năng từng chiều và lịch đề xuất |
| Báo cáo sự cố | Thuộc MVP; admin chốt phiên sang `COMPLETED` hoặc `CANCELLED` |

Các điểm spec chưa quyết định nhưng cần cho thiết kế được ghi là **giả định** và tổng hợp ở mục 8.

## 2. Kiến trúc tổng quan

```mermaid
flowchart LR
    B["Trình duyệt<br/>React + TypeScript"] -- "HTTPS / JSON<br/>JWT Bearer" --> API["Backend<br/>Spring Boot 4 (Java 21)"]
    API -- "JPA / JDBC" --> DB[("PostgreSQL 17")]
    API -. "SMTP" .-> M["Máy chủ email<br/>(gửi mã xác thực)"]
```

| Thành phần | Công nghệ | Vai trò |
| --- | --- | --- |
| Frontend | React, TypeScript, Vite | Giao diện người dùng, gọi REST API |
| Backend | Spring Boot 4, Spring Security, Spring Data JPA | Nghiệp vụ, phân quyền, thuật toán matching |
| Cơ sở dữ liệu | PostgreSQL 17, Flyway | Lưu dữ liệu; Flyway quản lý phiên bản lược đồ |
| Xác thực | JWT (access token) | Mỗi request gửi token trong header `Authorization` |

**Tổ chức mã nguồn backend theo tính năng (package-by-feature):**

| Package | Trách nhiệm |
| --- | --- |
| `auth` | Đăng ký, xác thực email, đăng nhập, JWT |
| `user` | Hồ sơ, lịch rảnh, hình thức học, kỹ năng của người dùng |
| `skill` | Danh mục kỹ năng, nhóm, quan hệ liên quan |
| `matching` | Lọc cứng, chấm điểm, phân loại hai chiều/một chiều. Lõi tính toán không phụ thuộc DB, dùng chung với `evaluation` (quyết định tuần 4, D34) |
| `exchange` | Yêu cầu trao đổi và phiên |
| `review` | Đánh giá, tính Reputation |
| `admin` | Quản trị danh mục, xử lý báo cáo sự cố |
| `evaluation` | Sinh dữ liệu giả lập, baseline, tính chỉ số (chạy offline) |
| `common` | Xử lý lỗi, cấu hình chung |

## 3. Thiết kế cơ sở dữ liệu

### 3.1. Quy ước chung

- Khóa chính: `BIGINT GENERATED ALWAYS AS IDENTITY`.
- Thời gian: `TIMESTAMPTZ` (có múi giờ), lưu theo UTC.
- Giá trị liệt kê (enum) lưu dạng `VARCHAR` kèm ràng buộc `CHECK`, thay vì kiểu enum riêng của PostgreSQL. Lý do: thêm giá trị mới chỉ cần sửa `CHECK` trong một migration, và JPA ánh xạ trực tiếp bằng `@Enumerated(EnumType.STRING)`.
- **"Chưa khai báo" được biểu diễn bằng việc không có dữ liệu:** người dùng không có dòng nào trong `user_availability` là chưa khai báo lịch rảnh; `learning_mode = NULL` là chưa khai báo hình thức học. Nhờ đó phân biệt được *chưa khai báo* với *đã khai báo nhưng không phù hợp*, đúng yêu cầu của lọc cứng.
- Cột liệt kê, cột trạng thái và cột số bắt buộc đều `NOT NULL`. Cột `status` có giá trị mặc định là trạng thái khởi đầu (`PENDING`, `SCHEDULED`, `OPEN`...). Cột `created_at` mặc định là thời điểm chèn.
- **Quy tắc xóa dữ liệu:**
  - Kỹ năng, lịch rảnh và mã xác thực của người dùng bị xóa theo khi xóa người dùng (`ON DELETE CASCADE`).
  - Yêu cầu, phiên, đánh giá và báo cáo là lịch sử nên **không được xóa** (mặc định `RESTRICT`). Người dùng đã có lịch sử thì chỉ khóa tài khoản (FR-08), không xóa.
  - Kỹ năng đang được dùng cũng không xóa được, chỉ ẩn bằng `is_active = false`.
- **Quy tắc nhất quán được đặt ngay trong database** bằng `CHECK`, không chỉ ở tầng service. Ví dụ: phiên `CANCELLED` bắt buộc có lý do hủy. Như vậy kể cả khi code có lỗi, dữ liệu sai cũng không ghi được.
- Lược đồ do Flyway tạo (`V1__init.sql`, ...). Hibernate chỉ kiểm tra khớp (`ddl-auto: validate`).

### 3.2. Sơ đồ thực thể – quan hệ (ERD)

```mermaid
erDiagram
    users ||--o{ user_tokens : "có"
    users ||--o{ user_teach_skills : "dạy"
    users ||--o{ user_learn_skills : "muốn học"
    users ||--o{ user_availability : "rảnh"
    skill_categories ||--o{ skills : "gồm"
    skills ||--o{ user_teach_skills : ""
    skills ||--o{ user_learn_skills : ""
    skills ||--o{ skill_relations : "liên quan"
    users ||--o{ exchange_requests : "gửi / nhận"
    skills ||--o{ exchange_requests : "kỹ năng từng chiều"
    exchange_requests ||--o| exchange_sessions : "tạo"
    exchange_sessions ||--o{ reviews : "được đánh giá"
    users ||--o{ reviews : "viết / nhận"
    exchange_sessions ||--o{ incident_reports : "bị báo cáo"
    users ||--o{ incident_reports : "gửi / xử lý"

    users {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar full_name
        varchar role
        varchar status
        varchar learning_mode
        varchar phone
        text contact_info
        text bio
    }
    user_tokens {
        bigint id PK
        bigint user_id FK
        varchar purpose
        varchar token_hash UK
        timestamptz expires_at
        timestamptz used_at
    }
    skill_categories {
        bigint id PK
        varchar name UK
    }
    skills {
        bigint id PK
        bigint category_id FK
        varchar name UK
        boolean is_active
    }
    skill_relations {
        bigint skill_a_id PK
        bigint skill_b_id PK
    }
    user_teach_skills {
        bigint user_id PK
        bigint skill_id PK
        smallint level
    }
    user_learn_skills {
        bigint user_id PK
        bigint skill_id PK
        smallint target_level
    }
    user_availability {
        bigint user_id PK
        smallint day_of_week PK
        varchar period PK
    }
    exchange_requests {
        bigint id PK
        bigint sender_id FK
        bigint receiver_id FK
        varchar type
        bigint sender_learn_skill_id FK
        bigint receiver_learn_skill_id FK
        timestamptz proposed_start_at
        varchar status
        int version
    }
    exchange_sessions {
        bigint id PK
        bigint request_id FK
        varchar status
        timestamptz started_at
        bigint first_completed_by FK
        int version
    }
    reviews {
        bigint id PK
        bigint session_id FK
        bigint reviewer_id FK
        bigint reviewee_id FK
        smallint rating
    }
    incident_reports {
        bigint id PK
        bigint session_id FK
        bigint reporter_id FK
        varchar status
        varchar resolution
    }
```

### 3.3. Mô tả chi tiết các bảng

#### `users` – tài khoản và hồ sơ

| Cột | Kiểu | Ràng buộc | Ý nghĩa |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | Email trường, dùng để đăng nhập |
| `password_hash` | VARCHAR(100) | NOT NULL | Mật khẩu đã băm bằng BCrypt (NFR-01) |
| `full_name` | VARCHAR(100) | NOT NULL | |
| `role` | VARCHAR(20) | NOT NULL, CHECK ∈ {`STUDENT`, `ADMIN`} | |
| `status` | VARCHAR(30) | NOT NULL, CHECK ∈ {`PENDING_VERIFICATION`, `ACTIVE`, `LOCKED`} | Chỉ `ACTIVE` mới được matching (FR-01, lọc cứng điều 2) |
| `email_verified_at` | TIMESTAMPTZ | NULL | Thời điểm xác thực email |
| `learning_mode` | VARCHAR(10) | NULL, CHECK ∈ {`ONLINE`, `OFFLINE`, `ANY`} | NULL = chưa khai báo |
| `bio` | TEXT | NULL | Giới thiệu công khai |
| `phone` | VARCHAR(20) | NULL | **Riêng tư**, chỉ lộ sau khi yêu cầu được chấp nhận (NFR-02) |
| `contact_info` | TEXT | NULL | **Riêng tư**: Zalo, Facebook, email cá nhân... |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL | |

#### `user_tokens` – mã xác thực email

| Cột | Kiểu | Ràng buộc | Ý nghĩa |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | |
| `user_id` | BIGINT | FK → users, NOT NULL | |
| `purpose` | VARCHAR(30) | NOT NULL, CHECK ∈ {`VERIFY_EMAIL`, `RESET_PASSWORD`} | Dùng chung cho xác thực email và quên mật khẩu |
| `token_hash` | VARCHAR(100) | NOT NULL, UNIQUE | Chỉ lưu bản băm của mã, không lưu mã gốc |
| `expires_at` | TIMESTAMPTZ | NOT NULL | |
| `used_at` | TIMESTAMPTZ | NULL | Đã dùng thì không dùng lại được |
| `created_at` | TIMESTAMPTZ | NOT NULL | |

#### `skill_categories`, `skills`, `skill_relations` – danh mục kỹ năng (mục 4.1 spec)

| Bảng | Cột chính | Ràng buộc |
| --- | --- | --- |
| `skill_categories` | `id`, `name` | `name` UNIQUE |
| `skills` | `id`, `category_id`, `name`, `is_active` | `name` UNIQUE; `is_active = false` là kỹ năng bị ẩn (FR-08) |
| `skill_relations` | `skill_a_id`, `skill_b_id` | PK (`skill_a_id`, `skill_b_id`); CHECK `skill_a_id < skill_b_id` |

- Mỗi cặp kỹ năng liên quan chỉ lưu **một dòng**. CHECK `a < b` ngăn lưu trùng (Java, Kotlin) và (Kotlin, Java).
- Điều kiện "cùng nhóm" được kiểm tra ở tầng service khi admin tạo quan hệ.
- Từ ba bảng này tính được $Sim(s,t)$: bằng 1.0 nếu $s = t$, bằng 0.5 nếu có dòng trong `skill_relations`, còn lại bằng 0.
- Kỹ năng bị ẩn không hiện trong danh sách chọn mới, nhưng vẫn giữ trong dữ liệu cũ để không làm hỏng yêu cầu và phiên đã có. Kỹ năng người dùng đang có vẫn được dùng bình thường trong matching, tìm kiếm và khi tạo yêu cầu (spec mục 4.1).

#### `user_teach_skills`, `user_learn_skills` – kỹ năng của người dùng (FR-03)

| Bảng | Cột | Ràng buộc |
| --- | --- | --- |
| `user_teach_skills` | `user_id`, `skill_id`, `level` | PK (`user_id`, `skill_id`); `level` NOT NULL, CHECK 1–4 |
| `user_learn_skills` | `user_id`, `skill_id`, `target_level` | PK (`user_id`, `skill_id`); `target_level` NULL, CHECK 1–4 |

Tập $T_v$ là các dòng của $v$ trong `user_teach_skills`; tập $W_u$ là các dòng của $u$ trong `user_learn_skills`.

#### `user_availability` – lịch rảnh (mục 4.6 spec)

| Cột | Kiểu | Ràng buộc |
| --- | --- | --- |
| `user_id` | BIGINT | FK → users |
| `day_of_week` | SMALLINT | CHECK 1–7 (1 = Thứ Hai, 7 = Chủ Nhật) |
| `period` | VARCHAR(10) | CHECK ∈ {`MORNING`, `AFTERNOON`, `EVENING`} |

Khóa chính gồm cả ba cột. Mỗi người có tối đa 21 dòng. Số khung trùng của hai người là số cặp (`day_of_week`, `period`) chung.

#### `exchange_requests` – yêu cầu trao đổi (FR-05, mục 6.4 spec)

| Cột | Kiểu | Ràng buộc | Ý nghĩa |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | |
| `sender_id`, `receiver_id` | BIGINT | FK → users, NOT NULL, CHECK `sender_id <> receiver_id` | |
| `type` | VARCHAR(10) | NOT NULL, CHECK ∈ {`TWO_WAY`, `ONE_WAY`} | |
| `sender_learn_skill_id` | BIGINT | FK → skills, NOT NULL | Kỹ năng người gửi muốn học |
| `receiver_learn_skill_id` | BIGINT | FK → skills, NULL | Kỹ năng người nhận muốn học |
| `proposed_start_at` | TIMESTAMPTZ | NOT NULL | Thời điểm đề xuất |
| `duration_minutes` | SMALLINT | NOT NULL, CHECK 15–480 | |
| `mode` | VARCHAR(10) | NOT NULL, CHECK ∈ {`ONLINE`, `OFFLINE`} | Không có `ANY`: buổi học phải chốt một hình thức |
| `location_or_link` | VARCHAR(500) | NULL | Riêng tư tới khi được chấp nhận |
| `message` | TEXT | NULL | Lời nhắn của người gửi |
| `status` | VARCHAR(20) | NOT NULL, mặc định `PENDING`, CHECK ∈ {`PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED`} | |
| `response_note` | TEXT | NULL | Ghi chú khi từ chối |
| `created_at` | TIMESTAMPTZ | NOT NULL | |
| `responded_at` | TIMESTAMPTZ | NULL | Thời điểm chấp nhận/từ chối |
| `version` | INTEGER | NOT NULL, mặc định 0 | Khóa lạc quan, chống người gửi hủy đúng lúc người nhận chấp nhận (mục 5.1) |

- **Ràng buộc nhất quán loại yêu cầu:** `CHECK ((type = 'TWO_WAY') = (receiver_learn_skill_id IS NOT NULL))`. Yêu cầu hai chiều bắt buộc có kỹ năng chiều ngược lại; yêu cầu một chiều thì không được có.
- **Chống gửi trùng (spec mục 6.2):** chỉ mục duy nhất một phần `UNIQUE (sender_id, receiver_id) WHERE status = 'PENDING'`, nghĩa là mỗi người chỉ có một yêu cầu đang chờ với cùng một người nhận.

#### `exchange_sessions` – phiên trao đổi (FR-06)

| Cột | Kiểu | Ràng buộc | Ý nghĩa |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | |
| `request_id` | BIGINT | FK → exchange_requests, NOT NULL, UNIQUE | Một yêu cầu tạo tối đa một phiên |
| `start_at`, `duration_minutes`, `mode` | | NOT NULL, cùng ràng buộc như bảng yêu cầu | Sao chép từ yêu cầu lúc chấp nhận |
| `location_or_link` | VARCHAR(500) | NULL | Sao chép từ yêu cầu |
| `status` | VARCHAR(25) | NOT NULL, mặc định `SCHEDULED`, CHECK ∈ {`SCHEDULED`, `IN_PROGRESS`, `WAITING_CONFIRMATION`, `COMPLETED`, `CANCELLED`} | |
| `started_at` | TIMESTAMPTZ | NULL | **Khác NULL nghĩa là phiên đã bắt đầu** |
| `first_completed_by` | BIGINT | FK → users, NULL | Người bấm hoàn thành trước |
| `completed_at` | TIMESTAMPTZ | NULL | |
| `cancelled_by` | BIGINT | FK → users, NULL | |
| `cancel_reason` | TEXT | NULL | Bắt buộc khi hủy |
| `cancelled_at` | TIMESTAMPTZ | NULL | |
| `version` | INTEGER | NOT NULL, mặc định 0 | Khóa lạc quan (optimistic locking), chống hai người cùng bấm một lúc |
| `created_at` | TIMESTAMPTZ | NOT NULL | Thời điểm tạo phiên (= lúc chấp nhận yêu cầu) |

- Hai người tham gia phiên là `sender_id` và `receiver_id` của yêu cầu gốc.
- Cột `started_at` giải quyết quy tắc CompletionRate: phiên `CANCELLED` có `started_at` khác NULL là *hủy sau khi bắt đầu*, nên được tính vào mẫu số. Phiên đang `IN_PROGRESS` hoặc `WAITING_CONFIRMATION` chưa kết thúc nên chưa tính (spec mục 4.3).
- **Ràng buộc nhất quán trạng thái** (`CHECK`):

  | Nếu `status` là | Thì bắt buộc có |
  | --- | --- |
  | `IN_PROGRESS`, `WAITING_CONFIRMATION`, `COMPLETED` | `started_at` |
  | `WAITING_CONFIRMATION` | `first_completed_by` |
  | `COMPLETED` | `completed_at` |
  | `CANCELLED` | `cancel_reason` và `cancelled_at` |

  Các ràng buộc này chỉ kiểm tra dữ liệu của **một trạng thái** có đủ hay không. Thứ tự chuyển trạng thái (ví dụ không được đi thẳng `SCHEDULED` → `COMPLETED`) và việc người thao tác có phải người tham gia hay không vẫn do tầng service kiểm tra (mục 5).

#### `reviews` – đánh giá (FR-07)

| Cột | Kiểu | Ràng buộc |
| --- | --- | --- |
| `id` | BIGINT | PK |
| `session_id` | BIGINT | FK → exchange_sessions, NOT NULL |
| `reviewer_id`, `reviewee_id` | BIGINT | FK → users, NOT NULL, CHECK `reviewer_id <> reviewee_id` |
| `rating` | SMALLINT | NOT NULL, CHECK 1–5 |
| `comment` | TEXT | NULL |
| `created_at` | TIMESTAMPTZ | NOT NULL |

`UNIQUE (session_id, reviewer_id)` bảo đảm mỗi người chỉ đánh giá một lần mỗi phiên. Trạng thái "đã đánh giá" (`has_rated`) suy ra từ việc có dòng hay không, tách biệt khỏi trạng thái phiên đúng như spec 6.3 yêu cầu.

#### `incident_reports` – báo cáo sự cố (FR-09)

| Cột | Kiểu | Ràng buộc | Ý nghĩa |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | |
| `session_id` | BIGINT | FK → exchange_sessions, NOT NULL | |
| `reporter_id` | BIGINT | FK → users, NOT NULL | |
| `reason` | TEXT | NOT NULL | |
| `status` | VARCHAR(10) | NOT NULL, mặc định `OPEN`, CHECK ∈ {`OPEN`, `RESOLVED`} | |
| `resolution` | VARCHAR(20) | NULL, CHECK ∈ {`SESSION_COMPLETED`, `SESSION_CANCELLED`} | Admin chốt phiên theo hướng nào |
| `admin_note` | TEXT | NULL | Lý do quyết định |
| `resolved_by` | BIGINT | FK → users, NULL | |
| `created_at` | TIMESTAMPTZ | NOT NULL | |
| `resolved_at` | TIMESTAMPTZ | NULL | |

- Mỗi phiên có tối đa một báo cáo đang mở: `UNIQUE (session_id) WHERE status = 'OPEN'`.
- **Ràng buộc nhất quán** (`CHECK`), viết thành hai điều kiện riêng:
  - `RESOLVED` ⇒ có đủ `resolution`, `resolved_by`, `resolved_at`.
  - `OPEN` ⇒ cả ba cột trên đều NULL.

  Không gộp thành một điều kiện dạng `(status = 'RESOLVED') = (đủ ba cột)`. Kiểm thử cho thấy cách gộp này để lọt trường hợp báo cáo `OPEN` có `resolution` nhưng thiếu hai cột còn lại (mục 3.6, ca B42).

### 3.4. Chỉ mục phục vụ truy vấn chính

| Chỉ mục | Phục vụ |
| --- | --- |
| `user_teach_skills (skill_id)` | Tìm người dạy một kỹ năng (lọc ứng viên, tìm kiếm) |
| `user_learn_skills (skill_id)` | Tìm người muốn học một kỹ năng (chiều ngược lại của ghép hai chiều) |
| `exchange_requests (receiver_id, status)`, `(sender_id, status)` | Hộp thư yêu cầu đến/đi |
| `exchange_sessions (status)` | Danh sách phiên theo trạng thái |
| `reviews (reviewee_id)` | Tính RatingNorm |
| `incident_reports (status)` | Hàng đợi báo cáo của admin |

### 3.5. Ánh xạ công thức matching sang dữ liệu

| Đại lượng | Lấy từ | Ghi chú |
| --- | --- | --- |
| $Sim(s,t)$ | `skills`, `skill_relations` | |
| $SkillFit(u,v)$ | `user_learn_skills` của $u$, `user_teach_skills` của $v$ | |
| $LevelFit(u,v)$ | `target_level` của $u$, `level` của $v$ | Trung bình trên các kỹ năng có $Sim > 0$ |
| $ScheduleFit(u,v)$ | `user_availability` | Không có dòng = chưa khai báo → 0.5 |
| $ModeFit(u,v)$ | `users.learning_mode` | NULL = chưa khai báo |
| $RatingNorm(v)$ | `AVG(reviews.rating)` với `reviewee_id = v` | Chưa có đánh giá → 0.5 |
| $CompletionRate(v)$ | `exchange_sessions` mà $v$ tham gia | Tử số: `status = COMPLETED`. Mẫu số: `started_at IS NOT NULL AND status IN ('COMPLETED', 'CANCELLED')`. Chưa có phiên nào đủ điều kiện thì truy vấn trả về NULL; service đổi thành **0.5** theo spec mục 4.3 |

**Cách chạy matching:** với quy mô mục tiêu 1.000 hồ sơ (NFR-03), backend nạp kỹ năng, lịch rảnh và Reputation của các người dùng `ACTIVE` có `role = STUDENT` vào bộ nhớ bằng vài truy vấn gộp, sau đó lọc và chấm điểm bằng Java. Cách này tránh truy vấn lặp cho từng ứng viên. Reputation tính trực tiếp từ dữ liệu, chưa cần bảng lưu đệm. Phần lọc và chấm điểm là một lõi thuần Java nhận dữ liệu trong bộ nhớ, không gọi Repository (chi tiết ở quyết định tuần 4, D34).

**Dữ liệu giả lập cho thực nghiệm** (tuần 5) được sinh và đánh giá trong bộ nhớ bởi module `evaluation`, không ghi vào các bảng nghiệp vụ. Module này gọi cùng lõi matching với API, nên kết quả thực nghiệm đo đúng thuật toán đang chạy trong hệ thống.

**Dữ liệu khởi tạo:** danh mục 20–30 kỹ năng, các nhóm và quan hệ liên quan được nạp sẵn bằng migration Flyway (`V2__seed_skills.sql`), để tuần 3–4 có dữ liệu trước khi làm màn hình quản trị.

### 3.6. Kiểm chứng lược đồ

Lược đồ trên đã được viết thành `V1__init.sql` và chạy thử trên PostgreSQL 17 (database tạm, không phải database của ứng dụng). Có 73 ca kiểm thử:

| Nhóm | Số ca | Mong đợi |
| --- | --- | --- |
| A. Dữ liệu hợp lệ: đi trọn kịch bản gửi yêu cầu → chấp nhận → bắt đầu → hoàn thành → đánh giá, và một phiên bị báo cáo sự cố rồi admin hủy | 23 | Được nhận |
| B. Dữ liệu sai: mỗi ràng buộc ít nhất một câu vi phạm (giá trị ngoài miền, trùng lặp, thiếu cột bắt buộc, sai nhất quán loại yêu cầu, gửi trùng `PENDING`, xóa dữ liệu đang được dùng...) | 44 | Bị chặn |
| C. Truy vấn công thức: $Sim$, số khung rảnh trùng, $RatingNorm$, $CompletionRate$ (kể cả mẫu số bằng 0) | 6 | Đúng giá trị tính tay |

**Kết quả: 73/73 đạt.** Lần chạy đầu có một ca không đạt (B42), giúp phát hiện lỗi ràng buộc của bảng `incident_reports` đã nêu ở mục 3.3. Kịch bản và kết quả chi tiết nằm trong thư mục `kiem_thu/`.

## 4. Thiết kế API

### 4.1. Quy ước chung

- Kiểu REST, dữ liệu JSON, tiền tố `/api`.
- **Xác thực:** sau khi đăng nhập, client gửi `Authorization: Bearer <JWT>` trong mọi request cần đăng nhập. Token có hạn dùng 24 giờ, chưa có refresh token (quyết định D14 tuần 3). Đăng xuất phía client bằng cách xóa token.
- **Phân quyền ở backend (NFR-01):** mỗi API kiểm tra (1) đã đăng nhập, (2) vai trò, (3) quyền sở hữu tài nguyên, ví dụ người gọi có phải người tham gia phiên không.
- **Hành động đổi trạng thái dùng endpoint riêng** (`POST /requests/{id}/accept`), không dùng `PATCH status`. Mỗi chuyển trạng thái có điều kiện và người được phép riêng, nên endpoint riêng giúp kiểm tra quyền rõ ràng và không cho client tự đặt trạng thái tùy ý.
- **Định dạng lỗi** theo chuẩn Problem Details (RFC 9457). Spring hỗ trợ sẵn cho lỗi phát sinh trong controller; lỗi 401/403 sinh ở tầng filter của Spring Security phải tự cấu hình (quyết định D24 tuần 3):

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Chỉ hủy được yêu cầu đang ở trạng thái PENDING",
  "instance": "/api/requests/42/cancel"
}
```

| Mã HTTP | Khi nào |
| --- | --- |
| 200 / 201 / 204 | Thành công / đã tạo mới / thành công không có nội dung |
| 400 | Dữ liệu không hợp lệ (thiếu trường, sai miền giá trị) |
| 401 | Chưa đăng nhập hoặc token hết hạn |
| 403 | Đã đăng nhập nhưng không có quyền (sai vai trò, không phải người tham gia, tài khoản chưa xác thực) |
| 404 | Không tìm thấy tài nguyên |
| 409 | Xung đột với dữ liệu đã có: chuyển trạng thái không hợp lệ, đánh giá lần hai, gửi trùng yêu cầu, email đã đăng ký. Trùng lặp ngay trong cùng một request (ví dụ một `skillId` xuất hiện hai lần) là dữ liệu không hợp lệ, trả 400 |

**Ký hiệu quyền** trong các bảng dưới: *Public* = không cần đăng nhập; *User* = đã đăng nhập; *Active* = sinh viên đã xác thực (`status = ACTIVE` và `role = STUDENT`; admin gọi API loại này nhận 403, spec mục 6.3); *Participant* = người tham gia phiên/yêu cầu; *Admin* = quản trị viên.

### 4.2. Xác thực – `auth`

| Phương thức | Đường dẫn | Quyền | Mô tả | FR |
| --- | --- | --- | --- | --- |
| POST | `/api/auth/register` | Public | Body `{email, password, fullName}`. Đăng ký bằng email trường (kiểm tra đuôi tên miền theo cấu hình), gửi link xác thực. 201 kèm `{id, email, status}` | FR-01 |
| POST | `/api/auth/verify-email` | Public | Body `{token}`. Tài khoản `PENDING_VERIFICATION` chuyển sang `ACTIVE`. 204; token không dùng được trả 400 | FR-01 |
| POST | `/api/auth/resend-verification` | Public | Body `{email}`. Gửi lại link xác thực. Email đúng định dạng thì luôn trả 204, kể cả email chưa đăng ký; thiếu hoặc sai định dạng trả 400 (D42) | FR-01 |
| POST | `/api/auth/login` | Public | Body `{email, password}`. 200 kèm `{accessToken}` | FR-01 |

Chi tiết quy tắc của các API trên (chuẩn hóa email, độ dài mật khẩu, đăng ký lại email chưa xác thực, giới hạn 60 giây, thứ tự kiểm tra khi đăng nhập) nằm ở quyết định tuần 3, D5–D13, D25–D29 và D42.

Quên mật khẩu (FR-23) thuộc mức "Nên có" nên chưa thiết kế API. Bảng `user_tokens` đã có `purpose = RESET_PASSWORD` để làm sau mà không phải đổi lược đồ.

### 4.3. Hồ sơ và kỹ năng của tôi – `user`

| Phương thức | Đường dẫn | Quyền | Mô tả | FR |
| --- | --- | --- | --- | --- |
| GET | `/api/me` | User | Xem hồ sơ của mình, kể cả thông tin liên hệ | FR-02 |
| PUT | `/api/me` | User | Cập nhật họ tên, giới thiệu, hình thức học, điện thoại, liên hệ | FR-02 |
| GET | `/api/me/availability` | User | Lịch rảnh hiện có `[{dayOfWeek, period}]` | FR-02 |
| PUT | `/api/me/availability` | User | Thay toàn bộ lịch rảnh bằng danh sách `{dayOfWeek, period}`; danh sách rỗng = chưa khai báo | FR-02 |
| GET | `/api/me/teach-skills` | User | Kỹ năng dạy hiện có, kèm tên, nhóm, `isActive`, `level` | FR-03 |
| PUT | `/api/me/teach-skills` | User | Thay danh sách kỹ năng dạy `{skillId, level}` | FR-03 |
| GET | `/api/me/learn-skills` | User | Kỹ năng muốn học hiện có, kèm tên, nhóm, `isActive`, `targetLevel` | FR-03 |
| PUT | `/api/me/learn-skills` | User | Thay danh sách kỹ năng muốn học `{skillId, targetLevel?}` | FR-03 |
| GET | `/api/users/{id}` | Active | Hồ sơ công khai, **không** có email trường và thông tin liên hệ (NFR-02) | FR-10 |
| GET | `/api/users/{id}/reviews` | Active | Các đánh giá người đó nhận được | FR-07 |
| GET | `/api/users/search?skillId=&q=` | Active | Tìm kiếm người dùng theo kỹ năng hoặc tên | FR-10 |

Lịch rảnh và danh sách kỹ năng dùng `PUT` thay cả danh sách, vì giao diện là một biểu mẫu chỉnh sửa toàn bộ. Cách này đơn giản hơn thêm/xóa từng dòng. Các `PUT` trả 200 kèm dữ liệu vừa lưu, cùng dạng và cùng thứ tự với `GET` tương ứng (thứ tự ở D41).

Quy tắc dữ liệu của nhóm API này (trường bỏ trống, giới hạn độ dài, tối đa 10 kỹ năng và 21 khung, không vừa dạy vừa học một kỹ năng, kỹ năng bị ẩn được giữ nhưng không thêm mới được) nằm ở quyết định tuần 3, D15–D21, D28–D29 và D41.

**Hồ sơ công khai và tìm kiếm (FR-10):**

- **Ai được hiển thị:** chỉ người dùng `ACTIVE` có `role = STUDENT`. `GET /api/users/{id}` với người không thỏa điều kiện này (không tồn tại, chưa xác thực, bị khóa, admin) trả **404**, để không lộ trạng thái tài khoản. Xem hồ sơ của chính mình qua `/users/{id}` vẫn được (200).
- **Hồ sơ công khai trả về:** `id`, `fullName`, `bio`, `learningMode`, `teachSkills[{skillId, skillName, categoryName, level}]`, `learnSkills[{skillId, skillName, categoryName, targetLevel}]`, `availability[{dayOfWeek, period}]`. Trình độ được ghi rõ trên giao diện là "tự khai báo". **Không** có `email`, `phone`, `contactInfo` (NFR-02). Điểm đánh giá trung bình và số lượt đánh giá được thêm ở tuần 7 cùng FR-07.
- **Tìm kiếm `GET /api/users/search?skillId=&q=`:** cần ít nhất một tham số, thiếu cả hai trả 400. `skillId` khớp người có kỹ năng đó trong danh sách **dạy** (trùng chính xác, không tính kỹ năng liên quan). `q` tìm chuỗi con trong `full_name`, không phân biệt hoa thường. Có cả hai tham số thì phải thỏa cả hai. Kết quả không gồm chính người gọi, xếp theo `fullName` rồi `id`, tối đa 20 người. Mỗi kết quả gồm `id`, `fullName`, `learningMode` và danh sách kỹ năng dạy.

**Thời điểm làm:** các API `/api/me*` làm ở tuần 3. `/api/users/{id}` và `/api/users/search` làm ở tuần 4 cùng màn hình S05, S06, S10. `/api/users/{id}/reviews` làm ở tuần 7 cùng chức năng đánh giá.

### 4.4. Danh mục kỹ năng – `skill`

| Phương thức | Đường dẫn | Quyền | Mô tả |
| --- | --- | --- | --- |
| GET | `/api/skill-categories` | User | Danh sách nhóm kỹ năng `[{id, name}]`, xếp theo tên (D40) |
| GET | `/api/skills?categoryId=&q=` | User | Kỹ năng đang hoạt động `[{id, name, categoryId, categoryName}]`, dùng cho ô chọn kỹ năng. `q` tìm chuỗi con trong tên, không phân biệt hoa thường. Xếp theo tên (D40) |

### 4.5. Gợi ý ứng viên – `matching`

| Phương thức | Đường dẫn | Quyền | Mô tả | FR |
| --- | --- | --- | --- | --- |
| GET | `/api/matches?type=TWO_WAY&limit=10` | Active | Danh sách ghép hai chiều, xếp theo MutualScore | FR-04 |
| GET | `/api/matches?type=ONE_WAY&limit=10` | Active | Danh sách mentoring một chiều, xếp theo $D(u,v)$ | FR-04 |

**Tham số:** `type` bắt buộc (`TWO_WAY` hoặc `ONE_WAY`), thiếu hoặc sai trả 400. `limit` từ 1 đến 50, mặc định 10, ngoài khoảng trả 400.

**Response** gồm tổng số ứng viên của nhóm và danh sách top `limit`. Mỗi kết quả có điểm, các thành phần điểm và lý do phù hợp để giao diện giải thích (mục 2 spec: "giải thích kết quả", NFR-09):

```json
{
  "type": "TWO_WAY",
  "needsLearnSkills": false,
  "total": 8,
  "items": [
    {
      "userId": 17,
      "fullName": "Trần B",
      "type": "TWO_WAY",
      "score": 0.78,
      "forward":  { "d": 0.94, "skillFit": 1.0, "levelFit": 1.0, "scheduleFit": 0.667, "modeFit": 1.0, "reputation": 0.8 },
      "backward": { "d": 0.65, "skillFit": 0.5, "levelFit": 1.0, "scheduleFit": 0.667, "modeFit": 1.0, "reputation": 0.5 },
      "learningMode": "ONLINE",
      "ratingAverage": 4.2,
      "reviewCount": 5,
      "hasPendingRequest": false,
      "youLearn":  [{ "want": "Java", "teach": "Java", "relation": "EXACT", "theirLevel": 3 }],
      "theyLearn": [{ "want": "IELTS Writing", "teach": "Tiếng Anh", "relation": "RELATED", "yourLevel": 3 }],
      "commonSlots": [{ "dayOfWeek": 2, "period": "EVENING" }, { "dayOfWeek": 5, "period": "EVENING" }]
    }
  ]
}
```

- Với `ONE_WAY` thì không có `backward` và `theyLearn`, còn `score` bằng $D(u,v)$.
- `ratingAverage` là `null` và `reviewCount = 0` khi chưa có đánh giá; giao diện hiện nhãn "Mới".
- `hasPendingRequest = true` khi người gọi đang có yêu cầu `PENDING` gửi tới ứng viên này; giao diện thay nút "Gửi yêu cầu" bằng "Đã gửi". Ứng viên vẫn nằm trong danh sách (lọc cứng không loại họ).
- Người gọi chưa khai báo kỹ năng muốn học ($W_u$ rỗng): trả 200 với `needsLearnSkills = true`, `total = 0`, `items = []`; giao diện dẫn sang S04.
- Điểm trong response làm tròn 3 chữ số thập phân để hiển thị; thứ tự xếp hạng dùng điểm chưa làm tròn (spec mục 4.4).
- Response không bao giờ chứa `email`, `phone`, `contactInfo` (NFR-02).

### 4.6. Yêu cầu trao đổi – `exchange`

| Phương thức | Đường dẫn | Quyền | Mô tả | FR |
| --- | --- | --- | --- | --- |
| POST | `/api/requests` | Active | Tạo yêu cầu (mục 6.4 spec) | FR-05 |
| GET | `/api/requests?box=sent\|received&status=` | Active | Hộp thư yêu cầu đi/đến | FR-05 |
| GET | `/api/requests/{id}` | Participant | Chi tiết yêu cầu | FR-05 |
| POST | `/api/requests/{id}/accept` | Receiver | Chấp nhận: yêu cầu → `ACCEPTED`, tạo phiên `SCHEDULED` | FR-05 |
| POST | `/api/requests/{id}/reject` | Receiver | Từ chối, kèm `note` tùy chọn | FR-05 |
| POST | `/api/requests/{id}/cancel` | Sender | Hủy khi còn `PENDING` | FR-05 |

Ví dụ tạo yêu cầu hai chiều:

```json
POST /api/requests
{
  "receiverId": 17,
  "type": "TWO_WAY",
  "senderLearnSkillId": 3,
  "receiverLearnSkillId": 21,
  "proposedStartAt": "2026-11-04T19:00:00+07:00",
  "durationMinutes": 90,
  "mode": "ONLINE",
  "locationOrLink": "https://meet.google.com/abc-defg-hij",
  "message": "Mình học Java tối thứ Ba được không?"
}
```

Backend kiểm tra khi tạo yêu cầu:

1. Người nhận đang `ACTIVE`, có `role = STUDENT` và khác người gửi.
2. `senderLearnSkillId` có $Sim > 0$ với một kỹ năng dạy của người nhận. Nếu là `TWO_WAY` thì `receiverLearnSkillId` cũng phải có $Sim > 0$ với một kỹ năng dạy của người gửi.
3. `proposedStartAt` ở tương lai.
4. Chưa có yêu cầu `PENDING` nào giữa hai người theo cùng chiều (spec mục 6.2).

### 4.7. Phiên trao đổi – `exchange`

| Phương thức | Đường dẫn | Quyền | Mô tả | FR |
| --- | --- | --- | --- | --- |
| GET | `/api/sessions?status=` | Active | Các phiên của tôi | FR-06 |
| GET | `/api/sessions/{id}` | Participant, Admin | Chi tiết phiên, **kèm thông tin liên hệ và địa điểm/link** của bên kia | FR-06, NFR-02 |
| POST | `/api/sessions/{id}/start` | Participant | `SCHEDULED` → `IN_PROGRESS` (spec mục 6.2) | FR-06 |
| POST | `/api/sessions/{id}/complete` | Participant | Lần 1: → `WAITING_CONFIRMATION`. Lần 2 (bên còn lại): → `COMPLETED` | FR-06 |
| POST | `/api/sessions/{id}/cancel` | Participant | Hủy kèm `reason` bắt buộc; chỉ từ `SCHEDULED` hoặc `IN_PROGRESS` | FR-06 |

### 4.8. Đánh giá và báo cáo sự cố – `review`, `admin`

| Phương thức | Đường dẫn | Quyền | Mô tả | FR |
| --- | --- | --- | --- | --- |
| POST | `/api/sessions/{id}/reviews` | Participant | Đánh giá `{rating, comment}`; chỉ khi phiên `COMPLETED`, mỗi người một lần | FR-07 |
| POST | `/api/sessions/{id}/reports` | Participant | Gửi báo cáo sự cố `{reason}` | FR-09 |

### 4.9. Quản trị – `admin`

| Phương thức | Đường dẫn | Quyền | Mô tả | FR |
| --- | --- | --- | --- | --- |
| POST | `/api/admin/skill-categories` | Admin | Thêm nhóm kỹ năng | FR-08 |
| POST | `/api/admin/skills` | Admin | Thêm kỹ năng | FR-08 |
| PUT | `/api/admin/skills/{id}` | Admin | Sửa tên, nhóm, ẩn/hiện (`isActive`) | FR-08 |
| POST | `/api/admin/skill-relations` | Admin | Đánh dấu hai kỹ năng cùng nhóm là liên quan | FR-08 |
| DELETE | `/api/admin/skill-relations/{aId}/{bId}` | Admin | Bỏ quan hệ liên quan | FR-08 |
| GET | `/api/admin/reports?status=OPEN` | Admin | Hàng đợi báo cáo sự cố | FR-09 |
| POST | `/api/admin/reports/{id}/resolve` | Admin | `{resolution: SESSION_COMPLETED \| SESSION_CANCELLED, note}`; chốt phiên tương ứng | FR-09 |
| PUT | `/api/admin/users/{id}/status` | Admin | Khóa/mở khóa tài khoản (`ACTIVE` ↔ `LOCKED`) | FR-08 |

### 4.10. Ma trận phân quyền rút gọn

| Hành động | Sender | Receiver | Người khác | Admin |
| --- | :---: | :---: | :---: | :---: |
| Tạo yêu cầu | ✔ | – | – | ✖ |
| Chấp nhận / từ chối | ✖ | ✔ | ✖ | ✖ |
| Hủy yêu cầu `PENDING` | ✔ | ✖ | ✖ | ✖ |
| Xem liên hệ, địa điểm/link | ✔ sau `ACCEPTED` | ✔ sau `ACCEPTED` | ✖ | ✔ |
| Bắt đầu / hoàn thành / hủy phiên | ✔ | ✔ | ✖ | ✖ |
| Đánh giá | ✔ | ✔ | ✖ | ✖ |
| Báo cáo sự cố | ✔ | ✔ | ✖ | – |
| Xử lý báo cáo | ✖ | ✖ | ✖ | ✔ |

## 5. Sơ đồ trạng thái

Yêu cầu và phiên được thiết kế thành **hai máy trạng thái riêng**, ứng với hai bảng `exchange_requests` và `exchange_sessions`. Đánh giá không phải là trạng thái của phiên mà là bản ghi riêng trong bảng `reviews`.

### 5.1. Yêu cầu trao đổi

```mermaid
stateDiagram-v2
    [*] --> PENDING : Sender tạo yêu cầu
    PENDING --> ACCEPTED : Receiver chấp nhận
    PENDING --> REJECTED : Receiver từ chối
    PENDING --> CANCELLED : Sender hủy
    ACCEPTED --> [*] : Tạo phiên SCHEDULED
    REJECTED --> [*]
    CANCELLED --> [*]
```

| Từ | Sang | Ai | API | Điều kiện / tác động phụ |
| --- | --- | --- | --- | --- |
| – | `PENDING` | Sender | `POST /requests` | Các kiểm tra ở mục 4.6 |
| `PENDING` | `ACCEPTED` | Receiver | `POST /requests/{id}/accept` | Tạo phiên `SCHEDULED` trong **cùng giao dịch** (transaction); mở quyền xem liên hệ |
| `PENDING` | `REJECTED` | Receiver | `POST /requests/{id}/reject` | Lưu `response_note` |
| `PENDING` | `CANCELLED` | Sender | `POST /requests/{id}/cancel` | |

`ACCEPTED`, `REJECTED` và `CANCELLED` là trạng thái cuối. Mọi thao tác khác trả về **409**.

**Chống thao tác đồng thời:** người gửi bấm "Hủy" đúng lúc người nhận bấm "Chấp nhận". Cột `version` của `exchange_requests` làm thao tác đến sau thất bại (409), nên không xảy ra trường hợp yêu cầu đã hủy mà vẫn sinh ra phiên.

### 5.2. Phiên trao đổi

```mermaid
stateDiagram-v2
    [*] --> SCHEDULED : Yêu cầu được chấp nhận
    SCHEDULED --> IN_PROGRESS : Một bên bấm "Bắt đầu"
    SCHEDULED --> CANCELLED : Một bên hủy (kèm lý do)
    IN_PROGRESS --> WAITING_CONFIRMATION : Một bên bấm "Hoàn thành"
    IN_PROGRESS --> CANCELLED : Một bên hủy (kèm lý do)
    WAITING_CONFIRMATION --> COMPLETED : Bên còn lại xác nhận
    WAITING_CONFIRMATION --> COMPLETED : Admin xử lý báo cáo
    WAITING_CONFIRMATION --> CANCELLED : Admin xử lý báo cáo
    COMPLETED --> [*]
    CANCELLED --> [*]
```

| Từ | Sang | Ai | API | Điều kiện / tác động phụ |
| --- | --- | --- | --- | --- |
| `SCHEDULED` | `IN_PROGRESS` | Participant | `POST /sessions/{id}/start` | Chỉ từ 15 phút trước `start_at` (spec mục 6.2); ghi `started_at` |
| `SCHEDULED` | `CANCELLED` | Participant | `POST /sessions/{id}/cancel` | Lý do bắt buộc; `started_at` vẫn NULL nên **không** tính vào CompletionRate |
| `IN_PROGRESS` | `WAITING_CONFIRMATION` | Participant | `POST /sessions/{id}/complete` | Ghi `first_completed_by` |
| `IN_PROGRESS` | `CANCELLED` | Participant | `POST /sessions/{id}/cancel` | Lý do bắt buộc; **có** tính vào mẫu số CompletionRate (phiên đã bắt đầu và đã kết thúc) |
| `WAITING_CONFIRMATION` | `COMPLETED` | Bên còn lại | `POST /sessions/{id}/complete` | Người gọi phải khác `first_completed_by`; mở quyền đánh giá |
| `WAITING_CONFIRMATION` | `COMPLETED` / `CANCELLED` | Admin | `POST /admin/reports/{id}/resolve` | Chỉ khi có báo cáo `OPEN`; báo cáo chuyển sang `RESOLVED` |

**Các chuyển trạng thái bị cấm** (trả về 409, cần có unit test):

- Người đã bấm hoàn thành bấm xác nhận lần hai.
- Hủy phiên đang `WAITING_CONFIRMATION` (spec 6.2: không tự động hoặc tùy tiện hủy khi có bất đồng; chỉ admin chốt).
- Mọi thao tác trên phiên `COMPLETED` hoặc `CANCELLED`, ngoại trừ đánh giá phiên `COMPLETED`.
- Bắt đầu phiên quá sớm so với `start_at`.

**Chống thao tác đồng thời:** nếu hai người cùng bấm "Hoàn thành" một lúc, cột `version` (optimistic locking) làm request thứ hai thất bại thay vì ghi đè. Client tải lại và nhận trạng thái mới.

## 6. Prototype giao diện

### 6.1. Danh sách màn hình

| Mã | Màn hình | Nội dung chính | API sử dụng |
| --- | --- | --- | --- |
| S01 | Đăng ký / Đăng nhập | Đăng ký: email trường, mật khẩu, họ tên. Đăng nhập: email, mật khẩu | `/auth/*` |
| S02 | Xác thực email | Mở từ link trong email (đọc `token` trên URL); nút gửi lại link | `/auth/verify-email`, `/auth/resend-verification` |
| S03 | Hồ sơ của tôi | Thông tin cá nhân, liên hệ riêng tư, hình thức học, lưới lịch rảnh 7×3 | `/me`, `/me/availability` |
| S04 | Kỹ năng của tôi | Hai danh sách: kỹ năng dạy (có trình độ 1–4) và kỹ năng muốn học (mục tiêu tùy chọn) | `/skills`, `/me/teach-skills`, `/me/learn-skills` |
| S05 | Gợi ý ghép cặp | Hai tab "Trao đổi hai chiều" và "Tìm người hướng dẫn"; thẻ ứng viên có điểm và lý do | `/matches` |
| S06 | Hồ sơ người khác | Thông tin công khai, kỹ năng, lịch rảnh; nút "Gửi yêu cầu". Phần đánh giá thêm ở tuần 7 | `/users/{id}`, `/users/{id}/reviews` |
| S07 | Gửi yêu cầu | Chọn kỹ năng từng chiều, thời điểm (gợi ý khung trùng), thời lượng, hình thức, địa điểm/link | `POST /requests` |
| S08 | Hộp yêu cầu | Tab Đã nhận / Đã gửi; nút Chấp nhận, Từ chối, Hủy | `/requests` |
| S09 | Phiên của tôi và chi tiết phiên | Trạng thái, liên hệ bên kia, nút Bắt đầu / Hoàn thành / Hủy / Báo cáo / Đánh giá | `/sessions`, `/sessions/{id}/*` |
| S10 | Tìm kiếm | Tìm theo kỹ năng dạy và/hoặc tên | `/users/search`, `/skills` |
| A01 | Quản trị danh mục kỹ năng | Bảng kỹ năng theo nhóm; thêm/sửa/ẩn; quản lý quan hệ liên quan | `/admin/skills*` |
| A02 | Xử lý báo cáo sự cố | Hàng đợi báo cáo; xem phiên; chốt Hoàn thành / Hủy kèm ghi chú | `/admin/reports*` |

### 6.2. Luồng điều hướng

```mermaid
flowchart TD
    S01[S01 Đăng ký / Đăng nhập] --> S02[S02 Xác thực email]
    S02 --> S03[S03 Hồ sơ của tôi]
    S03 --> S04[S04 Kỹ năng của tôi]
    S04 --> S05[S05 Gợi ý ghép cặp]
    S05 --> S06[S06 Hồ sơ người khác]
    S10[S10 Tìm kiếm] --> S06
    S06 --> S07[S07 Gửi yêu cầu]
    S07 --> S08[S08 Hộp yêu cầu]
    S08 -- "Chấp nhận" --> S09[S09 Phiên của tôi]
    S09 -- "Hoàn thành" --> R[Đánh giá]
    S09 -- "Bất đồng" --> RP[Báo cáo sự cố] --> A02[A02 Admin xử lý]
```

Người dùng mới được dẫn qua S03 → S04 vì chưa có kỹ năng thì không có gợi ý.

### 6.3. Bản phác thảo (wireframe) các màn hình chính

**S05 – Gợi ý ghép cặp**

```
+------------------------------------------------------------------+
| Skill Exchange    Gợi ý | Yêu cầu (2) | Phiên | Tìm kiếm  [Avatar]|
+------------------------------------------------------------------+
| [ Trao đổi hai chiều (8) ]  [ Tìm người hướng dẫn (15) ]           |
|                                                                  |
| +--------------------------------------------------------------+ |
| | Trần B                           Mức phù hợp: 78%   ★ 4.2    | |
| | Bạn học: Java (trùng, trình độ 3)                            | |
| | Bạn dạy: Tiếng Anh → IELTS Writing (kỹ năng liên quan)       | |
| | Lịch trùng: Tối T3, Tối T6     Hình thức: Online             | |
| | [Xem chi tiết điểm]                [Xem hồ sơ] [Gửi yêu cầu] | |
| +--------------------------------------------------------------+ |
| +--------------------------------------------------------------+ |
| | Lê C                             Mức phù hợp: 74%   Mới      | |
| | ...                                                          | |
+------------------------------------------------------------------+
```

**S07 – Gửi yêu cầu**

```
+------------------------------------------------------------------+
| Gửi yêu cầu trao đổi tới Trần B                  Loại: Hai chiều |
|------------------------------------------------------------------|
| Bạn muốn học:      [ Java                         v ]            |
| Bạn sẽ dạy:        [ Tiếng Anh                    v ]            |
| Thời điểm:         [ 04/11/2026 ] [ 19:00 ]                      |
|                    Gợi ý khung trùng: (Tối T3) (Tối T6)          |
| Thời lượng:        [ 90 ] phút                                   |
| Hình thức:         (o) Online   ( ) Offline                      |
| Link / địa điểm:   [ https://meet.google.com/...   ]             |
|                    Chỉ hiển thị cho Trần B sau khi chấp nhận     |
| Lời nhắn:          [                                ]            |
|                                         [Hủy]  [Gửi yêu cầu]     |
+------------------------------------------------------------------+
```

**S09 – Chi tiết phiên**

```
+------------------------------------------------------------------+
| Phiên #12  Java <-> Tiếng Anh         Trạng thái: CHỜ XÁC NHẬN   |
|------------------------------------------------------------------|
| Thời gian: 19:00 04/11/2026 (90 phút)    Hình thức: Online       |
| Link: https://meet.google.com/abc-defg-hij                       |
| Liên hệ Trần B: 09xx xxx xxx · zalo: ...                         |
|------------------------------------------------------------------|
| Trần B đã bấm Hoàn thành. Bạn xác nhận buổi học đã diễn ra?      |
|               [Xác nhận hoàn thành]   [Báo cáo sự cố]            |
+------------------------------------------------------------------+
```

Các nút hiện ra tùy theo trạng thái phiên và vai trò của người xem, đúng bảng chuyển trạng thái ở mục 5.2. Giao diện chỉ ẩn nút để dễ dùng; quyền vẫn được kiểm tra lại ở backend.

## 7. Bảng truy vết yêu cầu

Cột "Kiểm thử" trỏ tới nhóm unit test ở spec mục 7.1 (N1–N5) hoặc file ca kiểm thử của tuần đó. Cột "Tuần" theo spec mục 9 (đã điều chỉnh 2026-10-29). Ô "chưa có" là phần sẽ bổ sung khi chốt tài liệu của tuần tương ứng.

| Yêu cầu | Bảng dữ liệu | API | Màn hình | Kiểm thử | Tuần |
| --- | --- | --- | --- | --- | --- |
| FR-01 Tài khoản, xác thực email | `users`, `user_tokens` | `/auth/*` | S01, S02 | `tuan_3/kiem_thu` | 3 |
| FR-02 Hồ sơ, lịch rảnh, hình thức học | `users`, `user_availability` | `/me`, `/me/availability` | S03 | `tuan_3/kiem_thu` | 3 |
| FR-03 Kỹ năng và trình độ | `user_teach_skills`, `user_learn_skills`, `skills`, `skill_categories` | `/me/teach-skills`, `/me/learn-skills`, `/skills`, `/skill-categories` | S04 | `tuan_3/kiem_thu` | 3 |
| FR-04 Gợi ý, phân loại, xếp hạng | Đọc các bảng trên, `skill_relations`, `reviews`, `exchange_sessions` | `/matches` | S05 | N1, N2, N3 | 4 |
| FR-05 Yêu cầu trao đổi | `exchange_requests` | `/requests*` | S06, S07, S08 | N5 | 6 |
| FR-06 Phiên và xác nhận hoàn thành | `exchange_sessions` | `/sessions*` | S09 | N5 | 6 |
| FR-07 Đánh giá | `reviews` | `/sessions/{id}/reviews`, `/users/{id}/reviews` | S09, S06 | N5 | 7 |
| FR-08 Quản trị danh mục kỹ năng | `skills`, `skill_categories`, `skill_relations` | `/admin/skill*` | A01 | chưa có | 5 |
| FR-08 Khóa/mở khóa tài khoản | `users.status` | `/admin/users/{id}/status` | chưa có màn hình (rà soát R41) | chưa có | 7 |
| FR-09 Báo cáo sự cố | `incident_reports` | `/sessions/{id}/reports` (tuần 6), `/admin/reports*` (tuần 7) | S09, A02 | N5 | 6, 7 |
| FR-10 Tìm kiếm, hồ sơ công khai | `users`, `user_teach_skills`, `user_learn_skills`, `user_availability` | `/users/search`, `/users/{id}` | S06, S10 | N5 (riêng tư) | 4 |
| FR-11 Thực nghiệm | Không dùng bảng nghiệp vụ (chạy trong bộ nhớ) | – (module `evaluation`, chạy offline) | – | N4 | 5 |
| FR-12 Lọc theo hình thức học (nên có) | – | Lọc phía giao diện trên kết quả `/matches` | S05 | chưa có | 8 nếu kịp |
| FR-13, FR-15, FR-16 (nên có) | Chưa thiết kế | – | – | – | 8 nếu kịp |
| FR-14 Lịch sử trao đổi (nên có) | `exchange_requests`, `exchange_sessions` | Dùng lại `/requests`, `/sessions` | S08, S09 | chưa có | 7 nếu kịp |
| FR-23 Quên mật khẩu (nên có) | `user_tokens` (`RESET_PASSWORD`) | Chưa thiết kế | – | – | 8 nếu kịp |
| NFR-01 Bảo mật | `password_hash`, `user_tokens.token_hash` | JWT, kiểm tra quyền mọi API | – | `tuan_3/kiem_thu` nhóm C, N5 | 3 trở đi; kiểm thử tổng ở 8 |
| NFR-02 Riêng tư | `email`, `phone`, `contact_info`, `location_or_link` | Liên hệ và địa điểm/link chỉ trả về trong `/me`, `/sessions/{id}`; email trường không có trong `/users/{id}` | S06, S09 | N5 | 4, 6 |
| NFR-03 Hiệu năng | – | `/matches`, module `evaluation` | – | Đo thời gian (cách đo chốt ở rà soát R30) | 5, 8 |
| NFR-04 Tính đúng | – | Lõi matching | – | N2 | 4 |
| NFR-05 Bảo trì | – | Tách lọc cứng và chấm điểm, trọng số tập trung | – | Xem xét code | 4 |
| NFR-06 Tái lập thực nghiệm | – | Module `evaluation` | – | N4 | 5 |
| NFR-07 Khả năng triển khai | `docker-compose.yml` | – | – | Dựng lại từ repo theo README | 9 |
| NFR-08 Tương thích nền tảng | – | – | Mọi màn hình | Kiểm tra thủ công 3 trình duyệt, rộng 360 px | 8 |
| NFR-09 Khả năng sử dụng | – | Trường lý do trong `/matches` | S05 | chưa có | 4 |

## 8. Giả định thiết kế (đã chốt hết)

Khi viết thiết kế tuần 2, spec tuần 1 chưa quy định các điểm dưới đây nên thiết kế tạm chọn phương án đơn giản nhất. Đến 2026-10-29 cả 9 giả định đã được chốt vào tài liệu nguồn; bảng này chỉ còn để tra cứu. Khi đọc hoặc sửa, dùng nơi chốt ở cột cuối, không dùng mã G.

| Mã | Nội dung | Đã chốt ở |
| --- | --- | --- |
| G1 | Phiên chuyển sang `IN_PROGRESS` khi một bên bấm "Bắt đầu", chỉ được bấm từ 15 phút trước giờ hẹn. Không dùng tác vụ chạy định kỳ | Spec 6.2 |
| G2 | Yêu cầu `PENDING` và phiên `IN_PROGRESS` chưa có thời hạn tự hết hạn trong MVP | Spec 6.2 |
| G3 | Khi một bên hủy phiên đang diễn ra, phiên tính vào mẫu số CompletionRate của **cả hai** bên | Spec 4.3, 6.2 |
| G4 | Mỗi người chỉ có một yêu cầu `PENDING` tới cùng một người nhận | Spec 6.2 |
| G5 | JWT có hạn 24 giờ, chưa dùng refresh token | Quyết định tuần 3, D14 |
| G6 | Email trường được nhận diện bằng đuôi tên miền cấu hình sẵn | Quyết định tuần 3, D9 |
| G7 | Có API khóa/mở khóa tài khoản cho admin | Spec FR-08, 6.3 |
| G8 | Link xác thực email hết hạn sau 24 giờ; gửi lại link thì vô hiệu link cũ | Quyết định tuần 3, D7 |
| G9 | Tài khoản `PENDING_VERIFICATION` được đăng nhập và sửa hồ sơ; `LOCKED` bị chặn (403), kể cả với token đã cấp trước khi bị khóa | Quyết định tuần 3, D12, D13 |

## 9. Kế hoạch tuần 3

Theo kế hoạch hợp nhất ở spec tuần 1 mục 9:

- `V1__init.sql` đã có và đã kiểm thử (mục 3.6). Viết thêm `V2__seed_skills.sql` cho danh mục kỹ năng.
- Làm luồng đăng ký, xác thực email, đăng nhập JWT (mục 4.2).
- Làm API `/api/me*` cho hồ sơ, lịch rảnh, kỹ năng (mục 4.3), API danh mục kỹ năng (mục 4.4) và màn hình S01–S04.
- **Không** làm ở tuần 3: `/api/users/{id}`, `/api/users/search` (tuần 4), `/api/users/{id}/reviews` (tuần 7).

Từ tuần 3, phạm vi và quyết định chi tiết của từng tuần nằm ở `docs/tuan_<n>/Quyet_dinh_tuan_<n>.md`.
