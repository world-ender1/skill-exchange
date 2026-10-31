package com.skillexchange.skill;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    /**
     * Kỹ năng đang hoạt động, lọc tùy chọn theo nhóm, tìm theo tên (D40).
     * - categoryId = null: không lọc theo nhóm.
     * - namePattern: mẫu LIKE do Service tạo sẵn ("%" khi không lọc theo tên).
     * JOIN FETCH lấy luôn nhóm trong cùng một câu SQL, tránh mỗi kỹ năng một câu (N+1).
     */
    @Query("""
            SELECT s FROM Skill s
            JOIN FETCH s.category c
            WHERE s.active = true
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND LOWER(s.name) LIKE LOWER(:namePattern) ESCAPE '\\'
            ORDER BY s.name, s.id
            """)
    List<Skill> findActive(@Param("categoryId") Long categoryId,
                           @Param("namePattern") String namePattern);
}
