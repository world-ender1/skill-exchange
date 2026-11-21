package com.skillexchange.exchange;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.skillexchange.TestAccounts;
import com.skillexchange.user.Role;
import com.skillexchange.user.UserRepository;
import com.skillexchange.user.UserStatus;

/**
 * Dữ liệu mẫu dùng chung của ca kiểm thử tuần 6 (docs/tuan_6/kiem_thu/ca_kiem_thu.md, mục "Dữ liệu mẫu dùng chung")
 * và lời gọi API yêu cầu, cho test từ bước 6 trở đi. CreateRequestApiTest (bước 5) giữ bản riêng vì là test đã duyệt.
 * Lớp thường, không phải bean: mỗi test tự tạo trong @BeforeEach như TestAccounts.
 */
class ExchangeTestData {

    static final Instant T0 = Instant.parse("2026-11-18T08:00:00Z");
    static final Instant T1 = T0.plus(Duration.ofHours(1));
    /** Giờ đề xuất của R0: 2026-11-20T12:00:00Z = 19:00 giờ Việt Nam. */
    static final Instant S = Instant.parse("2026-11-20T12:00:00Z");
    static final String S_VN = "2026-11-20T19:00:00+07:00";
    static final String MEET = "https://meet.google.com/abc-defg-hij";

    static final String AN = "an@student.example.edu.vn";
    static final String BINH = "binh@student.example.edu.vn";
    static final String CHI = "chi@student.example.edu.vn";
    static final String GIANG = "giang@student.example.edu.vn";
    static final String DUNG = "dung@student.example.edu.vn";
    static final String EM = "em@student.example.edu.vn";
    static final String ADMIN = "qtv@student.example.edu.vn";

    static final String INVALID = "Dữ liệu không hợp lệ";

    /** Một sinh viên đã đăng nhập: id, email (để đăng nhập lại) và vé. */
    record Student(long id, String email, String token) {
    }

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final TestAccounts accounts;

    ExchangeTestData(MockMvc mockMvc, JdbcTemplate jdbcTemplate,
                     UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
        this.accounts = new TestAccounts(mockMvc, jdbcTemplate, userRepository, passwordEncoder);
    }

    // ---------- Người dùng (bảng "Người dùng" của ca kiểm thử) ----------

    /** An: dạy Tiếng Anh (3); học Java (3), Python (trống); có phone, contactInfo. */
    Student an() throws Exception {
        long id = accounts.activeStudent(AN, "An");
        String token = accounts.login(AN);
        accounts.putProfile(token, "An", null, "0901000001", "Zalo: An");
        accounts.putTeachSkills(token, accounts.teach("Tiếng Anh", 3));
        accounts.putLearnSkills(token, accounts.learn("Java", 3), accounts.learn("Python", null));
        return new Student(id, AN, token);
    }

    /** Bình: dạy Kotlin (3); học Tiếng Anh, Figma (trống); không có phone. */
    Student binh() throws Exception {
        long id = accounts.activeStudent(BINH, "Bình");
        String token = accounts.login(BINH);
        accounts.putProfile(token, "Bình", null, null, "Facebook: Bình");
        accounts.putTeachSkills(token, accounts.teach("Kotlin", 3));
        accounts.putLearnSkills(token, accounts.learn("Tiếng Anh", null), accounts.learn("Figma", null));
        return new Student(id, BINH, token);
    }

    /** Chi: dạy Java (2); học Kotlin (trống). */
    Student chi() throws Exception {
        long id = accounts.activeStudent(CHI, "Chi");
        String token = accounts.login(CHI);
        accounts.putTeachSkills(token, accounts.teach("Java", 2));
        accounts.putLearnSkills(token, accounts.learn("Kotlin", null));
        return new Student(id, CHI, token);
    }

    /** Giang: dạy Java (4), không học gì. */
    Student giang() throws Exception {
        long id = accounts.activeStudent(GIANG, "Giang");
        String token = accounts.login(GIANG);
        accounts.putTeachSkills(token, accounts.teach("Java", 4));
        return new Student(id, GIANG, token);
    }

    /** Vé của một tài khoản chưa xác thực (đăng nhập được nhưng không có quyền Active). */
    String dungToken() throws Exception {
        accounts.create(DUNG, "Dung", UserStatus.PENDING_VERIFICATION, Role.STUDENT);
        return accounts.login(DUNG);
    }

    String adminToken() throws Exception {
        accounts.create(ADMIN, "Quản trị viên", UserStatus.ACTIVE, Role.ADMIN);
        return accounts.login(ADMIN);
    }

    /** Chỉ tạo tài khoản admin, trả id (ví dụ làm người chốt báo cáo bằng SQL). Không gọi cùng adminToken(). */
    long adminId() {
        return accounts.create(ADMIN, "Quản trị viên", UserStatus.ACTIVE, Role.ADMIN);
    }

