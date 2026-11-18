package com.skillexchange.exchange;

import java.time.Instant;

/**
 * Một yêu cầu kèm tên hai người, tên kỹ năng và mã phiên, đọc bằng MỘT câu JPQL
 * (ExchangeRequestRepository.findRowById). Dữ liệu thô, chưa theo người xem: RequestItem.from mới quyết
 * người xem thấy gì (D91).
 *
 * public và dùng kiểu bọc (Long, Short) như TeachSkillRow: Hibernate gọi constructor này từ "SELECT new ...",
 * và LEFT JOIN có thể đưa null vào (receiverLearnSkill khi ONE_WAY, sessionId khi chưa ACCEPTED).
 */
public record RequestRow(
        Long id,
        RequestType type,
        RequestStatus status,
        Long senderId,
        String senderName,
        Long receiverId,
        String receiverName,
        Long senderLearnSkillId,
        String senderLearnSkillName,
        Long receiverLearnSkillId,
        String receiverLearnSkillName,
        Instant proposedStartAt,
        Short durationMinutes,
        MeetingMode mode,
        String locationOrLink,
        String message,
        String responseNote,
        Instant createdAt,
        Instant respondedAt,
        Long sessionId) {
}
