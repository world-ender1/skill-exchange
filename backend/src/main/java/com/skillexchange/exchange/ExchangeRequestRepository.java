package com.skillexchange.exchange;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Truy cập exchange_requests: tạo (bước 5), hộp yêu cầu và chi tiết (bước 6). */
public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, Long> {

    /**
     * Bước (8) của D89: đã có yêu cầu đang chờ cùng chiều chưa. Spring Data tự viết câu SQL từ tên hàm
     * (exists + By + điều kiện nối bằng And). Chiều ngược (receiver gửi lại sender) là cặp khác nên không tính.
     */
    boolean existsBySenderIdAndReceiverIdAndStatus(Long senderId, Long receiverId, RequestStatus status);

    /**
     * Phần SELECT ... FROM ... JOIN dùng chung cho chi tiết và danh sách, để mọi API yêu cầu đọc cùng một dạng
     * RequestRow. Tên người và tên kỹ năng lấy trong MỘT câu SQL (skill jpa-patterns: không N+1).
     * Entity giữ khóa ngoại dạng Long nên JOIN theo điều kiện ON, không theo quan hệ.
     * Kỹ năng không lọc is_active: kỹ năng đã ẩn vẫn hiện tên (bảng 2.2, D75).
     * LEFT JOIN: ONE_WAY không có kỹ năng chiều ngược; yêu cầu chưa ACCEPTED không có phiên.
     *
     * Field của interface luôn là public static final. Ghép hai hằng chuỗi vẫn là hằng lúc biên dịch,
     * nên dùng được trong @Query (annotation chỉ nhận hằng).
     */
    String REQUEST_ROW_QUERY = """
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
            """;

    @Query(REQUEST_ROW_QUERY + "WHERE r.id = :id")
    Optional<RequestRow> findRowById(@Param("id") Long id);

    /**
     * Hộp "Đã gửi" (D91): status null nghĩa là mọi trạng thái (cùng cách viết với SkillRepository.findActive).
     * Xếp created_at rồi id giảm dần: hai yêu cầu tạo cùng thời điểm vẫn có thứ tự cố định.
     */
    @Query(REQUEST_ROW_QUERY + """
            WHERE r.senderId = :userId AND (:status IS NULL OR r.status = :status)
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<RequestRow> findSentRows(@Param("userId") Long userId, @Param("status") RequestStatus status);

    /** Hộp "Đã nhận" (D91), như findSentRows nhưng theo người nhận. */
    @Query(REQUEST_ROW_QUERY + """
            WHERE r.receiverId = :userId AND (:status IS NULL OR r.status = :status)
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<RequestRow> findReceivedRows(@Param("userId") Long userId, @Param("status") RequestStatus status);
}
