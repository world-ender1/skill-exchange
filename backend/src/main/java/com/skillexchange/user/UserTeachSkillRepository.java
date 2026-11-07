package com.skillexchange.user;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserTeachSkillRepository extends JpaRepository<UserTeachSkill, UserTeachSkill.Key> {

    @Query("SELECT t.skillId FROM UserTeachSkill t WHERE t.userId = :userId")
    List<Long> findSkillIdsByUserId(@Param("userId") Long userId);

    /**
     * Danh sách dạy kèm tên kỹ năng và nhóm (D28), xếp theo tên rồi id (D41).
     * Một câu SQL JOIN và tạo thẳng DTO ("SELECT new ..."): không có N+1, không cần quan hệ LAZY.
     * Không lọc is_active: kỹ năng đã bị ẩn vẫn hiện, kèm isActive = false (D20, TC79).
     */
    @Query("""
            SELECT new com.skillexchange.user.TeachSkillResponse(s.id, s.name, c.id, c.name, s.active, t.level)
            FROM UserTeachSkill t
            JOIN Skill s ON s.id = t.skillId
            JOIN s.category c
            WHERE t.userId = :userId
            ORDER BY s.name, s.id
            """)
    List<TeachSkillResponse> findResponsesByUserId(@Param("userId") Long userId);

    /**
     * Kỹ năng dạy của NHIỀU người trong một câu (hồ sơ công khai, kết quả tìm kiếm, TK 4.3).
     * Kết quả tìm kiếm có tới 20 người: gọi findResponsesByUserId cho từng người là 20 câu (N+1).
     * Xếp theo người rồi tên kỹ năng, skillId (D41, D46c); không lọc is_active.
     */
    @Query("""
            SELECT new com.skillexchange.user.TeachSkillRow(t.userId, s.id, s.name, c.name, t.level)
            FROM UserTeachSkill t
            JOIN Skill s ON s.id = t.skillId
            JOIN s.category c
            WHERE t.userId IN :userIds
            ORDER BY t.userId, s.name, s.id
            """)
    List<TeachSkillRow> findRowsByUserIds(@Param("userIds") Collection<Long> userIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM UserTeachSkill t WHERE t.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
