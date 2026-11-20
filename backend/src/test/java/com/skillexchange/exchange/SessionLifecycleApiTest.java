package com.skillexchange.exchange;

import static com.skillexchange.exchange.ExchangeTestData.MEET;
import static com.skillexchange.exchange.ExchangeTestData.S;
import static com.skillexchange.exchange.ExchangeTestData.T0;
import static com.skillexchange.exchange.ExchangeTestData.T1;
import static com.skillexchange.exchange.ExchangeTestData.body;
import static com.skillexchange.exchange.ExchangeTestData.expectDetail;
import static com.skillexchange.exchange.ExchangeTestData.expectFieldError;
import static com.skillexchange.exchange.ExchangeTestData.ids;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.skillexchange.IntegrationTestBase;
import com.skillexchange.exchange.ExchangeTestData.Student;
import com.skillexchange.user.UserRepository;

/**
 * Vòng đời phiên (kế hoạch tuần 6, bước 7): TC338–TC350 (TC348 trừ phần reports, ở bước 8), TC355;
 * D87, D88, D92, D93, bảng 2.1–2.3.
 * GET /api/sessions?status=, GET /api/sessions/{id}, POST /api/sessions/{id}/start, /complete, /cancel.
 *
 * Dữ liệu mẫu theo docs/tuan_6/kiem_thu/ca_kiem_thu.md: An và Bình có sẵn trước mỗi test, đồng hồ giả ở T0.
 * Phiên P = An gửi R0 lúc T0, Bình chấp nhận lúc T1; start_at = S.
 */
class SessionLifecycleApiTest extends IntegrationTestBase {

    // Câu detail của bảng 2.1 (Quyet_dinh_tuan_6.md).
    private static final String SESSION_NOT_FOUND = "Không tìm thấy phiên";
    private static final String NOT_PARTICIPANT = "Bạn không tham gia phiên này";
    private static final String ONLY_SCHEDULED = "Chỉ bắt đầu được phiên đã lên lịch";
    private static final String TOO_EARLY = "Chỉ bắt đầu được từ 15 phút trước giờ hẹn";
    private static final String CANNOT_COMPLETE = "Phiên không ở trạng thái xác nhận hoàn thành được";
    private static final String ALREADY_COMPLETED = "Bạn đã xác nhận hoàn thành, đang chờ bên kia";
    private static final String CANNOT_CANCEL_WAITING =
            "Phiên đang chờ xác nhận, không hủy được; hãy gửi báo cáo sự cố nếu có bất đồng";
    private static final String ENDED = "Phiên đã kết thúc";
    private static final String REASON_MISSING = "Chưa nhập lý do";
    private static final String REASON_TOO_LONG = "Lý do dài tối đa 1000 ký tự";
    private static final String STALE = "Dữ liệu vừa được thay đổi, hãy tải lại";

    /** 12 khóa của SessionItem (bảng 2.2). */
    private static final String[] SESSION_ITEM_KEYS = {"id", "requestId", "type", "status", "myRole", "partner",
            "senderLearnSkill", "receiverLearnSkill", "startAt", "durationMinutes", "mode", "createdAt"};

    /** SessionDetail = 12 khóa của SessionItem + 10 khóa riêng (bảng 2.2). */
    private static final String[] SESSION_DETAIL_KEYS = Stream.concat(Stream.of(SESSION_ITEM_KEYS),
            Stream.of("locationOrLink", "startedAt", "completedAt", "myCompleted", "cancelledBy", "cancelReason",
                    "cancelledAt", "myReviewed", "openReport", "resolution")).toArray(String[]::new);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Lấy kết nối JDBC riêng cho test khóa lạc quan (một giao dịch khác ngoài ứng dụng). */
    @Autowired
    private DataSource dataSource;

    private ExchangeTestData data;
    private Student an;
    private Student binh;

    @BeforeEach
    void setUp() throws Exception {
        data = new ExchangeTestData(mockMvc, jdbcTemplate, userRepository, passwordEncoder);
        // Chạy sau @BeforeEach của lớp cha (xóa dữ liệu, đặt đồng hồ về START), nên ghi đè được.
        clock.setInstant(T0);
        an = data.an();
        binh = data.binh();
    }

    // ---------- Tiện ích ----------

    /** Phiên P: An gửi R0 lúc T0, Bình chấp nhận lúc T1. Đồng hồ để lại ở T1. */
    private long sessionP() throws Exception {
        long requestId = data.create(an, data.r0(binh.id()));
        clock.setInstant(T1);
        return data.acceptToSession(binh, requestId);
    }

