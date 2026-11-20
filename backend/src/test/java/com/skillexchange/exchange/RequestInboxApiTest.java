package com.skillexchange.exchange;

import static com.skillexchange.exchange.ExchangeTestData.MEET;
import static com.skillexchange.exchange.ExchangeTestData.S;
import static com.skillexchange.exchange.ExchangeTestData.T0;
import static com.skillexchange.exchange.ExchangeTestData.T1;
import static com.skillexchange.exchange.ExchangeTestData.expectDetail;
import static com.skillexchange.exchange.ExchangeTestData.expectFieldError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.skillexchange.IntegrationTestBase;
import com.skillexchange.exchange.ExchangeTestData.Student;
import com.skillexchange.user.UserRepository;

/**
 * Hộp yêu cầu và phản hồi (kế hoạch tuần 6, bước 6): TC326–TC337; D87, D88, D90, D91, bảng 2.1–2.3.
 * GET /api/requests?box=&status=, GET /api/requests/{id}, POST /api/requests/{id}/accept, /reject, /cancel.
 * Dữ liệu mẫu theo docs/tuan_6/kiem_thu/ca_kiem_thu.md: An gửi R0 cho Bình; đồng hồ giả đứng ở T0.
 */
class RequestInboxApiTest extends IntegrationTestBase {

    private static final String REQUEST_NOT_FOUND = "Không tìm thấy yêu cầu";
    private static final String NOT_PARTICIPANT = "Bạn không tham gia yêu cầu này";
    private static final String ONLY_RECEIVER = "Chỉ người nhận được chấp nhận hoặc từ chối yêu cầu";
    private static final String ONLY_SENDER = "Chỉ người gửi được hủy yêu cầu";
    private static final String NOT_PENDING = "Yêu cầu không còn ở trạng thái chờ";
    private static final String TOO_LATE = "Đã tới thời điểm đề xuất, không thể chấp nhận yêu cầu";
    private static final String SENDER_INACTIVE = "Người gửi không còn hoạt động";
    private static final String NOTE_TOO_LONG = "Ghi chú dài tối đa 1000 ký tự";
    private static final String STALE = "Dữ liệu vừa được thay đổi, hãy tải lại";
    /** Câu chung của lưới D24 khi DB chặn mà service không kiểm trước. */
    private static final String DB_CONFLICT = "Dữ liệu xung đột với dữ liệu đã có";

    /** 17 khóa của RequestItem (bảng 2.2). */
    private static final String[] REQUEST_ITEM_KEYS = {"id", "type", "status", "myRole", "sender", "receiver",
            "senderLearnSkill", "receiverLearnSkill", "proposedStartAt", "durationMinutes", "mode",
            "locationOrLink", "message", "responseNote", "createdAt", "respondedAt", "sessionId"};

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Lấy kết nối JDBC riêng cho test khóa lạc quan (một giao dịch khác ngoài ứng dụng). */
    @Autowired
    private DataSource dataSource;

    private ExchangeTestData data;

    @BeforeEach
    void setUp() {
        data = new ExchangeTestData(mockMvc, jdbcTemplate, userRepository, passwordEncoder);
        // Chạy sau @BeforeEach của lớp cha (đặt đồng hồ về START), nên ghi đè được.
        clock.setInstant(T0);
    }

    // ---------- Tiện ích đọc response ----------

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static List<Long> ids(String jsonArray) {
        List<Number> raw = JsonPath.read(jsonArray, "$[*].id");
        return raw.stream().map(Number::longValue).toList();
    }

    // ================= Chấp nhận =================

    @Test
    @DisplayName("TC326 Bình chấp nhận R0 lúc T1: 200 {requestId, sessionId}; yêu cầu ACCEPTED, đúng một phiên SCHEDULED chép từ yêu cầu")
    void tc326_acceptCreatesScheduledSession() throws Exception {
        Student an = data.an();
        Student binh = data.binh();
        long requestId = data.create(an, data.r0(binh.id()));
        clock.setInstant(T1);

        String response = body(data.accept(binh.token(), requestId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId)));
        Map<String, Object> result = JsonPath.read(response, "$");
        assertThat(result).containsOnlyKeys("requestId", "sessionId");
        long sessionId = ((Number) result.get("sessionId")).longValue();

