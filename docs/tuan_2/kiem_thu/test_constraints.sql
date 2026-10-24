-- =====================================================================
-- Kiểm thử lược đồ V1__init.sql trên database TẠM (design_test).
-- Mỗi ca: chạy 1 câu SQL, so với kết quả mong đợi (bị chặn / được nhận).
-- Cách chạy: xem docs/tuan_2/kiem_thu/README.md
-- =====================================================================
\set QUIET on
\o /dev/null
SET client_min_messages = warning;

CREATE TEMP TABLE results (no SERIAL, label TEXT, expected TEXT, actual TEXT, detail TEXT);

-- Câu SQL phải bị DB từ chối
CREATE FUNCTION pg_temp.expect_fail(label TEXT, stmt TEXT) RETURNS void AS $$
BEGIN
    EXECUTE stmt;
    INSERT INTO results(label, expected, actual, detail) VALUES (label, 'CHẶN', 'LỌT', 'câu sai vẫn được nhận');
EXCEPTION WHEN others THEN
    INSERT INTO results(label, expected, actual, detail) VALUES (label, 'CHẶN', 'CHẶN', SQLERRM);
END $$ LANGUAGE plpgsql;

-- Câu SQL phải được DB nhận
CREATE FUNCTION pg_temp.expect_ok(label TEXT, stmt TEXT) RETURNS void AS $$
BEGIN
    EXECUTE stmt;
    INSERT INTO results(label, expected, actual, detail) VALUES (label, 'NHẬN', 'NHẬN', '');
EXCEPTION WHEN others THEN
    INSERT INTO results(label, expected, actual, detail) VALUES (label, 'NHẬN', 'CHẶN', SQLERRM);
END $$ LANGUAGE plpgsql;

-- Giá trị mong đợi của truy vấn tính toán
CREATE FUNCTION pg_temp.expect_eq(label TEXT, query TEXT, want TEXT) RETURNS void AS $$
DECLARE got TEXT;
BEGIN
    EXECUTE query INTO got;
    INSERT INTO results(label, expected, actual, detail)
    VALUES (label, want, coalesce(got, 'NULL'), CASE WHEN got IS NOT DISTINCT FROM want THEN '' ELSE 'sai giá trị' END);
END $$ LANGUAGE plpgsql;

-- =====================================================================
-- A. Bộ dữ liệu HỢP LỆ (phải nhận hết) – kịch bản trao đổi thành công
-- =====================================================================
SELECT pg_temp.expect_ok('A01 nhóm kỹ năng',
  $q$INSERT INTO skill_categories(name) VALUES ('Lập trình'), ('Ngoại ngữ')$q$);
SELECT pg_temp.expect_ok('A02 kỹ năng',
  $q$INSERT INTO skills(category_id, name) VALUES (1,'Java'), (1,'Kotlin'), (2,'Tiếng Anh'), (1,'Python')$q$);
SELECT pg_temp.expect_ok('A03 quan hệ Java–Kotlin (1<2)',
  $q$INSERT INTO skill_relations VALUES (1, 2)$q$);
SELECT pg_temp.expect_ok('A04 ba người dùng + admin',
  $q$INSERT INTO users(email, password_hash, full_name, status, learning_mode) VALUES
     ('a@st.edu.vn','$2a$hash','An','ACTIVE','ONLINE'),
     ('b@st.edu.vn','$2a$hash','Bình','ACTIVE','ANY'),
     ('c@st.edu.vn','$2a$hash','Chi','ACTIVE',NULL)$q$);
SELECT pg_temp.expect_ok('A05 admin',
  $q$INSERT INTO users(email, password_hash, full_name, role, status) VALUES ('admin@st.edu.vn','$2a$hash','Admin','ADMIN','ACTIVE')$q$);
SELECT pg_temp.expect_ok('A06 kỹ năng dạy (level 1–4)',
  $q$INSERT INTO user_teach_skills VALUES (1,3,4), (2,1,3), (3,1,2)$q$);
