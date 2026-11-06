package com.skillexchange.matching;

import static com.skillexchange.TestAccounts.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.skillexchange.IntegrationTestBase;
import com.skillexchange.TestAccounts;
import com.skillexchange.user.Role;
import com.skillexchange.user.UserRepository;
import com.skillexchange.user.UserStatus;

/**
 * GET /api/matches (kế hoạch tuần 4, bước 8): TC145–TC156, TC185; D32, D38, D43(c), D46, D49.
 * An và Bình là dữ liệu mẫu dùng chung của bộ ca (đáp án tính tay ở bài học C2):
 * An học từ Bình D = 0.6875, Bình học từ An D = 0.925, MutualScore = sqrt(0.6875 * 0.925) ≈ 0.797.
 */
class MatchesApiTest extends IntegrationTestBase {

    private static final String AN = "an@student.example.edu.vn";
    private static final String BINH = "binh@student.example.edu.vn";
    private static final String CUONG = "cuong@student.example.edu.vn";
    private static final String BINH_PHONE = "0901234567";
    private static final String BINH_CONTACT = "zalo-binh-rieng-tu";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private TestAccounts accounts;

    @BeforeEach
    void setUpAccounts() {
        accounts = new TestAccounts(mockMvc, jdbcTemplate, userRepository, passwordEncoder);
    }

    // ---------- Dữ liệu mẫu ----------

    /** An: học Java (3), Tiếng Anh (không đặt); dạy Python (4); Tối T2, T4, T6; ONLINE. Trả vé. */
    private String an() throws Exception {
        accounts.activeStudent(AN, "An");
        String token = accounts.login(AN);
        accounts.putProfile(token, "An", "ONLINE", null, null);
        accounts.putLearnSkills(token, accounts.learn("Java", 3), accounts.learn("Tiếng Anh", null));
        accounts.putTeachSkills(token, accounts.teach("Python", 4));
        accounts.putAvailability(token, slot(1, "EVENING"), slot(3, "EVENING"), slot(5, "EVENING"));
        return token;
    }

    /**
     * Bình: học Python (2); dạy Kotlin (2), Tiếng Anh (3); Tối T4, T6, Sáng T7, Sáng CN; ANY.
     * Có số điện thoại và thông tin liên hệ để kiểm riêng tư (TC156). Trả id.
     */
    private long binh() throws Exception {
        long id = accounts.activeStudent(BINH, "Bình");
        String token = accounts.login(BINH);
        accounts.putProfile(token, "Bình", "ANY", BINH_PHONE, BINH_CONTACT);
        accounts.putLearnSkills(token, accounts.learn("Python", 2));
        accounts.putTeachSkills(token, accounts.teach("Kotlin", 2), accounts.teach("Tiếng Anh", 3));
        accounts.putAvailability(token, slot(3, "EVENING"), slot(5, "EVENING"),
                slot(6, "MORNING"), slot(7, "MORNING"));
        return id;
    }

    /**
     * Cường: dạy Tiếng Anh (2) cho An nhưng chỉ muốn học Guitar, An không dạy được → ONE_WAY với An.
     * Không khai lịch và hình thức (0.5 trung tính, vẫn qua lọc cứng). Có số điện thoại (TC156).
     */
    private long cuong() throws Exception {
        long id = accounts.activeStudent(CUONG, "Cường");
        String token = accounts.login(CUONG);
        accounts.putProfile(token, "Cường", null, "0911111111", "fb-cuong");
        accounts.putLearnSkills(token, accounts.learn("Guitar", null));
        accounts.putTeachSkills(token, accounts.teach("Tiếng Anh", 2));
        return id;
    }

    /** Sinh viên hai chiều với An: dạy Java (3), học Python. Không khai lịch, hình thức. */
    private long twoWayCandidate(String email, String fullName) throws Exception {
        long id = accounts.activeStudent(email, fullName);
        String token = accounts.login(email);
        accounts.putLearnSkills(token, accounts.learn("Python", null));
        accounts.putTeachSkills(token, accounts.teach("Java", 3));
        return id;
    }

    // ---------- Gọi API và đọc kết quả ----------

