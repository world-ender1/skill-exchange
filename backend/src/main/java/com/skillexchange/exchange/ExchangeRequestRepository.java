package com.skillexchange.exchange;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Truy cập exchange_requests. Danh sách hộp yêu cầu thêm ở bước 6. */
public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, Long> {

    /**
     * Bước (8) của D89: đã có yêu cầu đang chờ cùng chiều chưa. Spring Data tự viết câu SQL từ tên hàm
     * (exists + By + điều kiện nối bằng And). Chiều ngược (receiver gửi lại sender) là cặp khác nên không tính.
     */
    boolean existsBySenderIdAndReceiverIdAndStatus(Long senderId, Long receiverId, RequestStatus status);

    /**
     * Một yêu cầu kèm tên người và tên kỹ năng trong MỘT câu SQL (skill jpa-patterns: không N+1).
     * Entity giữ khóa ngoại dạng Long nên JOIN theo điều kiện ON, không theo quan hệ.
     * Kỹ năng không lọc is_active: kỹ năng đã ẩn vẫn hiện tên (bảng 2.2, D75).
     * LEFT JOIN: ONE_WAY không có kỹ năng chiều ngược; yêu cầu chưa ACCEPTED không có phiên.
     */
    @Query("""
            SELECT new com.skillexchange.exchange.RequestRow(
                r.id, r.type, r.status,
                sender.id, sender.fullName, receiver.id, receiver.fullName,
                senderSkill.id, senderSkill.name, receiverSkill.id, receiverSkill.name,
                r.proposedStartAt, r.durationMinutes, r.mode, r.locationOrLink,
                r.message, r.responseNote, r.createdAt, r.respondedAt, exchangeSession.id)
            FROM ExchangeRequest r
            JOIN User sender ON sender.id = r.senderId
            JOIN User receiver ON receiver.id = r.receiverId
            JOIN Skill senderSkill ON senderSkill.id = r.senderLearnSkillId
            LEFT JOIN Skill receiverSkill ON receiverSkill.id = r.receiverLearnSkillId
            LEFT JOIN ExchangeSession exchangeSession ON exchangeSession.requestId = r.id
            WHERE r.id = :id
            """)
    Optional<RequestRow> findRowById(@Param("id") Long id);
}