SELECT pg_temp.expect_ok('A07 kỹ năng muốn học, target NULL được phép',
  $q$INSERT INTO user_learn_skills VALUES (1,2,3), (2,3,NULL)$q$);
SELECT pg_temp.expect_ok('A08 lịch rảnh',
  $q$INSERT INTO user_availability VALUES (1,1,'MORNING'),(1,3,'EVENING'),(1,6,'AFTERNOON'),
                                           (2,1,'MORNING'),(2,3,'EVENING'),(2,7,'EVENING')$q$);
SELECT pg_temp.expect_ok('A09 yêu cầu TWO_WAY A→B đang chờ',
  $q$INSERT INTO exchange_requests(sender_id, receiver_id, type, sender_learn_skill_id, receiver_learn_skill_id,
       proposed_start_at, duration_minutes, mode) VALUES (1,2,'TWO_WAY',2,3,'2026-11-02 08:00+07',90,'ONLINE')$q$);
SELECT pg_temp.expect_ok('A10 yêu cầu ONE_WAY C→B đang chờ',
  $q$INSERT INTO exchange_requests(sender_id, receiver_id, type, sender_learn_skill_id,
       proposed_start_at, duration_minutes, mode) VALUES (3,2,'ONE_WAY',1,'2026-11-03 19:00+07',60,'OFFLINE')$q$);
SELECT pg_temp.expect_ok('A11 B chấp nhận yêu cầu 1',
  $q$UPDATE exchange_requests SET status='ACCEPTED', responded_at=now() WHERE id=1$q$);
SELECT pg_temp.expect_ok('A12 tạo phiên từ yêu cầu 1',
  $q$INSERT INTO exchange_sessions(request_id, start_at, duration_minutes, mode)
     SELECT id, proposed_start_at, duration_minutes, mode FROM exchange_requests WHERE id=1$q$);
SELECT pg_temp.expect_ok('A13 bắt đầu phiên',
  $q$UPDATE exchange_sessions SET status='IN_PROGRESS', started_at=now(), version=version+1 WHERE id=1$q$);
SELECT pg_temp.expect_ok('A14 A bấm hoàn thành trước',
  $q$UPDATE exchange_sessions SET status='WAITING_CONFIRMATION', first_completed_by=1, version=version+1 WHERE id=1$q$);
SELECT pg_temp.expect_ok('A15 B xác nhận → COMPLETED',
  $q$UPDATE exchange_sessions SET status='COMPLETED', completed_at=now(), version=version+1 WHERE id=1$q$);
SELECT pg_temp.expect_ok('A16 hai người đánh giá nhau',
  $q$INSERT INTO reviews(session_id, reviewer_id, reviewee_id, rating) VALUES (1,1,2,5), (1,2,1,4)$q$);
SELECT pg_temp.expect_ok('A17 sau khi A→B hết PENDING, A gửi B yêu cầu mới được',
  $q$INSERT INTO exchange_requests(sender_id, receiver_id, type, sender_learn_skill_id,
       proposed_start_at, duration_minutes, mode) VALUES (1,2,'ONE_WAY',2,'2026-11-09 08:00+07',60,'ONLINE')$q$);
SELECT pg_temp.expect_ok('A18 chiều ngược B→A vẫn gửi được dù A→B đang chờ',
  $q$INSERT INTO exchange_requests(sender_id, receiver_id, type, sender_learn_skill_id,
       proposed_start_at, duration_minutes, mode) VALUES (2,1,'ONE_WAY',3,'2026-11-10 08:00+07',60,'ONLINE')$q$);

