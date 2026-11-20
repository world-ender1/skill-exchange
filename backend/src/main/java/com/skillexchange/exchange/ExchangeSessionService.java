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
                // Bước 8 thêm ở đây: phiên có báo cáo OPEN thì 409 (R33), xét sau câu trên (D93).
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

    // ================= Tiện ích =================

    /**
     * 404 nếu không có phiên, 403 nếu người gọi không phải người gửi hay người nhận của yêu cầu gốc.
     * Trả Entity (không phải SessionRow) vì ba thao tác cần đổi field rồi để Hibernate ghi kèm kiểm version.
     */
    private ExchangeSession findForParticipant(long sessionId, long viewerId) {
        ExchangeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(SESSION_NOT_FOUND));
        // request_id là khóa ngoại NOT NULL nên yêu cầu gốc luôn có.
        ExchangeRequest request = requestRepository.findById(session.getRequestId()).orElseThrow();
        if (viewerId != request.getSenderId() && viewerId != request.getReceiverId()) {
            throw new ForbiddenException(NOT_PARTICIPANT);
        }
        return session;
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
