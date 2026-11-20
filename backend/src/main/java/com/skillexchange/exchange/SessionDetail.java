package com.skillexchange.exchange;

import java.time.Instant;

import com.skillexchange.exchange.RequestItem.SkillRef;

/**
 * Chi tiết phiên GET /api/sessions/{id}, cũng là response của start, complete, cancel (bảng 2.2; D92, D93).
 * = 12 khóa của SessionItem (partner có thêm phone, contactInfo) + 10 khóa riêng; tổng 22 khóa, luôn có mặt.
 *
 * Viết phẳng thay vì lồng SessionItem: JSON phải phẳng như bảng 2.2, và đọc record là thấy đủ 22 khóa.
 */
public record SessionDetail(
        // ----- 12 khóa như SessionItem -----
        long id,
        long requestId,
        RequestType type,
        SessionStatus status,
        RequestRole myRole,
        Partner partner,
        SkillRef senderLearnSkill,
        SkillRef receiverLearnSkill,
        Instant startAt,
        int durationMinutes,
        MeetingMode mode,
        Instant createdAt,
        // ----- 10 khóa riêng của chi tiết -----
        String locationOrLink,
        Instant startedAt,
        Instant completedAt,
        boolean myCompleted,
        CancelledBy cancelledBy,
        String cancelReason,
        Instant cancelledAt,
        boolean myReviewed,
        OpenReport openReport,
        Resolution resolution) {

    /** Bên kia của phiên, kèm liên hệ HIỆN TẠI trong hồ sơ (null nếu chưa khai), ở mọi trạng thái (D92). */
    public record Partner(long id, String fullName, String phone, String contactInfo) {
    }

    /** Báo cáo OPEN của phiên. Không có lý do: lý do chỉ dành cho admin (A02, D92). */
    public record OpenReport(long id, boolean reportedByMe, Instant createdAt) {
    }

    /** Kết quả admin chốt của báo cáo RESOLVED mới nhất (D92; tuần 7 mới có dữ liệu thật). */
    public record Resolution(ReportResolution result, String adminNote, Instant resolvedAt) {
    }

    /**
     * Dựng cho viewerId (đã kiểm là người tham gia). Ba giá trị đọc riêng từ DB do service truyền vào:
     * myReviewed, báo cáo OPEN (có thể null), báo cáo RESOLVED mới nhất (có thể null).
     */
    static SessionDetail from(SessionRow row, long viewerId, boolean myReviewed,
                              IncidentReport openReport, IncidentReport latestResolved) {
        // Phần chung với danh sách tính một chỗ, để hai API không lệch nhau (myRole, kỹ năng...).
        SessionItem item = SessionItem.from(row, viewerId);
        Partner partner = item.myRole() == RequestRole.SENDER
                ? new Partner(row.receiverId(), row.receiverName(), row.receiverPhone(), row.receiverContactInfo())
                : new Partner(row.senderId(), row.senderName(), row.senderPhone(), row.senderContactInfo());

        // D92: myCompleted = "mình là first_completed_by", nên vẫn true sau khi phiên COMPLETED.
        boolean myCompleted = row.firstCompletedBy() != null && row.firstCompletedBy() == viewerId;

        return new SessionDetail(item.id(), item.requestId(), item.type(), item.status(), item.myRole(), partner,
                item.senderLearnSkill(), item.receiverLearnSkill(),
                item.startAt(), item.durationMinutes(), item.mode(), item.createdAt(),
                row.locationOrLink(), row.startedAt(), row.completedAt(), myCompleted,
                cancelledBy(row.cancelledBy(), viewerId, partner.id()), row.cancelReason(), row.cancelledAt(),
                myReviewed,
                openReport == null ? null : new OpenReport(
                        openReport.getId(), openReport.getReporterId() == viewerId, openReport.getCreatedAt()),
                latestResolved == null ? null : new Resolution(
                        latestResolved.getResolution(), latestResolved.getAdminNote(), latestResolved.getResolvedAt()));
    }

    /** cancelled_by → ME, PARTNER, ADMIN theo người xem (Chốt ở bước 7); chưa hủy thì null. */
    private static CancelledBy cancelledBy(Long cancelledById, long viewerId, long partnerId) {
        if (cancelledById == null) {
            return null;
        }
        if (cancelledById == viewerId) {
            return CancelledBy.ME;
        }
        if (cancelledById == partnerId) {
            return CancelledBy.PARTNER;
        }
        return CancelledBy.ADMIN;
    }
}
