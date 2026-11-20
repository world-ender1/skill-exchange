package com.skillexchange.exchange;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Truy cập exchange_sessions: tạo phiên khi chấp nhận yêu cầu (bước 6), danh sách và chi tiết (bước 7). */
public interface ExchangeSessionRepository extends JpaRepository<ExchangeSession, Long> {

    /**
     * Phần SELECT ... FROM ... JOIN dùng chung cho chi tiết và danh sách, như REQUEST_ROW_QUERY của bước 6:
     * phiên, yêu cầu gốc, hai người (kể cả liên hệ), hai kỹ năng trong MỘT câu SQL (không N+1).
     * Danh sách cũng đọc liên hệ nhưng SessionItem không có chỗ để trả ra; đổi lại chỉ có một dạng SessionRow.
     * Kỹ năng không lọc is_active: kỹ năng đã ẩn vẫn hiện tên (bảng 2.2). LEFT JOIN vì ONE_WAY không có
     * kỹ năng chiều ngược.
     */
    String SESSION_ROW_QUERY = """
            SELECT new com.skillexchange.exchange.SessionRow(
                s.id, r.id, r.type, s.status,
                sender.id, sender.fullName, sender.phone, sender.contactInfo,
                receiver.id, receiver.fullName, receiver.phone, receiver.contactInfo,
                senderSkill.id, senderSkill.name, receiverSkill.id, receiverSkill.name,
                s.startAt, s.durationMinutes, s.mode, s.locationOrLink,
                s.startedAt, s.firstCompletedBy, s.completedAt,
                s.cancelledBy, s.cancelReason, s.cancelledAt, s.createdAt)
            FROM ExchangeSession s
            JOIN ExchangeRequest r ON r.id = s.requestId
            JOIN User sender ON sender.id = r.senderId
            JOIN User receiver ON receiver.id = r.receiverId
            JOIN Skill senderSkill ON senderSkill.id = r.senderLearnSkillId
            LEFT JOIN Skill receiverSkill ON receiverSkill.id = r.receiverLearnSkillId
            """;

    @Query(SESSION_ROW_QUERY + "WHERE s.id = :id")
    Optional<SessionRow> findRowById(@Param("id") Long id);

    /**
     * Các phiên userId tham gia (gửi hoặc nhận yêu cầu gốc) (D92). status null nghĩa là mọi trạng thái.
     * Xếp created_at rồi id giảm dần, như hộp yêu cầu.
     */
    @Query(SESSION_ROW_QUERY + """
            WHERE (r.senderId = :userId OR r.receiverId = :userId)
              AND (:status IS NULL OR s.status = :status)
            ORDER BY s.createdAt DESC, s.id DESC
            """)
    List<SessionRow> findRowsOf(@Param("userId") Long userId, @Param("status") SessionStatus status);

    /**
     * myReviewed của SessionDetail (D92): reviewerId đã viết đánh giá cho phiên chưa.
     * Bảng reviews chưa có Entity (tuần 7) nên JPQL không gọi tên được; nativeQuery = true là SQL thật của
     * PostgreSQL, chạy nguyên văn. Tuần 7 có Entity Review thì chuyển sang ReviewRepository bằng JPQL.
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM reviews WHERE session_id = :sessionId AND reviewer_id = :reviewerId)
            """, nativeQuery = true)
    boolean hasReviewBy(@Param("sessionId") Long sessionId, @Param("reviewerId") Long reviewerId);
}
