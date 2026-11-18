package com.skillexchange.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.skillexchange.IntegrationTestBase;
import com.skillexchange.TestAccounts;
import com.skillexchange.user.UserRepository;

/**
 * Bước 4 tuần 6: Entity của yêu cầu, phiên, báo cáo khớp bảng của V1 (D30) và mốc thời gian lấy từ Clock (D88).
 * Bước này chưa có ca TC (kế hoạch tuần 6, mục 3); test riêng của bước.
 *
 * Đọc lại bằng Repository ngoài transaction: mỗi lời gọi mở một persistence context mới, nên dữ liệu đọc ra
 * đi từ DB lên chứ không phải object đang nằm trong bộ nhớ.
 */
class ExchangeMappingTest extends IntegrationTestBase {

    /** 2026-11-20T19:00:00+07:00 (thời điểm S của bộ ca tuần 6). */
    private static final Instant PROPOSED = Instant.parse("2026-11-20T12:00:00Z");

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ExchangeRequestRepository requestRepository;
    @Autowired
    private ExchangeSessionRepository sessionRepository;
    @Autowired
    private IncidentReportRepository reportRepository;

    private long an;
    private long binh;
    private long java;
    private long english;

    @BeforeEach
    void setUp() {
        TestAccounts accounts = new TestAccounts(mockMvc, jdbcTemplate, userRepository, passwordEncoder);
        an = accounts.activeStudent("an@student.example.edu.vn", "An");
        binh = accounts.activeStudent("binh@student.example.edu.vn", "Bình");
        java = accounts.skillId("Java");
        english = accounts.skillId("Tiếng Anh");
    }

    private ExchangeRequest oneWayRequest() {
        return ExchangeRequest.create(an, binh, RequestType.ONE_WAY, java, null, PROPOSED, (short) 90,
                MeetingMode.ONLINE, "https://meet.google.com/abc-defg-hij", "Học Java nhé", clock.instant());
    }

