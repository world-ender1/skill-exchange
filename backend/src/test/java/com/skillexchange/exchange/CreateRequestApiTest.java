package com.skillexchange.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.skillexchange.IntegrationTestBase;
import com.skillexchange.TestAccounts;
import com.skillexchange.user.Role;
import com.skillexchange.user.UserRepository;
import com.skillexchange.user.UserStatus;

/**
 * POST /api/requests (kế hoạch tuần 6, bước 5): TC311–TC325; D87–D89, bảng 2.1 và 2.2.
 * Dữ liệu mẫu theo docs/tuan_6/kiem_thu/ca_kiem_thu.md: An gửi, Bình nhận; đồng hồ giả đứng ở T0.
 * Chỗ nào ca cần "hủy yêu cầu" thì hủy bằng SQL, vì API hủy có ở bước 6 (Chốt ở bước 5).
 */
class CreateRequestApiTest extends IntegrationTestBase {

    private static final Instant T0 = Instant.parse("2026-11-18T08:00:00Z");
    /** S = 2026-11-20T12:00:00Z, viết theo giờ Việt Nam như ví dụ TK 4.6. */
    private static final String S_VN = "2026-11-20T19:00:00+07:00";
    private static final String MEET = "https://meet.google.com/abc-defg-hij";

    private static final String AN = "an@student.example.edu.vn";
    private static final String BINH = "binh@student.example.edu.vn";
    private static final String GIANG = "giang@student.example.edu.vn";
    private static final String DUNG = "dung@student.example.edu.vn";
    private static final String EM = "em@student.example.edu.vn";
    private static final String ADMIN = "qtv@student.example.edu.vn";

    private static final String INVALID = "Dữ liệu không hợp lệ";
    private static final String NOT_FOUND = "Không tìm thấy người dùng";
    private static final String DUPLICATE = "Bạn đã có một yêu cầu đang chờ gửi tới người này";
    private static final String UNREADABLE = "Nội dung JSON không đọc được";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private TestAccounts accounts;
    private String anToken;
    private String binhToken;

    @BeforeEach
    void setUp() {
        accounts = new TestAccounts(mockMvc, jdbcTemplate, userRepository, passwordEncoder);
        // Chạy sau @BeforeEach của lớp cha (đặt đồng hồ về START), nên ghi đè được.
        clock.setInstant(T0);
    }

    // ---------- Dữ liệu mẫu ----------

    /** An: dạy Tiếng Anh (3); học Java (3), Python (trống); có phone, contactInfo. */
    private long an() throws Exception {
        long id = accounts.activeStudent(AN, "An");
        anToken = accounts.login(AN);
        accounts.putProfile(anToken, "An", null, "0901000001", "Zalo: An");
        accounts.putTeachSkills(anToken, accounts.teach("Tiếng Anh", 3));
        accounts.putLearnSkills(anToken, accounts.learn("Java", 3), accounts.learn("Python", null));
        return id;
    }

    /** Bình: dạy Kotlin (3); học Tiếng Anh, Figma (trống); không có phone. */
    private long binh() throws Exception {
        long id = accounts.activeStudent(BINH, "Bình");
        binhToken = accounts.login(BINH);
        accounts.putProfile(binhToken, "Bình", null, null, "Facebook: Bình");
        accounts.putTeachSkills(binhToken, accounts.teach("Kotlin", 3));
        accounts.putLearnSkills(binhToken, accounts.learn("Tiếng Anh", null), accounts.learn("Figma", null));
        return id;
    }

    /** Giang: dạy Java (4), không học gì. */
    private long giang() throws Exception {
        long id = accounts.activeStudent(GIANG, "Giang");
        accounts.putTeachSkills(accounts.login(GIANG), accounts.teach("Java", 4));
        return id;
    }

