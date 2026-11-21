package com.skillexchange.exchange;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillexchange.common.ConflictException;
import com.skillexchange.common.ForbiddenException;
import com.skillexchange.common.NotFoundException;

/**
 * Phiên trao đổi (FR-06, TK 4.7, 5.2). Bước 7: danh sách, chi tiết (D92), bắt đầu, hoàn thành, hủy (D93).
 * Bước 8: gửi báo cáo sự cố (D94, FR-09 phần người dùng); báo cáo OPEN chặn xác nhận hoàn thành (R33).
 *
 * Quyền Active của người gọi đã kiểm ở tầng filter (D44, D87). Các API theo {id} kiểm theo thứ tự D87:
 * 404 → 403 không tham gia → 409; lỗi 400 ({id} không phải số, body sai) Spring đã trả trước khi tới đây.
 * Không xét trạng thái tài khoản của bên kia (R37): bên kia bị khóa thì mình vẫn bắt đầu, hoàn thành, hủy được.
 */
@Service
public class ExchangeSessionService {

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
    private static final String BLOCKED_BY_REPORT = "Phiên đang có báo cáo sự cố chờ quản trị viên xử lý";
    private static final String ONLY_WAITING = "Chỉ gửi được báo cáo khi phiên đang chờ xác nhận hoàn thành";
    private static final String ALREADY_OPEN = "Phiên đã có báo cáo sự cố đang chờ xử lý";

    /** Được bắt đầu sớm nhất 15 phút trước giờ hẹn; không có cận trên (D93, R34). */
    private static final Duration EARLIEST_START_BEFORE = Duration.ofMinutes(15);

    private final ExchangeSessionRepository sessionRepository;
    private final ExchangeRequestRepository requestRepository;
    private final IncidentReportRepository reportRepository;
    private final Clock clock;

    public ExchangeSessionService(ExchangeSessionRepository sessionRepository,
                                  ExchangeRequestRepository requestRepository,
                                  IncidentReportRepository reportRepository,
                                  Clock clock) {
        this.sessionRepository = sessionRepository;
        this.requestRepository = requestRepository;
        this.reportRepository = reportRepository;
        this.clock = clock;
    }

    // ================= Danh sách và chi tiết (D92) =================

    /** GET /api/sessions?status=. status null = mọi trạng thái; status sai đã bị Spring trả 400. */
    @Transactional(readOnly = true)
    public List<SessionItem> list(long viewerId, SessionStatus status) {
        return sessionRepository.findRowsOf(viewerId, status).stream()
                .map(row -> SessionItem.from(row, viewerId))
                .toList();
    }

    /** GET /api/sessions/{id}: chỉ hai người tham gia. */
    @Transactional(readOnly = true)
    public SessionDetail get(long viewerId, long sessionId) {
        SessionRow row = sessionRepository.findRowById(sessionId)
                .orElseThrow(() -> new NotFoundException(SESSION_NOT_FOUND));
        if (!row.isParticipant(viewerId)) {
            throw new ForbiddenException(NOT_PARTICIPANT);
        }
        return toDetail(row, viewerId);
    }

    // ================= Chuyển trạng thái (D93, bảng 2.3) =================

    /** POST /api/sessions/{id}/start. Hai lỗi 409 xét theo thứ tự: trạng thái trước, mốc 15 phút sau. */
    @Transactional
    public SessionDetail start(long viewerId, long sessionId) {
        Instant now = clock.instant();
        ExchangeSession session = findForParticipant(sessionId, viewerId);
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new ConflictException(ONLY_SCHEDULED);
        }
        Instant opensAt = session.getStartAt().minus(EARLIEST_START_BEFORE);
        if (now.isBefore(opensAt)) {                                      // đúng mốc thì được
            throw new ConflictException(TOO_EARLY);
        }

