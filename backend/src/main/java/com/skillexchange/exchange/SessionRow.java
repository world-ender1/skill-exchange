package com.skillexchange.exchange;

import java.time.Instant;

/**
 * Một phiên kèm loại yêu cầu, hai người (cả liên hệ) và tên kỹ năng, đọc bằng MỘT câu JPQL
 * (ExchangeSessionRepository.findRowById). Dữ liệu thô, chưa theo người xem: SessionItem.from và
 * SessionDetail.from mới quyết người xem thấy gì (D92), như RequestRow của bước 6.
 *
 * public và dùng kiểu bọc như RequestRow: Hibernate gọi constructor này từ "SELECT new ...", và LEFT JOIN hay
 * cột chưa có giá trị đưa null vào (receiverLearnSkill khi ONE_WAY, startedAt khi chưa bắt đầu...).
 */
public record SessionRow(
        Long id,
        Long requestId,
        RequestType type,
        SessionStatus status,
        Long senderId,
        String senderName,
        String senderPhone,
        String senderContactInfo,
        Long receiverId,
        String receiverName,
        String receiverPhone,
        String receiverContactInfo,
        Long senderLearnSkillId,
        String senderLearnSkillName,
        Long receiverLearnSkillId,
        String receiverLearnSkillName,
        Instant startAt,
        Short durationMinutes,
        MeetingMode mode,
        String locationOrLink,
        Instant startedAt,
        Long firstCompletedBy,
        Instant completedAt,
        Long cancelledBy,
        String cancelReason,
        Instant cancelledAt,
        Instant createdAt) {

    /** Người tham gia phiên = người gửi và người nhận của yêu cầu gốc (Chốt ở bước 7). */
    boolean isParticipant(long userId) {
        return userId == senderId || userId == receiverId;
    }

    /** Vai trò của người xem trong yêu cầu gốc. viewerId phải là người tham gia. */
    RequestRole roleOf(long viewerId) {
        return viewerId == senderId ? RequestRole.SENDER : RequestRole.RECEIVER;
    }
}