    /** Dung (chưa xác thực) và Em (bị khóa) cùng dạy Java (2): kỹ năng ghi bằng SQL vì Em không đăng nhập được. */
    private long withStatus(String email, String fullName, UserStatus status) {
        long id = accounts.create(email, fullName, status, Role.STUDENT);
        jdbcTemplate.update("INSERT INTO user_teach_skills (user_id, skill_id, level) VALUES (?, ?, 2)",
                id, accounts.skillId("Java"));
        return id;
    }

    private long admin() {
        return accounts.create(ADMIN, "Quản trị viên", UserStatus.ACTIVE, Role.ADMIN);
    }

    /** R0: An gửi Bình, ONE_WAY, học Java, 90 phút, ONLINE. LinkedHashMap để test bớt hoặc đổi từng khóa. */
    private Map<String, Object> r0(long receiverId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("receiverId", receiverId);
        body.put("type", "ONE_WAY");
        body.put("senderLearnSkillId", accounts.skillId("Java"));
        body.put("proposedStartAt", S_VN);
        body.put("durationMinutes", 90);
        body.put("mode", "ONLINE");
        body.put("locationOrLink", MEET);
        body.put("message", "Học Java nhé");
        return body;
    }

    /** R2 = R0 với TWO_WAY, Bình học Tiếng Anh của An. */
    private Map<String, Object> r2(long receiverId) {
        Map<String, Object> body = r0(receiverId);
        body.put("type", "TWO_WAY");
        body.put("receiverLearnSkillId", accounts.skillId("Tiếng Anh"));
        return body;
    }

    // ---------- Gọi API và kiểm ----------

    private ResultActions send(String token, Map<String, Object> body) throws Exception {
        return sendRaw(token, json(body));
    }

    private ResultActions sendRaw(String token, String json) throws Exception {
        return mockMvc.perform(post("/api/requests")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    /** 400 với đúng một lỗi theo trường (D24). */
    private static void expectFieldError(ResultActions result, String field, String message) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(INVALID))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(jsonPath("$.errors[0].message").value(message));
    }

    /** Lỗi chỉ có detail, không có errors (bảng 2.1). */
    private static void expectDetail(ResultActions result, int status, String detail) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    private static long createdId(ResultActions result) throws Exception {
        String body = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** Hủy như D90 (CANCELLED, có responded_at) nhưng bằng SQL: API hủy có ở bước 6. */
    private void cancelBySql(long requestId) {
        jdbcTemplate.update("UPDATE exchange_requests SET status = 'CANCELLED', responded_at = ? WHERE id = ?",
                Timestamp.from(clock.instant()), requestId);
    }

    private int requestCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM exchange_requests", Integer.class);
    }

