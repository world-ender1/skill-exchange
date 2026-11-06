package com.skillexchange.matching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.skillexchange.user.LearningMode;
import com.skillexchange.user.Period;
import com.skillexchange.user.Role;
import com.skillexchange.user.UserStatus;

/**
 * Nạp dữ liệu thô cho matching bằng SQL thuần (TK 3.5, D43; kế hoạch tuần 4 bước 8).
 *
 * Mỗi hàm là MỘT câu SQL nạp dữ liệu của mọi sinh viên ACTIVE cùng lúc, rồi gom theo user_id trong Java.
 * Nhờ vậy một lần gọi /matches luôn chạy cùng số câu SQL dù có 20 hay 1.000 người (không N+1, TC157).
 *
 * Dùng JdbcTemplate thay vì Entity: exchange_requests, exchange_sessions, reviews, skill_relations
 * chưa có Entity (tuần 6–7), và SQL thuần dễ đối chiếu với TK 3.5.
 * Kỹ năng bị ẩn (skills.is_active = false) KHÔNG bị lọc: vẫn tính khi ghép cặp (spec 4.1, TC153).
 */
@Repository
public class MatchingDataRepository {

    /** Điều kiện 2 của lọc cứng, lọc sẵn trong SQL cho nhẹ; quy tắc chính thức vẫn ở HardFilter (D43a). */
    private static final String ACTIVE_STUDENT = "u.status = 'ACTIVE' AND u.role = 'STUDENT'";

    /** Một dòng của bảng users, chỉ các cột matching cần (không có email, phone, contact_info: NFR-02). */
    public record UserRow(long id, String fullName, UserStatus status, Role role, LearningMode learningMode) {
    }

    private final JdbcTemplate jdbcTemplate;

    public MatchingDataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UserRow> findActiveStudents() {
        // query(sql, RowMapper): Spring mở kết nối, chạy câu lệnh, gọi lambda cho từng dòng rồi đóng kết nối.
        return jdbcTemplate.query("""
                SELECT u.id, u.full_name, u.status, u.role, u.learning_mode
                FROM users u
                WHERE %s
                ORDER BY u.id
                """.formatted(ACTIVE_STUDENT),
                (rs, rowNum) -> new UserRow(
                        rs.getLong("id"),
                        rs.getString("full_name"),
                        UserStatus.valueOf(rs.getString("status")),
                        Role.valueOf(rs.getString("role")),
                        learningMode(rs.getString("learning_mode"))));
    }

    public Map<Long, List<TeachItem>> findTeachSkills() {
        Map<Long, List<TeachItem>> byUser = new HashMap<>();
        // query(sql, RowCallbackHandler): không trả danh sách, lambda tự gom từng dòng vào map.
        jdbcTemplate.query("""
                SELECT t.user_id, t.skill_id, s.name, t.level
                FROM user_teach_skills t
                JOIN users u ON u.id = t.user_id
                JOIN skills s ON s.id = t.skill_id
                WHERE %s
                ORDER BY t.user_id, t.skill_id
                """.formatted(ACTIVE_STUDENT),
                rs -> {
                    TeachItem item = new TeachItem(rs.getLong("skill_id"), rs.getString("name"), rs.getInt("level"));
                    byUser.computeIfAbsent(rs.getLong("user_id"), id -> new ArrayList<>()).add(item);
                });
        return byUser;
    }

    public Map<Long, List<LearnItem>> findLearnSkills() {
        Map<Long, List<LearnItem>> byUser = new HashMap<>();
        jdbcTemplate.query("""
                SELECT l.user_id, l.skill_id, s.name, l.target_level
                FROM user_learn_skills l
                JOIN users u ON u.id = l.user_id
                JOIN skills s ON s.id = l.skill_id
                WHERE %s
                ORDER BY l.user_id, l.skill_id
                """.formatted(ACTIVE_STUDENT),
                rs -> {
                    // getObject(..., Integer.class) giữ được NULL (không đặt mục tiêu); getInt sẽ biến NULL thành 0.
                    LearnItem item = new LearnItem(rs.getLong("skill_id"), rs.getString("name"),
                            rs.getObject("target_level", Integer.class));
                    byUser.computeIfAbsent(rs.getLong("user_id"), id -> new ArrayList<>()).add(item);
                });
        return byUser;
    }

