package com.skillexchange.exchange;

import java.time.Instant;

/**
 * Một yêu cầu như người xem thấy (RequestItem, bảng 2.2; D89–D91). Mọi khóa luôn có mặt, không có giá trị thì null.
 *
 * Cố ý không có phone, contactInfo, email của ai cả: API yêu cầu không bao giờ trả liên hệ, liên hệ chỉ có ở
 * chi tiết phiên (D91, D92, NFR-02).
 */
public record RequestItem(
        long id,
        RequestType type,
        RequestStatus status,
        RequestRole myRole,
        Person sender,
        Person receiver,
        SkillRef senderLearnSkill,
        SkillRef receiverLearnSkill,
        Instant proposedStartAt,
        int durationMinutes,
        MeetingMode mode,
        String locationOrLink,
        String message,
        String responseNote,
        Instant createdAt,
        Instant respondedAt,
        Long sessionId) {

    /** Chỉ id và họ tên. */
    public record Person(long id, String fullName) {
    }

    /** Kỹ năng kèm tên, kể cả kỹ năng đã ẩn (bảng 2.2). */
    public record SkillRef(long id, String name) {
    }

    /**
     * Dựng response cho viewerId. viewerId phải là người gửi hoặc người nhận: service kiểm (403) trước khi gọi.
     * Đây là chỗ DUY NHẤT quyết định ai thấy địa điểm/link, để mọi API yêu cầu (tạo, danh sách, chi tiết,
     * phản hồi) theo cùng một luật.
     */
    static RequestItem from(RequestRow row, long viewerId) {
        RequestRole myRole = row.senderId() == viewerId ? RequestRole.SENDER : RequestRole.RECEIVER;
        SkillRef receiverLearnSkill = row.receiverLearnSkillId() == null
                ? null
                : new SkillRef(row.receiverLearnSkillId(), row.receiverLearnSkillName());
        String locationOrLink = canSeeLocation(myRole, row.status()) ? row.locationOrLink() : null;

        return new RequestItem(row.id(), row.type(), row.status(), myRole,
                new Person(row.senderId(), row.senderName()),
                new Person(row.receiverId(), row.receiverName()),
                new SkillRef(row.senderLearnSkillId(), row.senderLearnSkillName()),
                receiverLearnSkill,
                row.proposedStartAt(), row.durationMinutes(), row.mode(), locationOrLink,
                row.message(), row.responseNote(), row.createdAt(), row.respondedAt(), row.sessionId());
    }

    /**
     * Q1, D91 (giữ NFR-02): người gửi luôn thấy địa điểm/link do chính mình nhập, như /api/me trả số điện thoại
     * của chính mình; người nhận chỉ thấy từ khi đã chấp nhận.
     */
    private static boolean canSeeLocation(RequestRole myRole, RequestStatus status) {
        return myRole == RequestRole.SENDER || status == RequestStatus.ACCEPTED;
    }
}