-- Phiên thứ 2 (C–B): bắt đầu rồi hủy → dùng cho CompletionRate và báo cáo sự cố
SELECT pg_temp.expect_ok('A19 phiên 2: chấp nhận, bắt đầu',
  $q$WITH r AS (UPDATE exchange_requests SET status='ACCEPTED' WHERE id=2 RETURNING *)
     INSERT INTO exchange_sessions(request_id, start_at, duration_minutes, mode, status, started_at)
     SELECT id, proposed_start_at, duration_minutes, mode, 'IN_PROGRESS', now() FROM r$q$);
SELECT pg_temp.expect_ok('A20 báo cáo sự cố đang mở cho phiên 2',
  $q$INSERT INTO incident_reports(session_id, reporter_id, reason) VALUES (2,3,'B không đến')$q$);
SELECT pg_temp.expect_ok('A21 admin xử lý báo cáo → hủy phiên',
  $q$UPDATE incident_reports SET status='RESOLVED', resolution='SESSION_CANCELLED', resolved_by=4, resolved_at=now() WHERE id=1$q$);
SELECT pg_temp.expect_ok('A22 phiên 2 bị hủy (sau khi đã bắt đầu)',
  $q$UPDATE exchange_sessions SET status='CANCELLED', cancelled_by=4, cancel_reason='Admin xử lý báo cáo', cancelled_at=now() WHERE id=2$q$);
SELECT pg_temp.expect_ok('A23 báo cáo mới cho phiên 2 sau khi báo cáo cũ đã đóng',
  $q$INSERT INTO incident_reports(session_id, reporter_id, reason) VALUES (2,2,'Báo cáo bổ sung')$q$);

-- =====================================================================
-- B. Dữ liệu SAI (phải bị chặn hết)
-- =====================================================================
-- users
SELECT pg_temp.expect_fail('B01 email trùng',
  $q$INSERT INTO users(email, password_hash, full_name) VALUES ('a@st.edu.vn','x','Trùng')$q$);
SELECT pg_temp.expect_fail('B02 role lạ',
  $q$INSERT INTO users(email, password_hash, full_name, role) VALUES ('x@st.edu.vn','x','X','TEACHER')$q$);
SELECT pg_temp.expect_fail('B03 status lạ',
  $q$INSERT INTO users(email, password_hash, full_name, status) VALUES ('y@st.edu.vn','x','Y','BANNED')$q$);
SELECT pg_temp.expect_fail('B04 learning_mode lạ',
  $q$INSERT INTO users(email, password_hash, full_name, learning_mode) VALUES ('z@st.edu.vn','x','Z','HYBRID')$q$);
SELECT pg_temp.expect_fail('B05 thiếu password_hash',
  $q$INSERT INTO users(email, full_name) VALUES ('w@st.edu.vn','W')$q$);
SELECT pg_temp.expect_fail('B06 token mục đích lạ',
  $q$INSERT INTO user_tokens(user_id, purpose, token_hash, expires_at) VALUES (1,'LOGIN','h1',now())$q$);
SELECT pg_temp.expect_fail('B07 token_hash trùng',
  $q$INSERT INTO user_tokens(user_id, purpose, token_hash, expires_at) VALUES (1,'VERIFY_EMAIL','dup',now()),(2,'VERIFY_EMAIL','dup',now())$q$);
-- skills
SELECT pg_temp.expect_fail('B08 tên kỹ năng trùng',
  $q$INSERT INTO skills(category_id, name) VALUES (1,'Java')$q$);
SELECT pg_temp.expect_fail('B09 kỹ năng thuộc nhóm không tồn tại',
  $q$INSERT INTO skills(category_id, name) VALUES (99,'Rust')$q$);
SELECT pg_temp.expect_fail('B10 quan hệ (2,1) với a > b',
  $q$INSERT INTO skill_relations VALUES (2, 1)$q$);
SELECT pg_temp.expect_fail('B11 quan hệ kỹ năng với chính nó (3,3)',
  $q$INSERT INTO skill_relations VALUES (3, 3)$q$);
SELECT pg_temp.expect_fail('B12 quan hệ trùng (1,2) lần hai',
  $q$INSERT INTO skill_relations VALUES (1, 2)$q$);
