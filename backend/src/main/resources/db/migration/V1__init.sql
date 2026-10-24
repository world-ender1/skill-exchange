-- =====================================================================
-- V1__init.sql – Lược đồ ban đầu của Skill Exchange
-- Nguồn: docs/tuan_2/Tuan_2_Thiet_ke.md, mục 3.
-- Chỗ ghi [BỔ SUNG] là chi tiết phát hiện thiếu khi viết SQL (K1–K7),
-- đã được cập nhật ngược vào tài liệu thiết kế.
-- =====================================================================

-- ---------- Tài khoản ----------
CREATE TABLE users (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email             VARCHAR(255) NOT NULL UNIQUE,
    password_hash     VARCHAR(100) NOT NULL,
    full_name         VARCHAR(100) NOT NULL,
    role              VARCHAR(20)  NOT NULL DEFAULT 'STUDENT'
                      CHECK (role IN ('STUDENT', 'ADMIN')),
    status            VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION'
                      CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED')),
    email_verified_at TIMESTAMPTZ,
    learning_mode     VARCHAR(10)  CHECK (learning_mode IN ('ONLINE', 'OFFLINE', 'ANY')), -- NULL = chưa khai báo
    bio               TEXT,
    phone             VARCHAR(20),   -- riêng tư (NFR-02)
    contact_info      TEXT,          -- riêng tư (NFR-02)
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_tokens (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose     VARCHAR(30) NOT NULL                       -- [BỔ SUNG] NOT NULL
                CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD')),
    token_hash  VARCHAR(100) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()          -- [BỔ SUNG]
);

-- ---------- Danh mục kỹ năng ----------
CREATE TABLE skill_categories (
    id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name  VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE skills (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id  BIGINT NOT NULL REFERENCES skill_categories(id),
    name         VARCHAR(100) NOT NULL UNIQUE,
    is_active    BOOLEAN NOT NULL DEFAULT TRUE             -- false = bị ẩn (FR-08)
);

CREATE TABLE skill_relations (
    skill_a_id  BIGINT NOT NULL REFERENCES skills(id),
    skill_b_id  BIGINT NOT NULL REFERENCES skills(id),
    PRIMARY KEY (skill_a_id, skill_b_id),
    CHECK (skill_a_id < skill_b_id)                         -- mỗi cặp lưu đúng một dòng
);

-- ---------- Kỹ năng và lịch của người dùng ----------
CREATE TABLE user_teach_skills (
    user_id   BIGINT   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id  BIGINT   NOT NULL REFERENCES skills(id),
    level     SMALLINT NOT NULL CHECK (level BETWEEN 1 AND 4),
    PRIMARY KEY (user_id, skill_id)
);

CREATE TABLE user_learn_skills (
    user_id       BIGINT   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id      BIGINT   NOT NULL REFERENCES skills(id),
    target_level  SMALLINT CHECK (target_level BETWEEN 1 AND 4),   -- NULL = không đặt mục tiêu
    PRIMARY KEY (user_id, skill_id)
);

CREATE TABLE user_availability (
    user_id      BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    day_of_week  SMALLINT    NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),  -- 1 = Thứ Hai
    period       VARCHAR(10) NOT NULL CHECK (period IN ('MORNING', 'AFTERNOON', 'EVENING')),
    PRIMARY KEY (user_id, day_of_week, period)
);

-- ---------- Yêu cầu trao đổi ----------
CREATE TABLE exchange_requests (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sender_id                BIGINT NOT NULL REFERENCES users(id),
    receiver_id              BIGINT NOT NULL REFERENCES users(id),
    type                     VARCHAR(10) NOT NULL CHECK (type IN ('TWO_WAY', 'ONE_WAY')),
    sender_learn_skill_id    BIGINT NOT NULL REFERENCES skills(id),
    receiver_learn_skill_id  BIGINT REFERENCES skills(id),
    proposed_start_at        TIMESTAMPTZ NOT NULL,
    duration_minutes         SMALLINT NOT NULL CHECK (duration_minutes BETWEEN 15 AND 480),
    mode                     VARCHAR(10) NOT NULL CHECK (mode IN ('ONLINE', 'OFFLINE')),
    location_or_link         VARCHAR(500),
    message                  TEXT,
    status                   VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
    response_note            TEXT,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    responded_at             TIMESTAMPTZ,
    version                  INTEGER NOT NULL DEFAULT 0,  -- khóa lạc quan: chống "hủy" và "chấp nhận" cùng lúc
    CHECK (sender_id <> receiver_id),
    -- Hai chiều bắt buộc có kỹ năng chiều ngược lại; một chiều thì không được có.
    CHECK ((type = 'TWO_WAY') = (receiver_learn_skill_id IS NOT NULL))
);

-- G4: mỗi cặp người gửi → người nhận chỉ có một yêu cầu đang chờ.
CREATE UNIQUE INDEX uq_requests_one_pending
    ON exchange_requests (sender_id, receiver_id) WHERE status = 'PENDING';

-- ---------- Phiên trao đổi ----------
CREATE TABLE exchange_sessions (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_id          BIGINT NOT NULL UNIQUE REFERENCES exchange_requests(id),
    start_at            TIMESTAMPTZ NOT NULL,
    duration_minutes    SMALLINT    NOT NULL CHECK (duration_minutes BETWEEN 15 AND 480),
    mode                VARCHAR(10) NOT NULL CHECK (mode IN ('ONLINE', 'OFFLINE')),
    location_or_link    VARCHAR(500),
    status              VARCHAR(25) NOT NULL DEFAULT 'SCHEDULED'
                        CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'WAITING_CONFIRMATION',
                                          'COMPLETED', 'CANCELLED')),
    started_at          TIMESTAMPTZ,                       -- khác NULL = đã bắt đầu
    first_completed_by  BIGINT REFERENCES users(id),
    completed_at        TIMESTAMPTZ,
    cancelled_by        BIGINT REFERENCES users(id),
    cancel_reason       TEXT,
    cancelled_at        TIMESTAMPTZ,
    version             INTEGER NOT NULL DEFAULT 0,        -- khóa lạc quan
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),-- [BỔ SUNG]
    -- [BỔ SUNG] Biến các câu chữ trong mục 3.3 thành ràng buộc DB:
    CHECK (status <> 'CANCELLED' OR (cancel_reason IS NOT NULL AND cancelled_at IS NOT NULL)),
    CHECK (status <> 'COMPLETED' OR completed_at IS NOT NULL),
    CHECK (status NOT IN ('IN_PROGRESS', 'WAITING_CONFIRMATION', 'COMPLETED') OR started_at IS NOT NULL),
    CHECK (status <> 'WAITING_CONFIRMATION' OR first_completed_by IS NOT NULL)
);

