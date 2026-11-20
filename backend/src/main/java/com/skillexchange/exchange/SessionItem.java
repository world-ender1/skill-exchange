package com.skillexchange.exchange;

import java.time.Instant;

import com.skillexchange.exchange.RequestItem.Person;
import com.skillexchange.exchange.RequestItem.SkillRef;

/**
 * Một phiên trong danh sách GET /api/sessions, như người xem thấy (SessionItem, bảng 2.2; D92). 12 khóa, luôn có
 * mặt, không có giá trị thì null.
 *
 * Cố ý không có địa điểm/link và liên hệ (phone, contactInfo): danh sách chỉ để chọn phiên; những thứ đó chỉ có
 * ở chi tiết phiên, cho hai người tham gia (D92, NFR-02). Không có field thì không thể lỡ trả ra.
 */
public record SessionItem(
        long id,
        long requestId,
        RequestType type,
        SessionStatus status,
        RequestRole myRole,
        Person partner,
        SkillRef senderLearnSkill,
        SkillRef receiverLearnSkill,
        Instant startAt,
        int durationMinutes,
        MeetingMode mode,
        Instant createdAt) {

    /** Dựng cho viewerId. viewerId phải là người tham gia: service đã lọc (danh sách) hoặc kiểm 403 (chi tiết). */
    static SessionItem from(SessionRow row, long viewerId) {
        RequestRole myRole = row.roleOf(viewerId);
        Person partner = myRole == RequestRole.SENDER
                ? new Person(row.receiverId(), row.receiverName())
                : new Person(row.senderId(), row.senderName());
        SkillRef receiverLearnSkill = row.receiverLearnSkillId() == null
                ? null
                : new SkillRef(row.receiverLearnSkillId(), row.receiverLearnSkillName());

        return new SessionItem(row.id(), row.requestId(), row.type(), row.status(), myRole, partner,
                new SkillRef(row.senderLearnSkillId(), row.senderLearnSkillName()),
                receiverLearnSkill,
                row.startAt(), row.durationMinutes(), row.mode(), row.createdAt());
    }
}