    /** Body JSON từ Map: chỉ có chuỗi, số và null. */
    private static String json(Map<String, Object> body) {
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

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    // ---------- Ca kiểm thử ----------

    @Test
    @DisplayName("TC311 An gửi R0 có khoảng trắng hai đầu: 201, đủ 17 khóa RequestItem, đã cắt, không có liên hệ")
    void tc311_createdRequestIsRequestItem() throws Exception {
        long anId = an();
        long binhId = binh();
        Map<String, Object> body = r0(binhId);
        body.put("locationOrLink", "  " + MEET + "  ");
        body.put("message", "  Học Java nhé  ");

        String response = send(anToken, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ONE_WAY"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.myRole").value("SENDER"))
                .andExpect(jsonPath("$.sender.id").value(anId))
                .andExpect(jsonPath("$.sender.fullName").value("An"))
                .andExpect(jsonPath("$.receiver.id").value(binhId))
                .andExpect(jsonPath("$.receiver.fullName").value("Bình"))
                .andExpect(jsonPath("$.senderLearnSkill.id").value(accounts.skillId("Java")))
                .andExpect(jsonPath("$.senderLearnSkill.name").value("Java"))
                .andExpect(jsonPath("$.proposedStartAt").value("2026-11-20T12:00:00Z"))
                .andExpect(jsonPath("$.durationMinutes").value(90))
                .andExpect(jsonPath("$.mode").value("ONLINE"))
                .andExpect(jsonPath("$.locationOrLink").value(MEET))
                .andExpect(jsonPath("$.message").value("Học Java nhé"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-18T08:00:00Z"))
                .andReturn().getResponse().getContentAsString();

        // Mọi khóa luôn có mặt, không có giá trị thì null (bảng 2.2).
        Map<String, Object> item = JsonPath.read(response, "$");
        assertThat(item).containsOnlyKeys("id", "type", "status", "myRole", "sender", "receiver",
                "senderLearnSkill", "receiverLearnSkill", "proposedStartAt", "durationMinutes", "mode",
                "locationOrLink", "message", "responseNote", "createdAt", "respondedAt", "sessionId");
        assertThat(item).containsEntry("receiverLearnSkill", null)
                .containsEntry("responseNote", null)
                .containsEntry("respondedAt", null)
                .containsEntry("sessionId", null);
        // Người gửi, người nhận chỉ có id và tên: API yêu cầu không bao giờ trả liên hệ (D91, NFR-02).
        Map<String, Object> sender = JsonPath.read(response, "$.sender");
        Map<String, Object> receiver = JsonPath.read(response, "$.receiver");
        Map<String, Object> skill = JsonPath.read(response, "$.senderLearnSkill");
        assertThat(sender).containsOnlyKeys("id", "fullName");
        assertThat(receiver).containsOnlyKeys("id", "fullName");
        assertThat(skill).containsOnlyKeys("id", "name");
        assertThat(response).doesNotContain("0901000001", "Zalo: An", "Facebook: Bình", "@student");

        // DB: đúng một dòng PENDING, chưa phản hồi.
        assertThat(requestCount()).isEqualTo(1);
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT id, sender_id, receiver_id, status, location_or_link, message, responded_at"
                        + " FROM exchange_requests");
        assertThat(((Number) row.get("id")).longValue()).isEqualTo(((Number) item.get("id")).longValue());
        assertThat(row.get("sender_id")).isEqualTo(anId);
        assertThat(row.get("receiver_id")).isEqualTo(binhId);
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(row.get("location_or_link")).isEqualTo(MEET);
        assertThat(row.get("message")).isEqualTo("Học Java nhé");
        assertThat(row.get("responded_at")).isNull();
    }

    @Test
    @DisplayName("TC312 An gửi R2 với lời nhắn toàn khoảng trắng: 201, TWO_WAY, có kỹ năng Bình học, message null")
    void tc312_twoWayWithBlankMessage() throws Exception {
        an();
        long binhId = binh();
        Map<String, Object> body = r2(binhId);
        body.put("message", "   ");

        String response = send(anToken, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TWO_WAY"))
                .andExpect(jsonPath("$.receiverLearnSkill.id").value(accounts.skillId("Tiếng Anh")))
                .andExpect(jsonPath("$.receiverLearnSkill.name").value("Tiếng Anh"))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> item = JsonPath.read(response, "$");
        assertThat(item).containsEntry("message", null);
        assertThat(jdbcTemplate.queryForObject("SELECT message FROM exchange_requests", String.class)).isNull();
    }

    @Test
    @DisplayName("TC313 R0 thiếu từng trường bắt buộc: 400 errors đúng trường, DB không có dòng mới")
    void tc313_eachMissingFieldIs400() throws Exception {
        an();
        long binhId = binh();
        Map<String, String> messages = new LinkedHashMap<>();
        messages.put("receiverId", "Chưa chọn người nhận");
        messages.put("type", "Chưa chọn loại yêu cầu");
        messages.put("senderLearnSkillId", "Chưa chọn kỹ năng bạn muốn học");
        messages.put("proposedStartAt", "Chưa chọn thời điểm");
        messages.put("durationMinutes", "Thời lượng từ 15 đến 480 phút");
        messages.put("mode", "Chưa chọn hình thức");
        messages.put("locationOrLink", "Chưa nhập địa điểm hoặc link");

        for (Map.Entry<String, String> missing : messages.entrySet()) {
            Map<String, Object> body = r0(binhId);
            body.remove(missing.getKey());
            expectFieldError(send(anToken, body), missing.getKey(), missing.getValue());
        }
        assertThat(requestCount()).isZero();
    }

    @Test
    @DisplayName("TC314 R0 thiếu cùng lúc type và durationMinutes: errors có cả hai (Bean Validation trả cùng lúc)")
    void tc314_beanValidationReportsAllErrors() throws Exception {
        an();
        long binhId = binh();
        Map<String, Object> body = r0(binhId);
        body.remove("type");
        body.remove("durationMinutes");

        send(anToken, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(INVALID))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("type", "durationMinutes")));
    }

