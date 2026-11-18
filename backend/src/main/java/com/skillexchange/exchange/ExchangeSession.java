package com.skillexchange.exchange;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Phiên trao đổi – bảng exchange_sessions (TK 3.3, FR-06). Một yêu cầu được chấp nhận sinh đúng một phiên
 * (request_id UNIQUE trong DB). Hai người tham gia là người gửi và người nhận của yêu cầu gốc.
 *
 * Khóa ngoại giữ dạng Long như ExchangeRequest. Chuyển trạng thái (bắt đầu, hoàn thành, hủy) thêm ở bước 7;
 * CHECK của DB bảo đảm mỗi trạng thái có đủ cột (ví dụ CANCELLED phải có cancel_reason, cancelled_at).
 */
@Entity
@Table(name = "exchange_sessions")
public class ExchangeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, updatable = false)
    private Long requestId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    // SMALLINT CHECK 15–480 → Short (D30)
    @Column(name = "duration_minutes", nullable = false)
    private Short durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MeetingMode mode;

    @Column(name = "location_or_link", length = 500)
    private String locationOrLink;

    // DEFAULT 'SCHEDULED' trong DB → gán ở field (D30).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private SessionStatus status = SessionStatus.SCHEDULED;

    /** Khác NULL nghĩa là phiên đã bắt đầu: phiên hủy có started_at được tính vào CompletionRate (TK 3.3). */
    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "first_completed_by")
    private Long firstCompletedBy;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "cancel_reason", columnDefinition = "text")
    private String cancelReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /** Khóa lạc quan: hai người cùng bấm "Hoàn thành" (TK 5.2). Không gán "= 0", lý do như ExchangeRequest.version. */
    @Version
    private Integer version;

    // Thời điểm tạo phiên = lúc chấp nhận yêu cầu, lấy từ Clock (D88).
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExchangeSession() {
        // JPA cần constructor không tham số để dựng object khi đọc từ DB.
    }

    /**
     * Phiên SCHEDULED sinh ra khi người nhận chấp nhận yêu cầu (D90): giờ, thời lượng, hình thức, địa điểm/link
     * chép nguyên từ yêu cầu. Yêu cầu phải đã được lưu (có id).
     */
    public static ExchangeSession scheduleFrom(ExchangeRequest request, Instant createdAt) {
        ExchangeSession session = new ExchangeSession();
        session.requestId = request.getId();
        session.startAt = request.getProposedStartAt();
        session.durationMinutes = request.getDurationMinutes();
        session.mode = request.getMode();
        session.locationOrLink = request.getLocationOrLink();
        session.createdAt = createdAt;
        return session;
    }

    public Long getId() {
        return id;
    }

    public Long getRequestId() {
        return requestId;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Short getDurationMinutes() {
        return durationMinutes;
    }

    public MeetingMode getMode() {
        return mode;
    }

    public String getLocationOrLink() {
        return locationOrLink;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Long getFirstCompletedBy() {
        return firstCompletedBy;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Long getCancelledBy() {
        return cancelledBy;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Integer getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
