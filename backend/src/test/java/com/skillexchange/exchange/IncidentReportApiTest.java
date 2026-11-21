package com.skillexchange.exchange;

import static com.skillexchange.exchange.ExchangeTestData.S;
import static com.skillexchange.exchange.ExchangeTestData.T0;
import static com.skillexchange.exchange.ExchangeTestData.T1;
import static com.skillexchange.exchange.ExchangeTestData.body;
import static com.skillexchange.exchange.ExchangeTestData.expectDetail;
import static com.skillexchange.exchange.ExchangeTestData.expectFieldError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.skillexchange.IntegrationTestBase;
import com.skillexchange.exchange.ExchangeTestData.Student;
import com.skillexchange.user.UserRepository;

/**
 * Gửi báo cáo sự cố (kế hoạch tuần 6, bước 8): TC351–TC354, phần reports của TC348; D87, D88, D93, D94, bảng 2.1–2.3.
 * POST /api/sessions/{id}/reports {reason}, và complete bị chặn khi phiên có báo cáo OPEN (R33).
 *
 * Dữ liệu mẫu theo docs/tuan_6/kiem_thu/ca_kiem_thu.md: An và Bình có sẵn trước mỗi test, đồng hồ giả ở T0.
 * Phiên P = An gửi R0 lúc T0, Bình chấp nhận lúc T1; start_at = S.
 */
class IncidentReportApiTest extends IntegrationTestBase {

    // Câu detail của bảng 2.1 (Quyet_dinh_tuan_6.md).
    private static final String SESSION_NOT_FOUND = "Không tìm thấy phiên";
    private static final String NOT_PARTICIPANT = "Bạn không tham gia phiên này";
    private static final String ONLY_WAITING = "Chỉ gửi được báo cáo khi phiên đang chờ xác nhận hoàn thành";
    private static final String ALREADY_OPEN = "Phiên đã có báo cáo sự cố đang chờ xử lý";
    private static final String BLOCKED_BY_REPORT = "Phiên đang có báo cáo sự cố chờ quản trị viên xử lý";
    private static final String ALREADY_COMPLETED = "Bạn đã xác nhận hoàn thành, đang chờ bên kia";
    private static final String CANNOT_CANCEL_WAITING =
            "Phiên đang chờ xác nhận, không hủy được; hãy gửi báo cáo sự cố nếu có bất đồng";
    private static final String REASON_MISSING = "Chưa nhập lý do";
    private static final String REASON_TOO_LONG = "Lý do dài tối đa 1000 ký tự";
    private static final String STALE = "Dữ liệu vừa được thay đổi, hãy tải lại";
    /** Câu chung của lưới D24 khi DB báo vi phạm ràng buộc (ở đây: chỉ mục uq_reports_one_open). */
    private static final String DUPLICATE = "Dữ liệu xung đột với dữ liệu đã có";
    private static final String UNREADABLE = "Nội dung JSON không đọc được";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Lấy kết nối JDBC riêng cho test tranh chấp (một giao dịch khác ngoài ứng dụng). */
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
     * nên đi từ T0 tới giờ hẹn S (hơn 2 ngày) thì vé cũ thành 401.
     */
    private void moveClockTo(Instant instant) throws Exception {
        clock.setInstant(instant);
        an = data.relogin(an);
        binh = data.relogin(binh);
    }