    /** Em: dạy Java (2). Vé cấp lúc Em còn ACTIVE, rồi Em bị khóa bằng SQL: vé đúng chữ ký nhưng filter chặn (D13). */
    String emTokenThenLocked() throws Exception {
        long id = accounts.activeStudent(EM, "Em");
        String token = accounts.login(EM);
        accounts.putTeachSkills(token, accounts.teach("Java", 2));
        jdbcTemplate.update("UPDATE users SET status = 'LOCKED' WHERE id = ?", id);
        return token;
    }

    /**
     * Đăng nhập lại để lấy vé theo đồng hồ hiện tại. Vé chỉ có hạn 24 giờ và bộ giải mã kiểm hạn bằng cùng đồng hồ
     * giả (D18), nên test vặn đồng hồ quá 24 giờ (ví dụ tới giờ đề xuất S) thì vé cũ thành 401.
     */
    Student relogin(Student student) throws Exception {
        return new Student(student.id(), student.email(), accounts.login(student.email()));
    }

    long skillId(String name) {
        return accounts.skillId(name);
    }

    // ---------- Body tạo yêu cầu ----------

    /** Yêu cầu ONE_WAY học skill, giờ S, 90 phút, ONLINE. LinkedHashMap để test bớt hoặc đổi từng khóa. */
    Map<String, Object> oneWay(long receiverId, String skill) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("receiverId", receiverId);
        body.put("type", "ONE_WAY");
        body.put("senderLearnSkillId", skillId(skill));
        body.put("proposedStartAt", S_VN);
        body.put("durationMinutes", 90);
        body.put("mode", "ONLINE");
        body.put("locationOrLink", MEET);
        body.put("message", "Học " + skill + " nhé");
        return body;
    }

    /** R0: An gửi Bình, ONE_WAY, học Java. */
    Map<String, Object> r0(long receiverId) {
        return oneWay(receiverId, "Java");
    }

    /** R2 = R0 với TWO_WAY, Bình học Tiếng Anh của An. */
    Map<String, Object> r2(long receiverId) {
        Map<String, Object> body = r0(receiverId);
        body.put("type", "TWO_WAY");
        body.put("receiverLearnSkillId", skillId("Tiếng Anh"));
        return body;
    }

    // ---------- Gọi API ----------

    /** POST /api/requests, phải ra 201; trả id yêu cầu. */
    long create(Student sender, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post("/api/requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sender.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    /** GET /api/requests + query, ví dụ "?box=sent&status=PENDING". */
    ResultActions listRequests(String token, String query) throws Exception {
        return mockMvc.perform(get("/api/requests" + query)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    /** GET /api/requests/{id}. id kiểu Object để test truyền được cả "abc". */
    ResultActions getRequest(String token, Object id) throws Exception {
        return mockMvc.perform(get("/api/requests/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    ResultActions accept(String token, Object id) throws Exception {
        return postWithoutBody(token, "/api/requests/" + id + "/accept");
    }

    /** Từ chối không gửi body (note không bắt buộc, D90). */
    ResultActions reject(String token, Object id) throws Exception {
        return postWithoutBody(token, "/api/requests/" + id + "/reject");
    }

    ResultActions reject(String token, Object id, String note) throws Exception {
        return mockMvc.perform(post("/api/requests/" + id + "/reject")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\": " + quote(note) + "}"));
    }

    ResultActions cancel(String token, Object id) throws Exception {
        return postWithoutBody(token, "/api/requests/" + id + "/cancel");
    }

    /** Lệnh POST không có body và không có Content-Type, như nút bấm của giao diện. */
    ResultActions postWithoutBody(String token, String path) throws Exception {
        return mockMvc.perform(post(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    /** Lệnh POST với body JSON viết sẵn, ví dụ "{}". */
    ResultActions postJson(String token, String path, String json) throws Exception {
        return mockMvc.perform(post(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    // ---------- Gọi API phiên (bước 7) ----------

    /** Người nhận chấp nhận yêu cầu, phải ra 200; trả id phiên vừa sinh ra. */
    long acceptToSession(Student receiver, long requestId) throws Exception {
        String response = accept(receiver.token(), requestId)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.sessionId")).longValue();
    }

    /**
     * Một phiên SCHEDULED mới (start_at = S): sender gửi R0 cho receiver ở đồng hồ hiện tại, receiver chấp nhận ngay.
     * Đồng hồ phải còn trước S. Gọi nhiều lần được: R0 trước đã ACCEPTED nên không còn PENDING chặn (D89 bước 8).
     */
    long session(Student sender, Student receiver) throws Exception {
        return acceptToSession(receiver, create(sender, r0(receiver.id())));
    }

    /** GET /api/sessions + query, ví dụ "?status=SCHEDULED" hoặc "". */
    ResultActions listSessions(String token, String query) throws Exception {
        return mockMvc.perform(get("/api/sessions" + query)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    /** GET /api/sessions/{id}. id kiểu Object để test truyền được cả "abc". */
    ResultActions getSession(String token, Object id) throws Exception {
        return mockMvc.perform(get("/api/sessions/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    ResultActions start(String token, Object id) throws Exception {
        return postWithoutBody(token, "/api/sessions/" + id + "/start");
    }

    ResultActions complete(String token, Object id) throws Exception {
        return postWithoutBody(token, "/api/sessions/" + id + "/complete");
    }

    /** Hủy phiên với body {"reason": ...}. */
    ResultActions cancelSession(String token, Object id, String reason) throws Exception {
        return postJson(token, "/api/sessions/" + id + "/cancel", "{\"reason\": " + quote(reason) + "}");
    }

    // ---------- Gọi API báo cáo sự cố (bước 8) ----------

    /** POST /api/sessions/{id}/reports với body {"reason": ...}. id kiểu Object để test truyền được cả "abc". */
    ResultActions report(String token, Object sessionId, String reason) throws Exception {
        return postJson(token, "/api/sessions/" + sessionId + "/reports", "{\"reason\": " + quote(reason) + "}");
    }

    // ---------- Đọc DB ----------

    /**
     * Những gì "dữ liệu không đổi" (quy ước của ca kiểm thử) phải giữ nguyên: trạng thái, ghi chú, mốc phản hồi,
     * version của yêu cầu và số phiên của nó. So trước và sau một lệnh bị từ chối.
     */
    Map<String, Object> snapshot(long requestId) {
        return jdbcTemplate.queryForMap("""
                SELECT r.status, r.response_note, r.responded_at, r.version,
                       (SELECT count(*) FROM exchange_sessions s WHERE s.request_id = r.id) AS sessions
                FROM exchange_requests r WHERE r.id = ?
                """, requestId);
    }

    String requestStatus(long requestId) {
        return jdbcTemplate.queryForObject("SELECT status FROM exchange_requests WHERE id = ?", String.class, requestId);
    }

    Instant respondedAt(long requestId) {
        Timestamp value = jdbcTemplate.queryForObject(
                "SELECT responded_at FROM exchange_requests WHERE id = ?", Timestamp.class, requestId);
        return value == null ? null : value.toInstant();
    }

    int sessionCount(long requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM exchange_sessions WHERE request_id = ?", Integer.class, requestId);
    }

    /**
     * "Dữ liệu không đổi" của một phiên: mọi cột (trạng thái, các mốc thời gian, người bấm, lý do, version) và số
     * báo cáo sự cố của phiên. So trước và sau một lệnh bị từ chối (spec 7.1 nhóm 5).
     */
    Map<String, Object> sessionSnapshot(long sessionId) {
        return jdbcTemplate.queryForMap("""
                SELECT s.*, (SELECT count(*) FROM incident_reports r WHERE r.session_id = s.id) AS reports
                FROM exchange_sessions s WHERE s.id = ?
                """, sessionId);
    }

    String sessionStatus(long sessionId) {
        return jdbcTemplate.queryForObject("SELECT status FROM exchange_sessions WHERE id = ?", String.class, sessionId);
    }

    /**
     * Đợi tới khi có một kết nối của DB test đang chờ khóa dòng (lệnh đang chạy đã đọc xong và đang ghi), hoặc lệnh
     * đã kết thúc mà không phải chờ (code sai: để phần kiểm kết quả báo đỏ). Tối đa 10 giây.
     * Bản dùng chung của hàm cùng tên trong RequestInboxApiTest (test đã duyệt nên giữ bản riêng).
     */
    void waitUntilSomeoneWaitsForRowLock(Future<?> running) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline && !running.isDone()) {
            Integer waiting = jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE datname = current_database() AND wait_event_type = 'Lock'
                    """, Integer.class);
            if (waiting > 0) {
                return;
            }
            Thread.sleep(20);
        }
    }

    // ---------- Đọc response ----------

    static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** Các id của một mảng JSON, theo thứ tự. */
    static List<Long> ids(String jsonArray) {
        List<Number> raw = JsonPath.read(jsonArray, "$[*].id");
        return raw.stream().map(Number::longValue).toList();
    }

    // ---------- Kiểm response ----------

    /** 400 với đúng một lỗi theo trường (D24). */
    static void expectFieldError(ResultActions result, String field, String message) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(INVALID))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(jsonPath("$.errors[0].message").value(message));
    }

    /** Lỗi chỉ có detail, không có errors (bảng 2.1). */
    static void expectDetail(ResultActions result, int status, String detail) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    // ---------- JSON ----------

    /** Body JSON từ Map: chỉ có chuỗi, số và null. */
    static String json(Map<String, Object> body) {
        return body.entrySet().stream()
                .map(entry -> quote(entry.getKey()) + ": " + jsonValue(entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private static String jsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        return value instanceof String text ? quote(text) : value.toString();
    }

    static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
