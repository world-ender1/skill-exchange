package com.skillexchange.exchange;

/**
 * Ai hủy phiên, nhìn từ phía người xem (khóa cancelledBy của SessionDetail, D92). DB chỉ lưu id người hủy
 * (cancelled_by); đổi sang ME/PARTNER/ADMIN để giao diện không phải tự so id.
 */
public enum CancelledBy {
    /** Chính người đang xem. */
    ME,
    /** Bên kia của phiên. */
    PARTNER,
    /** Không phải hai người tham gia: admin chốt báo cáo theo hướng hủy phiên (tuần 7, A02). */
    ADMIN
}