-- kỹ năng người dùng, lịch rảnh
SELECT pg_temp.expect_fail('B13 level = 5',
  $q$INSERT INTO user_teach_skills VALUES (1,1,5)$q$);
SELECT pg_temp.expect_fail('B14 level = 0',
  $q$INSERT INTO user_teach_skills VALUES (1,1,0)$q$);
SELECT pg_temp.expect_fail('B15 level NULL (kỹ năng dạy bắt buộc có trình độ)',
  $q$INSERT INTO user_teach_skills VALUES (1,1,NULL)$q$);
SELECT pg_temp.expect_fail('B16 khai báo dạy cùng kỹ năng hai lần',
  $q$INSERT INTO user_teach_skills VALUES (2,1,2)$q$);
SELECT pg_temp.expect_fail('B17 target_level = 5',
  $q$INSERT INTO user_learn_skills VALUES (3,3,5)$q$);
SELECT pg_temp.expect_fail('B18 day_of_week = 8',
  $q$INSERT INTO user_availability VALUES (1,8,'MORNING')$q$);
SELECT pg_temp.expect_fail('B19 period lạ',
  $q$INSERT INTO user_availability VALUES (1,2,'NIGHT')$q$);
SELECT pg_temp.expect_fail('B20 khung giờ trùng',
  $q$INSERT INTO user_availability VALUES (1,1,'MORNING')$q$);
-- exchange_requests
SELECT pg_temp.expect_fail('B21 TWO_WAY thiếu receiver_learn_skill_id',
  $q$INSERT INTO exchange_requests(sender_id, receiver_id, type, sender_learn_skill_id,
       proposed_start_at, duration_minutes, mode) VALUES (3,1,'TWO_WAY',3,now(),60,'ONLINE')$q$);
SELECT pg_temp.expect_fail('B22 ONE_WAY lại có receiver_learn_skill_id',
  $q$INSERT INTO exchange_requests(sender_id, receiver_id, type, sender_learn_skill_id, receiver_learn_skill_id,
       proposed_start_at, duration_minutes, mode) VALUES (3,1,'ONE_WAY',3,1,now(),60,'ONLINE')$q$);
SELECT pg_temp.expect_fail('B23 tự gửi cho chính mình',
  $q$INSERT INTO exchange_requests(sender_id, receiver_id, type, sender_learn_skill_id,
       proposed_start_at, duration_minutes, mode) VALUES (3,3,'ONE_WAY',3,now(),60,'ONLINE')$q$);
SELECT pg_temp.expect_fail('B24 PENDING thứ hai A→B (G4)',
  $q$INSERT INTO exchange_requests(sender_id, receiver_id, type, sender_learn_skill_id,
       proposed_start_at, duration_minutes, mode) VALUES (1,2,'ONE_WAY',2,now(),60,'ONLINE')$q$);
SELECT pg_temp.expect_fail('B25 thời lượng 10 phút (<15)',
  $q$INSERT INTO exchange_requests(sender_id, receiver_id, type, sender_learn_skill_id,
       proposed_start_at, duration_minutes, mode) VALUES (3,1,'ONE_WAY',3,now(),10,'ONLINE')$q$);
SELECT pg_temp.expect_fail('B26 thời lượng 500 phút (>480)',
  $q$INSERT INTO exchange_requests(sender_id, receiver_id, type, sender_learn_skill_id,
       proposed_start_at, duration_minutes, mode) VALUES (3,1,'ONE_WAY',3,now(),500,'ONLINE')$q$);
SELECT pg_temp.expect_fail('B27 mode = ANY trong yêu cầu',
  $q$INSERT INTO exchange_requests(sender_id, receiver_id, type, sender_learn_skill_id,
       proposed_start_at, duration_minutes, mode) VALUES (3,1,'ONE_WAY',3,now(),60,'ANY')$q$);