    /**
     * count phiên An–Bình ở WAITING_CONFIRMATION, An bấm hoàn thành trước. Tạo và chấp nhận ở đồng hồ hiện tại (giờ
     * hẹn S phải còn ở tương lai), rồi vặn đồng hồ tới S để bắt đầu. Đồng hồ để lại ở S.
     */
    private List<Long> waitingSessions(int count) throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(data.session(an, binh));
        }
        moveClockTo(S);
        for (long id : ids) {
            data.start(binh.token(), id).andExpect(status().isOk());
            data.complete(an.token(), id).andExpect(status().isOk());
        }
        return ids;
    }

    /** Người gửi của các báo cáo sự cố của một phiên, theo thứ tự id. */
    private List<Long> reporters(long sessionId) {
        return jdbcTemplate.queryForList(
                "SELECT reporter_id FROM incident_reports WHERE session_id = ? ORDER BY id", Long.class, sessionId);
    }

    /** Đọc object JSON của response thành Map để kiểm đủ khóa. */
    private static Map<String, Object> object(String json, String path) {
        return JsonPath.read(json, path);
    }

    // ================= Gửi báo cáo thành công =================

    @Test
    @DisplayName("TC351 An (người bấm trước) báo cáo: 201 {id, sessionId, OPEN, createdAt}, lý do đã cắt; openReport theo người xem, không lộ lý do; Bình báo cáo phiên khác: 201")
    void tc351_participantsCanReportWaitingSession() throws Exception {
        List<Long> waiting = waitingSessions(2);
        long first = waiting.get(0);
        long second = waiting.get(1);
        clock.setInstant(S.plus(Duration.ofMinutes(100)));

        // (a) An, người đã bấm hoàn thành, vẫn gửi được (D94).
        String response = body(data.report(an.token(), first, "  Bình không đến  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(first))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-20T13:40:00Z")));
        assertThat(object(response, "$")).containsOnlyKeys("id", "sessionId", "status", "createdAt");
        long reportId = ((Number) JsonPath.read(response, "$.id")).longValue();

        // DB: lý do đã cắt khoảng trắng (D88), người gửi là An, giờ lấy từ đồng hồ giả.
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT session_id, reporter_id, reason, status FROM incident_reports WHERE id = ?", reportId);
        assertThat(row).containsEntry("session_id", first)
                .containsEntry("reporter_id", an.id())
                .containsEntry("reason", "Bình không đến")
                .containsEntry("status", "OPEN");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT created_at FROM incident_reports WHERE id = ?", Timestamp.class, reportId).toInstant())
                .isEqualTo(S.plus(Duration.ofMinutes(100)));

        // Chi tiết phiên: openReport đúng ba khóa, reportedByMe theo người xem; phiên vẫn chờ xác nhận.
        String anView = body(data.getSession(an.token(), first)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_CONFIRMATION"))
                .andExpect(jsonPath("$.openReport.id").value(reportId))
                .andExpect(jsonPath("$.openReport.reportedByMe").value(true))
                .andExpect(jsonPath("$.openReport.createdAt").value("2026-11-20T13:40:00Z")));
        assertThat(object(anView, "$.openReport")).containsOnlyKeys("id", "reportedByMe", "createdAt");

        String binhView = body(data.getSession(binh.token(), first)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openReport.id").value(reportId))
                .andExpect(jsonPath("$.openReport.reportedByMe").value(false)));

        // Lý do dành cho admin (A02), người tham gia không thấy, kể cả người gửi.
        for (String view : List.of(anView, binhView)) {
            assertThat(view).doesNotContain("\"reason\"", "Bình không đến");
        }

        // (b) Bình, người chưa bấm hoàn thành, gửi cho phiên khác.
        data.report(binh.token(), second, "An chưa dạy xong")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(second))
                .andExpect(jsonPath("$.status").value("OPEN"));
        assertThat(reporters(second)).containsExactly(binh.id());
    }

    // ================= Trạng thái phiên không cho báo cáo =================

    @Test
    @DisplayName("TC352 phiên SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED: báo cáo 409, không có báo cáo mới, dữ liệu không đổi")
    void tc352_onlyWaitingSessionsCanBeReported() throws Exception {
        long scheduled = data.session(an, binh);
        long inProgress = data.session(an, binh);
        long completed = data.session(an, binh);
        long cancelled = data.session(an, binh);
        data.cancelSession(an.token(), cancelled, "Bận").andExpect(status().isOk());

        moveClockTo(S);
        data.start(binh.token(), inProgress).andExpect(status().isOk());
        data.start(binh.token(), completed).andExpect(status().isOk());
        data.complete(an.token(), completed).andExpect(status().isOk());
        data.complete(binh.token(), completed).andExpect(status().isOk());

        for (long id : new long[] {scheduled, inProgress, completed, cancelled}) {
            Map<String, Object> before = data.sessionSnapshot(id);
            expectDetail(data.report(an.token(), id, "Có sự cố"), 409, ONLY_WAITING);
            assertThat(data.sessionSnapshot(id)).as("phiên %d", id).isEqualTo(before);
        }
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM incident_reports", Integer.class)).isZero();
    }

    // ================= Một báo cáo mở mỗi phiên; lý do bắt buộc =================

    @Test
    @DisplayName("TC353 đã có báo cáo OPEN của An: An gửi lại, Bình gửi: 409, vẫn một báo cáo; lý do {}, trắng, 1001 ký tự: 400 errors reason")
    void tc353_oneOpenReportPerSessionAndReasonRequired() throws Exception {
        List<Long> waiting = waitingSessions(2);
        long reported = waiting.get(0);
        long fresh = waiting.get(1);

        data.report(an.token(), reported, "Bình không đến").andExpect(status().isCreated());
        Map<String, Object> before = data.sessionSnapshot(reported);
        expectDetail(data.report(an.token(), reported, "Gửi lại cho chắc"), 409, ALREADY_OPEN);
        expectDetail(data.report(binh.token(), reported, "An nói sai"), 409, ALREADY_OPEN);
        assertThat(data.sessionSnapshot(reported)).isEqualTo(before);
        assertThat(reporters(reported)).containsExactly(an.id());

        // Lý do sai: 400 errors reason, không có báo cáo nào.
        String path = "/api/sessions/" + fresh + "/reports";
        Map<String, Object> freshBefore = data.sessionSnapshot(fresh);
        expectFieldError(data.postJson(an.token(), path, "{}"), "reason", REASON_MISSING);
        expectFieldError(data.report(an.token(), fresh, "  "), "reason", REASON_MISSING);
        expectFieldError(data.report(an.token(), fresh, "l".repeat(1001)), "reason", REASON_TOO_LONG);
        assertThat(data.sessionSnapshot(fresh)).isEqualTo(freshBefore);
        assertThat(reporters(fresh)).isEmpty();
    }

    // ================= Báo cáo mở chặn xác nhận hoàn thành (R33) =================

    @Test
    @DisplayName("TC354 có báo cáo OPEN: Bình xác nhận hoàn thành 409, An và Bình hủy 409; phiên vẫn WAITING_CONFIRMATION, dữ liệu không đổi")
    void tc354_openReportBlocksConfirmation() throws Exception {
        long p = waitingSessions(1).get(0);
        data.report(an.token(), p, "Bình không đến").andExpect(status().isCreated());
        Map<String, Object> before = data.sessionSnapshot(p);

        expectDetail(data.complete(binh.token(), p), 409, BLOCKED_BY_REPORT);
        expectDetail(data.cancelSession(an.token(), p, "Hủy cho xong"), 409, CANNOT_CANCEL_WAITING);
        expectDetail(data.cancelSession(binh.token(), p, "Hủy cho xong"), 409, CANNOT_CANCEL_WAITING);

        assertThat(data.sessionSnapshot(p)).isEqualTo(before);
        assertThat(data.sessionStatus(p)).isEqualTo("WAITING_CONFIRMATION");
    }

    @Test
    @DisplayName("D93 có báo cáo OPEN: An (người bấm trước) hoàn thành lần nữa ra câu 'Bạn đã xác nhận hoàn thành...' (xét trước báo cáo OPEN)")
    void d93_firstCompleterIsCheckedBeforeOpenReport() throws Exception {
        long p = waitingSessions(1).get(0);
        data.report(binh.token(), p, "An chưa dạy xong").andExpect(status().isCreated());
        Map<String, Object> before = data.sessionSnapshot(p);

        expectDetail(data.complete(an.token(), p), 409, ALREADY_COMPLETED);
        assertThat(data.sessionSnapshot(p)).isEqualTo(before);
    }

    // ================= Người ngoài, {id} sai =================

    @Test
    @DisplayName("TC348 (phần bước 8) Chi gửi báo cáo cho phiên P: 403 Bạn không tham gia phiên này, không có báo cáo")
    void tc348_outsiderCannotReport() throws Exception {
        Student chi = data.chi();
        long p = sessionP();
        Map<String, Object> before = data.sessionSnapshot(p);

        // P đang SCHEDULED: người tham gia sẽ nhận 409, Chi nhận 403 vì 403 đứng trước 409 (D87).
        expectDetail(data.report(chi.token(), p, "Phá đám"), 403, NOT_PARTICIPANT);
        assertThat(data.sessionSnapshot(p)).isEqualTo(before);
    }

    @Test
    @DisplayName("D87 báo cáo: không vé 401; admin, tài khoản chưa xác thực 403 ở filter; {id} 99999: 404; abc: 400; 99999 lý do rỗng: 400 trước 404")
    void d87_reportGuardsAndCheckOrder() throws Exception {
        mockMvc.perform(post("/api/sessions/1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Bận\"}"))
                .andExpect(status().isUnauthorized());
        for (String token : List.of(data.adminToken(), data.dungToken())) {
            data.report(token, 1, "Bận").andExpect(status().isForbidden());
        }

        expectDetail(data.report(an.token(), 99999, "Bận"), 404, SESSION_NOT_FOUND);
        expectDetail(data.report(an.token(), "abc", "Bận"), 400, "Tham số id có giá trị không hợp lệ: abc"); // D38
        expectFieldError(data.report(an.token(), 99999, ""), "reason", REASON_MISSING);
    }

    @Test
    @DisplayName("D88 lý do báo cáo dài đúng 1000 ký tự sau khi cắt được nhận; không có body: 400 Nội dung JSON không đọc được (⚠2)")
    void d88_reportReasonBoundary() throws Exception {
        long p = waitingSessions(1).get(0);

        expectDetail(data.postWithoutBody(an.token(), "/api/sessions/" + p + "/reports"), 400, UNREADABLE);
        assertThat(reporters(p)).isEmpty();

        String reason = "l".repeat(1000);
        data.report(an.token(), p, "  " + reason + "  ").andExpect(status().isCreated());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT reason FROM incident_reports WHERE session_id = ?", String.class, p)).isEqualTo(reason);
    }

    // ================= Tranh chấp (D94) =================

    @Test
    @DisplayName("D94 báo cáo đúng lúc Bình xác nhận hoàn thành, tranh chấp chắc chắn: An 409 khóa lạc quan, phiên COMPLETED, không có báo cáo treo")
    void d94_reportRacingConfirmationIsRejectedByVersion() throws Exception {
        long p = waitingSessions(1).get(0);
        Instant confirmedAt = S.plus(Duration.ofMinutes(90));
        clock.setInstant(confirmedAt);

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try (Connection other = dataSource.getConnection()) {
            // Một giao dịch khác làm thay lệnh "xác nhận hoàn thành" của Bình nhưng CHƯA commit: PostgreSQL khóa dòng.
            other.setAutoCommit(false);
            try (PreparedStatement binhConfirms = other.prepareStatement("""
                    UPDATE exchange_sessions
                    SET status = 'COMPLETED', completed_at = ?, version = version + 1
                    WHERE id = ?
                    """)) {
                binhConfirms.setTimestamp(1, Timestamp.from(confirmedAt));
                binhConfirms.setLong(2, p);
                binhConfirms.executeUpdate();
            }

            // An đọc bản đã commit (WAITING_CONFIRMATION, chưa có báo cáo) nên qua mọi kiểm tra, rồi phải đứng chờ
            // khi ép tăng version của dòng phiên đang bị khóa.
            Future<MvcResult> anReports = pool.submit(() -> data.report(an.token(), p, "Bình không đến").andReturn());
            data.waitUntilSomeoneWaitsForRowLock(anReports);
            other.commit();

            // Lúc được ghi, version đã tăng: lệnh của An thua khóa lạc quan, báo cáo vừa chèn bị rollback.
            MvcResult result = anReports.get(10, TimeUnit.SECONDS);
            status().isConflict().match(result);
            jsonPath("$.detail").value(STALE).match(result);
        } finally {
            pool.shutdownNow();
        }

        // Không bao giờ có phiên COMPLETED mà còn báo cáo OPEN (kịch bản treo của R33).
        assertThat(data.sessionStatus(p)).isEqualTo("COMPLETED");
        assertThat(reporters(p)).isEmpty();
    }

    @Test
    @DisplayName("D94 hai báo cáo chạy chồng, tranh chấp chắc chắn: báo cáo của An đụng chỉ mục uq_reports_one_open → 409 lưới D24, vẫn một báo cáo")
    void d94_concurrentReportHitsUniqueIndex() throws Exception {
        long p = waitingSessions(1).get(0);

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try (Connection other = dataSource.getConnection()) {
            // Một giao dịch khác chèn báo cáo OPEN của Bình nhưng CHƯA commit. Chỉ chèn, không tăng version phiên:
            // lệnh báo cáo thật ép tăng version lúc commit, nên trước khi commit nó cũng chỉ mới chèn như thế này.
            other.setAutoCommit(false);
            try (PreparedStatement binhReports = other.prepareStatement("""
                    INSERT INTO incident_reports (session_id, reporter_id, reason, created_at)
                    VALUES (?, ?, 'An chưa dạy xong', ?)
                    """)) {
                binhReports.setLong(1, p);
                binhReports.setLong(2, binh.id());
                binhReports.setTimestamp(3, Timestamp.from(clock.instant()));
                binhReports.executeUpdate();
            }

            // An không thấy dòng chưa commit nên qua bước kiểm "đã có báo cáo OPEN", rồi đứng chờ ở chỉ mục unique.
            Future<MvcResult> anReports = pool.submit(() -> data.report(an.token(), p, "Bình không đến").andReturn());
            data.waitUntilSomeoneWaitsForRowLock(anReports);
            other.commit();

            // Báo cáo của Bình đã commit: lệnh chèn của An vi phạm uq_reports_one_open, lưới D24 trả 409.
            MvcResult result = anReports.get(10, TimeUnit.SECONDS);
            status().isConflict().match(result);
            jsonPath("$.detail").value(DUPLICATE).match(result);
        } finally {
            pool.shutdownNow();
        }

        assertThat(reporters(p)).containsExactly(binh.id());
    }
}