    public Map<Long, Set<Slot>> findSlots() {
        Map<Long, Set<Slot>> byUser = new HashMap<>();
        jdbcTemplate.query("""
                SELECT a.user_id, a.day_of_week, a.period
                FROM user_availability a
                JOIN users u ON u.id = a.user_id
                WHERE %s
                """.formatted(ACTIVE_STUDENT),
                rs -> {
                    Slot slot = new Slot(rs.getInt("day_of_week"), Period.valueOf(rs.getString("period")));
                    byUser.computeIfAbsent(rs.getLong("user_id"), id -> new HashSet<>()).add(slot);
                });
        return byUser;
    }

    public List<SkillRelation> findSkillRelations() {
        return jdbcTemplate.query("SELECT skill_a_id, skill_b_id FROM skill_relations",
                (rs, rowNum) -> new SkillRelation(rs.getLong("skill_a_id"), rs.getLong("skill_b_id")));
    }

    /** Các điểm 1–5 mỗi người NHẬN được (reviewee_id), dạng thô; trung bình do ReputationCalculator và service tính. */
    public Map<Long, List<Integer>> findRatingsReceived() {
        Map<Long, List<Integer>> byUser = new HashMap<>();
        jdbcTemplate.query("""
                SELECT r.reviewee_id, r.rating
                FROM reviews r
                JOIN users u ON u.id = r.reviewee_id
                WHERE %s
                """.formatted(ACTIVE_STUDENT),
                rs -> {
                    byUser.computeIfAbsent(rs.getLong("reviewee_id"), id -> new ArrayList<>()).add(rs.getInt("rating"));
                });
        return byUser;
    }

    /**
     * Mọi phiên mỗi người đã tham gia, dạng thô (trạng thái, đã bắt đầu chưa), D43(b).
     * Người tham gia phiên = sender hoặc receiver của yêu cầu sinh ra phiên. UNION ALL tách mỗi yêu cầu
     * thành hai dòng (một cho mỗi người); sender_id <> receiver_id (CHECK của V1) nên không ai bị đếm trùng.
     * Không lọc trạng thái ở đây: ReputationCalculator tự quyết phiên nào vào mẫu số.
     */
    public Map<Long, List<SessionOutcome>> findSessionOutcomes() {
        Map<Long, List<SessionOutcome>> byUser = new HashMap<>();
        jdbcTemplate.query("""
                SELECT p.user_id, s.status, s.started_at IS NOT NULL AS started
                FROM exchange_sessions s
                JOIN (SELECT id, sender_id AS user_id FROM exchange_requests
                      UNION ALL
                      SELECT id, receiver_id AS user_id FROM exchange_requests) p ON p.id = s.request_id
                JOIN users u ON u.id = p.user_id
                WHERE %s
                """.formatted(ACTIVE_STUDENT),
                rs -> {
                    SessionOutcome outcome = new SessionOutcome(
                            SessionStatus.valueOf(rs.getString("status")), rs.getBoolean("started"));
                    byUser.computeIfAbsent(rs.getLong("user_id"), id -> new ArrayList<>()).add(outcome);
                });
        return byUser;
    }

    /** Những người mà người gọi đang có yêu cầu PENDING gửi tới (hasPendingRequest, TK 4.5). */
    public Set<Long> findPendingReceivers(long senderId) {
        // Tham số truyền bằng "?": JDBC gửi riêng giá trị, không ghép vào chuỗi SQL, nên không bị SQL injection.
        return new HashSet<>(jdbcTemplate.queryForList(
                "SELECT receiver_id FROM exchange_requests WHERE sender_id = ? AND status = 'PENDING'",
                Long.class, senderId));
    }

    private static LearningMode learningMode(String value) {
        // NULL = chưa khai báo hình thức học (spec 4.3), lõi hiểu null là trung tính 0.5.
        return value == null ? null : LearningMode.valueOf(value);
    }
}