SELECT pg_temp.expect_fail('B28 thiếu proposed_start_at',
  $q$INSERT INTO exchange_requests(sender_id, receiver_id, type, sender_learn_skill_id,
       duration_minutes, mode) VALUES (3,1,'ONE_WAY',3,60,'ONLINE')$q$);
SELECT pg_temp.expect_fail('B29 kỹ năng không tồn tại',
  $q$INSERT INTO exchange_requests(sender_id, receiver_id, type, sender_learn_skill_id,
       proposed_start_at, duration_minutes, mode) VALUES (3,1,'ONE_WAY',999,now(),60,'ONLINE')$q$);
-- exchange_sessions
SELECT pg_temp.expect_fail('B30 tạo phiên thứ hai cho cùng yêu cầu 1',
  $q$INSERT INTO exchange_sessions(request_id, start_at, duration_minutes, mode) VALUES (1, now(), 60, 'ONLINE')$q$);
SELECT pg_temp.expect_fail('B31 trạng thái phiên lạ',
  $q$UPDATE exchange_sessions SET status='DONE' WHERE id=1$q$);
SELECT pg_temp.expect_fail('B32 hủy phiên không có lý do',
  $q$UPDATE exchange_sessions SET status='CANCELLED', cancelled_at=now() WHERE id=1$q$);
SELECT pg_temp.expect_fail('B33 IN_PROGRESS mà chưa có started_at',
  $q$INSERT INTO exchange_sessions(request_id, start_at, duration_minutes, mode, status)
     VALUES (3, now(), 60, 'ONLINE', 'IN_PROGRESS')$q$);
SELECT pg_temp.expect_fail('B34 WAITING_CONFIRMATION mà không biết ai bấm trước',
  $q$INSERT INTO exchange_sessions(request_id, start_at, duration_minutes, mode, status, started_at)
     VALUES (3, now(), 60, 'ONLINE', 'WAITING_CONFIRMATION', now())$q$);
-- reviews
SELECT pg_temp.expect_fail('B35 đánh giá lần hai cùng phiên',
  $q$INSERT INTO reviews(session_id, reviewer_id, reviewee_id, rating) VALUES (1,1,2,3)$q$);
SELECT pg_temp.expect_fail('B36 tự đánh giá mình',
  $q$INSERT INTO reviews(session_id, reviewer_id, reviewee_id, rating) VALUES (2,3,3,5)$q$);
SELECT pg_temp.expect_fail('B37 rating = 6',
  $q$INSERT INTO reviews(session_id, reviewer_id, reviewee_id, rating) VALUES (2,3,2,6)$q$);
SELECT pg_temp.expect_fail('B38 rating = 0',
  $q$INSERT INTO reviews(session_id, reviewer_id, reviewee_id, rating) VALUES (2,3,2,0)$q$);
-- incident_reports
SELECT pg_temp.expect_fail('B39 báo cáo mở thứ hai cho cùng phiên 2',
  $q$INSERT INTO incident_reports(session_id, reporter_id, reason) VALUES (2,3,'Lần nữa')$q$);
SELECT pg_temp.expect_fail('B40 báo cáo không có lý do',
  $q$INSERT INTO incident_reports(session_id, reporter_id, reason) VALUES (1,1,NULL)$q$);
SELECT pg_temp.expect_fail('B41 RESOLVED mà không có kết luận',
  $q$UPDATE incident_reports SET status='RESOLVED', resolved_by=4, resolved_at=now() WHERE id=2$q$);
SELECT pg_temp.expect_fail('B42 OPEN mà đã có kết luận',
  $q$UPDATE incident_reports SET resolution='SESSION_COMPLETED' WHERE id=2$q$);
-- xóa dữ liệu đang được dùng
SELECT pg_temp.expect_fail('B43 xóa kỹ năng đang được dùng (phải ẩn, không xóa)',
  $q$DELETE FROM skills WHERE id=1$q$);