    /**
     * Vặn đồng hồ rồi đăng nhập lại An và Bình. Vé chỉ có hạn 24 giờ và được kiểm bằng cùng đồng hồ giả (D18),
     * nên đi từ T0 tới quanh giờ hẹn S (hơn 2 ngày) thì vé cũ thành 401.
     */
    private void moveClockTo(Instant instant) throws Exception {
        clock.setInstant(instant);
        an = data.relogin(an);
        binh = data.relogin(binh);
    }

    /** Đọc object JSON của response thành Map để kiểm đủ khóa và các khóa null. */
    private static Map<String, Object> object(String json, String path) {
        return JsonPath.read(json, path);
    }

    /** Reputation của thẻ Bình trong GET /api/matches?type=TWO_WAY của An (forward = Bình là người dạy). */
    private double binhReputationSeenByAn() throws Exception {
        String response = body(mockMvc.perform(get("/api/matches?type=TWO_WAY")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + an.token()))
                .andExpect(status().isOk()));
        List<Number> values = JsonPath.read(response, "$.items[?(@.userId == " + binh.id() + ")].forward.reputation");
        assertThat(values).as("thẻ của Bình").hasSize(1);
        return values.get(0).doubleValue();
    }

    // ================= Quyền ở filter =================

    @Test
    @DisplayName("TC338 admin, tài khoản chưa xác thực, tài khoản bị khóa (vé cũ): 403 ở filter với hộp yêu cầu, tạo yêu cầu {}, danh sách phiên")
    void tc338_onlyActiveStudentsReachExchangeApis() throws Exception {
        List<String> tokens = List.of(data.adminToken(), data.dungToken(), data.emTokenThenLocked());

        for (String token : tokens) {
            data.listRequests(token, "?box=sent").andExpect(status().isForbidden());
            // {} sẽ là 400 nếu tới được controller: 403 nghĩa là bị chặn ở filter trước đó.
            data.postJson(token, "/api/requests", "{}").andExpect(status().isForbidden());
            data.listSessions(token, "").andExpect(status().isForbidden());
        }
    }

    // ================= Danh sách và chi tiết =================

    @Test
    @DisplayName("TC339 danh sách phiên: An 1 phiên (SENDER), Bình 2 phiên Q rồi P (RECEIVER); đúng 12 khóa, không có địa điểm, liên hệ; lọc status")
    void tc339_listMySessions() throws Exception {
        Student chi = data.chi();
        long requestP = data.create(an, data.r0(binh.id()));
        long requestQ = data.create(chi, data.oneWay(binh.id(), "Kotlin"));
        clock.setInstant(T1);
        long p = data.acceptToSession(binh, requestP);
        clock.setInstant(T1.plus(Duration.ofMinutes(1)));
        long q = data.acceptToSession(binh, requestQ);

        // An: chỉ phiên P, phía bên kia là Bình (chỉ id, họ tên).
        String anList = body(data.listSessions(an.token(), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(requestP))
                .andExpect(jsonPath("$[0].type").value("ONE_WAY"))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$[0].myRole").value("SENDER"))
                .andExpect(jsonPath("$[0].partner.id").value(binh.id()))
                .andExpect(jsonPath("$[0].partner.fullName").value("Bình"))
                .andExpect(jsonPath("$[0].senderLearnSkill.id").value(data.skillId("Java")))
                .andExpect(jsonPath("$[0].senderLearnSkill.name").value("Java"))
                .andExpect(jsonPath("$[0].startAt").value("2026-11-20T12:00:00Z"))
                .andExpect(jsonPath("$[0].durationMinutes").value(90))
                .andExpect(jsonPath("$[0].mode").value("ONLINE"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-11-18T09:00:00Z")));
        assertThat(ids(anList)).containsExactly(p);
        assertThat(object(anList, "$[0]")).containsOnlyKeys(SESSION_ITEM_KEYS).containsEntry("receiverLearnSkill", null);
        assertThat(object(anList, "$[0].partner")).containsOnlyKeys("id", "fullName");

        // Bình: Q (tạo sau) đứng trước P.
        String binhList = body(data.listSessions(binh.token(), "").andExpect(status().isOk()));
        assertThat(ids(binhList)).containsExactly(q, p);
        List<String> roles = JsonPath.read(binhList, "$[*].myRole");
        assertThat(roles).containsExactly("RECEIVER", "RECEIVER");
        List<String> partners = JsonPath.read(binhList, "$[*].partner.fullName");
        assertThat(partners).containsExactly("Chi", "An");
        List<Map<String, Object>> items = JsonPath.read(binhList, "$");
        assertThat(items).allSatisfy(item -> assertThat(item).containsOnlyKeys(SESSION_ITEM_KEYS));

        // Danh sách không bao giờ có địa điểm/link và liên hệ (D92).
        for (String list : List.of(anList, binhList)) {
            assertThat(list).doesNotContain("\"locationOrLink\"", "\"phone\"", "\"contactInfo\"",
                    MEET, "0901000001", "Zalo: An", "Facebook: Bình");
        }

        String scheduled = body(data.listSessions(binh.token(), "?status=SCHEDULED").andExpect(status().isOk()));
        assertThat(ids(scheduled)).containsExactly(q, p);
        String inProgress = body(data.listSessions(binh.token(), "?status=IN_PROGRESS").andExpect(status().isOk()));
        assertThat(ids(inProgress)).isEmpty();
        expectDetail(data.listSessions(binh.token(), "?status=FOO"), 400,
                "Tham số status có giá trị không hợp lệ: FOO");
    }

    @Test
    @DisplayName("TC340 chi tiết phiên: liên hệ hiện tại của bên kia và địa điểm/link; người ngoài 403; 99999: 404; abc: 400")
    void tc340_sessionDetailShowsPartnerContact() throws Exception {
        Student chi = data.chi();
        long p = sessionP();

        // An xem: bên kia là Bình (không có số điện thoại).
        String anView = body(data.getSession(an.token(), p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(p))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.myRole").value("SENDER"))
                .andExpect(jsonPath("$.partner.id").value(binh.id()))
                .andExpect(jsonPath("$.partner.contactInfo").value("Facebook: Bình"))
                .andExpect(jsonPath("$.locationOrLink").value(MEET))
                .andExpect(jsonPath("$.myCompleted").value(false))
                .andExpect(jsonPath("$.myReviewed").value(false)));
        assertThat(object(anView, "$")).containsOnlyKeys(SESSION_DETAIL_KEYS)
                .containsEntry("startedAt", null)
                .containsEntry("completedAt", null)
                .containsEntry("cancelledBy", null)
                .containsEntry("cancelReason", null)
                .containsEntry("cancelledAt", null)
                .containsEntry("openReport", null)
                .containsEntry("resolution", null);
        assertThat(object(anView, "$.partner")).containsOnlyKeys("id", "fullName", "phone", "contactInfo")
                .containsEntry("phone", null);

        // Bình xem: bên kia là An.
        String binhView = body(data.getSession(binh.token(), p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("RECEIVER"))
                .andExpect(jsonPath("$.partner.id").value(an.id()))
                .andExpect(jsonPath("$.partner.fullName").value("An"))
                .andExpect(jsonPath("$.partner.phone").value("0901000001"))
                .andExpect(jsonPath("$.partner.contactInfo").value("Zalo: An"))
                .andExpect(jsonPath("$.locationOrLink").value(MEET))
                .andExpect(jsonPath("$.myCompleted").value(false))
                .andExpect(jsonPath("$.myReviewed").value(false)));
        assertThat(object(binhView, "$")).containsOnlyKeys(SESSION_DETAIL_KEYS)
                .containsEntry("cancelledBy", null)
                .containsEntry("openReport", null)
                .containsEntry("resolution", null);

        expectDetail(data.getSession(chi.token(), p), 403, NOT_PARTICIPANT);
        expectDetail(data.getSession(an.token(), 99999), 404, SESSION_NOT_FOUND);
        expectDetail(data.getSession(an.token(), "abc"), 400, "Tham số id có giá trị không hợp lệ: abc");
    }

    // ================= Bắt đầu =================

    @Test
    @DisplayName("TC341 start lúc S − 15 phút − 1 giây: 409, dữ liệu không đổi; đúng S − 15 phút: 200 IN_PROGRESS, startedAt = đồng hồ")
    void tc341_startFromFifteenMinutesBefore() throws Exception {
        long p = sessionP();
        Instant opensAt = S.minus(Duration.ofMinutes(15));

        moveClockTo(opensAt.minusSeconds(1));
        Map<String, Object> before = data.sessionSnapshot(p);
        expectDetail(data.start(binh.token(), p), 409, TOO_EARLY);
        assertThat(data.sessionSnapshot(p)).isEqualTo(before);

        moveClockTo(opensAt);
        String response = body(data.start(binh.token(), p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(p))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.myRole").value("RECEIVER"))
                .andExpect(jsonPath("$.startedAt").value("2026-11-20T11:45:00Z")));
        assertThat(object(response, "$")).containsOnlyKeys(SESSION_DETAIL_KEYS);
        assertThat(data.sessionStatus(p)).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("TC342 ba ngày sau giờ hẹn: phiên vẫn SCHEDULED (không tự hết hạn); start 200 (không có cận trên)")
    void tc342_scheduledNeverExpires() throws Exception {
        long p = sessionP();
        moveClockTo(S.plus(Duration.ofDays(3)));

        data.getSession(an.token(), p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
        data.start(an.token(), p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedAt").value("2026-11-23T12:00:00Z"));
    }

    // ================= Hoàn thành =================

    @Test
    @DisplayName("TC343 An hoàn thành (WAITING_CONFIRMATION, myCompleted); An bấm lần nữa 409; Bình xác nhận: COMPLETED, completedAt = đồng hồ")
    void tc343_twoStepCompletion() throws Exception {
        long p = sessionP();
        moveClockTo(S);
        data.start(binh.token(), p).andExpect(status().isOk());

        clock.setInstant(S.plus(Duration.ofMinutes(60)));
        data.complete(an.token(), p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_CONFIRMATION"))
                .andExpect(jsonPath("$.myCompleted").value(true));

        Map<String, Object> before = data.sessionSnapshot(p);
        expectDetail(data.complete(an.token(), p), 409, ALREADY_COMPLETED);
        assertThat(data.sessionSnapshot(p)).isEqualTo(before);

        data.getSession(binh.token(), p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_CONFIRMATION"))
                .andExpect(jsonPath("$.myCompleted").value(false));

        clock.setInstant(S.plus(Duration.ofMinutes(90)));
        data.complete(binh.token(), p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").value("2026-11-20T13:30:00Z"))
                .andExpect(jsonPath("$.myCompleted").value(false));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT first_completed_by FROM exchange_sessions WHERE id = ?", Long.class, p)).isEqualTo(an.id());
    }

    // ================= Hủy =================

    @Test
    @DisplayName("TC344 hủy SCHEDULED trước giờ hẹn (lý do đã cắt, cancelledBy ME/PARTNER) và sau giờ hẹn hai ngày: 200 CANCELLED")
    void tc344_cancelScheduledAtAnyTime() throws Exception {
        // (a) Một ngày trước giờ hẹn, An hủy.
        long p = sessionP();
        moveClockTo(S.minus(Duration.ofDays(1)));
        String response = body(data.cancelSession(an.token(), p, "  Bận đột xuất  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("Bận đột xuất"))
                .andExpect(jsonPath("$.cancelledAt").value("2026-11-19T12:00:00Z"))
                .andExpect(jsonPath("$.cancelledBy").value("ME")));
        assertThat(object(response, "$")).containsOnlyKeys(SESSION_DETAIL_KEYS).containsEntry("startedAt", null);

        data.getSession(an.token(), p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledBy").value("ME"));
        data.getSession(binh.token(), p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledBy").value("PARTNER"))
                .andExpect(jsonPath("$.cancelReason").value("Bận đột xuất"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT cancelled_by FROM exchange_sessions WHERE id = ?", Long.class, p)).isEqualTo(an.id());

        // (b) Phiên P mới; hai ngày sau giờ hẹn, Bình hủy: vẫn được (R34).
        moveClockTo(T0);
        long second = data.session(an, binh);
        moveClockTo(S.plus(Duration.ofDays(2)));
        data.cancelSession(binh.token(), second, "Không liên lạc được")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledBy").value("ME"));
    }

    // ================= CompletionRate (Reputation) =================
    // Ba bộ dữ liệu riêng của TC345 là ba test: IntegrationTestBase xóa dữ liệu trước mỗi test.

    @Test
    @DisplayName("TC345a hủy từ SCHEDULED: Reputation của Bình 0.5 (chưa có phiên tính được)")
    void tc345a_cancelBeforeStartDoesNotCount() throws Exception {
        long p = sessionP();
        data.cancelSession(an.token(), p, "Bận").andExpect(status().isOk());

        assertThat(binhReputationSeenByAn()).isCloseTo(0.7 * 0.5 + 0.3 * 0.5, within(1e-9));
    }

    @Test
    @DisplayName("TC345b hủy từ IN_PROGRESS: Reputation của Bình 0.7 × 0.5 + 0.3 × 0 = 0.35")
    void tc345b_cancelAfterStartCounts() throws Exception {
        long p = sessionP();
        moveClockTo(S);
        data.start(an.token(), p).andExpect(status().isOk());
        data.cancelSession(an.token(), p, "Mất mạng").andExpect(status().isOk());

        assertThat(binhReputationSeenByAn()).isCloseTo(0.7 * 0.5 + 0.3 * 0, within(1e-9));
    }

    @Test
    @DisplayName("TC345c COMPLETED: Reputation của Bình 0.7 × 0.5 + 0.3 × 1 = 0.65")
    void tc345c_completedCounts() throws Exception {
        long p = sessionP();
        moveClockTo(S);
        data.start(an.token(), p).andExpect(status().isOk());
        data.complete(an.token(), p).andExpect(status().isOk());
        data.complete(binh.token(), p).andExpect(status().isOk());

        assertThat(binhReputationSeenByAn()).isCloseTo(0.7 * 0.5 + 0.3 * 1, within(1e-9));
    }

    @Test
    @DisplayName("TC346 hủy IN_PROGRESS: không body 400; {}, lý do trắng, 1001 ký tự: 400 errors reason; lý do hợp lệ: 200, giữ startedAt")
    void tc346_cancelInProgressNeedsReason() throws Exception {
        long p = sessionP();
        moveClockTo(S);
        data.start(binh.token(), p).andExpect(status().isOk());
        String path = "/api/sessions/" + p + "/cancel";
        Map<String, Object> before = data.sessionSnapshot(p);

        data.postWithoutBody(an.token(), path).andExpect(status().isBadRequest());
        expectFieldError(data.postJson(an.token(), path, "{}"), "reason", REASON_MISSING);
        expectFieldError(data.cancelSession(an.token(), p, "   "), "reason", REASON_MISSING);
        expectFieldError(data.cancelSession(an.token(), p, "l".repeat(1001)), "reason", REASON_TOO_LONG);
        assertThat(data.sessionSnapshot(p)).isEqualTo(before);

        clock.setInstant(S.plus(Duration.ofMinutes(30)));
        data.cancelSession(an.token(), p, "Mất điện")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("Mất điện"))
                .andExpect(jsonPath("$.cancelledBy").value("ME"))
                .andExpect(jsonPath("$.startedAt").value("2026-11-20T12:00:00Z"))
                .andExpect(jsonPath("$.cancelledAt").value("2026-11-20T12:30:00Z"));
        // Giữ started_at: phiên hủy sau khi bắt đầu được tính vào CompletionRate (D93).
        assertThat(jdbcTemplate.queryForObject(
                "SELECT started_at FROM exchange_sessions WHERE id = ?", Timestamp.class, p).toInstant()).isEqualTo(S);
    }

    // ================= Thao tác bị cấm theo trạng thái =================

    @Test
    @DisplayName("TC347 mọi ô 409 của bảng trạng thái phiên: đúng câu bảng 2.1, dữ liệu không đổi; trạng thái xét trước mốc 15 phút")
    void tc347_forbiddenTransitionsAre409() throws Exception {
        // Năm phiên An–Bình, cùng start_at = S, tạo và chấp nhận lúc T0.
        long inProgress = data.session(an, binh);
        long waiting = data.session(an, binh);
        long completed = data.session(an, binh);
        long scheduled = data.session(an, binh);
        long cancelled = data.session(an, binh);

        // Còn sớm hơn mốc 15 phút: phiên đã hủy bị từ chối vì trạng thái, không phải vì giờ (Chốt ở bước 7).
        data.cancelSession(an.token(), cancelled, "Bận").andExpect(status().isOk());
        Map<String, Object> cancelledBefore = data.sessionSnapshot(cancelled);
        expectDetail(data.start(an.token(), cancelled), 409, ONLY_SCHEDULED);
        assertThat(data.sessionSnapshot(cancelled)).isEqualTo(cancelledBefore);

        // Dựng các trạng thái còn lại lúc giờ hẹn.
        moveClockTo(S);
        for (long id : new long[] {inProgress, waiting, completed}) {
            data.start(binh.token(), id).andExpect(status().isOk());
        }
        data.complete(an.token(), waiting).andExpect(status().isOk());
        data.complete(an.token(), completed).andExpect(status().isOk());
        data.complete(binh.token(), completed).andExpect(status().isOk());

        Map<Long, Map<String, Object>> before = new LinkedHashMap<>();
        for (long id : new long[] {inProgress, waiting, completed, scheduled, cancelled}) {
            before.put(id, data.sessionSnapshot(id));
        }

        // WAITING_CONFIRMATION: không ai hủy được, không bắt đầu lại được.
        expectDetail(data.cancelSession(an.token(), waiting, "Đổi ý"), 409, CANNOT_CANCEL_WAITING);
        expectDetail(data.cancelSession(binh.token(), waiting, "Đổi ý"), 409, CANNOT_CANCEL_WAITING);
        expectDetail(data.start(an.token(), waiting), 409, ONLY_SCHEDULED);

        // COMPLETED, CANCELLED: trạng thái cuối.
        for (long ended : new long[] {completed, cancelled}) {
            expectDetail(data.start(an.token(), ended), 409, ONLY_SCHEDULED);
            expectDetail(data.complete(an.token(), ended), 409, CANNOT_COMPLETE);
            expectDetail(data.cancelSession(an.token(), ended, "Đổi ý"), 409, ENDED);
        }

        expectDetail(data.complete(an.token(), scheduled), 409, CANNOT_COMPLETE);
        expectDetail(data.start(an.token(), inProgress), 409, ONLY_SCHEDULED);

        before.forEach((id, snapshot) ->
                assertThat(data.sessionSnapshot(id)).as("phiên %d", id).isEqualTo(snapshot));
    }

    // ================= Người ngoài, {id} sai =================

    @Test
    @DisplayName("TC348 (phần bước 7) Chi start/complete/cancel: 403; {id} 99999: 404; abc: 400; cancel 99999 lý do rỗng: 400 trước 404")
    void tc348_outsiderAndBadIds() throws Exception {
        Student chi = data.chi();
        long p = sessionP();
        Map<String, Object> before = data.sessionSnapshot(p);

        expectDetail(data.start(chi.token(), p), 403, NOT_PARTICIPANT);
        expectDetail(data.complete(chi.token(), p), 403, NOT_PARTICIPANT);
        expectDetail(data.cancelSession(chi.token(), p, "Phá đám"), 403, NOT_PARTICIPANT);
        // Phần Chi gọi reports: bước 8 (API báo cáo chưa có).
        assertThat(data.sessionSnapshot(p)).isEqualTo(before);

        expectDetail(data.start(an.token(), 99999), 404, SESSION_NOT_FOUND);
        expectDetail(data.complete(an.token(), 99999), 404, SESSION_NOT_FOUND);
        expectDetail(data.cancelSession(an.token(), 99999, "Bận"), 404, SESSION_NOT_FOUND);

        String malformed = "Tham số id có giá trị không hợp lệ: abc"; // D38
        expectDetail(data.start(an.token(), "abc"), 400, malformed);
        expectDetail(data.complete(an.token(), "abc"), 400, malformed);
        expectDetail(data.cancelSession(an.token(), "abc", "Bận"), 400, malformed);

        expectFieldError(data.cancelSession(an.token(), 99999, ""), "reason", REASON_MISSING);
    }

    // ================= Khóa lạc quan =================

    @Test
    @DisplayName("TC349 20 lần An và Bình cùng bấm hoàn thành: (200, 409, WAITING_CONFIRMATION) hoặc (200, 200, COMPLETED); không 500")
    void tc349_bothCompleteAtTheSameTime() throws Exception {
        // 20 phiên tạo sẵn lúc T0 (yêu cầu mới phải có giờ hẹn ở tương lai), rồi bắt đầu cả 20 lúc S.
        List<Long> sessions = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            sessions.add(data.session(an, binh));
        }
        moveClockTo(S);
        for (long id : sessions) {
            data.start(an.token(), id).andExpect(status().isOk());
        }

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < sessions.size(); round++) {
                long id = sessions.get(round);
                CountDownLatch start = new CountDownLatch(1);
                Future<Integer> anCompletes = pool.submit(() -> {
                    start.await();
                    return data.complete(an.token(), id).andReturn().getResponse().getStatus();
                });
                Future<Integer> binhCompletes = pool.submit(() -> {
                    start.await();
                    return data.complete(binh.token(), id).andReturn().getResponse().getStatus();
                });
                start.countDown(); // thả hai luồng cùng lúc

                List<Integer> statuses = Stream.of(anCompletes.get(10, TimeUnit.SECONDS),
                                binhCompletes.get(10, TimeUnit.SECONDS))
                        .sorted()
                        .toList();
                // Chạy nối tiếp: hai 200, phiên COMPLETED. Tranh chấp thật: người ghi sau nhận 409 vì version.
                // Không bao giờ hai 200 mà phiên vẫn chờ xác nhận (một lần bấm bị ghi đè mất).
                String outcome = statuses + " " + data.sessionStatus(id);
                assertThat(outcome).as("lần %d", round + 1)
                        .isIn("[200, 409] WAITING_CONFIRMATION", "[200, 200] COMPLETED");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("D93 khóa lạc quan, tranh chấp chắc chắn: Bình vừa hoàn thành trong lúc An đang ghi → An 409, Bình vẫn là người bấm trước")
    void d93_staleCompleteIsRejectedByVersion() throws Exception {
        long p = sessionP();
        moveClockTo(S);
        data.start(an.token(), p).andExpect(status().isOk());

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try (Connection other = dataSource.getConnection()) {
            // Một giao dịch khác làm thay lệnh "hoàn thành" của Bình nhưng CHƯA commit: PostgreSQL khóa dòng này.
            other.setAutoCommit(false);
            try (PreparedStatement binhCompletes = other.prepareStatement("""
                    UPDATE exchange_sessions
                    SET status = 'WAITING_CONFIRMATION', first_completed_by = ?, version = version + 1
                    WHERE id = ?
                    """)) {
                binhCompletes.setLong(1, binh.id());
                binhCompletes.setLong(2, p);
                binhCompletes.executeUpdate();
            }

            // An đọc bản đã commit (IN_PROGRESS) nên qua mọi kiểm tra, rồi đứng chờ khi ghi vào dòng bị khóa.
            Future<MvcResult> anCompletes = pool.submit(() -> data.complete(an.token(), p).andReturn());
            data.waitUntilSomeoneWaitsForRowLock(anCompletes);
            other.commit();

            // Lúc được ghi, version đã tăng: câu UPDATE ... WHERE version = <bản An đọc> không khớp dòng nào.
            MvcResult result = anCompletes.get(10, TimeUnit.SECONDS);
            status().isConflict().match(result);
            jsonPath("$.detail").value(STALE).match(result);
        } finally {
            pool.shutdownNow();
        }

        assertThat(data.sessionStatus(p)).isEqualTo("WAITING_CONFIRMATION");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT first_completed_by FROM exchange_sessions WHERE id = ?", Long.class, p)).isEqualTo(binh.id());
    }

    // ================= Trạng thái tài khoản bên kia =================

    @Test
    @DisplayName("TC350 Bình bị khóa: An vẫn start, GET, complete được (200); Bình dùng vé cũ: 403")
    void tc350_partnerStatusIsIgnored() throws Exception {
        long p = sessionP();
        moveClockTo(S);
        jdbcTemplate.update("UPDATE users SET status = 'LOCKED' WHERE id = ?", binh.id());

        data.start(an.token(), p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        data.getSession(an.token(), p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partner.id").value(binh.id()));
        data.complete(an.token(), p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_CONFIRMATION"));

        data.getSession(binh.token(), p).andExpect(status().isForbidden());
    }

    // ================= Đánh giá, báo cáo dựng bằng SQL =================

    @Test
    @DisplayName("TC355 myReviewed theo người xem; báo cáo đã chốt hủy phiên: resolution, openReport null, cancelledBy ADMIN")
    void tc355_reviewAndResolutionFromDb() throws Exception {
        long adminId = data.adminId();
        long first = data.session(an, binh);
        long second = data.session(an, binh);
        moveClockTo(S);
        for (long id : new long[] {first, second}) {
            data.start(an.token(), id).andExpect(status().isOk());
            data.complete(an.token(), id).andExpect(status().isOk());
        }

        // (a) Phiên COMPLETED qua API; An đánh giá Bình (bằng SQL: API đánh giá có ở tuần 7).
        data.complete(binh.token(), first).andExpect(status().isOk());
        jdbcTemplate.update("""
                INSERT INTO reviews (session_id, reviewer_id, reviewee_id, rating, created_at)
                VALUES (?, ?, ?, 5, ?)
                """, first, an.id(), binh.id(), Timestamp.from(S.plus(Duration.ofHours(1))));
        data.getSession(an.token(), first)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myReviewed").value(true));
        data.getSession(binh.token(), first)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myReviewed").value(false));

        // (b) Phiên WAITING_CONFIRMATION có báo cáo OPEN; admin chốt "hủy phiên" (bằng SQL: A02 có ở tuần 7).
        Instant resolvedAt = S.plus(Duration.ofHours(2));
        Long reportId = jdbcTemplate.queryForObject("""
                INSERT INTO incident_reports (session_id, reporter_id, reason, created_at)
                VALUES (?, ?, 'An không đến', ?) RETURNING id
                """, Long.class, second, binh.id(), Timestamp.from(S.plus(Duration.ofMinutes(10))));
        jdbcTemplate.update("""
                UPDATE incident_reports
                SET status = 'RESOLVED', resolution = 'SESSION_CANCELLED', admin_note = 'Không diễn ra',
                    resolved_by = ?, resolved_at = ?
                WHERE id = ?
                """, adminId, Timestamp.from(resolvedAt), reportId);
        jdbcTemplate.update("""
                UPDATE exchange_sessions
                SET status = 'CANCELLED', cancelled_by = ?, cancel_reason = 'Không diễn ra', cancelled_at = ?,
                    version = version + 1
                WHERE id = ?
                """, adminId, Timestamp.from(resolvedAt), second);

        for (Student viewer : List.of(an, binh)) {
            String view = body(data.getSession(viewer.token(), second)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.cancelledBy").value("ADMIN"))
                    .andExpect(jsonPath("$.resolution.result").value("SESSION_CANCELLED"))
                    .andExpect(jsonPath("$.resolution.adminNote").value("Không diễn ra"))
                    .andExpect(jsonPath("$.resolution.resolvedAt").value("2026-11-20T14:00:00Z")));
            assertThat(object(view, "$")).as("người xem %d", viewer.id()).containsEntry("openReport", null);
            assertThat(object(view, "$.resolution")).containsOnlyKeys("result", "adminNote", "resolvedAt");
        }
    }

    // ================= Phần D mà TC chưa phủ =================

    @Test
    @DisplayName("D87 không vé: 401; admin, tài khoản chưa xác thực: 403 ở filter với cả năm API phiên")
    void d87_filterGuardsSessionApis() throws Exception {
        mockMvc.perform(get("/api/sessions")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/sessions/1/start")).andExpect(status().isUnauthorized());

        for (String token : List.of(data.adminToken(), data.dungToken())) {
            data.listSessions(token, "").andExpect(status().isForbidden());
            data.getSession(token, 1).andExpect(status().isForbidden());
            data.start(token, 1).andExpect(status().isForbidden());
            data.complete(token, 1).andExpect(status().isForbidden());
            data.cancelSession(token, 1, "Bận").andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("D88 lý do hủy dài đúng 1000 ký tự sau khi cắt được nhận")
    void d88_cancelReasonBoundary() throws Exception {
        long p = sessionP();
        String reason = "l".repeat(1000);

        data.cancelSession(an.token(), p, "  " + reason + "  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelReason").value(reason));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT cancel_reason FROM exchange_sessions WHERE id = ?", String.class, p)).isEqualTo(reason);
    }

    @Test
    @DisplayName("D92 openReport đọc từ DB: đúng ba khóa, reportedByMe theo người xem, không lộ lý do; resolution lấy báo cáo resolved_at muộn nhất")
    void d92_openReportAndLatestResolution() throws Exception {
        long adminId = data.adminId();
        long p = sessionP();
        moveClockTo(S);
        data.start(an.token(), p).andExpect(status().isOk());
        data.complete(an.token(), p).andExpect(status().isOk());

        // Hai báo cáo đã chốt: cái chèn trước (id nhỏ hơn) lại chốt muộn hơn, nên phải xếp theo resolved_at.
        String insertResolved = """
                INSERT INTO incident_reports (session_id, reporter_id, reason, status, resolution, admin_note,
                                              resolved_by, created_at, resolved_at)
                VALUES (?, ?, 'Lý do cũ', 'RESOLVED', ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(insertResolved, p, binh.id(), "SESSION_CANCELLED", "Chốt muộn", adminId,
                Timestamp.from(S), Timestamp.from(S.plus(Duration.ofHours(2))));
        jdbcTemplate.update(insertResolved, p, binh.id(), "SESSION_COMPLETED", "Chốt sớm", adminId,
                Timestamp.from(S), Timestamp.from(S.plus(Duration.ofHours(1))));
        // Báo cáo đang mở do An gửi.
        Long openId = jdbcTemplate.queryForObject("""
                INSERT INTO incident_reports (session_id, reporter_id, reason, created_at)
                VALUES (?, ?, 'Lý do bí mật cho admin', ?) RETURNING id
                """, Long.class, p, an.id(), Timestamp.from(S.plus(Duration.ofHours(3))));

        String anView = body(data.getSession(an.token(), p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openReport.id").value(openId))
                .andExpect(jsonPath("$.openReport.reportedByMe").value(true))
                .andExpect(jsonPath("$.openReport.createdAt").value("2026-11-20T15:00:00Z"))
                .andExpect(jsonPath("$.resolution.result").value("SESSION_CANCELLED"))
                .andExpect(jsonPath("$.resolution.adminNote").value("Chốt muộn"))
                .andExpect(jsonPath("$.resolution.resolvedAt").value("2026-11-20T14:00:00Z")));
        assertThat(object(anView, "$.openReport")).containsOnlyKeys("id", "reportedByMe", "createdAt");

        String binhView = body(data.getSession(binh.token(), p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openReport.id").value(openId))
                .andExpect(jsonPath("$.openReport.reportedByMe").value(false)));

        // Lý do báo cáo dành cho admin (A02), người tham gia không thấy.
        for (String view : List.of(anView, binhView)) {
            assertThat(view).doesNotContain("\"reason\"", "Lý do bí mật cho admin", "Lý do cũ");
        }
    }
}