        session.start(now);
        return flushAndRead(session, viewerId);
    }

    /**
     * POST /api/sessions/{id}/complete. Hai bước: người bấm trước chuyển sang chờ, bên còn lại xác nhận.
     * Người bấm trước bấm lại thì 409; một người không thể tự xác nhận cả hai bước.
     */
    @Transactional
    public SessionDetail complete(long viewerId, long sessionId) {
        ExchangeSession session = findForParticipant(sessionId, viewerId);
        switch (session.getStatus()) {
            case IN_PROGRESS -> session.markCompletedBy(viewerId);
            case WAITING_CONFIRMATION -> {
                if (session.getFirstCompletedBy() == viewerId) {
                    throw new ConflictException(ALREADY_COMPLETED);
                }
                // Có báo cáo OPEN thì chỉ admin chốt được phiên (R33), xét sau câu trên (D93). Báo cáo đang gửi mà
                // chưa commit thì câu này không thấy; khi đó khóa lạc quan chặn (report ép tăng version, D94).
                if (reportRepository.existsBySessionIdAndStatus(sessionId, ReportStatus.OPEN)) {
                    throw new ConflictException(BLOCKED_BY_REPORT);
                }
                session.confirmCompleted(clock.instant());
            }
            default -> throw new ConflictException(CANNOT_COMPLETE);    // SCHEDULED, COMPLETED, CANCELLED
        }
        return flushAndRead(session, viewerId);
    }

    /**
     * POST /api/sessions/{id}/cancel. reason đã cắt khoảng trắng và kiểm độ dài (ReasonRequest).
     * Hủy SCHEDULED được ở mọi thời điểm, kể cả sau giờ hẹn (R34).
     */
    @Transactional
    public SessionDetail cancel(long viewerId, long sessionId, String reason) {
        ExchangeSession session = findForParticipant(sessionId, viewerId);
        switch (session.getStatus()) {
            case SCHEDULED, IN_PROGRESS -> session.cancel(viewerId, reason, clock.instant());
            case WAITING_CONFIRMATION -> throw new ConflictException(CANNOT_CANCEL_WAITING);
            case COMPLETED, CANCELLED -> throw new ConflictException(ENDED);
        }
        return flushAndRead(session, viewerId);
    }

    // ================= Báo cáo sự cố (D94, R33) =================

    /**
     * POST /api/sessions/{id}/reports. reason đã cắt khoảng trắng và kiểm độ dài (ReasonRequest). Cả hai người tham
     * gia đều gửi được, kể cả người đã bấm hoàn thành. Hai lỗi 409 xét theo thứ tự của D94: trạng thái trước,
     * báo cáo OPEN sau.
     *
     * Phiên được đọc kèm ép tăng version: báo cáo thành công làm version của phiên tăng 1 lúc commit. Nhờ vậy báo
     * cáo và lệnh xác nhận hoàn thành chạy chồng nhau thì lệnh ghi sau nhận 409 khóa lạc quan, không bao giờ có
     * phiên COMPLETED mà còn báo cáo OPEN. Hai báo cáo chạy chồng thì cái sau đụng chỉ mục uq_reports_one_open,
     * lưới D24 trả 409.
     */
    @Transactional
    public ReportResponse report(long viewerId, long sessionId, String reason) {
        ExchangeSession session = sessionRepository.findByIdIncrementingVersion(sessionId)
                .orElseThrow(() -> new NotFoundException(SESSION_NOT_FOUND));
        requireParticipant(session, viewerId);
        if (session.getStatus() != SessionStatus.WAITING_CONFIRMATION) {
            throw new ConflictException(ONLY_WAITING);
        }
        if (reportRepository.existsBySessionIdAndStatus(sessionId, ReportStatus.OPEN)) {   // do ai gửi cũng vậy
            throw new ConflictException(ALREADY_OPEN);
        }

        // IDENTITY: save chạy INSERT ngay để có id. Lỗi unique (báo cáo khác vừa commit) nổ ra ở dòng này.
        IncidentReport report = reportRepository.save(
                IncidentReport.open(sessionId, viewerId, reason, clock.instant()));
        return ReportResponse.from(report);
    }

    // ================= Tiện ích =================

    /**
     * 404 nếu không có phiên, 403 nếu người gọi không tham gia phiên.
     * Trả Entity (không phải SessionRow) vì ba thao tác cần đổi field rồi để Hibernate ghi kèm kiểm version.
     */
    private ExchangeSession findForParticipant(long sessionId, long viewerId) {
        ExchangeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(SESSION_NOT_FOUND));
        requireParticipant(session, viewerId);
        return session;
    }

    /** 403 nếu người gọi không phải người gửi hay người nhận của yêu cầu gốc. */
    private void requireParticipant(ExchangeSession session, long viewerId) {
        // request_id là khóa ngoại NOT NULL nên yêu cầu gốc luôn có.
        ExchangeRequest request = requestRepository.findById(session.getRequestId()).orElseThrow();
        if (viewerId != request.getSenderId() && viewerId != request.getReceiverId()) {
            throw new ForbiddenException(NOT_PARTICIPANT);
        }
    }

    /**
     * Ghi thay đổi xuống DB rồi đọc lại SessionDetail, như flushAndRead của ExchangeRequestService.
     * flush là chỗ lỗi khóa lạc quan nổ ra (người kia vừa đổi phiên, version đã khác): GlobalExceptionHandler
     * trả 409 trước khi kịp dựng response. Câu đọc sau đó thấy trạng thái mới vì cùng transaction.
     */
    private SessionDetail flushAndRead(ExchangeSession session, long viewerId) {
        sessionRepository.flush();
        SessionRow row = sessionRepository.findRowById(session.getId()).orElseThrow();
        return toDetail(row, viewerId);
    }

    /** Ba giá trị của SessionDetail không nằm trong SessionRow: đọc thật từ DB (Chốt ở bước 7, TC355). */
    private SessionDetail toDetail(SessionRow row, long viewerId) {
        boolean myReviewed = sessionRepository.hasReviewBy(row.id(), viewerId);
        IncidentReport openReport = reportRepository
                .findFirstBySessionIdAndStatus(row.id(), ReportStatus.OPEN)
                .orElse(null);
        IncidentReport latestResolved = reportRepository
                .findFirstBySessionIdAndStatusOrderByResolvedAtDescIdDesc(row.id(), ReportStatus.RESOLVED)
                .orElse(null);
        return SessionDetail.from(row, viewerId, myReviewed, openReport, latestResolved);
    }
}