-- ---------- Đánh giá ----------
CREATE TABLE reviews (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id   BIGINT   NOT NULL REFERENCES exchange_sessions(id),
    reviewer_id  BIGINT   NOT NULL REFERENCES users(id),
    reviewee_id  BIGINT   NOT NULL REFERENCES users(id),
    rating       SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (reviewer_id <> reviewee_id),
    UNIQUE (session_id, reviewer_id)
);

-- ---------- Báo cáo sự cố ----------
CREATE TABLE incident_reports (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id   BIGINT NOT NULL REFERENCES exchange_sessions(id),
    reporter_id  BIGINT NOT NULL REFERENCES users(id),
    reason       TEXT   NOT NULL,
    status       VARCHAR(10) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED')),
    resolution   VARCHAR(20) CHECK (resolution IN ('SESSION_COMPLETED', 'SESSION_CANCELLED')),
    admin_note   TEXT,
    resolved_by  BIGINT REFERENCES users(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at  TIMESTAMPTZ,
    -- [BỔ SUNG] Đã xử lý thì phải có đủ kết luận, người xử lý, thời điểm; đang mở thì chưa được có cột nào.
    CHECK (status <> 'RESOLVED' OR (resolution IS NOT NULL AND resolved_by IS NOT NULL AND resolved_at IS NOT NULL)),
    CHECK (status <> 'OPEN'     OR (resolution IS NULL     AND resolved_by IS NULL     AND resolved_at IS NULL))
);

-- Mỗi phiên có tối đa một báo cáo đang mở.
CREATE UNIQUE INDEX uq_reports_one_open
    ON incident_reports (session_id) WHERE status = 'OPEN';

-- ---------- Chỉ mục (mục 3.4) ----------
CREATE INDEX idx_teach_skill        ON user_teach_skills (skill_id);
CREATE INDEX idx_learn_skill        ON user_learn_skills (skill_id);
CREATE INDEX idx_requests_receiver  ON exchange_requests (receiver_id, status);
CREATE INDEX idx_requests_sender    ON exchange_requests (sender_id, status);
CREATE INDEX idx_sessions_status    ON exchange_sessions (status);
CREATE INDEX idx_reviews_reviewee   ON reviews (reviewee_id);
CREATE INDEX idx_reports_status     ON incident_reports (status);