SELECT pg_temp.expect_fail('B44 xóa người đã có yêu cầu/phiên',
  $q$DELETE FROM users WHERE id=1$q$);

-- =====================================================================
-- C. Truy vấn ánh xạ công thức (mục 3.5) trên dữ liệu ở phần A
-- =====================================================================
-- Sim(Java=1, Kotlin=2) = 0.5 vì có dòng trong skill_relations
SELECT pg_temp.expect_eq('C01 Sim(Java,Kotlin) = 0.5',
  $q$SELECT CASE WHEN 1=2 THEN 1.0
                 WHEN EXISTS (SELECT 1 FROM skill_relations WHERE skill_a_id=LEAST(1,2) AND skill_b_id=GREATEST(1,2)) THEN 0.5
                 ELSE 0 END::text$q$, '0.5');
-- Số khung rảnh trùng giữa An(1) và Bình(2): (T2 sáng), (T4 tối) = 2
SELECT pg_temp.expect_eq('C02 số khung rảnh trùng A–B = 2',
  $q$SELECT count(*)::text FROM user_availability a JOIN user_availability b
       ON a.day_of_week=b.day_of_week AND a.period=b.period WHERE a.user_id=1 AND b.user_id=2$q$, '2');
-- Chi (3) không có dòng lịch rảnh = chưa khai báo
SELECT pg_temp.expect_eq('C03 Chi chưa khai báo lịch (0 dòng)',
  $q$SELECT count(*)::text FROM user_availability WHERE user_id=3$q$, '0');
-- RatingNorm(Bình) = (AVG-1)/4 = (5-1)/4 = 1.00
SELECT pg_temp.expect_eq('C04 RatingNorm(Bình) = 1.00',
  $q$SELECT round((avg(rating)-1)/4, 2)::text FROM reviews WHERE reviewee_id=2$q$, '1.00');
-- CompletionRate(Bình): tham gia 2 phiên đã bắt đầu, 1 hoàn thành → 0.50
SELECT pg_temp.expect_eq('C05 CompletionRate(Bình) = 0.50',
  $q$SELECT round(count(*) FILTER (WHERE s.status='COMPLETED')::numeric
                  / NULLIF(count(*) FILTER (WHERE s.started_at IS NOT NULL),0), 2)::text
       FROM exchange_sessions s JOIN exchange_requests r ON r.id=s.request_id
      WHERE 2 IN (r.sender_id, r.receiver_id)$q$, '0.50');
-- Người chưa có phiên nào: mẫu số 0 → NULL, service phải đổi thành mặc định
SELECT pg_temp.expect_eq('C06 CompletionRate(Admin) = NULL khi chưa có phiên',
  $q$SELECT round(count(*) FILTER (WHERE s.status='COMPLETED')::numeric
                  / NULLIF(count(*) FILTER (WHERE s.started_at IS NOT NULL),0), 2)::text
       FROM exchange_sessions s JOIN exchange_requests r ON r.id=s.request_id
      WHERE 4 IN (r.sender_id, r.receiver_id)$q$, NULL);

-- =====================================================================
-- Kết quả
-- =====================================================================
\o
\set QUIET off
\pset footer off
SELECT no, label,
       CASE WHEN expected IS NOT DISTINCT FROM actual
             OR (expected IS NULL AND actual = 'NULL') THEN 'PASS' ELSE 'FAIL' END AS ket_qua,
       expected AS mong_doi, actual AS thuc_te, left(detail, 90) AS chi_tiet
  FROM results ORDER BY no;

SELECT count(*) FILTER (WHERE expected IS NOT DISTINCT FROM actual OR (expected IS NULL AND actual='NULL')) AS pass,
       count(*) FILTER (WHERE NOT (expected IS NOT DISTINCT FROM actual OR (expected IS NULL AND actual='NULL'))) AS fail,
       count(*) AS tong
  FROM results;