    @Test
    @DisplayName("TC315 biên thời lượng 14/15/480/481, địa điểm rỗng/500/501, lời nhắn 1000/1001")
    void tc315_boundaries() throws Exception {
        an();
        long binhId = binh();

        // Thời lượng 15–480 (CHECK của DB, kiểm trước ở DTO).
        Map<String, Object> body = r0(binhId);
        body.put("durationMinutes", 14);
        expectFieldError(send(anToken, body), "durationMinutes", "Thời lượng từ 15 đến 480 phút");
        body.put("durationMinutes", 15);
        cancelBySql(createdId(send(anToken, body)));
        body.put("durationMinutes", 480);
        cancelBySql(createdId(send(anToken, body)));
        body.put("durationMinutes", 481);
        expectFieldError(send(anToken, body), "durationMinutes", "Thời lượng từ 15 đến 480 phút");

        // Địa điểm/link: bắt buộc, tối đa 500 ký tự đếm sau khi cắt (Q3, D88).
        body = r0(binhId);
        body.put("locationOrLink", "   ");
        expectFieldError(send(anToken, body), "locationOrLink", "Chưa nhập địa điểm hoặc link");
        body.put("locationOrLink", " " + "a".repeat(500) + " ");
        long id = createdId(send(anToken, body));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT length(location_or_link) FROM exchange_requests WHERE id = ?", Integer.class, id))
                .isEqualTo(500);
        cancelBySql(id);
        body.put("locationOrLink", "a".repeat(501));
        expectFieldError(send(anToken, body), "locationOrLink", "Địa điểm hoặc link dài tối đa 500 ký tự");

        // Lời nhắn: tối đa 1000 ký tự (Q4).
        body = r0(binhId);
        body.put("message", "m".repeat(1000));
        cancelBySql(createdId(send(anToken, body)));
        body.put("message", "m".repeat(1001));
        expectFieldError(send(anToken, body), "message", "Lời nhắn dài tối đa 1000 ký tự");

        assertThat(requestCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("TC316 TWO_WAY thiếu receiverLearnSkillId, ONE_WAY có receiverLearnSkillId: 400, hai câu khác nhau")
    void tc316_receiverSkillMustMatchType() throws Exception {
        an();
        long binhId = binh();

        Map<String, Object> twoWay = r2(binhId);
        twoWay.remove("receiverLearnSkillId");
        expectFieldError(send(anToken, twoWay), "receiverLearnSkillId",
                "Yêu cầu hai chiều cần kỹ năng người nhận muốn học");

        Map<String, Object> oneWay = r0(binhId);
        oneWay.put("receiverLearnSkillId", accounts.skillId("Tiếng Anh"));
        expectFieldError(send(anToken, oneWay), "receiverLearnSkillId",
                "Yêu cầu một chiều không có kỹ năng người nhận muốn học");

        assertThat(requestCount()).isZero();
    }

    @Test
    @DisplayName("TC317 thời điểm đúng T0 hoặc trước: 400; T0 + 1 giây: 201; mode ANY, thiếu múi giờ: JSON không đọc được")
    void tc317_proposedStartAtMustBeInFuture() throws Exception {
        an();
        long binhId = binh();
        Map<String, Object> body = r0(binhId);

        // T0 = 2026-11-18T08:00:00Z = 15:00 giờ Việt Nam. Bằng hiện tại cũng là sai (D89 bước 3).
        body.put("proposedStartAt", "2026-11-18T15:00:00+07:00");
        expectFieldError(send(anToken, body), "proposedStartAt", "Thời điểm phải ở tương lai");
        body.put("proposedStartAt", "2026-11-18T14:59:59+07:00");
        expectFieldError(send(anToken, body), "proposedStartAt", "Thời điểm phải ở tương lai");

        body.put("mode", "ANY");
        body.put("proposedStartAt", "2026-11-18T15:00:01+07:00");
        expectDetail(send(anToken, body), 400, UNREADABLE);

        body.put("mode", "ONLINE");
        body.put("proposedStartAt", "2026-11-20T19:00:00");
        expectDetail(send(anToken, body), 400, UNREADABLE);
        assertThat(requestCount()).isZero();

        body.put("proposedStartAt", "2026-11-18T15:00:01+07:00");
        send(anToken, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedStartAt").value("2026-11-18T08:00:01Z"));
    }

    @Test
    @DisplayName("TC318 An gửi cho chính mình: 400 detail, không có errors")
    void tc318_cannotSendToSelf() throws Exception {
        long anId = an();
        expectDetail(send(anToken, r0(anId)), 400, "Không thể gửi yêu cầu cho chính mình");
        assertThat(requestCount()).isZero();
    }

    @Test
    @DisplayName("TC319 người nhận 99999, chưa xác thực, bị khóa, admin: cả bốn 404 cùng một câu")
    void tc319_hiddenReceiversAreAll404() throws Exception {
        an();
        long dungId = withStatus(DUNG, "Dung", UserStatus.PENDING_VERIFICATION);
        long emId = withStatus(EM, "Em", UserStatus.LOCKED);
        long adminId = admin();

        for (long receiverId : new long[] {99999, dungId, emId, adminId}) {
            expectDetail(send(anToken, r0(receiverId)), 404, NOT_FOUND);
        }
        assertThat(requestCount()).isZero();
    }

    @Test
    @DisplayName("TC320 kỹ năng không thuộc danh sách muốn học của người gửi / người nhận: 400 errors")
    void tc320_skillsMustBeInLearnLists() throws Exception {
        an();
        long binhId = binh();

        Map<String, Object> notMine = r0(binhId);
        notMine.put("senderLearnSkillId", accounts.skillId("Kotlin"));
        expectFieldError(send(anToken, notMine), "senderLearnSkillId",
                "Kỹ năng này không có trong danh sách muốn học của bạn");

        Map<String, Object> notTheirs = r2(binhId);
        notTheirs.put("receiverLearnSkillId", accounts.skillId("Guitar"));
        expectFieldError(send(anToken, notTheirs), "receiverLearnSkillId",
                "Kỹ năng này không có trong danh sách muốn học của người nhận");

        assertThat(requestCount()).isZero();
    }

    @Test
    @DisplayName("TC321 Sim = 0 ở chiều người gửi học (Python), chiều người nhận học (Figma): 400 errors")
    void tc321_similarityMustBePositive() throws Exception {
        an();
        long binhId = binh();

        Map<String, Object> python = r0(binhId);
        python.put("senderLearnSkillId", accounts.skillId("Python"));
        expectFieldError(send(anToken, python), "senderLearnSkillId",
                "Người nhận không dạy kỹ năng này hay kỹ năng liên quan");

        Map<String, Object> figma = r2(binhId);
        figma.put("receiverLearnSkillId", accounts.skillId("Figma"));
        expectFieldError(send(anToken, figma), "receiverLearnSkillId",
                "Bạn không dạy kỹ năng này hay kỹ năng liên quan");

        assertThat(requestCount()).isZero();
    }

    @Test
    @DisplayName("TC322 thứ tự kiểm: 400 trước 404, 404 trước 400 của kỹ năng, 403 ở filter trước mọi thứ")
    void tc322_checkOrder() throws Exception {
        an();
        binh();
        long emId = withStatus(EM, "Em", UserStatus.LOCKED);

        // (a) Body sai và người nhận không có: Bean Validation chạy trước service → 400.
        Map<String, Object> a = r0(99999);
        a.remove("durationMinutes");
        expectFieldError(send(anToken, a), "durationMinutes", "Thời lượng từ 15 đến 480 phút");

        // (b) Người nhận bị khóa và kỹ năng có Sim = 0: 404 trước (bước 5 trước bước 7).
        Map<String, Object> b = r0(emId);
        b.put("senderLearnSkillId", accounts.skillId("Python"));
        expectDetail(send(anToken, b), 404, NOT_FOUND);

        // (c) Tài khoản chưa xác thực gửi body rỗng: filter chặn trước khi đọc body.
        withStatus(DUNG, "Dung", UserStatus.PENDING_VERIFICATION);
        sendRaw(accounts.login(DUNG), "{}").andExpect(status().isForbidden());

        // (d) Admin gửi R0 hợp lệ: 403 (D86).
        admin();
        send(accounts.login(ADMIN), r0(accounts.idOf(BINH))).andExpect(status().isForbidden());

        assertThat(requestCount()).isZero();
    }

    @Test
    @DisplayName("TC323 trùng PENDING cùng chiều: 409; chiều ngược được; hủy rồi gửi lại được")
    void tc323_onePendingPerDirection() throws Exception {
        long anId = an();
        long binhId = binh();

        long first = createdId(send(anToken, r0(binhId)));
        expectDetail(send(anToken, r0(binhId)), 409, DUPLICATE);

        // Bình gửi An: chiều ngược, không tính là trùng.
        Map<String, Object> reverse = r0(anId);
        reverse.put("senderLearnSkillId", accounts.skillId("Tiếng Anh"));
        createdId(send(binhToken, reverse));

        cancelBySql(first);
        createdId(send(anToken, r0(binhId)));

        assertThat(requestCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("TC324 kỹ năng đã ẩn mà còn trong danh sách học: vẫn gửi được, tên vẫn hiện")
    void tc324_hiddenSkillStillUsable() throws Exception {
        an();
        long binhId = binh();
        admin();
        String adminToken = accounts.login(ADMIN);
        long javaId = accounts.skillId("Java");
        long categoryId = jdbcTemplate.queryForObject(
                "SELECT category_id FROM skills WHERE id = ?", Long.class, javaId);

        try {
            mockMvc.perform(put("/api/admin/skills/" + javaId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"Java\", \"categoryId\": " + categoryId + ", \"isActive\": false}"))
                    .andExpect(status().isOk());

            send(anToken, r0(binhId))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.senderLearnSkill.name").value("Java"));
        } finally {
            // Danh mục không bị TRUNCATE giữa các test: trả lại như V2 để test khác không lệch.
            jdbcTemplate.update("UPDATE skills SET is_active = TRUE WHERE id = ?", javaId);
        }
    }

    @Test
    @DisplayName("TC325 không kiểm hình thức học của người nhận, không kiểm chồng giờ; thẻ Bình có hasPendingRequest")
    void tc325_noModeOrOverlapCheck() throws Exception {
        an();
        long binhId = binh();
        long giangId = giang();
        accounts.putProfile(binhToken, "Bình", "ONLINE", null, "Facebook: Bình");

        Map<String, Object> offline = r0(binhId);
        offline.put("mode", "OFFLINE");
        send(anToken, offline).andExpect(status().isCreated()).andExpect(jsonPath("$.mode").value("OFFLINE"));

        // Cùng giờ S với yêu cầu trên, gửi người khác.
        send(anToken, r0(giangId)).andExpect(status().isCreated());

        String matches = mockMvc.perform(get("/api/matches?type=TWO_WAY")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + anToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> binhCard = JsonPath.read(matches, "$.items[?(@.userId == " + binhId + ")]");
        assertThat(binhCard).hasSize(1);
        assertThat(binhCard.get(0)).containsEntry("hasPendingRequest", true);
    }

    // ---------- Phần D mà TC chưa phủ ----------

    @Test
    @DisplayName("D87 không có vé: 401")
    void d87_noTokenIs401() throws Exception {
        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("D89 dừng ở lỗi đầu tiên theo thứ tự (2) → (3) → (6) → (7) → (8); trong (6), (7) người gửi trước")
    void d89_stopsAtFirstFailedStep() throws Exception {
        long anId = an();
        long binhId = binh();

        // (2) trước (3): TWO_WAY thiếu kỹ năng chiều ngược và thời điểm đã qua.
        Map<String, Object> body = r2(binhId);
        body.remove("receiverLearnSkillId");
        body.put("proposedStartAt", "2026-11-18T07:00:00Z");
        expectFieldError(send(anToken, body), "receiverLearnSkillId",
                "Yêu cầu hai chiều cần kỹ năng người nhận muốn học");

        // (3) trước (4): gửi chính mình với thời điểm đã qua.
        body = r0(anId);
        body.put("proposedStartAt", "2026-11-18T07:00:00Z");
        expectFieldError(send(anToken, body), "proposedStartAt", "Thời điểm phải ở tương lai");

        // (6) trước (7): Guitar không trong danh sách học của An, cũng không ai dạy.
        body = r0(binhId);
        body.put("senderLearnSkillId", accounts.skillId("Guitar"));
        expectFieldError(send(anToken, body), "senderLearnSkillId",
                "Kỹ năng này không có trong danh sách muốn học của bạn");

        // Trong (6): người gửi trước người nhận.
        body = r2(binhId);
        body.put("senderLearnSkillId", accounts.skillId("Kotlin"));
        body.put("receiverLearnSkillId", accounts.skillId("Guitar"));
        expectFieldError(send(anToken, body), "senderLearnSkillId",
                "Kỹ năng này không có trong danh sách muốn học của bạn");

        // Cả (6) xong rồi mới tới (7): Python có trong danh sách học nhưng Sim = 0; Guitar sai ở (6).
        body = r2(binhId);
        body.put("senderLearnSkillId", accounts.skillId("Python"));
        body.put("receiverLearnSkillId", accounts.skillId("Guitar"));
        expectFieldError(send(anToken, body), "receiverLearnSkillId",
                "Kỹ năng này không có trong danh sách muốn học của người nhận");

        // Trong (7): người gửi trước người nhận.
        body = r2(binhId);
        body.put("senderLearnSkillId", accounts.skillId("Python"));
        body.put("receiverLearnSkillId", accounts.skillId("Figma"));
        expectFieldError(send(anToken, body), "senderLearnSkillId",
                "Người nhận không dạy kỹ năng này hay kỹ năng liên quan");

        // Đã có PENDING: lỗi (3) và (7) vẫn ra 400, không phải 409.
        createdId(send(anToken, r0(binhId)));
        body = r0(binhId);
        body.put("proposedStartAt", "2026-11-18T07:00:00Z");
        expectFieldError(send(anToken, body), "proposedStartAt", "Thời điểm phải ở tương lai");
        body = r0(binhId);
        body.put("senderLearnSkillId", accounts.skillId("Python"));
        expectFieldError(send(anToken, body), "senderLearnSkillId",
                "Người nhận không dạy kỹ năng này hay kỹ năng liên quan");

        assertThat(requestCount()).isEqualTo(1);
    }
}