    private ResultActions matches(String token, String query) throws Exception {
        return mockMvc.perform(get("/api/matches" + query)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String matchesBody(String token, String query) throws Exception {
        return matches(token, query)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Phần tử của items có userId cho trước; không có thì test hỏng ngay. */
    private static Map<String, Object> item(String body, long userId) {
        List<Map<String, Object>> items = JsonPath.read(body, "$.items");
        return items.stream()
                .filter(i -> ((Number) i.get("userId")).longValue() == userId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không có userId " + userId + " trong " + body));
    }

    private static DocumentContext json(Map<String, Object> item) {
        return JsonPath.parse(item);
    }

    private static List<Long> userIds(String body) {
        List<Number> ids = JsonPath.read(body, "$.items[*].userId");
        return ids.stream().map(Number::longValue).toList();
    }

    // ---------- Chèn dữ liệu tuần 6–7 bằng SQL (chưa có API) ----------

    /** Một yêu cầu ONE_WAY (không cần kỹ năng chiều ngược, CHECK của V1) từ sender tới receiver. */
    private long request(long senderId, long receiverId, String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO exchange_requests (sender_id, receiver_id, type, sender_learn_skill_id,
                                               proposed_start_at, duration_minutes, mode, status)
                VALUES (?, ?, 'ONE_WAY', ?, ?, 60, 'ONLINE', ?)
                RETURNING id
                """, Long.class, senderId, receiverId, accounts.skillId("Java"), Timestamp.from(START), status);
    }

    /** Một phiên của yêu cầu; điền đủ các cột mà CHECK của V1 bắt buộc theo trạng thái. */
    private long session(long requestId, String status, boolean started) {
        Timestamp now = Timestamp.from(START);
        boolean completed = status.equals("COMPLETED");
        boolean cancelled = status.equals("CANCELLED");
        return jdbcTemplate.queryForObject("""
                INSERT INTO exchange_sessions (request_id, start_at, duration_minutes, mode, status,
                                               started_at, completed_at, cancel_reason, cancelled_at)
                VALUES (?, ?, 60, 'ONLINE', ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, requestId, now, status,
                started ? now : null,
                completed ? now : null,
                cancelled ? "Bận đột xuất" : null,
                cancelled ? now : null);
    }

    private void review(long sessionId, long reviewerId, long revieweeId, int rating) {
        jdbcTemplate.update("INSERT INTO reviews (session_id, reviewer_id, reviewee_id, rating) VALUES (?, ?, ?, ?)",
                sessionId, reviewerId, revieweeId, rating);
    }

    // ================= Tham số =================

    @Test
    @DisplayName("TC145 thiếu type; type=FOO; limit=0; limit=51; limit=abc: cả 5 trả 400 Problem Details")
    void tc145_invalidParametersAre400() throws Exception {
        String token = an();

        matches(token, "")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", containsString("type")));
        matches(token, "?type=FOO")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", containsString("type")));
        for (String limit : new String[] {"0", "51"}) {
            matches(token, "?type=TWO_WAY&limit=" + limit)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors[*].field", hasItem("limit")));
        }
        // D38: không phải câu tiếng Anh mặc định của Spring ("Method parameter 'limit': Failed to convert...").
        matches(token, "?type=TWO_WAY&limit=abc")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Tham số limit có giá trị không hợp lệ: abc"));
    }

    @Test
    @DisplayName("TC146 limit=1 và limit=50 (hai biên): 200")
    void tc146_limitBoundsAccepted() throws Exception {
        String token = an();
        binh();
        twoWayCandidate("vu@student.example.edu.vn", "Vũ");

        matches(token, "?type=TWO_WAY&limit=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items", hasSize(1)));
        matches(token, "?type=TWO_WAY&limit=50")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    @DisplayName("TC147 12 ứng viên hai chiều, không truyền limit: total = 12, items có 10 phần tử")
    void tc147_defaultLimitIsTen() throws Exception {
        String token = an();
        for (int i = 1; i <= 12; i++) {
            twoWayCandidate("sv" + i + "@student.example.edu.vn", "Sinh viên " + i);
        }

        matches(token, "?type=TWO_WAY")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TWO_WAY"))
                .andExpect(jsonPath("$.total").value(12))
                .andExpect(jsonPath("$.items", hasSize(10)));
    }

    @Test
    @DisplayName("TC148 người gọi chưa khai kỹ năng muốn học: 200, needsLearnSkills = true, total = 0, items = []")
    void tc148_callerWithoutLearnSkills() throws Exception {
        accounts.activeStudent(AN, "An");
        String token = accounts.login(AN);
        accounts.putTeachSkills(token, accounts.teach("Python", 4));
        binh(); // Bình muốn học Python: nếu không kiểm W_u rỗng, Bình vẫn có thể hiện ra.

        for (String type : new String[] {"TWO_WAY", "ONE_WAY"}) {
            matches(token, "?type=" + type)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.needsLearnSkills").value(true))
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.items", hasSize(0)));
        }
    }

    // ================= Kết quả An – Bình =================

    @Test
    @DisplayName("TC149 An gọi TWO_WAY: Bình score 0.797, forward.d 0.688, backward.d 0.925, scheduleFit 0.667, reputation 0.5")
    void tc149_anSeesBinhWithHandComputedScores() throws Exception {
        String token = an();
        long binhId = binh();

        String body = matchesBody(token, "?type=TWO_WAY");
        assertThat(JsonPath.<Boolean>read(body, "$.needsLearnSkills")).isFalse();
        DocumentContext binh = json(item(body, binhId));

        assertThat(binh.<String>read("$.type")).isEqualTo("TWO_WAY");
        assertThat(binh.<String>read("$.fullName")).isEqualTo("Bình");
        assertThat(binh.<String>read("$.learningMode")).isEqualTo("ANY");
        assertThat(binh.<Double>read("$.score")).isEqualTo(0.797);
        assertThat(binh.<Double>read("$.forward.d")).isEqualTo(0.688);
        assertThat(binh.<Double>read("$.forward.skillFit")).isEqualTo(0.75);
        assertThat(binh.<Double>read("$.forward.levelFit")).isEqualTo(0.5);
        assertThat(binh.<Double>read("$.forward.scheduleFit")).isEqualTo(0.667);
        assertThat(binh.<Double>read("$.forward.modeFit")).isEqualTo(1.0);
        assertThat(binh.<Double>read("$.forward.reputation")).isEqualTo(0.5);
        assertThat(binh.<Double>read("$.backward.d")).isEqualTo(0.925);
        assertThat(binh.<Double>read("$.backward.skillFit")).isEqualTo(1.0);
        assertThat(binh.<Double>read("$.backward.reputation")).isEqualTo(0.5);
    }

    @Test
    @DisplayName("D46 youLearn, theyLearn, commonSlots của Bình: kỹ năng đã chọn, quan hệ, trình độ, thứ tự")
    void d46_explanationListsForBinh() throws Exception {
        String token = an();
        long binhId = binh();

        DocumentContext binh = json(item(matchesBody(token, "?type=TWO_WAY"), binhId));

        // Java ← Kotlin (liên quan), Tiếng Anh ← Tiếng Anh (trùng); xếp theo tên kỹ năng muốn học.
        assertThat(binh.<List<String>>read("$.youLearn[*].want")).containsExactly("Java", "Tiếng Anh");
        assertThat(binh.<List<String>>read("$.youLearn[*].teach")).containsExactly("Kotlin", "Tiếng Anh");
        assertThat(binh.<List<String>>read("$.youLearn[*].relation")).containsExactly("RELATED", "EXACT");
        assertThat(binh.<List<Integer>>read("$.youLearn[*].theirLevel")).containsExactly(2, 3);
        // Bình học Python ← Python của An, trình độ của An là 4.
        assertThat(binh.<List<String>>read("$.theyLearn[*].want")).containsExactly("Python");
        assertThat(binh.<List<String>>read("$.theyLearn[*].relation")).containsExactly("EXACT");
        assertThat(binh.<List<Integer>>read("$.theyLearn[*].yourLevel")).containsExactly(4);
        // Mỗi danh sách chỉ có key trình độ của mình.
        assertThat(binh.<List<Map<String, Object>>>read("$.youLearn")).allSatisfy(row ->
                assertThat(row).doesNotContainKey("yourLevel"));
        assertThat(binh.<List<Map<String, Object>>>read("$.theyLearn")).allSatisfy(row ->
                assertThat(row).doesNotContainKey("theirLevel"));
        // Tối T4, Tối T6.
        assertThat(binh.<List<Integer>>read("$.commonSlots[*].dayOfWeek")).containsExactly(3, 5);
        assertThat(binh.<List<String>>read("$.commonSlots[*].period")).containsExactly("EVENING", "EVENING");
    }

    @Test
    @DisplayName("TC150 An gọi ONE_WAY: không có Bình (Bình thuộc nhóm hai chiều)")
    void tc150_binhNotInOneWay() throws Exception {
        String token = an();
        long binhId = binh();
        long cuongId = cuong();

        String body = matchesBody(token, "?type=ONE_WAY");
        assertThat(userIds(body)).containsExactly(cuongId).doesNotContain(binhId);
        assertThat(JsonPath.<Integer>read(body, "$.total")).isEqualTo(1);
    }

    @Test
    @DisplayName("TC151 Bình chưa có đánh giá: ratingAverage = null, reviewCount = 0")
    void tc151_noReviewsYet() throws Exception {
        String token = an();
        long binhId = binh();

        Map<String, Object> binh = item(matchesBody(token, "?type=TWO_WAY"), binhId);
        assertThat(binh).containsEntry("ratingAverage", null).containsEntry("reviewCount", 0);
    }

    @Test
    @DisplayName("TC152 hasPendingRequest chỉ tính yêu cầu PENDING người gọi gửi đi")
    void tc152_pendingRequestOnlyFromCaller() throws Exception {
        String token = an();
        long anId = accounts.idOf(AN);
        long binhId = binh();

        // Lần 1: An đã gửi yêu cầu PENDING cho Bình → Bình vẫn trong danh sách, có cờ.
        long fromAn = request(anId, binhId, "PENDING");
        assertThat(item(matchesBody(token, "?type=TWO_WAY"), binhId)).containsEntry("hasPendingRequest", true);

        // Lần 2: chỉ có yêu cầu PENDING từ Bình tới An → không tính.
        jdbcTemplate.update("DELETE FROM exchange_requests WHERE id = ?", fromAn);
        request(binhId, anId, "PENDING");
        assertThat(item(matchesBody(token, "?type=TWO_WAY"), binhId)).containsEntry("hasPendingRequest", false);
    }

    @Test
    @DisplayName("TC153 ẩn Kotlin (skills.is_active = false): Bình vẫn có, cùng score 0.797")
    void tc153_hiddenSkillStillMatches() throws Exception {
        String token = an();
        long binhId = binh();

        jdbcTemplate.update("UPDATE skills SET is_active = false WHERE name = 'Kotlin'");
        try {
            Map<String, Object> binh = item(matchesBody(token, "?type=TWO_WAY"), binhId);
            assertThat(binh).containsEntry("score", 0.797);
        } finally {
            // Danh mục kỹ năng không bị TRUNCATE giữa các test: phải trả lại.
            jdbcTemplate.update("UPDATE skills SET is_active = true WHERE name = 'Kotlin'");
        }
    }

    @Test
    @DisplayName("TC154 ba người khớp kỹ năng nhưng PENDING_VERIFICATION, LOCKED, admin: không xuất hiện ở cả hai nhóm")
    void tc154_inactiveAccountsAndAdminExcluded() throws Exception {
        String token = an();
        long binhId = binh();
        long pending = accounts.create("cho@student.example.edu.vn", "Chờ", UserStatus.PENDING_VERIFICATION, Role.STUDENT);
        long locked = accounts.create("khoa@student.example.edu.vn", "Khóa", UserStatus.LOCKED, Role.STUDENT);
        long admin = accounts.create("qtv@student.example.edu.vn", "Quản trị", UserStatus.ACTIVE, Role.ADMIN);
        // Cả ba dạy Java cho An và muốn học Python của An: nếu lọt qua thì vào nhóm hai chiều.
        // Chèn bằng SQL vì tài khoản bị khóa không gọi được API.
        for (long id : new long[] {pending, locked, admin}) {
            jdbcTemplate.update("INSERT INTO user_teach_skills (user_id, skill_id, level) VALUES (?, ?, 3)",
                    id, accounts.skillId("Java"));
            jdbcTemplate.update("INSERT INTO user_learn_skills (user_id, skill_id) VALUES (?, ?)",
                    id, accounts.skillId("Python"));
        }

        for (String type : new String[] {"TWO_WAY", "ONE_WAY"}) {
            assertThat(userIds(matchesBody(token, "?type=" + type))).doesNotContain(pending, locked, admin);
        }
        assertThat(userIds(matchesBody(token, "?type=TWO_WAY"))).containsExactly(binhId);
    }

    @Test
    @DisplayName("TC155 Reputation từ dữ liệu thô: 0.85 (RatingNorm 1.0, CompletionRate 1/2); ratingAverage 5, reviewCount 1")
    void tc155_reputationFromRawSessionsAndReviews() throws Exception {
        String token = an();
        long vu = twoWayCandidate("vu@student.example.edu.vn", "Vũ");
        long xuan = accounts.activeStudent("xuan@student.example.edu.vn", "Xuân");
        long yen = accounts.activeStudent("yen@student.example.edu.vn", "Yến");

        // Vũ gửi, hoàn thành, được Xuân chấm 5 sao → tử số và mẫu số.
        long completed = session(request(vu, xuan, "ACCEPTED"), "COMPLETED", true);
        review(completed, xuan, vu, 5);
        // Vũ chấm Xuân 1 sao: đánh giá Vũ GỬI ĐI, không được tính cho Vũ.
        review(completed, vu, xuan, 1);
        // Vũ nhận, đã bắt đầu rồi hủy → chỉ mẫu số (phải JOIN theo cả receiver_id).
        session(request(xuan, vu, "ACCEPTED"), "CANCELLED", true);
        // Hủy trước khi bắt đầu và đang diễn ra → không tính.
        session(request(vu, yen, "ACCEPTED"), "CANCELLED", false);
        session(request(yen, vu, "ACCEPTED"), "IN_PROGRESS", true);
        // Phiên hoàn thành của hai người khác → không tính cho Vũ.
        session(request(xuan, yen, "ACCEPTED"), "COMPLETED", true);

        Map<String, Object> vuItem = item(matchesBody(token, "?type=TWO_WAY"), vu);
        assertThat(json(vuItem).<Double>read("$.forward.reputation")).isEqualTo(0.85);
        assertThat(vuItem).containsEntry("ratingAverage", 5.0).containsEntry("reviewCount", 1);
    }

    // ================= Riêng tư và hình dạng JSON =================

    @Test
    @DisplayName("TC156 Bình, Cường có phone, contactInfo: không có key email, phone, contactInfo ở mọi mức JSON")
    void tc156_noPrivateFields() throws Exception {
        String token = an();
        binh();
        cuong();

        for (String type : new String[] {"TWO_WAY", "ONE_WAY"}) {
            String body = matchesBody(token, "?type=" + type);
            assertThat(JsonPath.<Integer>read(body, "$.total")).isEqualTo(1);
            // So trên chuỗi JSON thô: bắt được key ở bất kỳ mức lồng nào.
            assertThat(body).doesNotContain("\"email\"", "\"phone\"", "\"contactInfo\"",
                    BINH_PHONE, BINH_CONTACT, "@student.example.edu.vn");
        }
    }

    @Test
    @DisplayName("TC185 ONE_WAY: không có key backward, theyLearn; TWO_WAY: có key ratingAverage với giá trị null")
    void tc185_oneWayOmitsBackwardKeys() throws Exception {
        String token = an();
        long binhId = binh();
        long cuongId = cuong();

        Map<String, Object> cuong = item(matchesBody(token, "?type=ONE_WAY"), cuongId);
        assertThat(cuong).containsEntry("type", "ONE_WAY")
                .doesNotContainKey("backward")
                .doesNotContainKey("theyLearn")
                // Các trường khác vẫn có key kể cả khi null (D49): Cường chưa khai hình thức học.
                .containsEntry("learningMode", null)
                .containsKey("youLearn")
                .containsKey("commonSlots");

        Map<String, Object> binh = item(matchesBody(token, "?type=TWO_WAY"), binhId);
        assertThat(binh).containsKey("backward").containsKey("theyLearn").containsEntry("ratingAverage", null);
    }
}
