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
 * Yêu cầu trao đổi – bảng exchange_requests (TK 3.3, FR-05).
 *
 * Khóa ngoại (người gửi, người nhận, kỹ năng) giữ dạng Long như UserTeachSkill, không map @ManyToOne:
 * danh sách yêu cầu cần tên người và tên kỹ năng, Repository JOIN rồi trả DTO trong một câu SQL (bước 6),
 * nên không có quan hệ LAZY nào để lỡ đọc ngoài transaction.
 *
 * Entity không tự kiểm luật nghiệp vụ (loại hai chiều phải có kỹ năng chiều ngược, giờ ở tương lai...):
 * service kiểm theo thứ tự của D89; CHECK của DB là lưới cuối.
 */
@Entity
@Table(name = "exchange_requests")
public class ExchangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RequestType type;

    @Column(name = "sender_learn_skill_id", nullable = false)
    private Long senderLearnSkillId;

    /** Chỉ có ở TWO_WAY (CHECK của TK 3.3). */
    @Column(name = "receiver_learn_skill_id")
    private Long receiverLearnSkillId;

    @Column(name = "proposed_start_at", nullable = false)
    private Instant proposedStartAt;

    // SMALLINT CHECK 15–480 → Short (D30)
    @Column(name = "duration_minutes", nullable = false)
    private Short durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MeetingMode mode;

    @Column(name = "location_or_link", length = 500)
    private String locationOrLink;

    @Column(columnDefinition = "text")
    private String message;

    // DEFAULT 'PENDING' trong DB: Hibernate luôn ghi đủ mọi cột khi INSERT nên phải gán ở đây (D30).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "response_note", columnDefinition = "text")
    private String responseNote;

    // Lấy từ Clock khi tạo (D88), không dùng DEFAULT now() của DB hay @CreationTimestamp.
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** NULL khi PENDING, có giá trị ở mọi trạng thái khác (D90). */
    @Column(name = "responded_at")
    private Instant respondedAt;

    /**
     * Khóa lạc quan (TK 5.1): chống người gửi hủy đúng lúc người nhận chấp nhận.
     * Cố ý KHÔNG gán "= 0" dù DB có DEFAULT 0 (ngoại lệ của quy ước D30): với version kiểu Integer, Spring Data
     * coi entity là mới khi version == null. Gán 0 thì save() gọi merge thay vì persist, chèn một bản sao,
     * và object đang cầm không nhận được id. Hibernate tự ghi version = 0 khi INSERT.
     */
    @Version
    private Integer version;

    protected ExchangeRequest() {
        // JPA cần constructor không tham số để dựng object khi đọc từ DB.
    }

    /** Yêu cầu mới ở trạng thái PENDING. createdAt truyền vào từ Clock của service (D88). */
    public static ExchangeRequest create(Long senderId, Long receiverId, RequestType type,
                                         Long senderLearnSkillId, Long receiverLearnSkillId,
                                         Instant proposedStartAt, Short durationMinutes, MeetingMode mode,
                                         String locationOrLink, String message, Instant createdAt) {
        ExchangeRequest request = new ExchangeRequest();
        request.senderId = senderId;
        request.receiverId = receiverId;
        request.type = type;
        request.senderLearnSkillId = senderLearnSkillId;
        request.receiverLearnSkillId = receiverLearnSkillId;
        request.proposedStartAt = proposedStartAt;
        request.durationMinutes = durationMinutes;
        request.mode = mode;
        request.locationOrLink = locationOrLink;
        request.message = message;
        request.createdAt = createdAt;
        return request;
    }

    public Long getId() {
        return id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public RequestType getType() {
        return type;
    }

    public Long getSenderLearnSkillId() {
        return senderLearnSkillId;
    }

    public Long getReceiverLearnSkillId() {
        return receiverLearnSkillId;
    }

    public Instant getProposedStartAt() {
        return proposedStartAt;
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

    public String getMessage() {
        return message;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public String getResponseNote() {
        return responseNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public Integer getVersion() {
        return version;
    }
}