    @Test
    @DisplayName("D30, D88: lưu rồi đọc lại yêu cầu – đủ cột, trạng thái PENDING, version 0, created_at từ Clock")
    void d30_requestRoundTrip() {
        ExchangeRequest request = oneWayRequest();

        ExchangeRequest saved = requestRepository.save(request);

        // save() phải persist chính object này (không merge ra bản sao): xem chú thích ở field version.
        assertThat(saved).isSameAs(request);
        assertThat(request.getId()).isNotNull();

        ExchangeRequest loaded = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(loaded.getSenderId()).isEqualTo(an);
        assertThat(loaded.getReceiverId()).isEqualTo(binh);
        assertThat(loaded.getType()).isEqualTo(RequestType.ONE_WAY);
        assertThat(loaded.getSenderLearnSkillId()).isEqualTo(java);
        assertThat(loaded.getReceiverLearnSkillId()).isNull();
        assertThat(loaded.getProposedStartAt()).isEqualTo(PROPOSED);
        assertThat(loaded.getDurationMinutes()).isEqualTo((short) 90);
        assertThat(loaded.getMode()).isEqualTo(MeetingMode.ONLINE);
        assertThat(loaded.getLocationOrLink()).isEqualTo("https://meet.google.com/abc-defg-hij");
        assertThat(loaded.getMessage()).isEqualTo("Học Java nhé");
        assertThat(loaded.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(loaded.getResponseNote()).isNull();
        assertThat(loaded.getCreatedAt()).isEqualTo(START);
        assertThat(loaded.getRespondedAt()).isNull();
        assertThat(loaded.getVersion()).isZero();

        // Enum lưu bằng tên (EnumType.STRING), không phải số thứ tự: CHECK IN (...) của DB cần đúng chữ.
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT type, mode, status, version FROM exchange_requests WHERE id = ?", request.getId());
        assertThat(row).containsEntry("type", "ONE_WAY").containsEntry("mode", "ONLINE")
                .containsEntry("status", "PENDING").containsEntry("version", 0);
    }

    @Test
    @DisplayName("D30, D90: phiên chép giờ, thời lượng, hình thức, địa điểm từ yêu cầu; trạng thái SCHEDULED")
    void d30_sessionRoundTrip() {
        ExchangeRequest request = requestRepository.save(oneWayRequest());
        clock.advance(Duration.ofHours(1));

        ExchangeSession session = sessionRepository.save(ExchangeSession.scheduleFrom(request, clock.instant()));

        ExchangeSession loaded = sessionRepository.findById(session.getId()).orElseThrow();
        assertThat(loaded.getRequestId()).isEqualTo(request.getId());
        assertThat(loaded.getStartAt()).isEqualTo(PROPOSED);
        assertThat(loaded.getDurationMinutes()).isEqualTo((short) 90);
        assertThat(loaded.getMode()).isEqualTo(MeetingMode.ONLINE);
        assertThat(loaded.getLocationOrLink()).isEqualTo("https://meet.google.com/abc-defg-hij");
        assertThat(loaded.getStatus()).isEqualTo(SessionStatus.SCHEDULED);
        assertThat(loaded.getCreatedAt()).isEqualTo(START.plus(Duration.ofHours(1)));
        assertThat(loaded.getStartedAt()).isNull();
        assertThat(loaded.getFirstCompletedBy()).isNull();
        assertThat(loaded.getCompletedAt()).isNull();
        assertThat(loaded.getCancelledBy()).isNull();
        assertThat(loaded.getCancelReason()).isNull();
        assertThat(loaded.getCancelledAt()).isNull();
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    @DisplayName("D30, D88: lưu rồi đọc lại báo cáo sự cố – trạng thái OPEN, chưa có kết quả xử lý")
    void d30_reportRoundTrip() {
        ExchangeRequest request = requestRepository.save(oneWayRequest());
        ExchangeSession session = sessionRepository.save(ExchangeSession.scheduleFrom(request, clock.instant()));

        IncidentReport report = reportRepository.save(
                IncidentReport.open(session.getId(), an, "Bình không đến", clock.instant()));

        IncidentReport loaded = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(loaded.getSessionId()).isEqualTo(session.getId());
        assertThat(loaded.getReporterId()).isEqualTo(an);
        assertThat(loaded.getReason()).isEqualTo("Bình không đến");
        assertThat(loaded.getStatus()).isEqualTo(ReportStatus.OPEN);
        assertThat(loaded.getResolution()).isNull();
        assertThat(loaded.getAdminNote()).isNull();
        assertThat(loaded.getResolvedBy()).isNull();
        assertThat(loaded.getCreatedAt()).isEqualTo(START);
        assertThat(loaded.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("TK 3.3: CHECK của DB vẫn chặn yêu cầu TWO_WAY thiếu kỹ năng chiều ngược (Entity không tự kiểm)")
    void tk33_twoWayWithoutReceiverSkillIsRejectedByDatabase() {
        ExchangeRequest broken = ExchangeRequest.create(an, binh, RequestType.TWO_WAY, java, null, PROPOSED,
                (short) 90, MeetingMode.ONLINE, "https://meet.google.com/abc-defg-hij", null, clock.instant());

        assertThatThrownBy(() -> requestRepository.save(broken)).isInstanceOf(DataIntegrityViolationException.class);

        ExchangeRequest twoWay = ExchangeRequest.create(an, binh, RequestType.TWO_WAY, java, english, PROPOSED,
                (short) 90, MeetingMode.ONLINE, "https://meet.google.com/abc-defg-hij", null, clock.instant());
        assertThat(requestRepository.save(twoWay).getId()).isNotNull();
    }

    @Test
    @DisplayName("TK 3.3: chỉ mục một phần của DB chặn yêu cầu PENDING thứ hai cùng chiều; chiều ngược vẫn lưu được")
    void tk33_secondPendingRequestSameDirectionIsRejectedByDatabase() {
        requestRepository.save(oneWayRequest());

        assertThatThrownBy(() -> requestRepository.save(oneWayRequest()))
                .isInstanceOf(DataIntegrityViolationException.class);

        ExchangeRequest reverse = ExchangeRequest.create(binh, an, RequestType.ONE_WAY, english, null, PROPOSED,
                (short) 60, MeetingMode.OFFLINE, "Thư viện tầng 2", null, clock.instant());
        assertThat(requestRepository.save(reverse).getId()).isNotNull();
    }
}