        assertThat(data.requestStatus(requestId)).isEqualTo("ACCEPTED");
        assertThat(data.respondedAt(requestId)).isEqualTo(T1);

        List<Map<String, Object>> sessions = jdbcTemplate.queryForList(
                "SELECT * FROM exchange_sessions WHERE request_id = ?", requestId);
        assertThat(sessions).hasSize(1);
        Map<String, Object> session = sessions.get(0);
        assertThat(((Number) session.get("id")).longValue()).isEqualTo(sessionId);
        assertThat(session.get("status")).isEqualTo("SCHEDULED");
        // Giờ, thời lượng, hình thức, địa điểm chép từ yêu cầu (D90).
        assertThat(((Timestamp) session.get("start_at")).toInstant()).isEqualTo(S);
        assertThat(((Number) session.get("duration_minutes")).intValue()).isEqualTo(90);
        assertThat(session.get("mode")).isEqualTo("ONLINE");
        assertThat(session.get("location_or_link")).isEqualTo(MEET);
        assertThat(((Timestamp) session.get("created_at")).toInstant()).isEqualTo(T1);
        assertThat(session)
                .containsEntry("started_at", null)
                .containsEntry("first_completed_by", null)
                .containsEntry("completed_at", null)
                .containsEntry("cancelled_by", null)
                .containsEntry("cancel_reason", null)
                .containsEntry("cancelled_at", null);
    }

    @Test
    @DisplayName("TC327 lệnh tạo phiên vi phạm UNIQUE: 409 lưới D24, yêu cầu vẫn PENDING, version không đổi (một transaction)")
    void tc327_acceptIsOneTransaction() throws Exception {
        Student an = data.an();
        Student binh = data.binh();
        long requestId = data.create(an, data.r0(binh.id()));
        // Dữ liệu không thể có qua API: R0 đã có phiên, nên lệnh tạo phiên của accept vi phạm UNIQUE(request_id).
        jdbcTemplate.update("""
                INSERT INTO exchange_sessions (request_id, start_at, duration_minutes, mode, location_or_link, created_at)
                VALUES (?, ?, 90, 'ONLINE', ?, ?)
                """, requestId, Timestamp.from(S), MEET, Timestamp.from(T0));
        Map<String, Object> before = data.snapshot(requestId);
        clock.setInstant(T1);

        expectDetail(data.accept(binh.token(), requestId), 409, DB_CONFLICT);

        // Đổi trạng thái yêu cầu bị hoàn tác cùng lệnh tạo phiên.
        assertThat(data.snapshot(requestId)).isEqualTo(before);
        assertThat(data.requestStatus(requestId)).isEqualTo("PENDING");
        assertThat(data.respondedAt(requestId)).isNull();
    }

    // ================= Quyền và trạng thái =================

    @Test
    @DisplayName("TC328 R0 PENDING: An accept/reject, Chi accept/reject/cancel/GET, Bình cancel: 403 đúng câu, dữ liệu không đổi")
    void tc328_wrongPeopleAre403() throws Exception {
        Student an = data.an();
        Student binh = data.binh();
        Student chi = data.chi();
        long id = data.create(an, data.r0(binh.id()));
        Map<String, Object> before = data.snapshot(id);

        expectDetail(data.accept(an.token(), id), 403, ONLY_RECEIVER);
        expectDetail(data.reject(an.token(), id), 403, ONLY_RECEIVER);

        expectDetail(data.accept(chi.token(), id), 403, NOT_PARTICIPANT);
        expectDetail(data.reject(chi.token(), id), 403, NOT_PARTICIPANT);
        expectDetail(data.cancel(chi.token(), id), 403, NOT_PARTICIPANT);
        expectDetail(data.getRequest(chi.token(), id), 403, NOT_PARTICIPANT);

        expectDetail(data.cancel(binh.token(), id), 403, ONLY_SENDER);

        assertThat(data.snapshot(id)).isEqualTo(before);
    }

    @Test
    @DisplayName("TC329 yêu cầu ACCEPTED, REJECTED, CANCELLED: đúng vai trò 409, sai vai trò 403 (xét trước trạng thái)")
    void tc329_finishedRequestsCannotChange() throws Exception {
        Student an = data.an();
        Student binh = data.binh();
        long accepted = data.create(an, data.r0(binh.id()));
        data.accept(binh.token(), accepted).andExpect(status().isOk());
        long rejected = data.create(an, data.r0(binh.id()));
        data.reject(binh.token(), rejected).andExpect(status().isOk());
        long cancelled = data.create(an, data.r0(binh.id()));
        data.cancel(an.token(), cancelled).andExpect(status().isOk());

        for (long id : new long[] {accepted, rejected, cancelled}) {
            Map<String, Object> before = data.snapshot(id);

            expectDetail(data.accept(binh.token(), id), 409, NOT_PENDING);
            expectDetail(data.reject(binh.token(), id), 409, NOT_PENDING);
            expectDetail(data.cancel(an.token(), id), 409, NOT_PENDING);
            // Sai vai trò xét trước trạng thái (D87): 403, không phải 409.
            expectDetail(data.accept(an.token(), id), 403, ONLY_RECEIVER);
            expectDetail(data.cancel(binh.token(), id), 403, ONLY_SENDER);

            assertThat(data.snapshot(id)).as("yêu cầu %d", id).isEqualTo(before);
        }
    }

    @Test
    @DisplayName("TC330 chấp nhận được tới S − 1 giây; đúng S thì 409; quá giờ không tự hết hạn, vẫn hủy, từ chối được")
    void tc330_acceptOnlyBeforeProposedStart() throws Exception {
        Student an = data.an();
        Student binh = data.binh();

        // Vé chỉ có hạn 24 giờ theo đồng hồ giả: mỗi lần vặn đồng hồ đi xa thì đăng nhập lại (relogin).

        // (a) Một giây trước giờ đề xuất: vẫn chấp nhận được.
        long a = data.create(an, data.r0(binh.id()));
        clock.setInstant(S.minusSeconds(1));
        binh = data.relogin(binh);
        data.accept(binh.token(), a).andExpect(status().isOk());

        // (b) Đúng giờ đề xuất: 409, không có phiên. Quá giờ một ngày: vẫn PENDING, người gửi vẫn hủy được.
        clock.setInstant(T0);
        an = data.relogin(an);
        long b = data.create(an, data.r0(binh.id()));
        Map<String, Object> before = data.snapshot(b);
        clock.setInstant(S);
        binh = data.relogin(binh);
        expectDetail(data.accept(binh.token(), b), 409, TOO_LATE);
        assertThat(data.snapshot(b)).isEqualTo(before);
        assertThat(data.sessionCount(b)).isZero();

        clock.setInstant(S.plus(Duration.ofDays(1)));
        an = data.relogin(an);
        data.getRequest(an.token(), b)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
        data.cancel(an.token(), b)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // (c) Quá giờ một ngày: người nhận vẫn từ chối được.
        clock.setInstant(T0);
        an = data.relogin(an);
        long c = data.create(an, data.r0(binh.id()));
        clock.setInstant(S.plus(Duration.ofDays(1)));
        binh = data.relogin(binh);
        data.reject(binh.token(), c)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("TC331 người gửi bị khóa: accept 409, yêu cầu vẫn PENDING, không có phiên; mở khóa rồi accept: 200")
    void tc331_senderMustBeActive() throws Exception {
        Student an = data.an();
        Student binh = data.binh();
        long id = data.create(an, data.r0(binh.id()));
        Map<String, Object> before = data.snapshot(id);

        jdbcTemplate.update("UPDATE users SET status = 'LOCKED' WHERE id = ?", an.id());
        expectDetail(data.accept(binh.token(), id), 409, SENDER_INACTIVE);
        assertThat(data.snapshot(id)).isEqualTo(before);

        jdbcTemplate.update("UPDATE users SET status = 'ACTIVE' WHERE id = ?", an.id());
        data.accept(binh.token(), id).andExpect(status().isOk());
        assertThat(data.sessionCount(id)).isEqualTo(1);
    }

    // ================= Từ chối, hủy =================

    @Test
    @DisplayName("TC332 từ chối có ghi chú (đã cắt), không body (null), ghi chú 1001 ký tự (400, vẫn PENDING)")
    void tc332_rejectWithOptionalNote() throws Exception {
        Student an = data.an();
        Student binh = data.binh();
        // Mỗi chiều chỉ một PENDING (D89 bước 8): tạo nối tiếp, lúc bị từ chối yêu cầu nào cũng đang PENDING.

        long first = data.create(an, data.r0(binh.id()));
        clock.setInstant(T1);
        String response = body(data.reject(binh.token(), first, "  Bận tuần này  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(first))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.myRole").value("RECEIVER"))
                .andExpect(jsonPath("$.responseNote").value("Bận tuần này"))
                .andExpect(jsonPath("$.respondedAt").value("2026-11-18T09:00:00Z")));
        Map<String, Object> item = JsonPath.read(response, "$");
        assertThat(item).containsOnlyKeys(REQUEST_ITEM_KEYS).containsEntry("sessionId", null);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT response_note FROM exchange_requests WHERE id = ?", String.class, first))
                .isEqualTo("Bận tuần này");
        assertThat(data.respondedAt(first)).isEqualTo(T1);

        long second = data.create(an, data.r0(binh.id()));
        String withoutBody = body(data.reject(binh.token(), second)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED")));
        Map<String, Object> secondItem = JsonPath.read(withoutBody, "$");
        assertThat(secondItem).containsEntry("responseNote", null);

        long third = data.create(an, data.r0(binh.id()));
        Map<String, Object> before = data.snapshot(third);
        expectFieldError(data.reject(binh.token(), third, "n".repeat(1001)), "note", NOTE_TOO_LONG);
        assertThat(data.snapshot(third)).isEqualTo(before);
        assertThat(data.requestStatus(third)).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("TC333 An hủy R0: 200 RequestItem CANCELLED, respondedAt = đồng hồ, sessionId null")
    void tc333_senderCancels() throws Exception {
        Student an = data.an();
        Student binh = data.binh();
        long id = data.create(an, data.r0(binh.id()));
        clock.setInstant(T1);

        String response = body(data.cancel(an.token(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.myRole").value("SENDER"))
                .andExpect(jsonPath("$.respondedAt").value("2026-11-18T09:00:00Z")));
        Map<String, Object> item = JsonPath.read(response, "$");
        assertThat(item).containsOnlyKeys(REQUEST_ITEM_KEYS).containsEntry("sessionId", null);

        assertThat(data.requestStatus(id)).isEqualTo("CANCELLED");
        assertThat(data.respondedAt(id)).isEqualTo(T1);
        assertThat(data.sessionCount(id)).isZero();
    }

    // ================= Khóa lạc quan =================

    @Test
    @DisplayName("TC334 20 lần Bình chấp nhận và An hủy cùng lúc: mỗi lần một 200 và một 409, không 500; chỉ ACCEPTED có phiên")
    void tc334_acceptAndCancelAtTheSameTime() throws Exception {
        Student an = data.an();
        Student binh = data.binh();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 1; round <= 20; round++) {
                long id = data.create(an, data.r0(binh.id()));
                CountDownLatch start = new CountDownLatch(1);
                Future<Integer> accept = pool.submit(() -> {
                    start.await();
                    return data.accept(binh.token(), id).andReturn().getResponse().getStatus();
                });
                Future<Integer> cancel = pool.submit(() -> {
                    start.await();
                    return data.cancel(an.token(), id).andReturn().getResponse().getStatus();
                });
                start.countDown(); // thả hai luồng cùng lúc

                List<Integer> statuses = Stream.of(accept.get(10, TimeUnit.SECONDS), cancel.get(10, TimeUnit.SECONDS))
                        .sorted()
                        .toList();
                assertThat(statuses).as("lần %d", round).containsExactly(200, 409);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM exchange_requests", Integer.class))
                .isEqualTo(20);
        // Mọi yêu cầu ACCEPTED có đúng một phiên; không yêu cầu nào khác có phiên (D90).
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM exchange_requests r
                WHERE r.status = 'ACCEPTED'
                  AND (SELECT count(*) FROM exchange_sessions s WHERE s.request_id = r.id) <> 1
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM exchange_sessions s JOIN exchange_requests r ON r.id = s.request_id
                WHERE r.status <> 'ACCEPTED'
                """, Integer.class)).isZero();
    }

    @Test
    @DisplayName("D90 khóa lạc quan, tranh chấp chắc chắn: yêu cầu bị hủy trong lúc accept đang ghi → 409, không có phiên")
    void d90_staleAcceptIsRejectedByVersion() throws Exception {
        Student an = data.an();
        Student binh = data.binh();
        long id = data.create(an, data.r0(binh.id()));

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try (Connection other = dataSource.getConnection()) {
            // Một giao dịch khác hủy yêu cầu và tăng version nhưng CHƯA commit: PostgreSQL khóa dòng này lại.
            other.setAutoCommit(false);
            try (PreparedStatement cancel = other.prepareStatement("""
                    UPDATE exchange_requests SET status = 'CANCELLED', responded_at = ?, version = version + 1
                    WHERE id = ?
                    """)) {
                cancel.setTimestamp(1, Timestamp.from(T0));
                cancel.setLong(2, id);
                cancel.executeUpdate();
            }

            // accept đọc bản đã commit (PENDING, version 0) nên qua mọi kiểm tra, rồi đứng chờ khi ghi vào dòng bị khóa.
            Future<MvcResult> accept = pool.submit(() -> data.accept(binh.token(), id).andReturn());
            waitUntilSomeoneWaitsForRowLock(accept);
            other.commit();

            // Lúc được ghi, dòng đã ở version 1: câu UPDATE ... WHERE version = 0 không khớp dòng nào.
            MvcResult result = accept.get(10, TimeUnit.SECONDS);
            status().isConflict().match(result);
            jsonPath("$.detail").value(STALE).match(result);
        } finally {
            pool.shutdownNow();
        }

        assertThat(data.requestStatus(id)).isEqualTo("CANCELLED");
        assertThat(data.sessionCount(id)).isZero();
    }

    /**
     * Đợi tới khi có một kết nối của DB test đang chờ khóa (lệnh accept đã đọc xong và đang ghi),
     * hoặc lệnh accept đã kết thúc mà không phải chờ (code sai: để phần kiểm kết quả báo đỏ). Tối đa 10 giây.
     */
    private void waitUntilSomeoneWaitsForRowLock(Future<?> running) throws InterruptedException {
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

    // ================= {id} sai =================

    @Test
    @DisplayName("TC335 {id} 99999: 404; {id} \"abc\": 400; reject 99999 với ghi chú 1001 ký tự: 400 (400 trước 404)")
    void tc335_unknownOrMalformedId() throws Exception {
        String token = data.an().token();

        expectDetail(data.getRequest(token, 99999), 404, REQUEST_NOT_FOUND);
        expectDetail(data.accept(token, 99999), 404, REQUEST_NOT_FOUND);
        expectDetail(data.reject(token, 99999), 404, REQUEST_NOT_FOUND);
        expectDetail(data.cancel(token, 99999), 404, REQUEST_NOT_FOUND);

        String malformed = "Tham số id có giá trị không hợp lệ: abc"; // D38
        expectDetail(data.getRequest(token, "abc"), 400, malformed);
        expectDetail(data.accept(token, "abc"), 400, malformed);
        expectDetail(data.reject(token, "abc"), 400, malformed);
        expectDetail(data.cancel(token, "abc"), 400, malformed);

        expectFieldError(data.reject(token, 99999, "n".repeat(1001)), "note", NOTE_TOO_LONG);
    }

    // ================= Danh sách, quyền xem địa điểm =================

    @Test
    @DisplayName("TC336 box=sent xếp created_at rồi id giảm dần; box=received; lọc status; thiếu box, box=FOO, status=FOO: 400")
    void tc336_listByBoxAndStatus() throws Exception {
        Student an = data.an();
        Student binh = data.binh();
        Student chi = data.chi();
        Student giang = data.giang();

        long toBinh = data.create(an, data.r0(binh.id()));
        long toGiang = data.create(an, data.r0(giang.id()));
        clock.setInstant(T0.plus(Duration.ofMinutes(1)));
        long toChi = data.create(an, data.r0(chi.id()));
        long fromBinh = data.create(binh, data.oneWay(an.id(), "Tiếng Anh"));
        data.cancel(an.token(), toChi).andExpect(status().isOk());

        // Giang và Bình cùng created_at (T0) nên xếp theo id giảm dần.
        String sent = body(data.listRequests(an.token(), "?box=sent").andExpect(status().isOk()));
        assertThat(ids(sent)).containsExactly(toChi, toGiang, toBinh);
        List<String> sentRoles = JsonPath.read(sent, "$[*].myRole");
        assertThat(sentRoles).containsOnly("SENDER");

        String received = body(data.listRequests(an.token(), "?box=received").andExpect(status().isOk()));
        assertThat(ids(received)).containsExactly(fromBinh);
        List<String> receivedRoles = JsonPath.read(received, "$[*].myRole");
        assertThat(receivedRoles).containsExactly("RECEIVER");

        String pending = body(data.listRequests(an.token(), "?box=sent&status=PENDING").andExpect(status().isOk()));
        assertThat(ids(pending)).containsExactly(toGiang, toBinh);

        expectDetail(data.listRequests(an.token(), ""), 400, "Thiếu tham số bắt buộc: box");
        expectDetail(data.listRequests(an.token(), "?box=FOO"), 400, "Tham số box có giá trị không hợp lệ: FOO");
        expectDetail(data.listRequests(an.token(), "?box=sent&status=FOO"), 400,
                "Tham số status có giá trị không hợp lệ: FOO");
    }

    @Test
    @DisplayName("TC337 địa điểm/link: người gửi luôn thấy; người nhận chỉ thấy khi ACCEPTED; không response nào có liên hệ")
    void tc337_locationVisibility() throws Exception {
        Student an = data.an();
        Student binh = data.binh();
        List<String> responses = new ArrayList<>();

        // (a) PENDING: An thấy, Bình nhận null ở cả chi tiết lẫn danh sách.
        long id = data.create(an, data.r0(binh.id()));
        responses.add(body(data.getRequest(an.token(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationOrLink").value(MEET))));
        responses.add(body(data.listRequests(an.token(), "?box=sent")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].locationOrLink").value(MEET))));

        String binhDetail = body(data.getRequest(binh.token(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("RECEIVER")));
        Map<String, Object> binhItem = JsonPath.read(binhDetail, "$");
        assertThat(binhItem).containsEntry("locationOrLink", null).containsEntry("sessionId", null);
        String binhList = body(data.listRequests(binh.token(), "?box=received").andExpect(status().isOk()));
        Map<String, Object> binhListItem = JsonPath.read(binhList, "$[0]");
        assertThat(binhListItem).containsEntry("locationOrLink", null).containsEntry("sessionId", null);
        responses.add(binhDetail);
        responses.add(binhList);

        // (b) Bình chấp nhận: thấy địa điểm/link và sessionId.
        String accepted = body(data.accept(binh.token(), id).andExpect(status().isOk()));
        long sessionId = ((Number) JsonPath.read(accepted, "$.sessionId")).longValue();
        responses.add(body(data.getRequest(binh.token(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.locationOrLink").value(MEET))
                .andExpect(jsonPath("$.sessionId").value(sessionId))));
        responses.add(body(data.listRequests(binh.token(), "?box=received")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].locationOrLink").value(MEET))
                .andExpect(jsonPath("$[0].sessionId").value(sessionId))));

        // (c) REJECTED, CANCELLED: người nhận lại nhận null.
        long rejected = data.create(an, data.r0(binh.id()));
        responses.add(body(data.reject(binh.token(), rejected).andExpect(status().isOk())));
        long cancelled = data.create(an, data.r0(binh.id()));
        responses.add(body(data.cancel(an.token(), cancelled).andExpect(status().isOk())));
        for (long finished : new long[] {rejected, cancelled}) {
            String view = body(data.getRequest(binh.token(), finished).andExpect(status().isOk()));
            Map<String, Object> item = JsonPath.read(view, "$");
            assertThat(item).as("yêu cầu %d", finished).containsEntry("locationOrLink", null);
            responses.add(view);
        }

        // API yêu cầu không bao giờ trả liên hệ (D91, NFR-02).
        for (String response : responses) {
            assertThat(response).doesNotContain("\"phone\"", "\"contactInfo\"", "\"email\"",
                    "0901000001", "Zalo: An", "Facebook: Bình", "@student");
        }
    }

    // ================= Phần D mà TC chưa phủ =================

    @Test
    @DisplayName("D87 không vé: 401; admin, tài khoản chưa xác thực: 403 ở filter với mọi API hộp yêu cầu")
    void d87_filterGuardsInboxApis() throws Exception {
        mockMvc.perform(get("/api/requests?box=sent")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/requests/1/accept")).andExpect(status().isUnauthorized());

        for (String token : List.of(data.adminToken(), data.dungToken())) {
            data.listRequests(token, "?box=sent").andExpect(status().isForbidden());
            data.getRequest(token, 1).andExpect(status().isForbidden());
            data.accept(token, 1).andExpect(status().isForbidden());
            data.reject(token, 1).andExpect(status().isForbidden());
            data.cancel(token, 1).andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("D88 ghi chú từ chối dài đúng 1000 ký tự được nhận; toàn khoảng trắng lưu NULL")
    void d88_rejectNoteBoundaries() throws Exception {
        Student an = data.an();
        Student binh = data.binh();

        long first = data.create(an, data.r0(binh.id()));
        data.reject(binh.token(), first, "n".repeat(1000))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseNote").value("n".repeat(1000)));

        long second = data.create(an, data.r0(binh.id()));
        String response = body(data.reject(binh.token(), second, "   ").andExpect(status().isOk()));
        Map<String, Object> item = JsonPath.read(response, "$");
        assertThat(item).containsEntry("responseNote", null);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT response_note FROM exchange_requests WHERE id = ?", String.class, second)).isNull();
    }

    @Test
    @DisplayName("D90 lỗi 409 của accept xét theo thứ tự: không còn PENDING → đã tới giờ đề xuất → người gửi không ACTIVE")
    void d90_acceptConflictOrder() throws Exception {
        Student an = data.an();
        Student binh = data.binh();

        // Đã hủy và đã tới giờ: câu "không còn ở trạng thái chờ".
        long cancelled = data.create(an, data.r0(binh.id()));
        data.cancel(an.token(), cancelled).andExpect(status().isOk());
        clock.setInstant(S);
        binh = data.relogin(binh); // vé cấp lúc T0 đã quá 24 giờ
        expectDetail(data.accept(binh.token(), cancelled), 409, NOT_PENDING);

        // Đã tới giờ và người gửi bị khóa: câu "đã tới thời điểm".
        clock.setInstant(T0);
        long pending = data.create(an, data.r0(binh.id()));
        jdbcTemplate.update("UPDATE users SET status = 'LOCKED' WHERE id = ?", an.id());
        clock.setInstant(S);
        expectDetail(data.accept(binh.token(), pending), 409, TOO_LATE);
    }
}
